package com.admin.server.framework.security.core.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.json.JSONUtil;
import com.admin.server.common.core.CommonResult;
import com.admin.server.framework.security.core.util.Sm4GcmUtil;
import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.framework.config.DynamicConfigProvider;
import com.admin.server.framework.security.core.RiskPrincipal;
import com.admin.server.framework.security.core.RiskPrincipalResolver;
import com.admin.server.framework.security.core.RiskScoringApi;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 全链路 API 安全防护与反爬风控过滤器
 * 支持：IP黑名单风控、爬虫UA嗅探、时间戳校验(5分钟窗口)、Nonce随机数防重放(Redis查重)、HMAC-SM3数字签名与满血标准 SM4-GCM 接口加解密。
 * <p>报文加密仅支持国密 SM4-GCM（AEAD），认证 Tag 校验失败即拒绝请求（Fail-Closed），不做 CBC/ECB 降级。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiSecurityFilter.class);

    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";
    public static final String HEADER_ENCRYPTED = "X-Encrypted";
    public static final String HEADER_ENCRYPTED_PAYLOAD = "X-Encrypted-Payload";
    public static final String HEADER_ACCEPT_ENCRYPTED = "X-Accept-Encrypted";
    /** 客户端内存内随机 ID，用于从 Redis 查找临时会话密钥 */
    public static final String HEADER_CLIENT_ID = "X-Client-Id";
    /** 会话签名临时密钥 Redis 前缀（与 AuthController 中定义保持一致） */
    private static final String SESSION_SIGN_KEY_PREFIX = "security:session-sign:";
    /** 会话 SM4 加密密钥 Redis 前缀（与 AuthController 中保持一致） */
    private static final String SESSION_SM4_KEY_PREFIX = "security:session-sm4:";

    private static final long MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L; // 5 分钟窗口

    private static final Set<String> EXCLUDE_PATH_PREFIXES = Set.of(
            "/doc.html",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars",
            "/favicon.ico",
            "/files",
            "/storage/files",
            "/ws",
            "/pay/notify",
            // 文件下载/预览/文本读取与各类导出使用原生 axios / uni.downloadFile，
            // 无法携带 X-Timestamp/X-Nonce/X-Signature；这些接口自身已有登录态和 @RequirePermission 校验。
            "/system/file/download",
            "/system/file/preview",
            "/system/file/text",
            "/system/export",
            "/monitor/api-access/export",
            "/monitor/online/export",
            "/tool/gen/download",
            // 蜜罐诱饵：爬虫不会携带签名头，若在此被拦下，陷阱只会返回验签失败，
            // 命中事件也无法落到 HONEYPOT 类型。
            "/system/internal"
    );

    @Resource
    private DynamicConfigProvider dynamicConfigProvider;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RiskScoringApi antiBotRiskService;

    @Resource
    private com.admin.server.framework.web.core.ApiErrorResponseWriter errorResponseWriter;

    /**
     * 基础认证与登录态接口：开启签名后仍允许不带 X-Signature 调用。
     * <p>须与前端 {@code AUTH_PUBLIC_SUFFIXES} 保持一致，避免初始化期或关键信息获取因缺少签名被 403 阻断。
     * 其中 /auth/info 内部具备严格的 Sa-Token 登录态校验，未登录直接 401，放行签名不影响安全性。</p>
     */
    /** JSON 请求体最大字节数，防止超大请求体占满内存 */
    private static final int MAX_BODY_BYTES = 1024 * 1024;

    private static final Set<String> PUBLIC_AUTH_URIS = Set.of(
            "/auth/config",
            "/auth/login",
            "/auth/register",
            "/auth/captcha",
            "/auth/sms-code",
            "/auth/sms-code/verify",
            "/auth/email-code",
            "/auth/forgot-password/check",
            "/auth/forgot-password/sms-code",
            "/auth/forgot-password/reset",
            "/auth/slider-challenge",
            "/auth/slider-verify",
            "/auth/session-sign-init",
            "/auth/info",
            "/auth/logout"
    );

    /** SSE 流式端点：保留请求验签，但响应体为增量推流，不可整体 SM4 加密 */
    private static final Set<String> STREAMING_URI_SUFFIXES = Set.of(
            "/ai/chat/stream"
    );

    private boolean isStreamingUri(String uri) {
        if (uri == null) return false;
        for (String suffix : STREAMING_URI_SUFFIXES) {
            if (uri.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicAuthUri(String uri) {
        if (uri == null) {
            return false;
        }
        for (String publicUri : PUBLIC_AUTH_URIS) {
            if (uri.equals(publicUri)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : EXCLUDE_PATH_PREFIXES) {
            if (matchesPrefix(uri, prefix) || matchesPrefix(uri, "/api" + prefix)) {
                return true;
            }
        }
        return false;
    }

    /** 精确匹配路径或其子路径，避免 contains 把未来新增的业务路径误排除 */
    private boolean matchesPrefix(String uri, String prefix) {
        return uri != null && (uri.equals(prefix) || uri.startsWith(prefix + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // 解析客户端真实 IP 与 User-Agent
        String clientIp = ClientIpUtils.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // IP 封禁与爬虫 UA 嗅探已前移到 AntiBotGateFilter：
        // 该过滤器位于本过滤器之前，且不排除 /files、/ws、/doc.html 等路径，
        // 避免被封禁客户端通过文件直出等路径绕过风控。

        // 0.1 如果是 OPTIONS 预检请求，直接放行，不做签名校验
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 0.2 文件上传请求无法整体读取 Body 参与签名/加密，放行签名阶段；
        // 封禁与 UA 风控已由 AntiBotGateFilter 完成，上传接口内部仍需校验登录态与权限。
        String contentType = request.getContentType();
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. 读取系统安全开关 (需同时配置了有效密钥才视为生效)
        boolean timestampEnabled = dynamicConfigProvider.isTimestampEnabled();
        boolean nonceEnabled = dynamicConfigProvider.isNonceEnabled();
        boolean sm2SignEnabled = dynamicConfigProvider.isSm2SignEffective();
        boolean sm4EncryptEnabled = dynamicConfigProvider.isSm4EncryptEffective();

        // 2. 时间戳校验
        String timestampStr = request.getHeader(HEADER_TIMESTAMP);
        if (timestampEnabled) {
            if (StrUtil.isBlank(timestampStr)) {
                antiBotRiskService.addRisk(resolveSubject(request), 10, "TIMESTAMP_INVALID", "BLOCKED",
                        uri, method, userAgent, "缺失 X-Timestamp 请求头");
                writeError(response, 403, "请求已被拒绝：缺失 X-Timestamp 请求头");
                return;
            }
            try {
                long timestamp = Long.parseLong(timestampStr);
                long diff = Math.abs(System.currentTimeMillis() - timestamp);
                if (diff > MAX_TIMESTAMP_DIFF_MS) {
                    antiBotRiskService.addRisk(resolveSubject(request), 20, "TIMESTAMP_INVALID", "BLOCKED",
                            uri, method, userAgent, "时间戳无效或已过期 (diff=" + diff + "ms)");
                    writeError(response, 403, "请求已被拒绝：时间戳无效或已过期");
                    return;
                }
            } catch (NumberFormatException e) {
                antiBotRiskService.addRisk(resolveSubject(request), 20, "TIMESTAMP_INVALID", "BLOCKED",
                        uri, method, userAgent, "非法时间戳格式");
                writeError(response, 403, "请求已被拒绝：非法时间戳格式");
                return;
            }
        }

        // 3. Nonce 防重放校验
        String nonce = request.getHeader(HEADER_NONCE);
        if (nonceEnabled) {
            if (StrUtil.isBlank(nonce)) {
                antiBotRiskService.addRisk(resolveSubject(request), 10, "ANTI_REPLAY", "BLOCKED",
                        uri, method, userAgent, "缺失 X-Nonce 请求头");
                writeError(response, 403, "请求已被拒绝：缺失 X-Nonce 请求头");
                return;
            }
            if (!nonce.matches("[0-9a-fA-F-]{16,64}")) {
                antiBotRiskService.addRisk(resolveSubject(request), 10, "ANTI_REPLAY", "BLOCKED",
                        uri, method, userAgent, "非法 X-Nonce 格式");
                writeError(response, 403, "请求已被拒绝：非法 X-Nonce 格式");
                return;
            }
            String redisKey = "security:nonce:" + nonce;
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", 5, TimeUnit.MINUTES);
            if (Boolean.FALSE.equals(success)) {
                antiBotRiskService.addRisk(resolveSubject(request), 30, "ANTI_REPLAY", "BLOCKED",
                        uri, method, userAgent, "检测到重复请求 (Anti-Replay nonce=" + nonce + ")");
                writeError(response, 403, "请求已被拒绝：检测到重复请求 (Anti-Replay)");
                return;
            }
        }

        // 读取原始 Body 内容 (可能是明文，也可能是 SM4 密文) 供签名校验。
        // 必须限制大小：Tomcat 的 maxPostSize 对 application/json 不生效，避免超大 JSON 拖垮内存。
        byte[] rawBodyBytes = readBodyWithLimit(request, response, clientIp, uri, method, userAgent);
        if (rawBodyBytes == null) {
            return;
        }
        String rawBodyString = new String(rawBodyBytes, StandardCharsets.UTF_8);
        String effectiveBodyStr = rawBodyString.trim();
        if (effectiveBodyStr.startsWith("\"") && effectiveBodyStr.endsWith("\"") && effectiveBodyStr.length() > 2) {
            effectiveBodyStr = effectiveBodyStr.substring(1, effectiveBodyStr.length() - 1);
        }

        // 4. 国密 HMAC-SM3 数字签名校验
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
                    antiBotRiskService.addRisk(resolveSubject(request), 15, "SIGN_FAIL", "BLOCKED",
                            uri, method, userAgent, "缺失 X-Signature 签名头");
                    writeError(response, 403, "请求已被拒绝：缺失 X-Signature 签名头");
                    return;
                }
            } else {
                // 优先从 Redis 中按 clientId 取会话临时密钥，回退到主配置密钥
                String signKey = resolveSignKey(request);
                if (StrUtil.isBlank(signKey)) {
                    // 签名已开启但拿不到任何密钥：严禁静默放行，否则伪造签名可绕过校验
                    if (!isPublicAuth) {
                        antiBotRiskService.addRisk(resolveSubject(request), 15, "SIGN_FAIL", "BLOCKED",
                                uri, method, userAgent, "签名密钥缺失或已过期");
                        writeError(response, 403, "请求已被拒绝：签名密钥缺失或已过期，请重新初始化会话");
                        return;
                    }
                } else {
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

                        if (!expectedSignature.equalsIgnoreCase(signature)) {
                            antiBotRiskService.addRisk(resolveSubject(request), 25, "SIGN_FAIL", "BLOCKED",
                                    uri, method, userAgent, "数字签名不匹配 (HMAC-SM3 digest mismatch)");
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

        // 5. SM4-GCM 请求体认证解密（仅支持满血标准 GCM，Fail-Closed：认证失败直接拒绝，不做任何降级）
        byte[] dispatchBodyBytes = rawBodyBytes;
        String isEncryptedHeader = request.getHeader(HEADER_ENCRYPTED);
        String encryptedPayloadType = request.getHeader(HEADER_ENCRYPTED_PAYLOAD);
        boolean isRequestEncrypted = "1".equals(isEncryptedHeader)
                || "true".equalsIgnoreCase(isEncryptedHeader)
                || "SM4-GCM".equalsIgnoreCase(encryptedPayloadType);
        // 固定本次请求实际使用的 SM4 密钥：响应加密必须复用同一把密钥。
        // 登录成功等业务会在请求处理期间轮换 Redis 会话密钥，若响应阶段重新解析，
        // 就会改用新密钥加密，而客户端仍持有旧密钥，必然触发 GCM Tag 校验失败。
        String requestSm4Key = null;
        if (isRequestEncrypted && StrUtil.isNotBlank(rawBodyString)) {
            // 优先从 Redis 取会话 SM4 密钥，回退到 DB 静态密钥（兼容旧客户端）
            String sm4Key = resolveEncryptKey(request);
            if (StrUtil.isBlank(sm4Key)) {
                log.error("SM4 加密已开启但无法获取解密密钥（clientId={} 无会话密钥且 DB 未配置）",
                        request.getHeader(HEADER_CLIENT_ID));
                writeError(response, 403, "请求已被拒绝：服务端加密密钥未配置");
                return;
            }
            try {
                String cipherText = rawBodyString.trim();
                if (cipherText.startsWith("\"") && cipherText.endsWith("\"") && cipherText.length() > 2) {
                    cipherText = cipherText.substring(1, cipherText.length() - 1);
                }
                // 统一使用 Sm4GcmUtil.resolveKeyBytes 解析密钥，与前端及 AiKeyCrypto 保持逐字节一致
                byte[] keyBytes = Sm4GcmUtil.resolveKeyBytes(sm4Key);
                // 标准 Base64 格式：Base64(IV[12] + Ciphertext + Tag[16])，Tag 校验失败即抛异常
                String decryptedBody = Sm4GcmUtil.decryptFromBase64(keyBytes, cipherText);
                dispatchBodyBytes = decryptedBody.getBytes(StandardCharsets.UTF_8);
                requestSm4Key = sm4Key;
            } catch (Exception e) {
                log.warn("SM4-GCM 请求体认证解密失败（clientId={}，原因：{}）",
                        request.getHeader(HEADER_CLIENT_ID), e.getMessage());
                writeError(response, 403, "请求已被拒绝：SM4-GCM 密文认证解密失败");
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

        // 7. 响应处理 (使用满血标准 SM4-GCM 认证加密模式加密响应体)
        String acceptEncryptedHeader = request.getHeader(HEADER_ACCEPT_ENCRYPTED);
        boolean wantsEncryptedResp = "1".equals(acceptEncryptedHeader)
                || "true".equalsIgnoreCase(acceptEncryptedHeader)
                || "SM4-GCM".equalsIgnoreCase(acceptEncryptedHeader);
        boolean shouldEncryptResponse = sm4EncryptEnabled && (isRequestEncrypted || wantsEncryptedResp)
                && !isStreamingUri(request.getRequestURI());

        if (shouldEncryptResponse) {
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            byte[] respContent = wrappedResponse.getContentAsByteArray();
            // 响应必须使用「客户端本次请求持有的密钥」加密：请求已解密时复用该密钥，
            // 避免业务处理期间 Redis 会话密钥被轮换导致客户端无法解密响应。
            String sm4Key = StrUtil.isNotBlank(requestSm4Key) ? requestSm4Key : resolveEncryptKey(request);
            if (respContent.length > 0 && StrUtil.isNotBlank(sm4Key)) {
                try {
                    String plainResp = new String(respContent, StandardCharsets.UTF_8);
                    byte[] keyBytes = Sm4GcmUtil.resolveKeyBytes(sm4Key);

                    // 使用满血标准国密 SM4-GCM 进行认证加密：输出 Base64(IV[12] + Cipher + Tag[16])
                    String encryptedResp = Sm4GcmUtil.encryptToBase64(keyBytes, plainResp);

                    response.setHeader(HEADER_ENCRYPTED, "1");
                    response.setHeader(HEADER_ENCRYPTED_PAYLOAD, "SM4-GCM");
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
     * 解析当前请求的签名密钥（HMAC-SM3）。
     * <p>优先从 Redis 中按 {@code X-Client-Id} 取会话临时密钥（安全加固路径），
     * 若未携带 clientId 或 Redis 中不存在，则回退到主配置密钥（兼容未升级客户端）。
     */
    private String resolveSignKey(HttpServletRequest request) {
        String clientId = request.getHeader(HEADER_CLIENT_ID);
        if (isValidClientId(clientId)) {
            String sessionKey = stringRedisTemplate.opsForValue().get(SESSION_SIGN_KEY_PREFIX + clientId);
            if (StrUtil.isNotBlank(sessionKey)) {
                return sessionKey;
            }
        }
        // 回退：使用系统配置的主密钥（兼容旧版未升级客户端）
        return dynamicConfigProvider.getSm3SignKey();
    }

    /**
     * 解析当前请求的 SM4 加解密密钥。
     * <p>优先从 Redis 中按 {@code X-Client-Id} 取会话 SM4 临时密钥（与 session-sign-init 协商），
     * 若未找到则回退到 DB 中配置的静态 SM4 密钥（兼容旧版客户端）。
     */
    private String resolveEncryptKey(HttpServletRequest request) {
        String clientId = request.getHeader(HEADER_CLIENT_ID);
        if (isValidClientId(clientId)) {
            String sessionSm4Key = stringRedisTemplate.opsForValue().get(SESSION_SM4_KEY_PREFIX + clientId);
            if (StrUtil.isNotBlank(sessionSm4Key)) {
                return sessionSm4Key;
            }
        }
        // 回退：使用 DB 静态 SM4 密钥（兼容未完成会话协商的旧版客户端）
        return dynamicConfigProvider.getSm4SecretKey();
    }

    /** clientId 只允许字母、数字、下划线和短横线，避免拼进 Redis key 时产生注入或异常 key */
    private boolean isValidClientId(String clientId) {
        return StrUtil.isNotBlank(clientId)
                && clientId.length() >= 8
                && clientId.length() <= 128
                && clientId.matches("[A-Za-z0-9_-]+");
    }

    /**
     * 有界读取请求体：先看 Content-Length，再按上限读取，避免超大 JSON 全量进内存。
     */
    private byte[] readBodyWithLimit(HttpServletRequest request, HttpServletResponse response,
                                     String clientIp, String uri, String method,
                                     String userAgent) throws IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_BODY_BYTES) {
            rejectOversizedBody(request, clientIp, uri, method, userAgent);
            writeError(response, 413, "请求体过大，最大允许 1MB");
            return null;
        }
        try (InputStream in = request.getInputStream()) {
            byte[] bytes = in.readNBytes(MAX_BODY_BYTES + 1);
            if (bytes.length > MAX_BODY_BYTES) {
                rejectOversizedBody(request, clientIp, uri, method, userAgent);
                writeError(response, 413, "请求体过大，最大允许 1MB");
                return null;
            }
            return bytes;
        }
    }

    private void rejectOversizedBody(HttpServletRequest request, String clientIp, String uri, String method, String userAgent) {
        try {
            antiBotRiskService.addRisk(resolveSubject(request), 20, "BODY_TOO_LARGE", "BLOCKED",
                    uri, method, userAgent, "请求体超过 1MB 上限");
        } catch (Exception e) {
            log.warn("记录超大请求体风险分失败: {}", e.getMessage());
        }
    }

    /**
     * 解析风险主体：只有确实要记分时才调用，避免给每个正常请求增加一次登录态查询。
     */
    private RiskPrincipal resolveSubject(HttpServletRequest request) {
        try {
            return RiskPrincipalResolver.resolve(request);
        } catch (Exception e) {
            return RiskPrincipal.ofIp(ClientIpUtils.resolve(request));
        }
    }

    private void writeError(HttpServletResponse response, int status, String msg) throws IOException {        try {
            errorResponseWriter.write(response, status, msg);
        } catch (Exception e) {
            log.error("写出安全过滤器错误响应失败: status={}, msg={}, err={}", status, msg, e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(status);
                response.setContentType("application/json;charset=UTF-8");
                CommonResult<Void> result = CommonResult.error(status, msg);
                response.getOutputStream().write(JSONUtil.toJsonStr(result).getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
