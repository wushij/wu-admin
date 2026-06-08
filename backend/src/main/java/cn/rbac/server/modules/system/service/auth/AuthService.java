package cn.rbac.server.modules.system.service.auth;

import cn.rbac.server.modules.system.api.auth.vo.LoginReqVO;
import cn.rbac.server.modules.system.api.auth.vo.RegisterReqVO;
import cn.rbac.server.modules.system.api.auth.vo.SmsCodeReqVO;
import cn.rbac.server.modules.system.api.auth.vo.SmsCodeVerifyReqVO;

import java.util.Map;

public interface AuthService {

    /** 生成图片验证码 */
    Map<String, Object> generateCaptcha(String scene, String clientIp);

    /** 获取公开登录配置 */
    Map<String, Object> getPublicConfig();

    /** 账号密码登录 */
    Map<String, Object> loginByAccount(LoginReqVO reqVO, String clientIp, String userAgent);

    /** 短信验证码登录 */
    Map<String, Object> loginBySms(LoginReqVO reqVO, String clientIp, String userAgent);

    /** 发送登录短信验证码 */
    void sendSmsCode(SmsCodeReqVO reqVO, String clientIp);

    /** 核验短信验证码 */
    boolean verifySmsCode(SmsCodeVerifyReqVO reqVO);

    /** 注册 */
    boolean register(RegisterReqVO reqVO, String clientIp);

    /** 获取已登录用户信息 */
    Map<String, Object> getUserInfo(Long userId);

    /** 登出 */
    void logout(Long userId);
}
