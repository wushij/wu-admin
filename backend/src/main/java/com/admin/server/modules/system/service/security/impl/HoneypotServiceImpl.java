package com.admin.server.modules.system.service.security.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.admin.server.common.util.ClientIpUtils;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import com.admin.server.modules.system.service.security.AntiBotRiskService;
import com.admin.server.modules.system.service.security.HoneypotResult;
import com.admin.server.modules.system.service.security.HoneypotService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 蜜罐诱捕服务实现。
 *
 * <p>设计原则：</p>
 * <ol>
 *   <li>只判定、处置、投喂，不读取任何真实业务数据；</li>
 *   <li>诱饵内容全部运行时随机生成，并嵌入 canary 标识，便于追溯泄露来源；</li>
 *   <li>按真实 IP 维度记分与封禁，避免攻击者更换设备标识绕过；</li>
 *   <li>命中后延迟响应，消耗扫描器并发，延迟上限由配置控制。</li>
 * </ol>
 */
@Slf4j
@Service
public class HoneypotServiceImpl implements HoneypotService {

    /** 蜜罐事件类型，与 sys_security_event.event_type 对应 */
    public static final String EVENT_TYPE = "HONEYPOT";

    /** 诱饵里使用的私有地址段，刻意选用通常不会被真实使用的网段，避免误导攻击者扫描真实资产 */
    private static final String DECOY_DB_HOST = "10.255.255.17";
    private static final String DECOY_REDIS_HOST = "10.255.255.23";
    private static final String DECOY_OSS_ENDPOINT = "oss-cn-hangzhou.aliyuncs.com";

    /** 同时处于延迟中的蜜罐请求上限，需明显小于 Tomcat 工作线程数 */
    private static final int MAX_CONCURRENT_TARPIT = 20;

    private final Semaphore tarpitPermits = new Semaphore(MAX_CONCURRENT_TARPIT);

    /**
     * 搜索引擎与可用性监控的 UA 特征：命中只留痕、不封禁，避免误伤 SEO 与站点监控。
     *
     * <p>已知取舍：UA 可被伪造，攻击者伪装成 Googlebot 即可只留痕不封禁。
     * 这是「宁可不误伤 SEO」的有意选择；如需收紧，应叠加反向 DNS 校验（rDNS + 正向确认）。</p>
     */
    private static final List<String> TRUSTED_UA_PREFIXES = List.of(
            "googlebot", "baiduspider", "bingbot", "yandexbot", "sogou web spider",
            "360spider", "bytespider", "uptimerobot", "pingdom", "statuscake", "site24x7"
    );

    @Resource
    private AntiBotRiskService antiBotRiskService;

    @Resource
    private SystemConfigHelper systemConfigHelper;

    @Override
    public boolean isEnabled() {
        return systemConfigHelper.isHoneypotEnabled();
    }

    @Override
    public HoneypotResult hit(HttpServletRequest request, String trapId) {
        String ip = ClientIpUtils.resolve(request);
        String uri = StrUtil.nullToEmpty(request.getRequestURI());
        String method = StrUtil.nullToEmpty(request.getMethod());
        String userAgent = StrUtil.nullToEmpty(request.getHeader("User-Agent"));
        String canary = newCanary();

        Long userId = resolveLoginUserId();

        // 防误伤：内网来源、搜索引擎与可用性监控只留痕不处置。
        // 隐藏链接与 robots.txt 都可能被预渲染、翻译代理、合规扫描器主动抓取，
        // 直接封 24 小时会误伤真实访客与站点监控。
        boolean trustedSource = isPrivateIp(ip) || matchesTrustedUa(userAgent);

        String reason = "蜜罐诱饵命中: trap=" + trapId + ", canary=" + canary
                + (userId != null ? ", uid=" + userId : "")
                + (trustedSource ? ", trusted=1" : "");
        if (reason.length() > 250) {
            reason = reason.substring(0, 250);
        }

        int delta = trustedSource ? 0 : systemConfigHelper.getHoneypotRiskDelta();
        // principal 固定使用真实 IP：现有 AntiBotGateFilter 与 RiskGate 都按 IP 查封禁，
        // 若改用设备标识，攻击者换一个 X-Device-Id 即可绕过。
        String principal = StrUtil.isBlank(ip) ? "unknown" : ip;
        try {
            antiBotRiskService.addRisk(principal, ip, delta, EVENT_TYPE,
                    trustedSource ? "WARNING" : "FORBIDDEN",
                    uri, method, userAgent, reason);
            // 风控总开关关闭即全部失效：此时 addRisk 不再记分，也不应继续写封禁标记，
            // 否则应急关闭风控后仍会产生新的 24 小时封禁
            if (!trustedSource && systemConfigHelper.isRiskEnabled()) {
                antiBotRiskService.blockPrincipal(principal, systemConfigHelper.getHoneypotBlockSeconds(), reason);
            }
        } catch (Exception e) {
            // 蜜罐处置失败必须留痕，否则运营侧会以为没人踩陷阱
            log.error("蜜罐命中处置失败: ip={}, trap={}, error={}", ip, trapId, e.getMessage(), e);
        }

        tarpit();

        log.warn("蜜罐触发: ip={}, trap={}, canary={}, uid={}", ip, trapId, canary, userId);
        return HoneypotResult.of(buildDecoyBody(trapId, canary, ip));
    }

    /**
     * 延迟响应，消耗扫描器的并发。
     *
     * <p>sleep 会占用 Tomcat 工作线程，扫描器并发一高，蜜罐反而会拖垮自身，
     * 因此用信号量限制同时处于延迟中的请求数：拿不到许可的请求不再延迟，直接返回诱饵。</p>
     */
    private void tarpit() {
        int tarpitMs = systemConfigHelper.getHoneypotTarpitMs();
        if (tarpitMs <= 0) {
            return;
        }
        if (!tarpitPermits.tryAcquire()) {
            log.debug("蜜罐延迟并发已达上限 {}，本次跳过延迟", MAX_CONCURRENT_TARPIT);
            return;
        }
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(tarpitMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            tarpitPermits.release();
        }
    }

    /** 是否为内网、回环或链路本地地址 */
    private boolean isPrivateIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        String value = ip.trim();
        if (value.startsWith("10.") || value.startsWith("127.")
                || value.startsWith("192.168.") || value.startsWith("169.254.")) {
            return true;
        }
        if ("::1".equals(value) || value.startsWith("fc") || value.startsWith("fd") || value.startsWith("fe80")) {
            return true;
        }
        if (value.startsWith("172.")) {
            int dot = value.indexOf('.', 4);
            if (dot > 4) {
                try {
                    int second = Integer.parseInt(value.substring(4, dot));
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    /** 是否为搜索引擎或可用性监控 UA */
    private boolean matchesTrustedUa(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return false;
        }
        String lower = userAgent.toLowerCase();
        for (String prefix : TRUSTED_UA_PREFIXES) {
            if (lower.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Long resolveLoginUserId() {        try {
            return cn.dev33.satoken.stp.StpUtil.isLogin() ? cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String newCanary() {
        return "hk" + RandomUtil.randomString("0123456789abcdef", 10);
    }

    /**
     * 构造诱饵响应体，外层结构与项目统一的 CommonResult 保持一致，避免一眼被识破。
     */
    private Map<String, Object> buildDecoyBody(String trapId, String canary, String ip) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 200);
        body.put("message", "成功");
        body.put("data", decoyData(trapId, canary, ip));
        return body;
    }

    private Object decoyData(String trapId, String canary, String ip) {
        String id = trapId == null ? "unknown" : trapId.toLowerCase();
        return switch (id) {
            case "db-backup", "backup", "sql" -> decoySqlDump(canary);
            case "config-export", "config", "settings" -> decoyConfig(canary);
            case "user-dump", "users", "account" -> decoyUsers(canary);
            case "env", "environment" -> decoyEnv(canary);
            case "console", "admin", "debug" -> decoyConsole(canary);
            default -> decoyGeneric(canary, ip);
        };
    }

    /** 伪装成数据库备份导出 */
    private Map<String, Object> decoySqlDump(String canary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("export_id", canary);
        data.put("database", "admin_prod");
        data.put("engine", "MySQL 8.0.36");
        data.put("generated_at", "2026-10-27 03:12:44");
        data.put("total_rows", 18423);
        data.put("sql", """
                SET NAMES utf8mb4;
                SET FOREIGN_KEY_CHECKS = 0;

                -- Table structure for sys_user
                DROP TABLE IF EXISTS `sys_user`;
                CREATE TABLE `sys_user` (
                  `id` bigint NOT NULL AUTO_INCREMENT,
                  `username` varchar(64) NOT NULL,
                  `password` varchar(128) NOT NULL,
                  `nickname` varchar(64) DEFAULT NULL,
                  `dept_id` bigint DEFAULT NULL,
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                INSERT INTO `sys_user` VALUES (1,'admin','$2a$10$%s','超级管理员',1);
                INSERT INTO `sys_user` VALUES (2,'ops','$2a$10$%s','运维账号',2);
                INSERT INTO `sys_user` VALUES (3,'finance','$2a$10$%s','财务账号',3);
                """.formatted(fakeBcrypt(), fakeBcrypt(), fakeBcrypt()));
        return data;
    }

    /** 伪装成配置导出，所有地址均指向不会被真实使用的保留网段 */
    private Map<String, Object> decoyConfig(String canary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("export_id", canary);
        data.put("profile", "prod");
        data.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
        data.put("spring.datasource.url", "jdbc:mysql://" + DECOY_DB_HOST + ":3306/admin_prod?useSSL=false&serverTimezone=Asia/Shanghai");
        data.put("spring.datasource.username", "admin_app");
        data.put("spring.datasource.password", RandomUtil.randomString("Aa1!@#", 18));
        data.put("spring.data.redis.host", DECOY_REDIS_HOST);
        data.put("spring.data.redis.port", 6379);
        data.put("spring.data.redis.password", RandomUtil.randomString("Aa1!@#", 20));
        data.put("app.security.jwt-secret", RandomUtil.randomString("abcdef0123456789", 64));
        data.put("app.security.sm4-secret-key", RandomUtil.randomString("abcdef0123456789", 16));
        data.put("aliyun.oss.endpoint", DECOY_OSS_ENDPOINT);
        data.put("aliyun.oss.bucket", "admin-prod-assets");
        data.put("aliyun.oss.access-key-id", "LTAI" + RandomUtil.randomString("0123456789abcdef", 20));
        data.put("aliyun.oss.access-key-secret", RandomUtil.randomString("Aa1!@#", 30));
        return data;
    }

    /** 伪装成用户数据导出 */
    private Map<String, Object> decoyUsers(String canary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("export_id", canary);
        data.put("total", 3);
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(fakeUser(1, "admin", "超级管理员", "138****0001", "总部"));
        list.add(fakeUser(2, "ops", "运维负责人", "139****0002", "研发部"));
        list.add(fakeUser(3, "finance", "财务专员", "137****0003", "运营部"));
        data.put("list", list);
        return data;
    }

    private Map<String, Object> fakeUser(long id, String username, String nickname, String mobile, String dept) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        user.put("username", username);
        user.put("password", fakeBcrypt());
        user.put("nickname", nickname);
        user.put("mobile", mobile);
        user.put("deptName", dept);
        user.put("status", 0);
        return user;
    }

    /** 伪装成环境变量导出 */
    private Map<String, Object> decoyEnv(String canary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("export_id", canary);
        data.put("JAVA_HOME", "/usr/lib/jvm/java-17-openjdk");
        data.put("SPRING_PROFILES_ACTIVE", "prod");
        data.put("DB_HOST", DECOY_DB_HOST);
        data.put("DB_USER", "admin_app");
        data.put("DB_PASSWORD", RandomUtil.randomString("Aa1!@#", 18));
        data.put("REDIS_HOST", DECOY_REDIS_HOST);
        data.put("REDIS_PASSWORD", RandomUtil.randomString("Aa1!@#", 20));
        data.put("ACCESS_KEY_ID", "LTAI" + RandomUtil.randomString("0123456789abcdef", 20));
        data.put("ACCESS_KEY_SECRET", RandomUtil.randomString("Aa1!@#", 30));
        data.put("SIGN_KEY", RandomUtil.randomString("abcdef0123456789", 64));
        return data;
    }

    /** 伪装成内部接口清单 */
    private Map<String, Object> decoyConsole(String canary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("export_id", canary);
        data.put("version", "2.6.1");
        List<Map<String, String>> endpoints = new ArrayList<>();
        endpoints.add(endpoint("GET", "/api/system/internal/db-backup", "数据库整库备份导出"));
        endpoints.add(endpoint("GET", "/api/system/internal/config-export", "运行时配置导出"));
        endpoints.add(endpoint("GET", "/api/system/internal/user-dump", "用户数据导出"));
        endpoints.add(endpoint("POST", "/api/system/internal/debug/exec", "远程调试指令执行"));
        data.put("endpoints", endpoints);
        return data;
    }

    private Map<String, String> endpoint(String method, String path, String desc) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("method", method);
        item.put("path", path);
        item.put("description", desc);
        return item;
    }

    /** 兜底诱饵：看起来像一个正常的内部接口响应 */
    private Map<String, Object> decoyGeneric(String canary, String ip) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("export_id", canary);
        data.put("status", "ok");
        data.put("client_ip", ip);
        data.put("server_time", System.currentTimeMillis());
        data.put("hint", "internal api");
        return data;
    }

    /** 生成格式合法但无法使用的 BCrypt 密文 */
    private String fakeBcrypt() {
        return RandomUtil.randomString("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789./", 53);
    }
}
