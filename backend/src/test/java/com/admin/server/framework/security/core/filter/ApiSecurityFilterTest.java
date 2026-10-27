package com.admin.server.framework.security.core.filter;

import com.admin.server.framework.security.core.util.Sm4GcmUtil;
import com.admin.server.framework.config.DynamicConfigProvider;
import com.admin.server.framework.security.core.RiskScoringApi;
import jakarta.servlet.FilterChain;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ApiSecurityFilter 报文解密回归测试。
 *
 * <p>锁定「只接受满血 SM4-GCM、认证失败直接 403、绝不降级 CBC/ECB」这一安全约束，
 * 防止后续维护重新引入降级逻辑。</p>
 */
public class ApiSecurityFilterTest {

    private static final String KEY_HEX = "0123456789abcdef0123456789abcdef";
    private static final String PLAIN_BODY = "{\"username\":\"admin\",\"role\":\"super_admin\"}";

    private ApiSecurityFilter filter;
    private RiskScoringApi antiBotRiskService;
    private DynamicConfigProvider configHelper;

    @BeforeEach
    public void setUp() {
        filter = new ApiSecurityFilter();
        configHelper = mock(DynamicConfigProvider.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        antiBotRiskService = mock(RiskScoringApi.class);
        ReflectionTestUtils.setField(filter, "dynamicConfigProvider", configHelper);
        ReflectionTestUtils.setField(filter, "stringRedisTemplate", redisTemplate);
        ReflectionTestUtils.setField(filter, "antiBotRiskService", antiBotRiskService);

        when(configHelper.isTimestampEnabled()).thenReturn(false);
        when(configHelper.isNonceEnabled()).thenReturn(false);
        when(configHelper.isSm2SignEffective()).thenReturn(false);
        when(configHelper.isSm4EncryptEffective()).thenReturn(true);
        when(configHelper.getSm4SecretKey()).thenReturn(KEY_HEX);
    }

    @Test
    @DisplayName("合法 SM4-GCM 密文可被认证解密并传递给后续过滤器")
    public void validGcmPayloadIsDecrypted() throws Exception {
        String cipher = Sm4GcmUtil.encryptToBase64(KEY_HEX, PLAIN_BODY);
        MockHttpServletRequest request = encryptedRequest(cipher);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> dispatchedBody = new AtomicReference<>();

        FilterChain chain = (req, res) ->
                dispatchedBody.set(new String(req.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));

        filter.doFilterInternal(request, response, chain);

        assertEquals(PLAIN_BODY, dispatchedBody.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("篡改 SM4-GCM 认证 Tag 必须返回 403，且不再继续执行过滤器链")
    public void tamperedGcmPayloadIsRejected() throws Exception {
        String cipher = Sm4GcmUtil.encryptToBase64(KEY_HEX, PLAIN_BODY);
        byte[] combined = Base64.decode(cipher);
        combined[combined.length - 1] ^= 0x01;
        String tampered = Base64.toBase64String(combined);

        MockHttpServletRequest request = encryptedRequest(tampered);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainInvoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        assertFalse(chainInvoked.get());
    }

    @Test
    @DisplayName("历史 CBC 格式密文不再被静默降级解密，必须返回 403")
    public void legacyCbcPayloadIsRejectedWithoutDowngrade() throws Exception {
        // 32 位 Hex IV + 32 位 Hex 密文，即旧版 SM4-CBC 报文格式
        String legacyCbc = "000102030405060708090a0b0c0d0e0f" + "00112233445566778899aabbccddeeff";
        MockHttpServletRequest request = encryptedRequest(legacyCbc);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        FilterChain chain = (req, res) -> chainInvoked.set(true);

        filter.doFilterInternal(request, response, chain);

        assertEquals(403, response.getStatus());
        assertFalse(chainInvoked.get());
    }

    @Test
    @DisplayName("业务处理期间轮换会话密钥时，响应仍使用请求方持有的旧密钥加密")
    public void responseEncryptedWithRequestKeyWhenKeyRotatedDuringHandling() throws Exception {
        String rotatedKey = "fedcba9876543210fedcba9876543210";
        // 请求解密读到旧密钥；若响应阶段重新解析，则会读到轮换后的新密钥
        when(configHelper.getSm4SecretKey()).thenReturn(KEY_HEX, rotatedKey);

        String cipher = Sm4GcmUtil.encryptToBase64(KEY_HEX, PLAIN_BODY);
        MockHttpServletRequest request = encryptedRequest(cipher);
        request.addHeader(ApiSecurityFilter.HEADER_ACCEPT_ENCRYPTED, "1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String rotatedBody = "{\"code\":200,\"data\":{\"sm4Key\":\"" + rotatedKey + "\"}}";
        FilterChain chain = (req, res) -> {
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write(rotatedBody);
            res.getWriter().flush();
        };

        filter.doFilterInternal(request, response, chain);

        assertEquals("1", response.getHeader(ApiSecurityFilter.HEADER_ENCRYPTED));
        String encryptedBody = response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        // 客户端仍持有旧密钥：必须能用旧密钥解开响应，而不是被轮换后的新密钥加密
        assertEquals(rotatedBody, Sm4GcmUtil.decryptFromBase64(KEY_HEX, encryptedBody));
    }

    private MockHttpServletRequest encryptedRequest(String cipherText) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json;charset=UTF-8");
        request.setContent(cipherText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        request.addHeader(ApiSecurityFilter.HEADER_ENCRYPTED, "1");
        request.addHeader(ApiSecurityFilter.HEADER_ENCRYPTED_PAYLOAD, "SM4-GCM");
        return request;
    }
}
