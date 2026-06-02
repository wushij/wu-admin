package cn.rbac.server.modules.system.api.approval;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalRecordDO;
import cn.rbac.server.modules.system.dal.dataobject.notice.NoticeDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserDO;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalFormMapper;
import cn.rbac.server.modules.system.dal.mysql.approval.ApprovalRecordMapper;
import cn.rbac.server.modules.system.dal.mysql.notice.NoticeMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserMapper;
import cn.rbac.server.modules.system.service.approval.RegisterApprovalService;
import cn.rbac.server.modules.system.service.permission.PermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "审批单中心")
@RestController
@RequestMapping("/system/approval")
public class ApprovalFormController {

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

    @Operation(summary = "审批单分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasRead('system:approval:list')")
    public CommonResult<PageResult<ApprovalFormDO>> page(PageParam pageParam,
                                                          @RequestParam(required = false) String title,
                                                          @RequestParam(required = false) String formType,
                                                          @RequestParam(required = false) String status) {
        Long userId = currentUserId();
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
        Page<ApprovalFormDO> page = approvalFormMapper.selectPage(new Page<>(pageParam.getPageNo(), pageParam.getPageSize()), wrapper);
        fillUserName(page.getRecords());
        return CommonResult.success(PageResult.of(page.getRecords(), page.getTotal()));
    }

    @Operation(summary = "审批单详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasRead('system:approval:list')")
    public CommonResult<ApprovalFormDO> get(@RequestParam Long id) {
        ApprovalFormDO form = approvalFormMapper.selectById(id);
        if (form == null) {
            return CommonResult.error(404, "审批单不存在");
        }
        fillUserName(java.util.Collections.singletonList(form));
        return CommonResult.success(form);
    }

    @Operation(summary = "审批单记录")
    @GetMapping("/record/list")
    @PreAuthorize("@ss.hasRead('system:approval:list')")
    public CommonResult<List<ApprovalRecordDO>> recordList(@RequestParam Long formId) {
        List<ApprovalRecordDO> records = approvalRecordMapper.selectList(new LambdaQueryWrapper<ApprovalRecordDO>()
                .eq(ApprovalRecordDO::getFormId, formId)
                .orderByAsc(ApprovalRecordDO::getCreateTime));
        fillRecordOperator(records);
        return CommonResult.success(records);
    }

    @Operation(summary = "提交审批单")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:approval:create')")
    public CommonResult<Long> create(@RequestBody ApprovalCreateReqVO reqVO) {
        if (reqVO.getApproverUserId() == null || reqVO.getApproverUserId() <= 0) {
            return CommonResult.error(400, "请选择审批人");
        }
        ApprovalFormDO form = new ApprovalFormDO();
        form.setFormNo(generateFormNo());
        form.setFormType(reqVO.getFormType() == null ? "GENERAL" : reqVO.getFormType());
        form.setTitle(reqVO.getTitle());
        form.setContent(reqVO.getContent());
        form.setStatus("SUBMITTED");
        form.setApplicantUserId(currentUserId());
        form.setApproverUserId(reqVO.getApproverUserId());
        approvalFormMapper.insert(form);
        createRecord(form.getId(), "SUBMIT", reqVO.getContent());
        createNotice(reqVO.getApproverUserId(), "审批待处理",
                "你有新的审批单待处理：" + form.getFormNo() + " - " + form.getTitle(), "APPROVAL", form.getId());
        return CommonResult.success(form.getId());
    }

    @Operation(summary = "审批操作")
    @PutMapping("/approve")
    @PreAuthorize("@ss.hasPermission('system:approval:approve')")
    public CommonResult<Boolean> approve(@RequestBody ApprovalApproveReqVO reqVO) {
        ApprovalFormDO form = approvalFormMapper.selectById(reqVO.getId());
        if (form == null) {
            return CommonResult.error(404, "审批单不存在");
        }
        Long userId = currentUserId();
        boolean canApproveAny = permissionService.hasRole(userId, "super_admin");
        boolean isRegisterForm = RegisterApprovalService.FORM_TYPE_REGISTER.equals(form.getFormType());
        boolean canApproveRegister = isRegisterForm
                && permissionService.hasPermission(userId, "system:approval:approve");
        if (!canApproveAny && !canApproveRegister
                && (form.getApproverUserId() == null || !form.getApproverUserId().equals(userId))) {
            return CommonResult.error(403, "仅审批人可操作");
        }
        if (!"SUBMITTED".equals(form.getStatus())) {
            return CommonResult.error(400, "仅待审批状态可审批");
        }
        String action = reqVO.getAction();
        Set<String> actions = new HashSet<>(Arrays.asList("APPROVE", "REJECT"));
        if (!actions.contains(action)) {
            return CommonResult.error(400, "审批动作非法");
        }
        form.setStatus("APPROVE".equals(action) ? "APPROVED" : "REJECTED");
        form.setResultRemark(reqVO.getRemark());
        approvalFormMapper.updateById(form);
        registerApprovalService.applyApprovalResult(form, action);
        createRecord(form.getId(), action, reqVO.getRemark());
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
        return CommonResult.success(true);
    }

    @Operation(summary = "归档审批单")
    @PutMapping("/archive")
    @PreAuthorize("@ss.hasPermission('system:approval:archive')")
    public CommonResult<Boolean> archive(@RequestBody ApprovalArchiveReqVO reqVO) {
        ApprovalFormDO form = approvalFormMapper.selectById(reqVO.getId());
        if (form == null) {
            return CommonResult.error(404, "审批单不存在");
        }
        if (!"APPROVED".equals(form.getStatus()) && !"REJECTED".equals(form.getStatus())) {
            return CommonResult.error(400, "仅审批结束单据可归档");
        }
        Long userId = currentUserId();
        boolean canArchiveAny = permissionService.hasRole(userId, "super_admin");
        boolean isRegisterForm = RegisterApprovalService.FORM_TYPE_REGISTER.equals(form.getFormType());
        boolean canArchiveRegister = isRegisterForm
                && permissionService.hasPermission(userId, "system:approval:archive");
        if (!canArchiveAny && !canArchiveRegister && !userId.equals(form.getApplicantUserId())) {
            return CommonResult.error(403, "仅申请人可归档");
        }
        form.setStatus("ARCHIVED");
        approvalFormMapper.updateById(form);
        createRecord(form.getId(), "ARCHIVE", reqVO.getRemark());
        return CommonResult.success(true);
    }

    @Operation(summary = "删除审批单")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:approval:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        ApprovalFormDO form = approvalFormMapper.selectById(id);
        if (form == null) {
            return CommonResult.error(404, "审批单不存在");
        }
        approvalRecordMapper.delete(new LambdaQueryWrapper<ApprovalRecordDO>()
                .eq(ApprovalRecordDO::getFormId, id));
        approvalFormMapper.deleteById(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "审批单回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasPermission('system:approval:delete')")
    public CommonResult<PageResult<ApprovalFormDO>> recyclePage(PageParam pageParam,
                                                                 @RequestParam(required = false) String title,
                                                                 @RequestParam(required = false) String formType,
                                                                 @RequestParam(required = false) String status) {
        Page<ApprovalFormDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<ApprovalFormDO> deletedPage = (Page<ApprovalFormDO>) approvalFormMapper.selectDeletedPage(page, title, formType, status);
        fillUserName(deletedPage.getRecords());
        return CommonResult.success(PageResult.of(deletedPage.getRecords(), deletedPage.getTotal()));
    }

    @Operation(summary = "恢复审批单")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasPermission('system:approval:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        int rows = approvalFormMapper.restoreById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站审批单不存在");
        }
        approvalRecordMapper.restoreByFormId(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除审批单")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasPermission('system:approval:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        approvalRecordMapper.deletePhysicalByFormId(id);
        int rows = approvalFormMapper.deletePhysicalById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站审批单不存在");
        }
        return CommonResult.success(true);
    }

    private String generateFormNo() {
        return "AP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private void createRecord(Long formId, String action, String remark) {
        ApprovalRecordDO record = new ApprovalRecordDO();
        record.setFormId(formId);
        record.setOperatorUserId(currentUserId());
        record.setAction(action);
        record.setRemark(remark);
        approvalRecordMapper.insert(record);
    }

    private void createNotice(Long userId, String title, String content, String bizType, Long bizId) {
        if (userId == null || userId <= 0 || userId.equals(currentUserId())) {
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

    @SuppressWarnings("deprecation")
    private void fillUserName(List<ApprovalFormDO> forms) {
        Set<Long> userIds = forms.stream()
                .flatMap(form -> java.util.stream.Stream.of(form.getApplicantUserId(), form.getApproverUserId()))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<UserDO> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> userMap = (users != null ? users : Collections.<UserDO>emptyList()).stream()
                .collect(Collectors.toMap(UserDO::getId, UserDO::getUsername, (a, b) -> a));
        forms.forEach(form -> {
            form.setApplicantName(userMap.getOrDefault(form.getApplicantUserId(), "-"));
            form.setApproverName(userMap.getOrDefault(form.getApproverUserId(), "-"));
        });
    }

    @SuppressWarnings("deprecation")
    private void fillRecordOperator(List<ApprovalRecordDO> records) {
        Set<Long> userIds = records.stream()
                .map(ApprovalRecordDO::getOperatorUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        List<UserDO> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> userMap = (users != null ? users : Collections.<UserDO>emptyList()).stream()
                .collect(Collectors.toMap(UserDO::getId, UserDO::getUsername, (a, b) -> a));
        records.forEach(record -> record.setOperatorName(userMap.getOrDefault(record.getOperatorUserId(), "-")));
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return 0L;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return Long.parseLong(principal.toString());
    }

    @Data
    public static class ApprovalCreateReqVO {
        private String formType;
        private String title;
        private String content;
        private Long approverUserId;
    }

    @Data
    public static class ApprovalApproveReqVO {
        private Long id;
        private String action;
        private String remark;
    }

    @Data
    public static class ApprovalArchiveReqVO {
        private Long id;
        private String remark;
    }
}
