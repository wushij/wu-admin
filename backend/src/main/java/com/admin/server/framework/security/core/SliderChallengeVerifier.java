package com.admin.server.framework.security.core;

/**
 * 滑块风控挑战校验 SPI（框架层定义，业务层 SliderCaptchaService 实现）
 */
public interface SliderChallengeVerifier {

    /**
     * 校验并消耗滑块验证凭证
     *
     * @param token    一次性凭证
     * @param code     验证码/偏移量参数（可为空）
     * @param scene    业务场景（可为空）
     * @param clientIp 客户端真实 IP
     * @return 错误信息，校验成功返回 null
     */
    String verifyAndConsume(String token, String code, String scene, String clientIp);
}
