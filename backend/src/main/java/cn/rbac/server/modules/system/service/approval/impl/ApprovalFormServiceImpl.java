package cn.rbac.server.modules.system.service.approval.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalApproveReqVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalArchiveReqVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalCreateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalRecordDO;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalFormMapper;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalRecordMapper;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.approval.ApprovalFormService;
import cn.rbac.server.modules.system.service.approval.RegisterApprovalService;
import cn.rbac.server.modules.system.service.config.SystemConfigHelper;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApprovalFormServiceImpl implements ApprovalFormService {

    private static final String NOTICE_BIZ_TYPE_APPROVAL = "APPROVAL";

    @Resource
    private ApprovalFormMapper approvalFormMapper;
    @Resource
    private ApprovalRecordMapper approvalRecordMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private NoticeMapper noticeMapper;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RegisterApprovalService registerApprovalService;
    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Override
    public PageResult<ApprovalFormDO> page(PageParam pageParam, String title, String formType, String status, Long userId) {
        boolean canQueryAll = permissionService.hasRole(userId, "super_admin")
                || permissionService.hasPermission(userId, "system:approval:query");
        LambdaQueryWrapper<ApprovalFormDO> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isEmpty()) {
            wrapper.like(ApprovalFormDO::getTitle, title);
        }
        if (formType != null && !formType.isEmpty()) {
            wrapper.eq(ApprovalFormDO::getFormType, formType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ApprovalFormDO::getStatus, status);
        }
        if (!canQueryAll) {
            wrapper.and(w -> w.eq(ApprovalFormDO::getApplicantUserId, userId)
                    .or()
                    .eq(ApprovalFormDO::getApproverUserId, userId));
        }
        wrapper.orderByDesc(ApprovalFormDO::getCreateTime);
        Page<ApprovalFormDO> page = approvalFormMapper.selectPage(
                new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        fillUserName(page.getRecords());
        return PageResult.of(page.getRecords(), page.getTotal());
    }

    @Override
    public ApprovalFormDO get(Long id) {
        ApprovalFormDO form = approvalFormMapper.selectById(id);
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        fillUserName(Collections.singletonList(form));
        return form;
    }

    @Override
    public List<ApprovalRecordDO> recordList(Long formId) {
        ApprovalFormDO form = approvalFormMapper.selectById(formId);
        List<ApprovalRecordDO> records = approvalRecordMapper.selectList(new LambdaQueryWrapper<ApprovalRecordDO>()
                .eq(ApprovalRecordDO::getFormId, formId)
                .orderByAsc(ApprovalRecordDO::getCreateTime));
        fillRecordOperator(records);
        patchRegisterRecordOperators(form, records);
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApprovalCreateReqVO reqVO, Long applicantUserId) {
        if (reqVO.getApproverUserId() == null || reqVO.getApproverUserId() <= 0) {
            throw new BusinessException(400, "请选择审批人");
        }
        ApprovalFormDO form = new ApprovalFormDO();
        form.setFormNo(generateFormNo());
        form.setFormType(reqVO.getFormType() == null ? "GENERAL" : reqVO.getFormType());
        form.setTitle(reqVO.getTitle());
        form.setContent(reqVO.getContent());
        form.setStatus("SUBMITTED");
        form.setApplicantUserId(applicantUserId);
        form.setApproverUserId(reqVO.getApproverUserId());
        approvalFormMapper.insert(form);
        createRecord(form.getId(), "SUBMIT", reqVO.getContent(), applicantUserId);
        createNotice(reqVO.getApproverUserId(), "审批待处理",
                "你有新的审批单待处理：" + form.getFormNo() + " - " + form.getTitle(), "APPROVAL", form.getId());
        return form.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(ApprovalApproveReqVO reqVO, Long operatorUserId) {
        ApprovalFormDO form = approvalFormMapper.selectById(reqVO.getId());
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        boolean canApproveAny = permissionService.hasRole(operatorUserId, "super_admin");
        boolean isRegisterForm = RegisterApprovalService.FORM_TYPE_REGISTER.equals(form.getFormType());
        boolean canApproveRegister = isRegisterForm
                && permissionService.hasPermission(operatorUserId, "system:approval:approve");
        boolean canApproveAsConfiguredAuditor = isRegisterForm
                && systemConfigHelper.getRegisterAuditorUserIds().contains(operatorUserId);
        if (!canApproveAny
                && !canApproveRegister
                && !canApproveAsConfiguredAuditor
                && (form.getApproverUserId() == null || !form.getApproverUserId().equals(operatorUserId))) {
            throw new BusinessException(403, "仅审批人可操作");
        }
        if (!"SUBMITTED".equals(form.getStatus())) {
            throw new BusinessException(400, "仅待审批状态可审批");
        }
        String action = reqVO.getAction();
        Set<String> actions = new HashSet<>(Arrays.asList("APPROVE", "REJECT"));
        if (!actions.contains(action)) {
            throw new BusinessException(400, "审批动作非法");
        }
        form.setStatus("APPROVE".equals(action) ? "APPROVED" : "REJECTED");
        form.setResultRemark(reqVO.getRemark());
        approvalFormMapper.updateById(form);
        registerApprovalService.applyApprovalResult(form, action);
        createRecord(form.getId(), action, reqVO.getRemark(), operatorUserId);
        if (isRegisterForm) {
            createNotice(form.getApplicantUserId(), "注册审核结果",
                    "你的注册申请已" + ("APPROVE".equals(action) ? "通过，现在可以登录" : "被驳回，请联系管理员")
                            + "（" + form.getFormNo() + "）",
                    "APPROVAL", form.getId());
        } else {
            createNotice(form.getApplicantUserId(), "审批结果通知",
                    "你的审批单已" + ("APPROVE".equals(action) ? "通过" : "驳回") + "：" + form.getFormNo() + " - " + form.getTitle(),
                    "APPROVAL", form.getId());
        }
    }

    @Override
    public void archive(ApprovalArchiveReqVO reqVO, Long operatorUserId) {
        ApprovalFormDO form = approvalFormMapper.selectById(reqVO.getId());
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        if (!"APPROVED".equals(form.getStatus()) && !"REJECTED".equals(form.getStatus())) {
            throw new BusinessException(400, "仅审批结束单据可归档");
        }
        boolean canArchiveAny = permissionService.hasRole(operatorUserId, "super_admin");
        boolean isRegisterForm = RegisterApprovalService.FORM_TYPE_REGISTER.equals(form.getFormType());
        boolean canArchiveRegister = isRegisterForm
                && permissionService.hasPermission(operatorUserId, "system:approval:archive");
        if (!canArchiveAny && !canArchiveRegister && !operatorUserId.equals(form.getApplicantUserId())) {
            throw new BusinessException(403, "仅申请人可归档");
        }
        form.setStatus("ARCHIVED");
        approvalFormMapper.updateById(form);
        createRecord(form.getId(), "ARCHIVE", reqVO.getRemark(), operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ApprovalFormDO form = approvalFormMapper.selectById(id);
        if (form == null) {
            throw new BusinessException(404, "审批单不存在");
        }
        approvalRecordMapper.delete(new LambdaQueryWrapper<ApprovalRecordDO>()
                .eq(ApprovalRecordDO::getFormId, id));
        deleteRelatedNotices(id, false);
        approvalFormMapper.deleteById(id);
    }

    @Override
    public PageResult<ApprovalFormDO> recyclePage(PageParam pageParam, String title, String formType, String status) {
        Page<ApprovalFormDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<ApprovalFormDO> deletedPage = (Page<ApprovalFormDO>) approvalFormMapper.selectDeletedPage(page, title, formType, status);
        fillUserName(deletedPage.getRecords());
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    public void restore(Long id) {
        int rows = approvalFormMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站审批单不存在");
        }
        approvalRecordMapper.restoreByFormId(id);
    }

    @Override
    public void deletePermanent(Long id) {
        approvalRecordMapper.deletePhysicalByFormId(id);
        deleteRelatedNotices(id, true);
        int rows = approvalFormMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站审批单不存在");
        }
    }

    private String generateFormNo() {
        return "AP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private void createRecord(Long formId, String action, String remark, Long operatorUserId) {
        ApprovalRecordDO record = new ApprovalRecordDO();
        record.setFormId(formId);
        record.setOperatorUserId(operatorUserId);
        record.setAction(action);
        record.setRemark(remark);
        approvalRecordMapper.insert(record);
    }

    private void createNotice(Long userId, String title, String content, String bizType, Long bizId) {
        if (userId == null || userId <= 0 || userId.equals(SecurityUtils.getLoginUserIdOrZero())) {
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

    private void deleteRelatedNotices(Long formId, boolean physical) {
        if (formId == null || formId <= 0) {
            return;
        }
        if (physical) {
            noticeMapper.deletePhysicalByBiz(NOTICE_BIZ_TYPE_APPROVAL, formId);
            return;
        }
        noticeMapper.delete(new LambdaQueryWrapper<NoticeDO>()
                .eq(NoticeDO::getBizType, NOTICE_BIZ_TYPE_APPROVAL)
                .eq(NoticeDO::getBizId, formId));
    }

    private void fillUserName(List<ApprovalFormDO> forms) {
        Set<Long> userIds = forms.stream()
                .flatMap(form -> java.util.stream.Stream.of(form.getApplicantUserId(), form.getApproverUserId()))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<UserDO> users = userMapper.selectByIds(userIds);
        Map<Long, String> userMap = (users != null ? users : Collections.<UserDO>emptyList()).stream()
                .collect(Collectors.toMap(UserDO::getId, UserDO::getUsername, (a, b) -> a));
        forms.forEach(form -> {
            form.setApplicantName(resolveApplicantName(form, userMap));
            form.setApproverName(userMap.getOrDefault(form.getApproverUserId(), "-"));
        });
    }

    private String resolveApplicantName(ApprovalFormDO form, Map<Long, String> userMap) {
        Long applicantUserId = form.getApplicantUserId();
        if (applicantUserId != null && applicantUserId > 0) {
            String name = userMap.get(applicantUserId);
            if (name != null) {
                return name;
            }
        }
        if (RegisterApprovalService.FORM_TYPE_REGISTER.equals(form.getFormType())) {
            String fromContent = RegisterApprovalServiceImpl.displayNameFromContent(form.getContent());
            if (fromContent != null) {
                return fromContent;
            }
        }
        return "-";
    }

    private void patchRegisterRecordOperators(ApprovalFormDO form, List<ApprovalRecordDO> records) {
        if (form == null || records == null || records.isEmpty()
                || !RegisterApprovalService.FORM_TYPE_REGISTER.equals(form.getFormType())) {
            return;
        }
        String registerName = RegisterApprovalServiceImpl.displayNameFromContent(form.getContent());
        if (registerName == null) {
            return;
        }
        for (ApprovalRecordDO record : records) {
            if ("SUBMIT".equals(record.getAction()) && isBlankOrDash(record.getOperatorName())) {
                record.setOperatorName(registerName);
            }
        }
    }

    private boolean isBlankOrDash(String name) {
        return name == null || name.isBlank() || "-".equals(name);
    }

    private void fillRecordOperator(List<ApprovalRecordDO> records) {
        Set<Long> userIds = records.stream()
                .map(ApprovalRecordDO::getOperatorUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<UserDO> users = userMapper.selectByIds(userIds);
        Map<Long, String> userMap = (users != null ? users : Collections.<UserDO>emptyList()).stream()
                .collect(Collectors.toMap(UserDO::getId, UserDO::getUsername, (a, b) -> a));
        records.forEach(record -> record.setOperatorName(userMap.getOrDefault(record.getOperatorUserId(), "-")));
    }
}