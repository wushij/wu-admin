package com.admin.server.common.util;

import com.admin.server.common.core.PageResult;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对象转换辅助工具类
 */
public final class BeanMappingUtils {

    private BeanMappingUtils() {
    }

    public static <S, T> T copyProperties(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean copy failed", e);
        }
    }

    public static <S, T> List<T> copyListProperties(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            result.add(copyProperties(source, targetClass));
        }
        return result;
    }

    public static <S, T> PageResult<T> copyPageProperties(PageResult<S> sourcePage, Class<T> targetClass) {
        if (sourcePage == null) {
            return PageResult.of(Collections.emptyList(), 0L);
        }
        List<T> targetList = copyListProperties(sourcePage.getList(), targetClass);
        return PageResult.of(targetList, sourcePage.getTotal());
    }
}
