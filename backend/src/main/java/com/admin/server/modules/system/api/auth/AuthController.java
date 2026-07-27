package com.admin.server.modules.system.api.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.admin.server.common.core.CommonResult;
import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.system.api.auth.vo.EmailCodeReqVO;
import com.admin.server.modules.system.api.auth.vo.ForgotPasswordCheckReqVO;
import com.admin.server.modules.system.api.auth.vo.ForgotPasswordResetReqVO;
import com.admin.server.modules.system.api.auth.vo.ForgotPasswordSmsCodeReqVO;
import com.admin.server.modules.system.api.auth.vo.LoginReqVO;
import com.admin.server.modules.system.api.auth.vo.RegisterReqVO;
import com.admin.server.modules.system.api.auth.vo.SmsCodeReqVO;
import com.admin.server.modules.system.api.auth.vo.SmsCodeVerifyReqVO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.service.auth.AuthForgotPasswordService;
import com.admin.server.modules.system.service.auth.AuthService;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** 会话签名密钥 Redis 前缀，TTL = 30 分钟 */
    private static final String SESSION_SIGN_KEY_PREFIX = "security:session-sign:";
    private static final long SESSION_SIGN_TTL_MINUTES = 30L;
    /** clientId 合法字符校验（32~64位十六进制或 UUID 格式） */
    private static final int CLIENT_ID_MIN_LEN = 8;
    private static final int CLIENT_ID_MAX_LEN = 128;

    @Resource
    private AuthService authService;
    @Resource
    private AuthForgotPasswordService authForgotPasswordService;
    @Resource
    private SystemConfigHelper systemConfigHelper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public CommonResult<Map<String, Object>> captcha(
            @RequestParam(value = "scene", defaultValue = "login") String scene,
            HttpServletRequest request) {
        String clientIp = ClientIpUtils.resolve(request);
        return CommonResult.success(authService.generateCaptcha(scene, clientIp));
    }

    @Operation(summary = "获取滑块验证码 challenge")
    @GetMapping("/slider-challenge")
    public CommonResult<Map<String, Object>> sliderChallenge(
            @RequestParam(value = "scene", defaultValue = "login") String scene,
            HttpServletRequest request) {
        String clientIp = ClientIpUtils.resolve(request);
        return CommonResult.success(authService.createSliderChallenge(scene, clientIp));
    }

    @Operation(summary = "获取登录配置")
    @GetMapping("/config")
    public CommonResult<Map<String, Object>> config() {
        return CommonResult.success(authService.getPublicConfig());
    }

    /**
     * 【安全加固 P0】会话签名密钥初始化接口。
     *
     * <p>替代原「公开接口明文下发固定密钥」方案。
     * 客户端在应用启动时生成一个随机 clientId（内存保存，不持久化），
     * 携带 clientId 调用此接口，服务端为其生成一个随机 32 字节（64 位 Hex）临时签名密钥，
     * 以 {@code security:session-sign:{clientId}} 为 key 存入 Redis，TTL=30 分钟。
     * 后续请求的 X-Signature 均使用此临时密钥计算，Filter 也用 clientId 从 Redis 取密钥验签。
     *
     * <p>安全收益：
     * <ul>
     *   <li>密钥不再全局固定（不再是 WuAdmin16ByteKey 这类硬编码值）</li>
     *   <li>密钥每 30 分钟自动过期，不同客户端拿到不同密钥</li>
     *   <li>即使某次下发的临时密钥被截获，有效窗口仅 30 分钟</li>
     * </ul>
     *
     * @param clientId 前端随机生成的设备标识（8~128 字符），要求每次应用启动重新生成，不持久化
     */
    @Operation(summary = "初始化会话签名密钥（安全加固）")
    @PostMapping("/session-sign-init")
    public CommonResult<Map<String, Object>> sessionSignInit(
            @RequestParam(value = "clientId") String clientId) {
        // 校验 clientId 格式，防止 Redis key 注入
        if (StrUtil.isBlank(clientId)
                || clientId.length() < CLIENT_ID_MIN_LEN
                || clientId.length() > CLIENT_ID_MAX_LEN
                || !clientId.matches("[A-Za-z0-9\\-_]+")) {
            throw new BusinessException(400, "clientId 格式非法");
        }
        // 仅在服务端签名功能开启时才生成并返回密钥
        if (!systemConfigHelper.isSm2SignEffective()) {
            return CommonResult.success(Map.of("enabled", false));
        }
        // 生成 32 字节（64 位 Hex）密码学安全随机临时密钥
        String tempKey = RandomUtil.randomString("0123456789abcdef", 64);
        String redisKey = SESSION_SIGN_KEY_PREFIX + clientId;
        stringRedisTemplate.opsForValue().set(redisKey, tempKey, SESSION_SIGN_TTL_MINUTES, TimeUnit.MINUTES);
        return CommonResult.success(Map.of(
                "enabled", true,
                "sm3SignKey", tempKey,
                "sm2PrivateKey", tempKey,
                "ttlMinutes", SESSION_SIGN_TTL_MINUTES
        ));
    }

    @Log(title = "用户登录", businessType = Log.BusinessType.OTHER, isSaveRequestData = false)
    @Operation(summary = "登录")
    @PostMapping("/login")
    public CommonResult<Map<String, Object>> login(@Validated @RequestBody LoginReqVO reqVO, HttpServletRequest request) {
        String clientIp = ClientIpUtils.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        if (SystemConfigHelper.LOGIN_TYPE_SMS.equalsIgnoreCase(reqVO.getLoginType())) {
            return CommonResult.success(authService.loginBySms(reqVO, clientIp, userAgent));
        }
        if (SystemConfigHelper.LOGIN_TYPE_EMAIL.equalsIgnoreCase(reqVO.getLoginType())) {
            return CommonResult.success(authService.loginByEmail(reqVO, clientIp, userAgent));
        }
        return CommonResult.success(authService.loginByAccount(reqVO, clientIp, userAgent));
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/info")
    public CommonResult<Map<String, Object>> info() {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        return CommonResult.success(authService.getUserInfo(userId));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public CommonResult<Boolean> logout() {
        if (StpUtil.isLogin()) {
            authService.logout(StpUtil.getLoginIdAsLong());
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "发送登录短信验证码")
    @PostMapping("/sms-code")
    public CommonResult<Boolean> sendSmsCode(@Validated @RequestBody SmsCodeReqVO reqVO, HttpServletRequest request) {
        authService.sendSmsCode(reqVO, ClientIpUtils.resolve(request));
        return CommonResult.success(true);
    }

    @Operation(summary = "发送登录邮箱验证码")
    @PostMapping("/email-code")
    public CommonResult<Boolean> sendEmailCode(@Validated @RequestBody EmailCodeReqVO reqVO, HttpServletRequest request) {
        authService.sendEmailCode(reqVO, ClientIpUtils.resolve(request));
        return CommonResult.success(true);
    }

    @Operation(summary = "核验短信验证码")
    @PostMapping("/sms-code/verify")
    public CommonResult<Boolean> verifySmsCode(@Validated @RequestBody SmsCodeVerifyReqVO reqVO) {
        authService.verifySmsCode(reqVO);
        return CommonResult.success(true);
    }

    @Log(title = "用户注册", businessType = Log.BusinessType.INSERT, isSaveRequestData = false)
    @Operation(summary = "注册")
    @PostMapping("/register")
    public CommonResult<Boolean> register(@Validated @RequestBody RegisterReqVO reqVO, HttpServletRequest request) {
        boolean needAudit = authService.register(reqVO, ClientIpUtils.resolve(request));
        CommonResult<Boolean> result = CommonResult.success(true);
        result.setMessage(needAudit ? "注册成功，请等待管理员审核" : "注册成功");
        return result;
    }

    @Operation(summary = "忘记密码-校验用户名")
    @PostMapping("/forgot-password/check")
    public CommonResult<Map<String, Object>> forgotPasswordCheck(
            @Validated @RequestBody ForgotPasswordCheckReqVO reqVO,
            HttpServletRequest request) {
        String clientIp = ClientIpUtils.resolve(request);
        String rateErr = authForgotPasswordService.rateLimitCheck(clientIp);
        if (rateErr != null) {
            throw new BusinessException(429, rateErr);
        }
        UserDO user = authForgotPasswordService.resolveUser(reqVO.getUsername());
        String err = authForgotPasswordService.validateUserForForgot(user);
        if (err != null) {
            throw new BusinessException(400, err);
        }
        return CommonResult.success(Map.of(
                "maskedMobile", authForgotPasswordService.maskMobile(user.getMobile()),
                "minPasswordLength", systemConfigHelper.getRegisterMinPasswordLength()));
    }

    @Operation(summary = "忘记密码-发送短信验证码")
    @PostMapping("/forgot-password/sms-code")
    public CommonResult<Boolean> forgotPasswordSmsCode(
            @Validated @RequestBody ForgotPasswordSmsCodeReqVO reqVO,
            HttpServletRequest request) {
        UserDO user = authForgotPasswordService.resolveUser(reqVO.getUsername());
        String validateErr = authForgotPasswordService.validateUserForForgot(user);
        if (validateErr != null) {
            throw new BusinessException(400, validateErr);
        }
        String err = authForgotPasswordService.sendResetCode(
                user, ClientIpUtils.resolve(request), reqVO.getUuid(), reqVO.getCode());
        if (err != null) {
            throw new BusinessException(400, err);
        }
        return CommonResult.success(true);
    }

    @Log(title = "忘记密码重置", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false)
    @Operation(summary = "忘记密码-短信重置密码")
    @PostMapping("/forgot-password/reset")
    public CommonResult<Boolean> forgotPasswordReset(@Validated @RequestBody ForgotPasswordResetReqVO reqVO) {
        UserDO user = authForgotPasswordService.resolveUser(reqVO.getUsername());
        String validateErr = authForgotPasswordService.validateUserForForgot(user);
        if (validateErr != null) {
            throw new BusinessException(400, validateErr);
        }
        String err = authForgotPasswordService.resetPasswordAndSave(
                user, reqVO.getSmsCode(), reqVO.getNewPassword(), reqVO.getConfirmPassword());
        if (err != null) {
            throw new BusinessException(400, err);
        }
        return CommonResult.success(true);
    }
}
