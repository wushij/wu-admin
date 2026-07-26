package com.admin.server.framework.security.core.filter;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.symmetric.SM4;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.core.CommonResult;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 全链路 API 安全防护过滤器
 * 支持：时间戳校验(5分钟窗口)、Nonce随机数防重放(Redis查重)、SM2数字签名与SM4接口加解密
 */
@Component
public class ApiSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiSecurityFilter.class);

    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String HEADER_ENCRYPTED = "X-Encrypted";

    private static final long MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L; // 5 分钟窗口

    private static final Set<String> EXCLUDE_PATH_PREFIXES = Set.of(
            "/doc.html",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars",
            "/favicon.ico"
    );

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : EXCLUDE_PATH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // 0. 如果是文件上传请求 (multipart/form-data)，直接放行，不做安全校验与 Body 缓存
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 读取系统安全开关
        boolean timestampEnabled = systemConfigHelper.isTimestampEnabled();
        boolean nonceEnabled = systemConfigHelper.isNonceEnabled();
        boolean sm2SignEnabled = systemConfigHelper.isSm2SignEnabled();
        boolean sm4EncryptEnabled = systemConfigHelper.isSm4EncryptEnabled();

        // 2. 时间戳校验
        String timestampStr = request.getHeader(HEADER_TIMESTAMP);
        if (timestampEnabled && StrUtil.isNotBlank(timestampStr)) {
            try {
                long timestamp = Long.parseLong(timestampStr);
                long diff = Math.abs(System.currentTimeMillis() - timestamp);
                if (diff > MAX_TIMESTAMP_DIFF_MS) {
                    writeError(response, 403, "请求已被拒绝：时间戳无效或已过期");
                    return;
                }
            } catch (NumberFormatException e) {
                writeError(response, 403, "请求已被拒绝：非法时间戳格式");
                return;
            }
        }

        // 3. Nonce 防重放校验
        String nonce = request.getHeader(HEADER_NONCE);
        if (nonceEnabled && StrUtil.isNotBlank(nonce)) {
            String redisKey = "security:nonce:" + nonce;
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", 5, TimeUnit.MINUTES);
            if (Boolean.FALSE.equals(success)) {
                writeError(response, 403, "请求已被拒绝：检测到重复请求 (Anti-Replay)");
                return;
            }
        }

        // 读取 Body 内容供解密与签名比对
        byte[] bodyBytes = request.getInputStream().readAllBytes();
        String bodyString = new String(bodyBytes, StandardCharsets.UTF_8);

        // 4. SM4 解密处理 (若请求体加密)
        String isEncryptedHeader = request.getHeader(HEADER_ENCRYPTED);
        boolean isRequestEncrypted = "1".equals(isEncryptedHeader) || "true".equalsIgnoreCase(isEncryptedHeader);
        if (isRequestEncrypted && StrUtil.isNotBlank(bodyString)) {
            String sm4Key = systemConfigHelper.getSm4SecretKey();
            if (StrUtil.isBlank(sm4Key)) {
                log.error("SM4 加密已开启但未配置密钥，无法解密请求体");
                writeError(response, 403, "请求已被拒绝：服务端加密密钥未配置");
                return;
            }
            try {
                String cipherText = bodyString.trim();
                // 兼容某些前端框架/Axios 自动将 String 序列化为带双引号的 JSON 字符串
                if (cipherText.startsWith("\"") && cipherText.endsWith("\"") && cipherText.length() > 2) {
                    cipherText = cipherText.substring(1, cipherText.length() - 1);
                }
                byte[] keyBytes = sm4Key.getBytes(StandardCharsets.UTF_8);
                SM4 sm4 = SmUtil.sm4(keyBytes);
                bodyString = sm4.decryptStr(cipherText);
                bodyBytes = bodyString.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("SM4 请求体解密失败: {}", e.getMessage());
                writeError(response, 403, "请求已被拒绝：SM4 请求解密失败");
                return;
            }
        }

        // 5. SM2 数字签名校验
        String signature = request.getHeader(HEADER_SIGNATURE);
        if (sm2SignEnabled && StrUtil.isNotBlank(signature)) {
            String publicKeyHex = systemConfigHelper.getSm2PublicKey();
            if (StrUtil.isNotBlank(publicKeyHex)) {
                try {
                    String signContent = request.getMethod() + "\n"
                            + request.getRequestURI() + "\n"
                            + StrUtil.nullToEmpty(timestampStr) + "\n"
                            + StrUtil.nullToEmpty(nonce) + "\n"
                            + bodyString;

                    SM2 sm2 = SmUtil.sm2(null, publicKeyHex);
                    boolean valid = sm2.verify(signContent.getBytes(StandardCharsets.UTF_8), HexUtil.decodeHex(signature));
                    if (!valid) {
                        writeError(response, 403, "请求已被拒绝：SM2 数字签名验证失败");
                        return;
                    }
                } catch (Exception e) {
                    log.error("SM2 签名验证异常: {}", e.getMessage());
                    writeError(response, 403, "请求已被拒绝：数字签名校验异常");
                    return;
                }
            }
        }

        // 6. 重构 HttpServletRequest 传入 Filter 链
        byte[] finalBodyBytes = bodyBytes;
        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() {
                ByteArrayInputStream bais = new ByteArrayInputStream(finalBodyBytes);
                return new ServletInputStream() {
                    @Override
                    public boolean isFinished() { return bais.available() == 0; }
                    @Override
                    public boolean isReady() { return true; }
                    @Override
                    public void setReadListener(ReadListener readListener) {}
                    @Override
                    public int read() { return bais.read(); }
                };
            }
        };

        // 7. 响应处理 (若开启 SM4 加密且客户端请求标记了加密)
        if (sm4EncryptEnabled && isRequestEncrypted) {
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            byte[] respContent = wrappedResponse.getContentAsByteArray();
            String sm4Key = systemConfigHelper.getSm4SecretKey();
            if (respContent.length > 0 && StrUtil.isNotBlank(sm4Key)) {
                try {
                    String plainResp = new String(respContent, StandardCharsets.UTF_8);
                    byte[] keyBytes = sm4Key.getBytes(StandardCharsets.UTF_8);
                    SM4 sm4 = SmUtil.sm4(keyBytes);
                    String encryptedResp = sm4.encryptBase64(plainResp);

                    response.setHeader(HEADER_ENCRYPTED, "1");
                    response.setContentType("application/json;charset=UTF-8");
                    response.getOutputStream().write(encryptedResp.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    log.error("SM4 响应加密失败: {}", e.getMessage());
                    wrappedResponse.copyBodyToResponse();
                }
            } else {
                wrappedResponse.copyBodyToResponse();
            }
        } else {
            filterChain.doFilter(wrappedRequest, response);
        }
    }

    private void writeError(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        CommonResult<Void> result = CommonResult.error(status, msg);
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }
}
