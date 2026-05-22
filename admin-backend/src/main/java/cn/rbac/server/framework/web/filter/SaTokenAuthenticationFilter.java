package cn.rbac.server.framework.web.filter;

import cn.dev33.satoken.stp.StpUtil;
import cn.rbac.server.framework.security.core.service.TokenService;
import cn.rbac.server.modules.system.service.monitor.OnlineUserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * 将 Sa-Token 登录态桥接到 Spring Security（与网关共用同一 token）
 */
@Component
public class SaTokenAuthenticationFilter extends OncePerRequestFilter {

    @jakarta.annotation.Resource
    private OnlineUserService onlineUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                onlineUserService.touchLastAccess(userId);
                Object usernameObj = StpUtil.getSession().get(TokenService.SESSION_USERNAME);
                String username = usernameObj != null ? usernameObj.toString() : String.valueOf(userId);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                authentication.setDetails(userId);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
