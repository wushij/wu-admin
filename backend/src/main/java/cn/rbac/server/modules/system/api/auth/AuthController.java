package cn.rbac.server.modules.system.api.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.util.ClientIpUtils;
import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.modules.system.api.auth.vo.ForgotPasswordCheckReqVO;
import cn.rbac.server.modules.system.api.auth.vo.ForgotPasswordResetReqVO;
import cn.rbac.server.modules.system.api.auth.vo.ForgotPasswordSmsCodeReqVO;
import cn.rbac.server.modules.system.api.auth.vo.LoginReqVO;
import cn.rbac.server.modules.system.api.auth.vo.RegisterReqVO;
import cn.rbac.server.modules.system.api.auth.vo.SmsCodeReqVO;
import cn.rbac.server.modules.system.api.auth.vo.SmsCodeVerifyReqVO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.service.auth.AuthForgotPasswordService;
import cn.rbac.server.modules.system.service.auth.AuthService;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;
    @Resource
    private AuthForgotPasswordService authForgotPasswordService;
    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public CommonResult<Map<String, Object>> captcha(
            @RequestParam(value = "scene", defaultValue = "login") String scene,
            HttpServletRequest request) {
        String clientIp = ClientIpUtils.resolve(request);
        return CommonResult.success(authService.generateCaptcha(scene, clientIp));
    }

    @Operation(summary = "获取登录配置")
    @GetMapping("/config")
    public CommonResult<Map<String, Object>> config() {
        return CommonResult.success(authService.getPublicConfig());
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
        String err = authForgotPasswordService.sendResetCode(user, ClientIpUtils.resolve(request), reqVO.getCode());
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
