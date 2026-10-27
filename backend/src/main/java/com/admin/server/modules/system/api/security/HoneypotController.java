package com.admin.server.modules.system.api.security;

import com.admin.server.framework.security.core.HoneypotPaths;
import com.admin.server.modules.system.service.security.HoneypotResult;
import com.admin.server.modules.system.service.security.HoneypotService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 蜜罐统一入口。
 *
 * <p>该 Controller 不出现在 Swagger、不挂权限注解，并且所在前缀下不得存在任何真实接口。
 * 关闭蜜罐时返回与真实不存在路径一致的 404，避免攻击者通过状态码差异判断蜜罐是否开启。</p>
 */
@Hidden
@RestController
@RequestMapping(HoneypotPaths.PREFIX)
public class HoneypotController {

    @Resource
    private HoneypotService honeypotService;

    @RequestMapping(value = "/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
                    RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<Object> trap(HttpServletRequest request) {
        if (!honeypotService.isEnabled()) {
            Map<String, Object> notFound = new LinkedHashMap<>();
            notFound.put("code", 404);
            notFound.put("message", "Not Found");
            return ResponseEntity.status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(notFound);
        }
        String trapId = HoneypotPaths.resolveTrapId(request.getRequestURI());
        HoneypotResult result = honeypotService.hit(request, trapId);
        return ResponseEntity.status(result.getHttpStatus())
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(result.getBody());
    }
}
