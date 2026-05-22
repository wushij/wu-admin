package com.admin.gateway.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.admin.gateway.util.ClientIpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证代理：登录/登出/用户信息由 RBAC 后端签发 Sa-Token，网关与后端共用同一 Redis 校验。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthProxyController {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final String backendAuthBase;
    private final WebClient webClient = WebClient.builder().build();

    public AuthProxyController(@Value("${app.backend.base-url:http://127.0.0.1:8081}") String backendBaseUrl) {
        String b = backendBaseUrl.endsWith("/") ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1) : backendBaseUrl;
        this.backendAuthBase = b + "/auth";
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody Map<String, Object> loginBody,
                                                             ServerWebExchange exchange) {
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String clientIp = ClientIpUtils.resolve(exchange.getRequest());

        return webClient.post()
                .uri(backendAuthBase + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> forwardClientHeaders(exchange, headers, userAgent, clientIp))
                .bodyValue(loginBody)
                .exchangeToMono(response ->
                        response.bodyToMono(MAP_TYPE).defaultIfEmpty(error(response.statusCode().value(), "登录失败"))
                )
                .onErrorReturn(error(500, "认证服务调用失败"))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> info(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(authorization)) {
            return Mono.just(ResponseEntity.ok(error(401, "未登录")));
        }

        return webClient.get()
                .uri(backendAuthBase + "/info")
                .header("Authorization", authorization)
                .exchangeToMono(response ->
                        response.bodyToMono(MAP_TYPE).defaultIfEmpty(error(response.statusCode().value(), "获取用户信息失败"))
                )
                .onErrorReturn(error(500, "认证服务调用失败"))
                .map(ResponseEntity::ok);
    }

    @PostMapping("/force-kick/{userId}")
    public Mono<ResponseEntity<Map<String, Object>>> forceKick(@PathVariable("userId") Long userId) {
        try {
            StpUtil.kickout(userId);
        } catch (Exception ignored) {
        }
        return Mono.just(ResponseEntity.ok(success(true, "已注销会话")));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, Object>>> logout(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(authorization)) {
            return Mono.just(ResponseEntity.ok(success(true, "退出成功")));
        }

        return webClient.post()
                .uri(backendAuthBase + "/logout")
                .header("Authorization", authorization)
                .exchangeToMono(response ->
                        response.bodyToMono(MAP_TYPE).defaultIfEmpty(success(true, "退出成功"))
                )
                .onErrorReturn(success(true, "退出成功"))
                .map(body -> ResponseEntity.ok(body == null ? success(true, "退出成功") : body));
    }

    private static void forwardClientHeaders(ServerWebExchange exchange, HttpHeaders target,
                                             String userAgent, String clientIp) {
        if (StringUtils.hasText(userAgent)) {
            target.set("User-Agent", userAgent);
        }
        if (StringUtils.hasText(clientIp)) {
            target.set("X-Forwarded-For", clientIp);
        }
        copyClientHintHeaders(exchange.getRequest().getHeaders(), target);
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("msg", message);
        result.put("message", message);
        return result;
    }

    private static void copyClientHintHeaders(HttpHeaders source, HttpHeaders target) {
        for (String name : new String[]{
                "Sec-CH-UA-Platform",
                "Sec-CH-UA-Platform-Version",
                "Sec-CH-UA",
                "Sec-CH-UA-Mobile",
                "Sec-CH-UA-Arch"
        }) {
            String value = source.getFirst(name);
            if (StringUtils.hasText(value)) {
                target.set(name, value);
            }
        }
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
