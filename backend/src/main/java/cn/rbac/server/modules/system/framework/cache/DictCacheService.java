package cn.rbac.server.modules.system.framework.cache;

import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;
import cn.rbac.server.modules.system.dal.dataobject.dict.DictTypeDO;
import cn.rbac.server.modules.system.dal.mysql.dict.DictDataMapper;
import cn.rbac.server.modules.system.dal.mysql.dict.DictTypeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字典数据全量 Redis 缓存（仅缓存「类型启用 + 数据启用」项）
 */
@Slf4j
@Service
public class DictCacheService {

    public static final String REDIS_DATA_KEY = "cache:sys:dict:data-by-type";
    public static final String REDIS_ENABLED_TYPES_KEY = "cache:sys:dict:enabled-types";

    private static final TypeReference<Map<String, List<DictDataDO>>> DATA_MAP_TYPE =
            new TypeReference<>() {};

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private DictTypeMapper dictTypeMapper;
    @Resource
    private ObjectMapper objectMapper;

    public void refreshAll() {
        try {
            List<DictTypeDO> enabledTypes = dictTypeMapper.selectList(
                    new LambdaQueryWrapper<DictTypeDO>().eq(DictTypeDO::getStatus, 1));
            Set<String> typeCodes = enabledTypes.stream()
                    .map(DictTypeDO::getDictType)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());

            Map<String, List<DictDataDO>> grouped = new LinkedHashMap<>();
            if (!typeCodes.isEmpty()) {
                List<DictDataDO> rows = dictDataMapper.selectList(
                        new LambdaQueryWrapper<DictDataDO>()
                                .eq(DictDataDO::getStatus, 1)
                                .eq(DictDataDO::getDeleted, 0)
                                .in(DictDataDO::getDictType, typeCodes)
                                .orderByAsc(DictDataDO::getSort)
                                .orderByAsc(DictDataDO::getId));
                for (DictDataDO row : rows) {
                    grouped.computeIfAbsent(row.getDictType(), k -> new java.util.ArrayList<>()).add(row);
                }
            }

            String dataJson = objectMapper.writeValueAsString(grouped);
            String typesJson = objectMapper.writeValueAsString(typeCodes);

            redissonClient.getBucket(REDIS_DATA_KEY).set(dataJson);
            redissonClient.getBucket(REDIS_ENABLED_TYPES_KEY).set(typesJson);
            log.info("字典 Redis 缓存已刷新，启用类型 {} 个", typeCodes.size());
        } catch (Exception e) {
            log.error("刷新字典缓存失败", e);
        }
    }

    public boolean isTypeEnabled(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return false;
        }
        try {
            Set<String> types = loadEnabledTypes();
            if (types == null || types.isEmpty()) {
                refreshAll();
                types = loadEnabledTypes();
            }
            return types != null && types.contains(dictType.trim());
        } catch (Exception e) {
            log.warn("读取字典类型缓存失败 dictType={}", dictType, e);
            return fallbackTypeEnabled(dictType);
        }
    }

    public List<DictDataDO> listByDictType(String dictType) {
        if (!StringUtils.hasText(dictType) || !isTypeEnabled(dictType)) {
            return Collections.emptyList();
        }
        String key = dictType.trim();
        try {
            Map<String, List<DictDataDO>> map = loadDataMap();
            if (map == null || map.isEmpty()) {
                refreshAll();
                map = loadDataMap();
            }
            if (map == null) {
                return Collections.emptyList();
            }
            return map.getOrDefault(key, Collections.emptyList());
        } catch (Exception e) {
            log.warn("读取字典数据缓存失败 dictType={}，回退数据库", dictType, e);
            return dictDataMapper.selectEnabledByDictType(key);
        }
    }

    public Map<String, List<DictDataDO>> batchByTypes(List<String> dictTypes) {
        if (CollectionUtils.isEmpty(dictTypes)) {
            return Collections.emptyMap();
        }
        Map<String, List<DictDataDO>> result = new LinkedHashMap<>();
        for (String type : dictTypes) {
            if (StringUtils.hasText(type)) {
                String key = type.trim();
                result.put(key, listByDictType(key));
            }
        }
        return result;
    }

    private Map<String, List<DictDataDO>> loadDataMap() throws Exception {
        RBucket<String> bucket = redissonClient.getBucket(REDIS_DATA_KEY);
        String json = bucket.get();
        if (!StringUtils.hasText(json)) {
            return null;
        }
        return objectMapper.readValue(json, DATA_MAP_TYPE);
    }

    private Set<String> loadEnabledTypes() throws Exception {
        RBucket<String> bucket = redissonClient.getBucket(REDIS_ENABLED_TYPES_KEY);
        String json = bucket.get();
        if (!StringUtils.hasText(json)) {
            return null;
        }
        return objectMapper.readValue(json, new TypeReference<Set<String>>() {});
    }

    private boolean fallbackTypeEnabled(String dictType) {
        DictTypeDO type = dictTypeMapper.selectOne(new LambdaQueryWrapper<DictTypeDO>()
                .eq(DictTypeDO::getDictType, dictType.trim()));
        return type != null && type.getStatus() != null && type.getStatus() == 1;
    }
}
