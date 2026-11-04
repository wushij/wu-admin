package com.admin.server.framework.redis;

import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;

/**
 * sa-token 1.46 的 {@link SaTokenDaoForRedisTemplate#update} 走 {@code setStringAndKeepTTL}，
 * 底层使用 Spring Data Redis 的 {@code Expiration.keepTtl()}。
 *
 * <p>本项目 Spring Data Redis 的 ConnectionFactory 由 Redisson 接管，而 Redisson 3.52
 * 会把 KEEPTTL 错译成 {@code SET key value PX -2000 XX}，Redis 直接返回
 * {@code ERR invalid expire time in 'set' command}，导致登录 500。</p>
 *
 * <p>这里改为先读取剩余 TTL，再用普通 SET 写回：语义与 KEEPTTL 等价，
 * 且不依赖底层客户端对 KEEPTTL 的支持。</p>
 */
public class CompatibleSaTokenDaoForRedisTemplate extends SaTokenDaoForRedisTemplate {

    @Override
    public void setStringAndKeepTTL(String key, String value) {
        long timeout = getTimeout(key);
        // timeout 语义：-2 表示 key 不存在，-1 表示无过期时间，>0 表示剩余秒数
        if (timeout > 0) {
            set(key, value, timeout);
        } else {
            set(key, value, -1);
        }
    }
}
