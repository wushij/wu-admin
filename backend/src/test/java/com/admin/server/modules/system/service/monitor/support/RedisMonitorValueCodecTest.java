package com.admin.server.modules.system.service.monitor.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisMonitorValueCodecTest {

    @Test
    void detectsRedissonStringPrefix() {
        assertTrue(RedisMonitorValueCodec.looksRedissonEncoded("\u0003\u0003[\"sys_yes_no\"]"));
        assertFalse(RedisMonitorValueCodec.looksRedissonEncoded("[\"sys_yes_no\"]"));
        assertFalse(RedisMonitorValueCodec.looksRedissonEncoded("{\"ok\":true}"));
    }

    @Test
    void detectsGarbledListElements() {
        assertTrue(RedisMonitorValueCodec.needsRedissonDecode(
                List.of("\u0003\u0003{\"id\":1}", "{\"id\":2}")));
        assertFalse(RedisMonitorValueCodec.needsRedissonDecode(List.of("{\"id\":1}")));
    }

    @Test
    void detectsShortBinaryScalars() {
        assertTrue(RedisMonitorValueCodec.looksRedissonEncoded("\u00AC\u00ED"));
        assertFalse(RedisMonitorValueCodec.looksRedissonEncoded("128"));
        assertFalse(RedisMonitorValueCodec.isReadableCacheText("\u00AC\u00ED"));
    }

    @Test
    void prefersRedissonForDashboardVisitKeys() {
        assertTrue(RedisMonitorValueCodec.shouldTryRedissonDecode(
                "dashboard:visit:date:2026-06-13", "128"));
        assertFalse(RedisMonitorValueCodec.shouldTryRedissonDecode(
                "satoken:login:session:xxx", "plain-token"));
    }
}
