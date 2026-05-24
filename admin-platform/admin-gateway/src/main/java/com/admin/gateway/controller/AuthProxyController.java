package com.admin.gateway.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthProxyController {

    private static final String RBAC_BASE_URL = "http://rbac-server:8081/auth";
    private static final String SESSION_JWT_KEY = "rbacJwtToken";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient = WebClient.builder().build();

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody Map<String, Object> loginBody, ServerWebExchange exchange) {
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (!StringUtils.hasText(xForwardedFor) && exchange.getRequest().getRemoteAddress() != null) {
            xForwardedFor = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        final String clientIp = xForwardedFor;
        final String clientUa = userAgent;

        return webClient.post()
                .uri(RBAC_BASE_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    if (StringUtils.hasText(clientUa)) {
                        headers.set("User-Agent", clientUa);
                    }
                    if (StringUtils.hasText(clientIp)) {
                        headers.set("X-Forwarded-For", clientIp);
                    }
                })
                .bodyValue(loginBody)
                .exchangeToMono(response ->
                        response.bodyToMono(MAP_TYPE).defaultIfEmpty(error(response.statusCode().value(), "登录失败"))
                )
                .onErrorReturn(error(500, "认证服务调用失败"))
                .map(body -> {
                    SaReactorSyncHolder.setContext(exchange);
                    try {
                        return handleLoginResponse(body);
                    } finally {
                        SaReactorSyncHolder.clearContext();
                    }
                });
    }

    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> info() {
        try {
            StpUtil.checkLogin();
            String rbacJwt = StpUtil.getSession().getString(SESSION_JWT_KEY);
            if (rbacJwt == null || rbacJwt.isEmpty()) {
                return Mono.just(ResponseEntity.ok(error(401, "登录信息已失效，请重新登录")));
            }

            return webClient.get()
                    .uri(RBAC_BASE_URL + "/info")
                    .header("Authorization", "Bearer " + rbacJwt)
                    .exchangeToMono(response ->
                            response.bodyToMono(MAP_TYPE).defaultIfEmpty(error(response.statusCode().value(), "获取用户信息失败"))
                    )
                    .onErrorReturn(error(500, "认证服务调用失败"))
                    .map(ResponseEntity::ok);
        } catch (Exception e) {
            return Mono.just(ResponseEntity.ok(error(401, e.getMessage())));
        }
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, Object>>> logout(ServerWebExchange exchange) {
        try {
            if (!StpUtil.isLogin()) {
                return Mono.just(ResponseEntity.ok(success(true, "退出成功")));
            }

            SaSession session = StpUtil.getSession();
            String rbacJwt = session.getString(SESSION_JWT_KEY);
            Long loginId = StpUtil.getLoginIdAsLong();

            Mono<Map<String, Object>> logoutMono;
            if (rbacJwt == null || rbacJwt.isEmpty()) {
                logoutMono = Mono.just(success(true, "退出成功"));
            } else {
                logoutMono = webClient.post()
                        .uri(RBAC_BASE_URL + "/logout")
                        .header("Authorization", "Bearer " + rbacJwt)
                        .exchangeToMono(response ->
                                response.bodyToMono(MAP_TYPE).defaultIfEmpty(success(true, "退出成功"))
                        )
                        .onErrorReturn(success(true, "退出成功"));
            }

            return logoutMono.map(result -> {
                SaReactorSyncHolder.setContext(exchange);
                try {
                    StpUtil.logout(loginId);
                } finally {
                    SaReactorSyncHolder.clearContext();
                }
                return ResponseEntity.ok(result == null ? success(true, "退出成功") : result);
            });
        } catch (Exception e) {
            return Mono.just(ResponseEntity.ok(error(401, e.getMessage())));
        }
    }

    private ResponseEntity<Map<String, Object>> handleLoginResponse(Map<String, Object> body) {
        Object codeObj = body.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : -1;
        if (code != 200) {
            return ResponseEntity.ok(body);
        }

        Object dataObj = body.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap)) {
            return ResponseEntity.ok(error(500, "登录响应格式错误"));
        }

        Object userIdObj = dataMap.get("userId");
        Object rbacJwtObj = dataMap.get("token");
        if (!(userIdObj instanceof Number) || !(rbacJwtObj instanceof String)) {
            return ResponseEntity.ok(error(500, "登录响应缺少用户信息"));
        }

        Long userId = ((Number) userIdObj).longValue();
        String rbacJwt = (String) rbacJwtObj;

        StpUtil.login(userId);
        SaSession session = StpUtil.getSession();
        session.set(SESSION_JWT_KEY, rbacJwt);

        Map<String, Object> resultData = new HashMap<>();
        resultData.putAll((Map<String, Object>) dataMap);
        resultData.put("token", StpUtil.getTokenValue());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", body.getOrDefault("msg", "登录成功"));
        result.put("message", body.getOrDefault("message", "登录成功"));
        result.put("data", resultData);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("msg", message);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> success(Object data, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", message);
        result.put("message", message);
        result.put("data", data);
        return result;
    }
}
