package com.admin.server.framework.security.core.filter;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.digest.HMac;
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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 全链路 API 安全防护过滤器
 * 支持：时间戳校验(5分钟窗口)、Nonce随机数防重放(Redis查重)、HMAC-SM3数字签名与SM4-CBC接口加解密
 */
@Component
public class ApiSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiSecurityFilter.class);

    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String HEADER_ENCRYPTED = "X-Encrypted";
    public static final String HEADER_ACCEPT_ENCRYPTED = "X-Accept-Encrypted";
    /** 客户端内存内随机 ID，用于从 Redis 查找临时会话密钥 */
    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    /** 会话签名临时密钥 Redis 前缀（与 AuthController 中定义保持一致） */
    private static final String SESSION_SIGN_KEY_PREFIX = "security:session-sign:";

    private static final long MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L; // 5 分钟窗口

    private static final Set<String> EXCLUDE_PATH_PREFIXES = Set.of(
            "/doc.html",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars",
            "/favicon.ico",
            "/files",
            "/ws",
            "/pay/notify"
    );

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final Set<String> PUBLIC_AUTH_URIS = Set.of(
            "/auth/config",
            "/auth/login",
            "/auth/register",
            "/auth/captcha",
            "/auth/sms-code",
            "/auth/slider-challenge",
            "/auth/session-sign-init"
    );

    private boolean isPublicAuthUri(String uri) {
        if (uri == null) return false;
        for (String publicUri : PUBLIC_AUTH_URIS) {
            if (uri.equals(publicUri) || uri.endsWith(publicUri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : EXCLUDE_PATH_PREFIXES) {
            if (uri.startsWith(prefix) || uri.startsWith("/api" + prefix) || uri.contains(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // 0. 如果是 OPTIONS 预检请求或文件上传请求，直接放行，不做安全头与签名校验
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 读取系统安全开关 (需同时配置了有效密钥才视为生效)
        boolean timestampEnabled = systemConfigHelper.isTimestampEnabled();
        boolean nonceEnabled = systemConfigHelper.isNonceEnabled();
        boolean sm2SignEnabled = systemConfigHelper.isSm2SignEffective();
        boolean sm4EncryptEnabled = systemConfigHelper.isSm4EncryptEffective();

        // 2. 时间戳校验
        String timestampStr = request.getHeader(HEADER_TIMESTAMP);
        if (timestampEnabled) {
            if (StrUtil.isBlank(timestampStr)) {
                writeError(response, 403, "请求已被拒绝：缺失 X-Timestamp 请求头");
                return;
            }
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
        if (nonceEnabled) {
            if (StrUtil.isBlank(nonce)) {
                writeError(response, 403, "请求已被拒绝：缺失 X-Nonce 请求头");
                return;
            }
            String redisKey = "security:nonce:" + nonce;
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", 5, TimeUnit.MINUTES);
            if (Boolean.FALSE.equals(success)) {
                writeError(response, 403, "请求已被拒绝：检测到重复请求 (Anti-Replay)");
                return;
            }
        }

        // 读取原始 Body 内容 (可能是明文，也可能是 SM4 密文) 供签名校验
        byte[] rawBodyBytes = request.getInputStream().readAllBytes();
        String rawBodyString = new String(rawBodyBytes, StandardCharsets.UTF_8);
        String effectiveBodyStr = rawBodyString.trim();
        if (effectiveBodyStr.startsWith("\"") && effectiveBodyStr.endsWith("\"") && effectiveBodyStr.length() > 2) {
            effectiveBodyStr = effectiveBodyStr.substring(1, effectiveBodyStr.length() - 1);
        }

        // 4. 国密 HMAC-SM3 / SM2 数字签名校验
        // 优先使用客户端会话临时密钥（X-Client-Id → Redis），回退到主密钥（兼容未升级客户端）
        String signature = request.getHeader(HEADER_SIGNATURE);
        if (sm2SignEnabled) {
            String servletPath = request.getServletPath();
            if (servletPath != null && servletPath.startsWith("/api/")) {
                servletPath = servletPath.substring(4);
            }
            boolean isPublicAuth = isPublicAuthUri(servletPath);
            if (StrUtil.isBlank(signature)) {
                if (!isPublicAuth) {
                    writeError(response, 403, "请求已被拒绝：缺失 X-Signature 签名头");
                    return;
                }
            } else {
                // 优先从 Redis 中按 clientId 取会话临时密钥，回退到主配置密钥
                String signKey = resolveSignKey(request);
                if (StrUtil.isNotBlank(signKey)) {
                    try {
                        String queryString = request.getQueryString();
                        String fullPath = servletPath + (StrUtil.isNotBlank(queryString) ? "?" + queryString : "");
                        try {
                            fullPath = URLDecoder.decode(fullPath, StandardCharsets.UTF_8);
                        } catch (Exception ignored) {}

                        String signContent = request.getMethod() + "\n"
                                + fullPath + "\n"
                                + StrUtil.nullToEmpty(timestampStr) + "\n"
                                + StrUtil.nullToEmpty(nonce) + "\n"
                                + effectiveBodyStr;

                        byte[] keyBytes = signKey.getBytes(StandardCharsets.UTF_8);
                        HMac hmac = SmUtil.hmacSm3(keyBytes);
                        String expectedSignature = hmac.digestHex(signContent);

                        boolean valid = expectedSignature.equalsIgnoreCase(signature);
                        if (!valid) {
                            // 降级支持 SM2 验签（兼容旧版客户端）
                            String publicKeyHex = systemConfigHelper.getSm2PublicKey();
                            if (StrUtil.isNotBlank(publicKeyHex)) {
                                try {
                                    SM2 sm2 = SmUtil.sm2(null, publicKeyHex);
                                    valid = sm2.verify(signContent.getBytes(StandardCharsets.UTF_8), HexUtil.decodeHex(signature));
                                } catch (Exception ignored) {}
                            }
                        }

                        if (!valid) {
                            writeError(response, 403, "请求已被拒绝：数字签名验证失败");
                            return;
                        }
                    } catch (Exception e) {
                        log.error("签名验证异常: {}", e.getMessage());
                        writeError(response, 403, "请求已被拒绝：数字签名校验异常");
                        return;
                    }
                }
            }
        }

        // 5. SM4 解密处理 (支持 CBC 模式提取前 32 位 Hex 作为 IV 向量，及旧版 ECB 降级)
        byte[] dispatchBodyBytes = rawBodyBytes;
        String isEncryptedHeader = request.getHeader(HEADER_ENCRYPTED);
        boolean isRequestEncrypted = "1".equals(isEncryptedHeader) || "true".equalsIgnoreCase(isEncryptedHeader);
        if (isRequestEncrypted && StrUtil.isNotBlank(rawBodyString)) {
            String sm4Key = systemConfigHelper.getSm4SecretKey();
            if (StrUtil.isBlank(sm4Key)) {
                log.error("SM4 加密已开启但未配置密钥，无法解密请求体");
                writeError(response, 403, "请求已被拒绝：服务端加密密钥未配置");
                return;
            }
            try {
                String cipherText = rawBodyString.trim();
                if (cipherText.startsWith("\"") && cipherText.endsWith("\"") && cipherText.length() > 2) {
                    cipherText = cipherText.substring(1, cipherText.length() - 1);
                }
                byte[] keyBytes = sm4Key.getBytes(StandardCharsets.UTF_8);
                String decryptedBody = null;

                // 尝试 SM4-CBC 模式解密 (提取前 32 字符 Hex 作为 16 字节 IV 向量)
                if (cipherText.length() > 32 && HexUtil.isHexNumber(cipherText.substring(0, 32))) {
                    try {
                        String hexIv = cipherText.substring(0, 32);
                        String rawCipher = cipherText.substring(32);
                        byte[] ivBytes = HexUtil.decodeHex(hexIv);
                        SM4 cbcSm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
                        decryptedBody = cbcSm4.decryptStr(rawCipher);
                    } catch (Exception e) {
                        log.warn("SM4-CBC 解密过程异常，尝试 ECB 降级解密: {}", e.getMessage());
                    }
                }

                // 降级使用 ECB 模式解密
                if (decryptedBody == null) {
                    SM4 ecbSm4 = SmUtil.sm4(keyBytes);
                    decryptedBody = ecbSm4.decryptStr(cipherText);
                }

                dispatchBodyBytes = decryptedBody.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("SM4 请求体解密失败: {}", e.getMessage());
                writeError(response, 403, "请求已被拒绝：SM4 请求解密失败");
                return;
            }
        }

        // 6. 重构 HttpServletRequest 传入 Filter 链
        byte[] finalBodyBytes = dispatchBodyBytes;
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

        // 7. 响应处理 (使用 SM4-CBC 模式 + 16 字节随机 IV 向量加密响应体)
        String acceptEncryptedHeader = request.getHeader(HEADER_ACCEPT_ENCRYPTED);
        boolean wantsEncryptedResp = "1".equals(acceptEncryptedHeader) || "true".equalsIgnoreCase(acceptEncryptedHeader);
        boolean shouldEncryptResponse = sm4EncryptEnabled && (isRequestEncrypted || wantsEncryptedResp);

        if (shouldEncryptResponse) {
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            byte[] respContent = wrappedResponse.getContentAsByteArray();
            String sm4Key = systemConfigHelper.getSm4SecretKey();
            if (respContent.length > 0 && StrUtil.isNotBlank(sm4Key)) {
                try {
                    String plainResp = new String(respContent, StandardCharsets.UTF_8);
                    byte[] keyBytes = sm4Key.getBytes(StandardCharsets.UTF_8);

                    // 动态生成 16 字节（128 位）密码学安全随机 IV
                    byte[] ivBytes = new byte[16];
                    new SecureRandom().nextBytes(ivBytes);
                    SM4 sm4Cbc = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);

                    // 拼接 IV (32位Hex) + SM4_CBC_Cipher (Hex)
                    String cipherHex = sm4Cbc.encryptHex(plainResp);
                    String encryptedResp = HexUtil.encodeHexStr(ivBytes) + cipherHex;

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

    /**
     * 解析当前请求的签名密钥。
     * <p>优先从 Redis 中按 {@code X-Client-Id} 取会话临时密钥（安全加固路径），
     * 若未携带 clientId 或 Redis 中不存在，则回退到主配置密钥（兼容未升级客户端）。
     */
    private String resolveSignKey(HttpServletRequest request) {
        String clientId = request.getHeader(HEADER_CLIENT_ID);
        if (StrUtil.isNotBlank(clientId) && clientId.length() >= 8 && clientId.length() <= 128) {
            String sessionKey = stringRedisTemplate.opsForValue().get(SESSION_SIGN_KEY_PREFIX + clientId);
            if (StrUtil.isNotBlank(sessionKey)) {
                return sessionKey;
            }
        }
        // 回退：使用系统配置的主密钥（兼容旧版未升级客户端，但主密钥本身不再公开下发）
        return systemConfigHelper.getSm3SignKey();
    }

    private void writeError(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        CommonResult<Void> result = CommonResult.error(status, msg);
        response.getWriter().write(JSONUtil.toJsonStr(result));
    }
}
