package com.admin.server.modules.system.service.auth;

import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.email.EmailCodeService;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人中心：邮箱验证重置/修改密码服务
 */
@Service
public class ProfileEmailPasswordService {

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private UserMapper userMapper;

    /**
     * 发送重置密码邮箱验证码
     */
    public String sendResetCode(UserDO user) {
        if (user == null || user.getId() == null) {
            return "用户不存在";
        }
        if (!systemConfigHelper.isEmailEnabled()) {
            return "邮件服务已关闭，请联系管理员开启";
        }
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        if (email.isEmpty() || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return "请先在基本资料中绑定正确的邮箱地址";
        }
        return emailCodeService.sendEmailCode(email, "resetPwd");
    }

    /**
     * 邮箱验证码重置密码并落库
     */
    @Transactional(rollbackFor = Exception.class)
    public String resetPasswordByEmail(UserDO user, String emailCode, String newPassword, String confirmPassword) {
        if (user == null || user.getId() == null) {
            return "用户不存在";
        }
        if (!systemConfigHelper.isEmailEnabled()) {
            return "邮件服务已关闭，请联系管理员开启";
        }
        String email = user.getEmail() == null ? "" : user.getEmail().trim();
        if (email.isEmpty() || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            return "请先在基本资料中绑定正确的邮箱地址";
        }
        String code = emailCode == null ? "" : emailCode.trim();
        if (code.isEmpty()) {
            return "请输入邮箱验证码";
        }

        int minLen = systemConfigHelper.getRegisterMinPasswordLength();
        if (newPassword == null || newPassword.length() < minLen) {
            return "新密码长度不能少于 " + minLen + " 位";
        }
        if (StrUtil.isNotBlank(confirmPassword) && !newPassword.equals(confirmPassword)) {
            return "两次输入的新密码不一致";
        }

        String err = emailCodeService.verifyEmailCode(email, code);
        if (err != null) {
            return err;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return null;
    }
}
