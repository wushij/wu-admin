package cn.rbac.server.modules.system.api.approval;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.framework.security.core.service.SecurityUtils;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalApproveReqVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalArchiveReqVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalApproverOptionVO;
import cn.rbac.server.modules.system.api.approval.vo.ApprovalCreateReqVO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import cn.rbac.server.modules.system.dal.dataobject.approval.ApprovalRecordDO;
import cn.rbac.server.modules.system.service.approval.ApprovalFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "审批单中心")
@RestController
@RequestMapping("/system/approval")
public class ApprovalFormController {

    @Resource
    private ApprovalFormService approvalFormService;

    @Operation(summary = "审批单分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.hasRead('system:approval:list')")
    public CommonResult<PageResult<ApprovalFormDO>> page(PageParam pageParam,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String formType,
            @RequestParam(required = false) String status) {
        return CommonResult.success(approvalFormService.page(
                pageParam, title, formType, status, SecurityUtils.getLoginUserIdOrZero()));
    }

    @Operation(summary = "审批单详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasRead('system:approval:list')")
    public CommonResult<ApprovalFormDO> get(@RequestParam Long id) {
        return CommonResult.success(approvalFormService.get(id));
    }

    @Operation(summary = "审批单记录")
    @GetMapping("/record/list")
    @PreAuthorize("@ss.hasRead('system:approval:list')")
    public CommonResult<List<ApprovalRecordDO>> recordList(@RequestParam Long formId) {
        return CommonResult.success(approvalFormService.recordList(formId));
    }

    @Operation(summary = "审批人下拉（排除当前用户）")
    @GetMapping("/approver-options")
    @PreAuthorize("@ss.hasPermission('system:approval:create')")
    public CommonResult<List<ApprovalApproverOptionVO>> approverOptions() {
        return CommonResult.success(approvalFormService.listApproverOptions(SecurityUtils.getLoginUserIdOrZero()));
    }

    @Log(title = "审批单管理", businessType = Log.BusinessType.INSERT)
    @Operation(summary = "提交审批单")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:approval:create')")
    public CommonResult<Long> create(@Validated @RequestBody ApprovalCreateReqVO reqVO) {
        return CommonResult.success(approvalFormService.create(reqVO, SecurityUtils.getLoginUserIdOrZero()));
    }

    @Log(title = "审批单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "审批操作")
    @PutMapping("/approve")
    @PreAuthorize("@ss.hasPermission('system:approval:approve')")
    public CommonResult<Boolean> approve(@Validated @RequestBody ApprovalApproveReqVO reqVO) {
        approvalFormService.approve(reqVO, SecurityUtils.getLoginUserIdOrZero());
        return CommonResult.success(true);
    }

    @Log(title = "审批单管理", businessType = Log.BusinessType.UPDATE)
    @Operation(summary = "归档审批单")
    @PutMapping("/archive")
    @PreAuthorize("@ss.hasPermission('system:approval:archive')")
    public CommonResult<Boolean> archive(@Validated @RequestBody ApprovalArchiveReqVO reqVO) {
        approvalFormService.archive(reqVO, SecurityUtils.getLoginUserIdOrZero());
        return CommonResult.success(true);
    }

    @Log(title = "审批单管理", businessType = Log.BusinessType.DELETE)
    @Operation(summary = "删除审批单")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:approval:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        approvalFormService.delete(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "审批单回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<PageResult<ApprovalFormDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String formType,
            @RequestParam(required = false) String status) {
        return CommonResult.success(approvalFormService.recyclePage(pageParam, title, formType, status));
    }

    @Operation(summary = "恢复审批单")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasRecycleRestore('system:approval:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        approvalFormService.restore(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除审批单")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasRecycleDelete('system:approval:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        approvalFormService.deletePermanent(id);
        return CommonResult.success(true);
    }
}
