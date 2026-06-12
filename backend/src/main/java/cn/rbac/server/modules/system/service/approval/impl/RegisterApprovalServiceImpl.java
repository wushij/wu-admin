package cn.rbac.server.modules.system.service.approval.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalRecordDO;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.RoleDO;
import cn.rbac.server.modules.system.dal.dataobject.permission.UserRoleDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalFormMapper;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalRecordMapper;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.RoleMapper;
import cn.rbac.server.modules.system.dal.mysql.permission.UserRoleMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserPostMapper;
import cn.rbac.server.modules.system.service.approval.RegisterApprovalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RegisterApprovalServiceImpl implements RegisterApprovalService {

    private static final String BIZ_TYPE = "USER_REGISTER";

    @Resource
    private ApprovalFormMapper approvalFormMapper;
    @Resource
    private ApprovalRecordMapper approvalRecordMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private NoticeMapper noticeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOnRegister(UserDO user) {
        if (user == null || user.getId() == null) {
            return;
        }
        Long pending = approvalFormMapper.selectCount(new LambdaQueryWrapper<ApprovalFormDO>()
                .eq(ApprovalFormDO::getFormType, FORM_TYPE_REGISTER)
                .eq(ApprovalFormDO::getApplicantUserId, user.getId())
                .eq(ApprovalFormDO::getStatus, "SUBMITTED"));
        if (pending != null && pending > 0) {
            return;
        }

        Long approverId = resolveRegisterApproverUserId();
        if (approverId == null || approverId <= 0) {
            approverId = 1L;
        }

        ApprovalFormDO form = new ApprovalFormDO();
        form.setFormNo(generateFormNo());
        form.setFormType(FORM_TYPE_REGISTER);
        form.setTitle("用户注册审核 - " + user.getUsername());
        form.setContent(buildContent(user));
        form.setStatus("SUBMITTED");
        form.setApplicantUserId(user.getId());
        form.setApproverUserId(approverId);
        approvalFormMapper.insert(form);

        ApprovalRecordDO record = new ApprovalRecordDO();
        record.setFormId(form.getId());
        record.setOperatorUserId(user.getId());
        record.setAction("SUBMIT");
        record.setRemark("用户提交注册，等待管理员审核");
        approvalRecordMapper.insert(record);

        createNotice(approverId, "注册待审核",
                "有新用户注册待审核：" + user.getUsername() + "（" + form.getFormNo() + "）",
                "APPROVAL", form.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyApprovalResult(ApprovalFormDO form, String action) {
        if (form == null || !FORM_TYPE_REGISTER.equals(form.getFormType())) {
            return;
        }
        Long userId = parseRegisterUserId(form.getContent());
        if (userId == null || userId <= 0) {
            userId = form.getApplicantUserId();
        }
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        if (user.getStatus() == null || user.getStatus() != 2) {
            return;
        }
        if ("APPROVE".equals(action)) {
            user.setStatus(1);
            userMapper.updateById(user);
        } else if ("REJECT".equals(action)) {
            user.setStatus(3);
            userMapper.updateById(user);
            userRoleMapper.deleteByUserId(user.getId());
            userPostMapper.deleteByUserId(user.getId());
            userMapper.deleteById(user.getId());
        } else {
            return;
        }
    }

    public static Long parseRegisterUserId(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(content);
            if (BIZ_TYPE.equals(obj.getStr("bizType"))) {
                return obj.getLong("userId");
            }
            return obj.getLong("userId");
        } catch (Exception ignored) {
            return null;
        }
    }

    /** 注册审批单 content 中的展示名（用户被驳回删除后仍可从 JSON 还原） */
    public static String displayNameFromContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            JSONObject obj = JSONUtil.parseObj(content);
            if (!BIZ_TYPE.equals(obj.getStr("bizType"))) {
                return null;
            }
            String nickname = obj.getStr("nickname");
            if (nickname != null && !nickname.isBlank()) {
                return nickname.trim();
            }
            String username = obj.getStr("username");
            return username != null && !username.isBlank() ? username.trim() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildContent(UserDO user) {
        JSONObject obj = new JSONObject();
        obj.set("bizType", BIZ_TYPE);
        obj.set("userId", user.getId());
        obj.set("username", user.getUsername());
        obj.set("nickname", user.getNickname());
        obj.set("mobile", user.getMobile());
        return obj.toString();
    }

    private Long resolveRegisterApproverUserId() {
        RoleDO superRole = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getCode, "super_admin")
                .eq(RoleDO::getDeleted, 0)
                .last("LIMIT 1"));
        if (superRole == null) {
            return 1L;
        }
        List<UserRoleDO> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRoleDO>()
                .eq(UserRoleDO::getRoleId, superRole.getId()));
        List<Long> userIds = links.stream().map(UserRoleDO::getUserId).collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return 1L;
        }
        List<UserDO> admins = userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                .in(UserDO::getId, userIds)
                .eq(UserDO::getStatus, 1)
                .eq(UserDO::getDeleted, 0)
                .orderByAsc(UserDO::getId));
        return admins.isEmpty() ? userIds.get(0) : admins.get(0).getId();
    }

    private String generateFormNo() {
        return "RG" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private void createNotice(Long userId, String title, String content, String bizType, Long bizId) {
        if (userId == null || userId <= 0) {
            return;
        }
        NoticeDO notice = new NoticeDO();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setBizType(bizType);
        notice.setBizId(bizId);
        notice.setReadStatus(0);
        noticeMapper.insert(notice);
    }
}
