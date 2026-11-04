package com.admin.server.framework.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;
import cn.dev33.satoken.exception.SaJsonConvertException;
import cn.dev33.satoken.json.SaJsonTemplate;
import cn.dev33.satoken.json.SaJsonTemplateForJackson;
import com.admin.server.framework.redis.CompatibleSaTokenDaoForRedisTemplate;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token Cookie、Redis JSON 反序列化与 DAO 兼容配置。
 *
 * <p>1. Sa-Token 只从 {@link SaTokenConfig#getCookie()} 读取 Cookie 属性，
 * 因此在 {@link #customizeCookie()} 中统一设置 HttpOnly / SameSite / Secure。</p>
 * <p>2. Sa-Token 1.39 升级至 1.46 后，旧版 Redis 中的 {@code SaSession} 含有已移除的
 * {@code TokenSign} 类型标记，直接反序列化会触发 {@link SaJsonConvertException}。
 * 此处提供容错型 {@link SaJsonTemplateForJackson}，遇到旧版会话格式无法反序列化时按空会话降级，
 * 由 Sa-Token 自动重建 1.46 格式会话，避免升级窗口期登录或读取会话报错。</p>
 * <p>3. sa-token 1.46 的 Redis DAO 更新会话时使用 KEEPTTL，Redisson 3.52 会错译为
 * {@code PX -2000}，因此替换为兼容实现 {@link CompatibleSaTokenDaoForRedisTemplate}。</p>
 */
@Slf4j
@Configuration
public class SaTokenCookieConfig implements SmartInitializingSingleton {

    private final SaTokenConfig saTokenConfig;
    private final boolean cookieSecure;

    public SaTokenCookieConfig(SaTokenConfig saTokenConfig,
                               @Value("${app.security.cookie-secure:false}") boolean cookieSecure) {
        this.saTokenConfig = saTokenConfig;
        this.cookieSecure = cookieSecure;
    }

    @PostConstruct
    public void customizeCookie() {
        saTokenConfig.getCookie()
                .setPath("/")
                .setHttpOnly(true)
                .setSecure(cookieSecure)
                .setSameSite("Lax");
    }

    @Bean
    public SaJsonTemplate saJsonTemplate() {
        SaJsonTemplate template = new SaJsonTemplateForJackson() {
            @Override
            public <T> T jsonToObject(String jsonStr, Class<T> type) {
                try {
                    return super.jsonToObject(jsonStr, type);
                } catch (SaJsonConvertException e) {
                    log.warn("Sa-Token Redis 旧版会话反序列化失败，按空会话降级重建 (type={}): {}",
                            type != null ? type.getSimpleName() : "unknown", e.getMessage());
                    return null;
                }
            }
        };
        SaManager.setSaJsonTemplate(template);
        return template;
    }

    @Override
    public void afterSingletonsInstantiated() {
        SaTokenDao current = SaManager.getSaTokenDao();
        if (current instanceof SaTokenDaoForRedisTemplate redisDao) {
            CompatibleSaTokenDaoForRedisTemplate compat = new CompatibleSaTokenDaoForRedisTemplate();
            compat.stringRedisTemplate = redisDao.stringRedisTemplate;
            compat.isInit = true;
            SaManager.setSaTokenDao(compat);
            log.info("Sa-Token Redis DAO 已启用 Redisson KEEPTTL 兼容补丁");
        }
    }
}
