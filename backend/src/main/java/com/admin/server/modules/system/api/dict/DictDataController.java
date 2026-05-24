package com.admin.server.modules.system.api.dict;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.common.util.BeanMappingUtils;
import com.admin.server.modules.system.api.dict.vo.DictDataRespVO;
import com.admin.server.modules.system.dal.dataobject.dict.DictDataDO;
import com.admin.server.modules.system.framework.cache.DictCacheService;
import com.admin.server.modules.system.service.dict.DictDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "字典数据")
@RestController
@RequestMapping("/system/dict-data")
public class DictDataController {

    @Resource
    private DictDataService dictDataService;
    @Resource
    private DictCacheService dictCacheService;

    @PostMapping("/refresh-cache")
    @Operation(summary = "刷新字典 Redis 全量缓存")
    @PreAuthorize("@ss.hasPermission('system:dict:list')")
    public CommonResult<Boolean> refreshCache() {
        dictCacheService.refreshAll();
        return CommonResult.success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "字典数据分页")
    @PreAuthorize("@ss.hasRead('system:dict:list')")
    public CommonResult<PageResult<DictDataRespVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String dictLabel,
            @RequestParam(required = false) Integer status) {
        PageResult<DictDataDO> page = dictDataService.page(pageNo, pageSize, dictType, dictLabel, status);
        return CommonResult.success(BeanMappingUtils.copyPageProperties(page, DictDataRespVO.class));
    }

    @GetMapping("/type/{dictType}")
    @Operation(summary = "按类型查询启用字典数据")
    public CommonResult<List<DictDataRespVO>> listByType(@PathVariable String dictType) {
        List<DictDataDO> list = dictDataService.listByDictType(dictType);
        return CommonResult.success(BeanMappingUtils.copyListProperties(list, DictDataRespVO.class));
    }

    @GetMapping("/batch")
    @Operation(summary = "批量按类型查询启用字典数据")
    public CommonResult<Map<String, List<DictDataRespVO>>> batchByTypes(@RequestParam String types) {
        List<String> typeList = Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        Map<String, List<DictDataDO>> map = dictDataService.batchByTypes(typeList);
        Map<String, List<DictDataRespVO>> resultMap = new HashMap<>();
        if (map != null) {
            for (Map.Entry<String, List<DictDataDO>> entry : map.entrySet()) {
                resultMap.put(entry.getKey(), BeanMappingUtils.copyListProperties(entry.getValue(), DictDataRespVO.class));
            }
        }
        return CommonResult.success(resultMap);
    }

    @GetMapping("/manage/{dictType}")
    @Operation(summary = "管理端按类型查询全部字典数据")
    @PreAuthorize("@ss.hasRead('system:dict:list')")
    public CommonResult<List<DictDataRespVO>> listForManage(@PathVariable String dictType) {
        List<DictDataDO> list = dictDataService.listByDictTypeForManage(dictType);
        return CommonResult.success(BeanMappingUtils.copyListProperties(list, DictDataRespVO.class));
    }

    @GetMapping("/{id}")
    @Operation(summary = "字典数据详情")
    @PreAuthorize("@ss.hasRead('system:dict:list')")
    public CommonResult<DictDataRespVO> detail(@PathVariable Long id) {
        DictDataDO dictData = dictDataService.getById(id);
        return CommonResult.success(BeanMappingUtils.copyProperties(dictData, DictDataRespVO.class));
    }

    @PostMapping
    @Operation(summary = "新增字典数据")
    @PreAuthorize("@ss.hasPermission('system:dict:create')")
    @Log(title = "字典数据", businessType = Log.BusinessType.INSERT)
    public CommonResult<Boolean> create(@RequestBody DictDataDO dictData) {
        dictDataService.create(dictData);
        return CommonResult.success(true);
    }

    @PutMapping
    @Operation(summary = "修改字典数据")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    @Log(title = "字典数据", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@RequestBody DictDataDO dictData) {
        dictDataService.update(dictData);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典数据")
    @PreAuthorize("@ss.hasPermission('system:dict:delete')")
    @Log(title = "字典数据", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        dictDataService.delete(id);
        return CommonResult.success(true);
    }

    @GetMapping("/recycle/page")
    @Operation(summary = "字典数据回收站分页")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<PageResult<DictDataRespVO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String dictLabel) {
        PageResult<DictDataDO> page = dictDataService.recyclePage(pageParam, dictType, dictLabel);
        return CommonResult.success(BeanMappingUtils.copyPageProperties(page, DictDataRespVO.class));
    }

    @PutMapping("/restore")
    @Operation(summary = "恢复字典数据")
    @PreAuthorize("@ss.hasRecycleRestore('system:dict:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        dictDataService.restore(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete-permanent")
    @Operation(summary = "彻底删除字典数据")
    @PreAuthorize("@ss.hasRecycleDelete('system:dict:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        dictDataService.deletePermanent(id);
        return CommonResult.success(true);
    }
}
