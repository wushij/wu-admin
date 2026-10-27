package com.admin.server.framework.security.core.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.admin.server.framework.security.api.RequirePermission;
import com.admin.server.framework.security.api.PermissionApi;
import com.admin.server.framework.security.api.PermissionDeniedException;
import com.admin.server.framework.security.core.RiskPrincipal;
import com.admin.server.framework.security.core.RiskPrincipalResolver;
import com.admin.server.framework.security.core.RiskScoringApi;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link RequirePermission} 的执行器：登录态由 Sa-Token 判定，权限码由 {@link PermissionApi} 判定。
 * <p>判定逻辑与改造前 {@code @PreAuthorize("@ss.hasXxx(...)")} 逐项对应，不改变任何权限语义。
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class RequirePermissionAspect {

    private final PermissionApi permissionApi;
    private final RiskScoringApi riskScoringApi;

    public RequirePermissionAspect(PermissionApi permissionApi, RiskScoringApi riskScoringApi) {
        this.permissionApi = permissionApi;
        this.riskScoringApi = riskScoringApi;
    }

    @Before("@annotation(requirePermission)")
    public void checkMethod(RequirePermission requirePermission) {
        check(requirePermission);
    }

    /** 兼容类级注解；方法上已有注解时不重复校验 */
    @Before("@within(requirePermission) && !@annotation(com.admin.server.framework.security.api.RequirePermission)")
    public void checkType(RequirePermission requirePermission) {
        check(requirePermission);
    }

    private void check(RequirePermission requirePermission) {
        // 未登录统一抛 NotLoginException，由全局异常处理器返回 401
        StpUtil.checkLogin();
        if (requirePermission.type() == RequirePermission.Type.LOGIN) {
            return;
        }
        boolean allowed = switch (requirePermission.type()) {
            case PERMISSION -> permissionApi.hasPermission(requirePermission.value());
            case READ -> permissionApi.hasRead(requirePermission.value());
            case RECYCLE_READ -> permissionApi.hasRecycleRead();
            case RECYCLE_RESTORE -> permissionApi.hasRecycleRestore(requirePermission.value());
            case RECYCLE_DELETE -> permissionApi.hasRecycleDelete(requirePermission.value());
            case ROLE -> permissionApi.hasRole(requirePermission.value());
            case LOGIN -> true;
        };
        if (!allowed) {
            recordPermissionDenied(requirePermission);
            throw new PermissionDeniedException("权限不足，无法访问");
        }
    }

    /**
     * 越权探测计分。
     *
     * <p>正常用户偶发一次 403 很常见（比如菜单没刷新），因此单次只加 15 分，
     * 连续撞接口才会在短时间内累积到处置阈值。</p>
     */
    private void recordPermissionDenied(RequirePermission requirePermission) {
        try {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
                return;
            }
            HttpServletRequest request = servletAttributes.getRequest();
            RiskPrincipal subject = RiskPrincipalResolver.resolve(request);
            riskScoringApi.addRisk(subject, 15, "PERMISSION_DENIED", "BLOCKED",
                    request.getRequestURI(), request.getMethod(),
                    request.getHeader("User-Agent"),
                    "越权探测：缺少权限 " + requirePermission.value());
        } catch (Exception e) {
            // 记分失败不能影响原本的 403 返回
        }
    }
}
