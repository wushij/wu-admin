package cn.rbac.server.modules.system.framework.cache;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.rbac.server.modules.system.dal.dataobject.config.SysConfigGroupDO;
import cn.rbac.server.modules.system.dal.mysql.config.SysConfigGroupMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置分组全量 Redis 缓存（group_code → config JSON 字符串）
 */
@Slf4j
@Service
public class SysConfigCacheService {

    public static final String REDIS_MAP_KEY = "cache:sys:config:groups";

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private SysConfigGroupMapper configGroupMapper;

    public void refreshAll() {
        try {
            List<SysConfigGroupDO> rows = configGroupMapper.selectList(
                    new LambdaQueryWrapper<SysConfigGroupDO>().orderByAsc(SysConfigGroupDO::getId));
            Map<String, String> snapshot = new HashMap<>();
            for (SysConfigGroupDO row : rows) {
                if (row.getGroupCode() != null) {
                    snapshot.put(row.getGroupCode(), StrUtil.nullToEmpty(row.getConfigValue()));
                }
            }
            RMap<String, String> map = redissonClient.getMap(REDIS_MAP_KEY);
            map.clear();
            if (!snapshot.isEmpty()) {
                map.putAll(snapshot);
            }
            log.info("系统配置 Redis 缓存已刷新，共 {} 组", snapshot.size());
        } catch (Exception e) {
            log.error("刷新系统配置缓存失败", e);
        }
    }

    public JSONObject getGroupJson(String groupCode) {
        if (StrUtil.isBlank(groupCode)) {
            return new JSONObject();
        }
        try {
            RMap<String, String> map = redissonClient.getMap(REDIS_MAP_KEY);
            if (map.isEmpty()) {
                refreshAll();
            }
            String json = map.get(groupCode);
            if (json == null) {
                SysConfigGroupDO row = configGroupMapper.selectOne(
                        new LambdaQueryWrapper<SysConfigGroupDO>().eq(SysConfigGroupDO::getGroupCode, groupCode));
                if (row != null && StrUtil.isNotBlank(row.getConfigValue())) {
                    map.put(groupCode, row.getConfigValue());
                    json = row.getConfigValue();
                }
            }
            if (StrUtil.isBlank(json)) {
                return new JSONObject();
            }
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            log.warn("读取配置缓存失败 groupCode={}，回退数据库", groupCode, e);
            return loadGroupJsonFromDb(groupCode);
        }
    }

    private JSONObject loadGroupJsonFromDb(String groupCode) {
        SysConfigGroupDO row = configGroupMapper.selectOne(
                new LambdaQueryWrapper<SysConfigGroupDO>().eq(SysConfigGroupDO::getGroupCode, groupCode));
        if (row == null || StrUtil.isBlank(row.getConfigValue())) {
            return new JSONObject();
        }
        return JSONUtil.parseObj(row.getConfigValue());
    }

    /** 管理端导出全量配置快照（可选） */
    public Map<String, String> snapshotAll() {
        try {
            RMap<String, String> map = redissonClient.getMap(REDIS_MAP_KEY);
            if (map.isEmpty()) {
                refreshAll();
            }
            return new HashMap<>(map.readAllMap());
        } catch (Exception e) {
            log.warn("读取配置缓存快照失败", e);
            return Map.of();
        }
    }
}
