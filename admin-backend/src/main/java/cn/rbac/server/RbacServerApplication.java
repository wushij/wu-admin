package cn.rbac.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("cn.rbac.server.modules.**.dal.mysql")
@EnableAsync
@EnableScheduling
public class RbacServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RbacServerApplication.class, args);
        System.out.println("==========================================");
        System.out.println("RBAC权限管理系统启动成功！");
        System.out.println("接口文档(直连): http://127.0.0.1:8081/doc.html");
        System.out.println("接口文档(调试推荐): 管理端内嵌，或网关 http://localhost:8080/api/doc.html");
        System.out.println("==========================================");
    }
}
