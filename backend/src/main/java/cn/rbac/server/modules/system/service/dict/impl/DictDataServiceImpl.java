package cn.rbac.server.modules.system.service.dict.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;
import cn.rbac.server.modules.system.dal.mysql.dict.DictDataMapper;
import cn.rbac.server.modules.system.framework.cache.DictCacheService;
import cn.rbac.server.modules.system.service.dict.DictDataService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class DictDataServiceImpl extends ServiceImpl<DictDataMapper, DictDataDO> implements DictDataService {

    @jakarta.annotation.Resource
    private DictCacheService dictCacheService;

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
        return dictCacheService.listByDictType(dictType);
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
            throw new BusinessException(404, "字典数据不存在");
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(DictDataDO dictData) {
        assertDictValueUnique(dictData.getDictType(), dictData.getDictValue(), null);
        save(dictData);
        applyDefaultUnique(dictData);
        dictCacheService.refreshAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DictDataDO dictData) {
        if (super.getById(dictData.getId()) == null) {
            throw new BusinessException(404, "字典数据不存在");
        }
        assertDictValueUnique(dictData.getDictType(), dictData.getDictValue(), dictData.getId());
        updateById(dictData);
        applyDefaultUnique(dictData);
        dictCacheService.refreshAll();
    }

    @Override
    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(404, "字典数据不存在");
        }
        dictCacheService.refreshAll();
    }

    @Override
    public PageResult<DictDataDO> recyclePage(PageParam pageParam, String dictType, String dictLabel) {
        Page<DictDataDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<DictDataDO> deletedPage = (Page<DictDataDO>) baseMapper.selectDeletedPage(page, dictType, dictLabel);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        int rows = baseMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站字典数据不存在");
        }
        dictCacheService.refreshAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        int rows = baseMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站字典数据不存在");
        }
        dictCacheService.refreshAll();
    }

    @Override
    public long countByDictType(String dictType) {
        return count(new LambdaQueryWrapper<DictDataDO>().eq(DictDataDO::getDictType, dictType));
    }

    @Override
    public Map<String, List<DictDataDO>> batchByTypes(List<String> dictTypes) {
        return dictCacheService.batchByTypes(dictTypes);
    }

    private void assertDictValueUnique(String dictType, String dictValue, Long excludeId) {
        LambdaQueryWrapper<DictDataDO> wrapper = new LambdaQueryWrapper<DictDataDO>()
                .eq(DictDataDO::getDictType, dictType)
                .eq(DictDataDO::getDictValue, dictValue);
        if (excludeId != null) {
            wrapper.ne(DictDataDO::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("同一字典类型下键值已存在");
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
