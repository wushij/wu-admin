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
        System.out.println("Admin Platform 后端启动成功！");
        System.out.println("接口文档: http://localhost:8080/api/doc.html");
        System.out.println("==========================================");
    }
}
