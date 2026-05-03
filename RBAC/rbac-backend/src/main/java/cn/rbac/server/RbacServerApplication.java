package cn.rbac.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.rbac.server.modules.**.dal.mysql")
public class RbacServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RbacServerApplication.class, args);
        System.out.println("==========================================");
        System.out.println("RBAC权限管理系统启动成功！");
        System.out.println("接口文档: http://localhost:8080/doc.html");
        System.out.println("==========================================");
    }
}
