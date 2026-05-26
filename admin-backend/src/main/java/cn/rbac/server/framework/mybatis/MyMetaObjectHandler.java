package cn.rbac.server.framework.mybatis;

import cn.rbac.server.framework.security.core.service.SecurityUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis Plus 自动填充处理器
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String username = SecurityUtils.getLoginUsername();
        String user = username != null ? username : "";
        
        // 自动填充创建时间
        this.setFieldValByName("createTime", now, metaObject);
        // 自动填充更新时间
        this.setFieldValByName("updateTime", now, metaObject);
        // 自动填充创建者
        this.setFieldValByName("creator", user, metaObject);
        // 自动填充更新者
        this.setFieldValByName("updater", user, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String username = SecurityUtils.getLoginUsername();
        String user = username != null ? username : "";
        
        // 自动填充更新时间
        this.setFieldValByName("updateTime", now, metaObject);
        // 自动填充更新者
        this.setFieldValByName("updater", user, metaObject);
    }
}
