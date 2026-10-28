package com.admin.server.modules.system.service.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 风险告警推送。
 *
 * <p>封禁、挑战、警告三类事件在落库之外，额外推一条 webhook，
 * 让运维不用盯着后台也能第一时间知道有人在撞接口。
 * 推送失败只记日志，绝不影响主流程，更不允许因为告警阻塞而让风控放行。</p>
 *
 * <p>稳健性约束：</p>
 * <ul>
 *   <li>使用独立的有界线程池且拒绝策略为 {@link ThreadPoolExecutor.AbortPolicy}：
 *       队列打满时直接丢弃并记日志，绝不退化为在请求线程里同步发 HTTP
 *       （全局 @Async 线程池是 CallerRunsPolicy，会把风控过滤器堵死数秒）；</li>
 *   <li>显式关闭重定向，并在推送前复校验目标地址，防止 SSRF；</li>
 *   <li>调用方负责按「主体 + 事件类型」做去重，避免攻击放大成告警风暴。</li>
 * </ul>
 */
@Slf4j
@Component
public class RiskAlertNotifier {

    /** 告警线程池：核心 1、最大 2、队列 100，足够覆盖去重后的正常告警量 */
    private static final int CORE_THREADS = 1;
    private static final int MAX_THREADS = 2;
    private static final int QUEUE_CAPACITY = 100;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            // 关闭重定向：校验过的公网域名也可能 302 到内网地址
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final AtomicInteger threadSeq = new AtomicInteger();

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            CORE_THREADS, MAX_THREADS, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(QUEUE_CAPACITY),
            runnable -> {
                Thread thread = new Thread(runnable, "risk-alert-" + threadSeq.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());

    @Resource
    private SystemConfigHelper systemConfigHelper;

    /**
     * 提交一条告警，立即返回；线程池已满时丢弃并记日志，不阻塞调用线程。
     */
    public void notify(String eventType, String principal, String level, int totalScore,
                       String action, String reason, String uri, String ip) {
        String webhook;
        try {
            webhook = systemConfigHelper.getRiskAlertWebhook();
        } catch (Exception e) {
            log.warn("读取告警 webhook 配置失败: error={}", e.getMessage());
            return;
        }
        if (StrUtil.isBlank(webhook)) {
            return;
        }
        String time = LocalDateTime.now().toString();
        try {
            executor.execute(() -> send(webhook, eventType, principal, level, totalScore,
                    action, reason, uri, ip, time));
        } catch (RejectedExecutionException e) {
            log.warn("风险告警队列已满，已丢弃本次推送: event={}, principal={}", eventType, principal);
        }
    }

    private void send(String webhook, String eventType, String principal, String level, int totalScore,
                      String action, String reason, String uri, String ip, String time) {
        try {
            // 推送前再校验一次：配置可能是旧数据，域名的解析结果也可能已经变化
            URI target = WebhookUrlValidator.validate(webhook);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", eventType);
            payload.put("principal", principal);
            payload.put("level", level);
            payload.put("score", totalScore);
            payload.put("action", action);
            payload.put("reason", reason);
            payload.put("uri", uri);
            payload.put("ip", ip);
            payload.put("time", time);

            String body = JSONUtil.toJsonStr(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(target)
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<Void> response = CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300) {
                // 3xx 也视为失败：已关闭重定向，不会跟随跳转
                log.warn("风险告警推送返回异常状态: webhook={}, status={}",
                        WebhookUrlValidator.mask(webhook), response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IllegalArgumentException e) {
            log.warn("风险告警推送被拒绝: webhook={}, reason={}", WebhookUrlValidator.mask(webhook), e.getMessage());
        } catch (Exception e) {
            log.warn("风险告警推送失败: webhook={}, error={}", WebhookUrlValidator.mask(webhook), e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
