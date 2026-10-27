package com.admin.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@MapperScan("com.admin.server.modules.**.dal.mysql")
@EnableAsync
@EnableScheduling
public class WuAdminApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(WuAdminApplication.class, args);
        printStartupSuccessBanner(context);
    }

    private static void printStartupSuccessBanner(ConfigurableApplicationContext context) {
        Environment env = context.getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String path = env.getProperty("server.servlet.context-path", "");
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            path = "";
        }

        String ip = "127.0.0.1";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
        }

        String[] activeProfiles = env.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "dev";

        String banner = """
                
                \u001B[32;1m=======================================================\u001B[0m
                \u001B[36;1m __        __ _   _       _       ____   __  __ ___ _   _  \u001B[0m
                \u001B[36;1m \\ \\      / /| | | |     / \\     |  _ \\ |  \\/  |_ _| \\ | | \u001B[0m
                \u001B[36;1m  \\ \\ /\\ / / | | | |    / _ \\    | | | || |\\/| || ||  \\| | \u001B[0m
                \u001B[36;1m   \\ V  V /  | |_| |   / ___ \\   | |_| || |  | || || |\\  | \u001B[0m
                \u001B[36;1m    \\_/\\_/    \\___/   /_/   \\_\\  |____/ |_|  |_|___|_| \\_| \u001B[0m
                
                \u001B[32;1m  ★ Wu Admin · 综合管理平台 —— 后端启动成功！★\u001B[0m
                
                \u001B[33m  > 本地服务入口:\u001B[0m \u001B[1mhttp://localhost:%s%s/\u001B[0m
                \u001B[33m  > 局域网络入口:\u001B[0m \u001B[1mhttp://%s:%s%s/\u001B[0m
                \u001B[33m  > 接口文档地址:\u001B[0m \u001B[34;1mhttp://localhost:%s%s/doc.html\u001B[0m
                \u001B[33m  > 当前激活环境:\u001B[0m \u001B[35;1m%s\u001B[0m
                \u001B[33m  > 安全风控中心:\u001B[0m \u001B[32;1m国密 SM2/SM3/SM4 & 反爬风控防护已启用\u001B[0m
                \u001B[32;1m=======================================================\u001B[0m
                """.formatted(
                port, path,
                ip, port, path,
                port, path,
                profile
        );

        System.out.println(banner);
    }
}
