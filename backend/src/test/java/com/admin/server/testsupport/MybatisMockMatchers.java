package com.admin.server.testsupport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import static org.mockito.ArgumentMatchers.any;

/**
 * MyBatis-Plus {@link LambdaQueryWrapper} 的 Mockito 泛型匹配，避免测试里 raw type 警告。
 */
public final class MybatisMockMatchers {

    private MybatisMockMatchers() {
    }

    @SuppressWarnings("unchecked")
    public static <T> LambdaQueryWrapper<T> anyLambdaQueryWrapper() {
        return any(LambdaQueryWrapper.class);
    }
}
