package com.admin.server.modules.system.service.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 风险告警推送。
 *
 * <p>封禁、挑战、警告三类事件在落库之外，额外推一条 webhook，
 * 让运维不用盯着后台也能第一时间知道有人在撞接口。
 * 推送失败只记日志，绝不影响主流程，更不允许因为告警阻塞而让风控放行。</p>
 */
@Slf4j
@Component
public class RiskAlertNotifier {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Async
    public void notify(String eventType, String principal, String level, int totalScore,
                       String action, String reason, String uri, String ip) {
        String webhook = systemConfigHelper.getRiskAlertWebhook();
        if (StrUtil.isBlank(webhook)) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", eventType);
            payload.put("principal", principal);
            payload.put("level", level);
            payload.put("score", totalScore);
            payload.put("action", action);
            payload.put("reason", reason);
            payload.put("uri", uri);
            payload.put("ip", ip);
            payload.put("time", LocalDateTime.now().toString());

            String body = JSONUtil.toJsonStr(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 400) {
                log.warn("风险告警推送返回异常状态: webhook={}, status={}", webhook, response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("风险告警推送失败: webhook={}, error={}", webhook, e.getMessage());
        }
    }
}
