package cn.rbac.server.modules.system.service.dict.impl;

import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;
import cn.rbac.server.modules.system.dal.dataobject.dict.DictTypeDO;
import cn.rbac.server.modules.system.dal.mysql.dict.DictDataMapper;
import cn.rbac.server.modules.system.dal.mysql.dict.DictTypeMapper;
import cn.rbac.server.modules.system.service.dict.DictDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DictDataServiceImpl extends ServiceImpl<DictDataMapper, DictDataDO> implements DictDataService {

    @jakarta.annotation.Resource
    private DictTypeMapper dictTypeMapper;

    @Override
    public PageResult<DictDataDO> page(Integer pageNo, Integer pageSize, String dictType, String dictLabel, Integer status) {
        Page<DictDataDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<DictDataDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(dictType), DictDataDO::getDictType, dictType)
                .like(StringUtils.hasText(dictLabel), DictDataDO::getDictLabel, dictLabel)
                .eq(status != null, DictDataDO::getStatus, status)
                .orderByAsc(DictDataDO::getSort)
                .orderByAsc(DictDataDO::getId);
        Page<DictDataDO> result = page(pageParam, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public List<DictDataDO> listByDictType(String dictType) {
        if (!isDictTypeEnabled(dictType)) {
            return Collections.emptyList();
        }
        return baseMapper.selectEnabledByDictType(dictType);
    }

    private boolean isDictTypeEnabled(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            return false;
        }
        DictTypeDO type = dictTypeMapper.selectOne(new LambdaQueryWrapper<DictTypeDO>()
                .eq(DictTypeDO::getDictType, dictType.trim()));
        return type != null && type.getStatus() != null && type.getStatus() == 1;
    }

    @Override
    public List<DictDataDO> listByDictTypeForManage(String dictType) {
        return list(new LambdaQueryWrapper<DictDataDO>()
                .eq(DictDataDO::getDictType, dictType)
                .orderByAsc(DictDataDO::getSort)
                .orderByAsc(DictDataDO::getId));
    }

    @Override
    public DictDataDO getById(Long id) {
        DictDataDO row = super.getById(id);
        if (row == null) {
            throw new IllegalArgumentException("字典数据不存在");
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DictDataDO dictData) {
        assertDictValueUnique(dictData.getDictType(), dictData.getDictValue(), null);
        save(dictData);
        applyDefaultUnique(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DictDataDO dictData) {
        if (super.getById(dictData.getId()) == null) {
            throw new IllegalArgumentException("字典数据不存在");
        }
        assertDictValueUnique(dictData.getDictType(), dictData.getDictValue(), dictData.getId());
        updateById(dictData);
        applyDefaultUnique(dictData);
    }

    @Override
    public void delete(Long id) {
        if (!removeById(id)) {
            throw new IllegalArgumentException("字典数据不存在");
        }
    }

    @Override
    public long countByDictType(String dictType) {
        return count(new LambdaQueryWrapper<DictDataDO>().eq(DictDataDO::getDictType, dictType));
    }

    @Override
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

    private void assertDictValueUnique(String dictType, String dictValue, Long excludeId) {
        LambdaQueryWrapper<DictDataDO> wrapper = new LambdaQueryWrapper<DictDataDO>()
                .eq(DictDataDO::getDictType, dictType)
                .eq(DictDataDO::getDictValue, dictValue);
        if (excludeId != null) {
            wrapper.ne(DictDataDO::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new IllegalArgumentException("同一字典类型下键值已存在");
        }
    }

    private void applyDefaultUnique(DictDataDO dictData) {
        if (dictData.getIsDefault() != null && dictData.getIsDefault() == 1) {
            update(new LambdaUpdateWrapper<DictDataDO>()
                    .eq(DictDataDO::getDictType, dictData.getDictType())
                    .ne(dictData.getId() != null, DictDataDO::getId, dictData.getId())
                    .set(DictDataDO::getIsDefault, 0));
        }
    }
}
