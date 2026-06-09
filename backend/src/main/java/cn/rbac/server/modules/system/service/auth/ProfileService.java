package cn.rbac.server.modules.system.service.auth;

import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.api.auth.vo.*;
import cn.rbac.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ProfileService {

    Map<String, Object> getProfile(Long userId);

    void updateProfile(Long userId, ProfileUpdateReqVO reqVO);

    String sendMobileBindSmsCode(Long userId, ProfileMobileBindSmsCodeReqVO reqVO, String clientIp);

    void bindMobile(Long userId, ProfileMobileBindReqVO reqVO);

    void changePassword(Long userId, ChangePasswordReqVO reqVO);

    String sendPasswordResetSmsCode(Long userId, ProfilePasswordSmsCodeReqVO reqVO, String clientIp);

    void resetPasswordBySms(Long userId, ProfilePasswordSmsResetReqVO reqVO);

    String uploadAvatar(Long userId, MultipartFile file);

    PageResult<LoginLogDO> myLoginLogs(Long userId, PageParam pageParam);
}
