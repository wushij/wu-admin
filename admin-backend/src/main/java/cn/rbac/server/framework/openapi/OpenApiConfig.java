package cn.rbac.server.framework.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 默认服务地址：Knife4j 调试时走网关 /api，避免请求发到前端 3000 同源导致 404/HTML。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(
            @Value("${app.api-docs.server-url:http://localhost:8080/api}") String serverUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("Admin Platform API")
                        .description("RBAC 管理端接口（经网关请使用 /api 前缀）")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url(serverUrl).description("开发环境（推荐：网关）"),
                        new Server().url("http://127.0.0.1:8081").description("直连后端（无 /api 前缀）")
                ));
    }
}
