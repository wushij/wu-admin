package com.admin.server.modules.system.api.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.admin.server.common.pojo.BusinessException;
import com.admin.server.common.pojo.CommonResult;
import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.system.api.auth.vo.*;
import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.admin.server.modules.system.service.auth.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/auth/profile")
public class ProfileController {

    @Resource
    private ProfileService profileService;

    @Operation(summary = "获取当前用户资料")
    @GetMapping
    public CommonResult<Map<String, Object>> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        return CommonResult.success(profileService.getProfile(userId));
    }

    @Operation(summary = "更新当前用户资料")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Boolean> updateProfile(@Validated @RequestBody ProfileUpdateReqVO reqVO) {
        profileService.updateProfile(StpUtil.getLoginIdAsLong(), reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "发送绑定手机号短信验证码（须先完成滑块验证）")
    @PostMapping("/mobile/sms-code")
    public CommonResult<Boolean> sendMobileBindSmsCode(
            @RequestBody ProfileMobileBindSmsCodeReqVO reqVO,
            HttpServletRequest request) {
        StpUtil.checkLogin();
        String err = profileService.sendMobileBindSmsCode(
                StpUtil.getLoginIdAsLong(), reqVO, ClientIpUtils.resolve(request));
        if (err != null) {
            throw new BusinessException(400, err);
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "短信验证绑定/更换手机号")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/mobile")
    public CommonResult<Boolean> bindMobile(@Validated @RequestBody ProfileMobileBindReqVO reqVO) {
        StpUtil.checkLogin();
        profileService.bindMobile(StpUtil.getLoginIdAsLong(), reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "发送绑定/更换邮箱验证码")
    @PostMapping("/email/code")
    public CommonResult<Boolean> sendEmailBindCode(@Validated @RequestBody ProfileEmailCodeReqVO reqVO) {
        StpUtil.checkLogin();
        String err = profileService.sendEmailBindCode(StpUtil.getLoginIdAsLong(), reqVO);
        if (err != null) {
            throw new BusinessException(400, err);
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "邮箱验证码绑定/更换邮箱")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE)
    @PutMapping("/email")
    public CommonResult<Boolean> bindEmail(@Validated @RequestBody ProfileEmailBindReqVO reqVO) {
        StpUtil.checkLogin();
        profileService.bindEmail(StpUtil.getLoginIdAsLong(), reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "修改当前用户密码")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping("/password")
    public CommonResult<Boolean> changePassword(@Validated @RequestBody ChangePasswordReqVO reqVO) {
        profileService.changePassword(StpUtil.getLoginIdAsLong(), reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "发送重置密码短信验证码（须先完成滑块验证）")
    @PostMapping("/password/sms-code")
    public CommonResult<Boolean> sendPasswordResetSmsCode(
            @RequestBody ProfilePasswordSmsCodeReqVO reqVO,
            HttpServletRequest request) {
        StpUtil.checkLogin();
        String err = profileService.sendPasswordResetSmsCode(
                StpUtil.getLoginIdAsLong(), reqVO, ClientIpUtils.resolve(request));
        if (err != null) {
            throw new BusinessException(400, err);
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "短信验证重置当前用户密码")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping("/password/sms-reset")
    public CommonResult<Boolean> resetPasswordBySms(@Validated @RequestBody ProfilePasswordSmsResetReqVO reqVO) {
        StpUtil.checkLogin();
        profileService.resetPasswordBySms(StpUtil.getLoginIdAsLong(), reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "发送重置密码邮箱验证码")
    @PostMapping("/password/email-code")
    public CommonResult<Boolean> sendPasswordResetEmailCode() {
        StpUtil.checkLogin();
        String err = profileService.sendPasswordResetEmailCode(StpUtil.getLoginIdAsLong());
        if (err != null) {
            throw new BusinessException(400, err);
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "邮箱验证重置当前用户密码")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false, isSaveResponseData = false)
    @PutMapping("/password/email-reset")
    public CommonResult<Boolean> resetPasswordByEmail(@Validated @RequestBody ProfilePasswordEmailResetReqVO reqVO) {
        StpUtil.checkLogin();
        profileService.resetPasswordByEmail(StpUtil.getLoginIdAsLong(), reqVO);
        return CommonResult.success(true);
    }

    @Operation(summary = "上传头像")
    @Log(title = "个人中心", businessType = Log.BusinessType.UPDATE, isSaveRequestData = false)
    @PostMapping("/avatar")
    public CommonResult<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = profileService.uploadAvatar(StpUtil.getLoginIdAsLong(), file);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return CommonResult.success(result);
    }

    @Operation(summary = "我的登录记录")
    @GetMapping("/login-logs")
    public CommonResult<PageResult<LoginLogDO>> myLoginLogs(PageParam pageParam) {
        return CommonResult.success(profileService.myLoginLogs(StpUtil.getLoginIdAsLong(), pageParam));
    }
}
