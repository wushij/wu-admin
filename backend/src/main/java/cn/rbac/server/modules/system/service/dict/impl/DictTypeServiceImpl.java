package cn.rbac.server.modules.system.service.dict.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;
import cn.rbac.server.modules.system.dal.dataobject.dict.DictTypeDO;
import cn.rbac.server.modules.system.dal.mysql.dict.DictDataMapper;
import cn.rbac.server.modules.system.dal.mysql.dict.DictTypeMapper;
import cn.rbac.server.modules.system.framework.cache.DictCacheService;
import cn.rbac.server.modules.system.service.dict.DictDataService;
import cn.rbac.server.modules.system.service.dict.DictTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DictTypeServiceImpl extends ServiceImpl<DictTypeMapper, DictTypeDO> implements DictTypeService {

    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private DictDataService dictDataService;
    @Resource
    private DictCacheService dictCacheService;

    @Override
    public PageResult<DictTypeDO> page(Integer pageNo, Integer pageSize, String dictName, String dictType, Integer status) {
        Page<DictTypeDO> pageParam = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<DictTypeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(dictName), DictTypeDO::getDictName, dictName)
                .like(StringUtils.hasText(dictType), DictTypeDO::getDictType, dictType)
                .eq(status != null, DictTypeDO::getStatus, status)
                .orderByDesc(DictTypeDO::getCreateTime);
        Page<DictTypeDO> result = page(pageParam, wrapper);
        result.getRecords().forEach(row ->
                row.setDataCount(dictDataService.countByDictType(row.getDictType())));
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public List<DictTypeDO> listEnabled() {
        return list(new LambdaQueryWrapper<DictTypeDO>()
                .eq(DictTypeDO::getStatus, 1)
                .orderByDesc(DictTypeDO::getCreateTime));
    }

    @Override
    public DictTypeDO getById(Long id) {
        DictTypeDO row = super.getById(id);
        if (row == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        row.setDataCount(dictDataService.countByDictType(row.getDictType()));
        return row;
    }

    @Override
    public void create(DictTypeDO dictType) {
        assertDictTypeUnique(dictType.getDictType(), null);
        save(dictType);
        dictCacheService.refreshAll();
    }

    @Override
    public void update(DictTypeDO dictType) {
        DictTypeDO exist = super.getById(dictType.getId());
        if (exist == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        if (!exist.getDictType().equals(dictType.getDictType())) {
            throw new BusinessException("字典类型编码不允许修改");
        }
        assertDictTypeUnique(dictType.getDictType(), dictType.getId());
        updateById(dictType);
        dictCacheService.refreshAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DictTypeDO row = super.getById(id);
        if (row == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        removeById(id);
        dictDataMapper.delete(new LambdaQueryWrapper<DictDataDO>()
                .eq(DictDataDO::getDictType, row.getDictType()));
        dictCacheService.refreshAll();
    }

    @Override
    public PageResult<DictTypeDO> recyclePage(PageParam pageParam, String dictName, String dictType) {
        Page<DictTypeDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<DictTypeDO> deletedPage = (Page<DictTypeDO>) baseMapper.selectDeletedPage(page, dictName, dictType);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        int rows = baseMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站字典类型不存在");
        }
        dictCacheService.refreshAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermanent(Long id) {
        int rows = baseMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站字典类型不存在");
        }
        dictCacheService.refreshAll();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copy(Long id) {
        DictTypeDO src = super.getById(id);
        if (src == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String newType = src.getDictType() + "_copy_" + suffix;
        if (newType.length() > 100) {
            newType = newType.substring(0, 100);
        }
        DictTypeDO copyType = new DictTypeDO();
        copyType.setDictName(src.getDictName() + "（副本）");
        copyType.setDictType(newType);
        copyType.setStatus(src.getStatus());
        copyType.setRemark(src.getRemark());
        save(copyType);

        List<DictDataDO> dataList = dictDataService.listByDictTypeForManage(src.getDictType());
        for (DictDataDO item : dataList) {
            DictDataDO copy = new DictDataDO();
            copy.setSort(item.getSort());
            copy.setDictLabel(item.getDictLabel());
            copy.setDictValue(item.getDictValue());
            copy.setDictType(newType);
            copy.setCssClass(item.getCssClass());
            copy.setListClass(item.getListClass());
            copy.setIsDefault(item.getIsDefault());
            copy.setStatus(item.getStatus());
            copy.setRemark(item.getRemark());
            dictDataMapper.insert(copy);
        }
        dictCacheService.refreshAll();
    }

    private void assertDictTypeUnique(String dictType, Long excludeId) {
        LambdaQueryWrapper<DictTypeDO> wrapper = new LambdaQueryWrapper<DictTypeDO>()
                .eq(DictTypeDO::getDictType, dictType);
        if (excludeId != null) {
            wrapper.ne(DictTypeDO::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("字典类型编码已存在");
        }
    }
}
