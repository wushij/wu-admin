package com.admin.server.modules.system.service.export;

import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.framework.export.ExportFormat;
import com.admin.server.framework.export.ExportHelper;
import com.admin.server.framework.export.ExportScope;
import com.admin.server.framework.security.core.service.SecurityUtils;
import com.admin.server.modules.system.dal.dataobject.approval.ApprovalFormDO;
import com.admin.server.modules.system.dal.dataobject.loginlog.LoginLogDO;
import com.admin.server.modules.system.dal.dataobject.monitor.ApiAccessLogDO;
import com.admin.server.modules.system.dal.dataobject.operlog.OperLogDO;
import com.admin.server.modules.system.dal.dataobject.permission.RoleDO;
import com.admin.server.common.util.UserDisplayNames;
import com.admin.server.modules.system.dal.dataobject.ticket.TicketDO;
import com.admin.server.modules.system.dal.dataobject.user.UserDO;
import com.admin.server.modules.system.api.monitor.vo.OnlineUserVO;
import com.admin.server.modules.system.dal.mysql.approval.ApprovalFormMapper;
import com.admin.server.modules.system.dal.mysql.permission.RoleMapper;
import com.admin.server.modules.system.dal.mysql.user.UserMapper;
import com.admin.server.modules.system.service.export.row.*;
import com.admin.server.modules.system.dal.dataobject.dict.DictDataDO;
import com.admin.server.modules.system.service.dict.DictDataService;
import com.admin.server.modules.system.service.loginlog.LoginLogService;
import com.admin.server.modules.system.service.monitor.ApiAccessLogService;
import com.admin.server.modules.system.service.monitor.OnlineUserService;
import com.admin.server.modules.system.service.operlog.OperLogService;
import com.admin.server.modules.system.service.permission.PermissionService;
import com.admin.server.modules.system.service.ticket.TicketService;
import com.admin.server.modules.system.service.user.UserService;
import com.admin.server.modules.system.service.approval.RegisterApprovalService;
import com.admin.server.modules.system.service.approval.impl.RegisterApprovalServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ListExportService {

    @Resource
    private UserService userService;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private OperLogService operLogService;
    @Resource
    private TicketService ticketService;
    @Resource
    private ApiAccessLogService apiAccessLogService;
    @Resource
    private OnlineUserService onlineUserService;
    @Resource
    private ApprovalFormMapper approvalFormMapper;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private DictDataService dictDataService;

    private static final String DICT_TICKET_STATUS = "sys_ticket_status";
    private static final String DICT_TICKET_PRIORITY = "sys_ticket_priority";
    private static final String DICT_APPROVAL_FORM_TYPE = "sys_approval_form_type";
    private static final String DICT_APPROVAL_STATUS = "sys_approval_status";

    public void exportUsers(HttpServletResponse response, ExportFormat format, ExportScope scope,
                            Integer pageNo, Integer pageSize,
                            String username, String mobile, Integer status, Long deptId, Long postId)
            throws IOException {
        String filename = ExportHelper.buildFilename("用户列表", format);
        if (scope == ExportScope.PAGE) {
            PageResult<UserDO> page = userService.page(pageParam(pageNo, pageSize),
                    null, username, mobile, status, deptId, postId, null);
            ExportHelper.writeOnce(response, format, filename, UserExportRow.class, "用户列表",
                    toUserRows(page.getList()));
            return;
        }
        ExportHelper.writeBatched(response, format, filename, UserExportRow.class, "用户列表",
                (pn, ps) -> toUserRows(userService.page(pageParam(pn, ps),
                        null, username, mobile, status, deptId, postId, null).getList()));
    }

    public void exportLoginLogs(HttpServletResponse response, ExportFormat format, ExportScope scope,
                                Integer pageNo, Integer pageSize,
                                String username, String ipaddr, Integer status) throws IOException {
        String filename = ExportHelper.buildFilename("登录日志", format);
        if (scope == ExportScope.PAGE) {
            Page<LoginLogDO> page = loginLogService.getPage(
                    pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize, username, ipaddr, status);
            ExportHelper.writeOnce(response, format, filename, LoginLogExportRow.class, "登录日志",
                    toLoginLogRows(page.getRecords()));
            return;
        }
        ExportHelper.writeBatched(response, format, filename, LoginLogExportRow.class, "登录日志",
                (pn, ps) -> toLoginLogRows(
                        loginLogService.getPage(pn, ps, username, ipaddr, status).getRecords()));
    }

    public void exportOperLogs(HttpServletResponse response, ExportFormat format, ExportScope scope,
                               Integer pageNo, Integer pageSize,
                               String title, String operName, Integer status) throws IOException {
        String filename = ExportHelper.buildFilename("操作日志", format);
        if (scope == ExportScope.PAGE) {
            PageResult<OperLogDO> page = operLogService.page(
                    pageNo == null ? 1 : pageNo, pageSize == null ? 10 : pageSize, title, operName, status);
            ExportHelper.writeOnce(response, format, filename, OperLogExportRow.class, "操作日志",
                    toOperLogRows(page.getList()));
            return;
        }
        ExportHelper.writeBatched(response, format, filename, OperLogExportRow.class, "操作日志",
                (pn, ps) -> toOperLogRows(operLogService.page(pn, ps, title, operName, status).getList()));
    }

    public void exportTickets(HttpServletResponse response, ExportFormat format, ExportScope scope,
                              Integer pageNo, Integer pageSize,
                              String title, String status, String priority, Long assigneeUserId) throws IOException {
        Long currentUserId = SecurityUtils.getLoginUserId();
        String filename = ExportHelper.buildFilename("工单列表", format);
        if (scope == ExportScope.PAGE) {
            PageResult<TicketDO> page = ticketService.page(pageParam(pageNo, pageSize),
                    title, status, priority, assigneeUserId, currentUserId);
            ExportHelper.writeOnce(response, format, filename, TicketExportRow.class, "工单列表",
                    toTicketRows(page.getList()));
            return;
        }
        ExportHelper.writeBatched(response, format, filename, TicketExportRow.class, "工单列表",
                (pn, ps) -> toTicketRows(ticketService.page(pageParam(pn, ps),
                        title, status, priority, assigneeUserId, currentUserId).getList()));
    }

    public void exportApprovals(HttpServletResponse response, ExportFormat format, ExportScope scope,
                                Integer pageNo, Integer pageSize,
                                String title, String formType, String status) throws IOException {
        String filename = ExportHelper.buildFilename("审批单列表", format);
        if (scope == ExportScope.PAGE) {
            List<ApprovalFormDO> list = queryApprovals(pageNo, pageSize, title, formType, status);
            ExportHelper.writeOnce(response, format, filename, ApprovalExportRow.class, "审批单列表",
                    toApprovalRows(list));
            return;
        }
        ExportHelper.writeBatched(response, format, filename, ApprovalExportRow.class, "审批单列表",
                (pn, ps) -> toApprovalRows(queryApprovals(pn, ps, title, formType, status)));
    }

    public void exportApiAccess(HttpServletResponse response, ExportFormat format, ExportScope scope,
                                Integer pageNo, Integer pageSize,
                                Long userId, String apiPath, String method, Integer success,
                                LocalDateTime startTime, LocalDateTime endTime) throws IOException {
        String filename = ExportHelper.buildFilename("API访问日志", format);
        if (scope == ExportScope.PAGE) {
            PageResult<ApiAccessLogDO> page = apiAccessLogService.page(
                    pageNo == null ? 1 : pageNo, pageSize == null ? 20 : pageSize,
                    userId, apiPath, method, success, startTime, endTime);
            ExportHelper.writeOnce(response, format, filename, ApiAccessExportRow.class, "API访问日志",
                    toApiAccessRows(page.getList()));
            return;
        }
        ExportHelper.writeBatched(response, format, filename, ApiAccessExportRow.class, "API访问日志",
                (pn, ps) -> toApiAccessRows(apiAccessLogService.page(
                        pn, ps, userId, apiPath, method, success, startTime, endTime).getList()));
    }

    public void exportOnlineUsers(HttpServletResponse response, ExportFormat format, ExportScope scope,
                                  Integer pageNo, Integer pageSize) throws IOException {
        String filename = ExportHelper.buildFilename("在线用户", format);
        List<OnlineUserVO> all = onlineUserService.listOnlineUsers();
        if (scope == ExportScope.PAGE) {
            int pn = pageNo == null || pageNo <= 0 ? 1 : pageNo;
            int ps = pageSize == null || pageSize <= 0 ? 10 : pageSize;
            int from = (pn - 1) * ps;
            List<OnlineUserVO> slice = from >= all.size()
                    ? List.of()
                    : all.subList(from, Math.min(from + ps, all.size()));
            ExportHelper.writeOnce(response, format, filename, OnlineUserExportRow.class, "在线用户",
                    toOnlineUserRows(slice));
            return;
        }
        ExportHelper.writeOnce(response, format, filename, OnlineUserExportRow.class, "在线用户",
                toOnlineUserRows(all));
    }

    private List<ApprovalFormDO> queryApprovals(Integer pageNo, Integer pageSize,
                                                String title, String formType, String status) {
        int pn = pageNo == null || pageNo <= 0 ? 1 : pageNo;
        int ps = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        Long userId = SecurityUtils.getLoginUserIdOrZero();
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
                    .or().eq(ApprovalFormDO::getApproverUserId, userId));
        }
        wrapper.orderByDesc(ApprovalFormDO::getCreateTime);
        Page<ApprovalFormDO> page = approvalFormMapper.selectPage(new Page<>(pn, ps), wrapper);
        fillApprovalUserName(page.getRecords());
        return page.getRecords();
    }

    private void fillApprovalUserName(List<ApprovalFormDO> forms) {
        if (forms == null || forms.isEmpty()) {
            return;
        }
        Set<Long> userIds = forms.stream()
                .flatMap(f -> java.util.stream.Stream.of(f.getApplicantUserId(), f.getApproverUserId()))
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> userMap = new HashMap<>();
        for (UserDO user : userMapper.selectByIds(userIds)) {
            if (user != null) {
                userMap.put(user.getId(), UserDisplayNames.of(user));
            }
        }
        forms.forEach(form -> {
            form.setApplicantName(resolveExportApplicantName(form, userMap));
            form.setApproverName(userMap.getOrDefault(form.getApproverUserId(), "-"));
        });
    }

    private String resolveExportApplicantName(ApprovalFormDO form, Map<Long, String> userMap) {
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

    private PageParam pageParam(Integer pageNo, Integer pageSize) {
        PageParam param = new PageParam();
        param.setPageNo(pageNo == null || pageNo <= 0 ? 1 : pageNo);
        param.setPageSize(pageSize == null || pageSize <= 0 ? 10 : pageSize);
        return param;
    }

    private Map<String, String> dictValueLabelMap(String dictType) {
        List<DictDataDO> items = dictDataService.listByDictType(dictType);
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        for (DictDataDO item : items) {
            if (item != null && StringUtils.hasText(item.getDictValue())) {
                map.put(item.getDictValue().trim(),
                        StringUtils.hasText(item.getDictLabel()) ? item.getDictLabel() : item.getDictValue());
            }
        }
        return map;
    }

    private String dictLabel(Map<String, String> labelMap, String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return labelMap.getOrDefault(value.trim(), value);
    }

    private List<UserExportRow> toUserRows(List<UserDO> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        Set<Long> roleIds = users.stream()
                .filter(u -> u.getRoleIds() != null)
                .flatMap(u -> u.getRoleIds().stream())
                .collect(Collectors.toSet());
        Map<Long, String> roleNameMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            for (RoleDO role : roleMapper.selectByIds(roleIds)) {
                if (role != null) {
                    roleNameMap.put(role.getId(), role.getName());
                }
            }
        }
        List<UserExportRow> rows = new ArrayList<>(users.size());
        for (UserDO user : users) {
            UserExportRow row = new UserExportRow();
            row.setId(user.getId());
            row.setUsername(user.getUsername());
            row.setNickname(user.getNickname());
            row.setMobile(user.getMobile());
            row.setEmail(user.getEmail());
            row.setDeptName(user.getDeptName());
            row.setPostNames(user.getPostNames());
            if (user.getRoleIds() != null && !user.getRoleIds().isEmpty()) {
                row.setRoleNames(user.getRoleIds().stream()
                        .map(roleNameMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("、")));
            }
            row.setStatusText(ExportLabelHelper.userStatus(user.getStatus()));
            row.setCreateTime(ExportLabelHelper.formatDateTime(user.getCreateTime()));
            rows.add(row);
        }
        return rows;
    }

    private List<LoginLogExportRow> toLoginLogRows(List<LoginLogDO> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        List<LoginLogExportRow> rows = new ArrayList<>(logs.size());
        for (LoginLogDO log : logs) {
            LoginLogExportRow row = new LoginLogExportRow();
            row.setId(log.getId());
            row.setUsername(log.getUsername());
            row.setIpaddr(log.getIpaddr());
            row.setLoginLocation(log.getLoginLocation());
            row.setBrowser(log.getBrowser());
            row.setOs(log.getOs());
            row.setStatusText(ExportLabelHelper.successFlag(log.getStatus()));
            row.setMsg(log.getMsg());
            row.setLoginTime(ExportLabelHelper.formatDateTime(log.getLoginTime()));
            rows.add(row);
        }
        return rows;
    }

    private List<OperLogExportRow> toOperLogRows(List<OperLogDO> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        List<OperLogExportRow> rows = new ArrayList<>(logs.size());
        for (OperLogDO log : logs) {
            OperLogExportRow row = new OperLogExportRow();
            row.setId(log.getId());
            row.setTitle(log.getTitle());
            row.setBusinessTypeText(ExportLabelHelper.operBusinessType(log.getBusinessType()));
            row.setOperName(log.getOperName());
            row.setRequestMethod(log.getRequestMethod());
            row.setOperUrl(log.getOperUrl());
            row.setOperIp(log.getOperIp());
            row.setStatusText(ExportLabelHelper.successFlag(log.getStatus()));
            row.setCostTime(log.getCostTime());
            row.setOperTime(ExportLabelHelper.formatDateTime(log.getOperTime()));
            rows.add(row);
        }
        return rows;
    }

    private List<TicketExportRow> toTicketRows(List<TicketDO> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return List.of();
        }
        Map<String, String> statusLabels = dictValueLabelMap(DICT_TICKET_STATUS);
        Map<String, String> priorityLabels = dictValueLabelMap(DICT_TICKET_PRIORITY);
        List<TicketExportRow> rows = new ArrayList<>(tickets.size());
        for (TicketDO ticket : tickets) {
            TicketExportRow row = new TicketExportRow();
            row.setTicketNo(ticket.getTicketNo());
            row.setTitle(ticket.getTitle());
            row.setStatus(dictLabel(statusLabels, ticket.getStatus()));
            row.setPriority(dictLabel(priorityLabels, ticket.getPriority()));
            row.setCreatorName(ticket.getCreatorName());
            row.setAssigneeName(ticket.getAssigneeName());
            row.setDeadline(ExportLabelHelper.formatDateTime(ticket.getDeadline()));
            row.setCreateTime(ExportLabelHelper.formatDateTime(ticket.getCreateTime()));
            rows.add(row);
        }
        return rows;
    }

    private List<ApprovalExportRow> toApprovalRows(List<ApprovalFormDO> forms) {
        if (forms == null || forms.isEmpty()) {
            return List.of();
        }
        Map<String, String> formTypeLabels = dictValueLabelMap(DICT_APPROVAL_FORM_TYPE);
        Map<String, String> statusLabels = dictValueLabelMap(DICT_APPROVAL_STATUS);
        List<ApprovalExportRow> rows = new ArrayList<>(forms.size());
        for (ApprovalFormDO form : forms) {
            ApprovalExportRow row = new ApprovalExportRow();
            row.setFormNo(form.getFormNo());
            row.setTitle(form.getTitle());
            row.setFormType(dictLabel(formTypeLabels, form.getFormType()));
            row.setStatus(dictLabel(statusLabels, form.getStatus()));
            row.setApplicantName(form.getApplicantName());
            row.setApproverName(form.getApproverName());
            row.setCreateTime(ExportLabelHelper.formatDateTime(form.getCreateTime()));
            row.setUpdateTime(ExportLabelHelper.formatDateTime(form.getUpdateTime()));
            rows.add(row);
        }
        return rows;
    }

    private List<ApiAccessExportRow> toApiAccessRows(List<ApiAccessLogDO> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        List<ApiAccessExportRow> rows = new ArrayList<>(logs.size());
        for (ApiAccessLogDO log : logs) {
            ApiAccessExportRow row = new ApiAccessExportRow();
            row.setId(log.getId());
            row.setUsername(log.getUsername());
            row.setMethod(log.getMethod());
            row.setApiPath(log.getApiPath());
            row.setSuccessText(ExportLabelHelper.apiSuccess(log.getSuccess()));
            row.setDurationMs(log.getCostTime());
            row.setClientIp(log.getIp());
            row.setStartTime(ExportLabelHelper.formatDateTime(log.getStartTime()));
            rows.add(row);
        }
        return rows;
    }

    private List<OnlineUserExportRow> toOnlineUserRows(List<OnlineUserVO> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        List<OnlineUserExportRow> rows = new ArrayList<>(users.size());
        for (OnlineUserVO user : users) {
            OnlineUserExportRow row = new OnlineUserExportRow();
            row.setUserId(user.getUserId());
            row.setLoginName(user.getLoginName());
            row.setDeptName(user.getDeptName());
            row.setIpaddr(user.getIpaddr());
            row.setLoginLocation(user.getLoginLocation());
            row.setBrowser(user.getBrowser());
            row.setOs(user.getOs());
            row.setStatusText(ExportLabelHelper.onlineStatus(user.getStatus()));
            row.setLoginTime(user.getLoginTime());
            row.setLastAccessTime(user.getLastAccessTime());
            rows.add(row);
        }
        return rows;
    }
}
