package com.admin.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API网关启动类
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("==========================================");
        System.out.println("API网关启动成功！");
        System.out.println("网关地址: http://localhost:8080");
        System.out.println("==========================================");
    }
}
