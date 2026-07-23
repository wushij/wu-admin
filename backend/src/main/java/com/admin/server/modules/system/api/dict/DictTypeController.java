package com.admin.server.modules.system.api.dict;

import com.admin.server.common.pojo.CommonResult;
import com.admin.server.common.pojo.PageParam;
import com.admin.server.common.pojo.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.system.dal.dataobject.dict.DictDataDO;
import com.admin.server.modules.system.dal.dataobject.dict.DictTypeDO;
import com.admin.server.modules.system.service.dict.DictDataService;
import com.admin.server.modules.system.service.dict.DictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "字典类型")
@RestController
@RequestMapping("/system/dict-type")
public class DictTypeController {

    @Resource
    private DictTypeService dictTypeService;
    @Resource
    private DictDataService dictDataService;

    @GetMapping("/page")
    @Operation(summary = "字典类型分页")
    @PreAuthorize("@ss.hasRead('system:dict:list')")
    public CommonResult<PageResult<DictTypeDO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(dictTypeService.page(pageNo, pageSize, dictName, dictType, status));
    }

    @GetMapping("/list")
    @Operation(summary = "启用字典类型列表")
    @PreAuthorize("@ss.hasRead('system:dict:list')")
    public CommonResult<List<DictTypeDO>> list() {
        return CommonResult.success(dictTypeService.listEnabled());
    }

    @GetMapping("/{id}")
    @Operation(summary = "字典类型详情")
    @PreAuthorize("@ss.hasRead('system:dict:list')")
    public CommonResult<DictTypeDO> detail(@PathVariable Long id) {
        return CommonResult.success(dictTypeService.getById(id));
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "导出字典类型及数据")
    @PreAuthorize("@ss.hasRead('system:dict:list')")
    public CommonResult<Map<String, Object>> export(@PathVariable Long id) {
        DictTypeDO type = dictTypeService.getById(id);
        List<DictDataDO> data = dictDataService.listByDictTypeForManage(type.getDictType());
        Map<String, Object> body = new HashMap<>(2);
        body.put("type", type);
        body.put("data", data);
        return CommonResult.success(body);
    }

    @PostMapping
    @Operation(summary = "新增字典类型")
    @PreAuthorize("@ss.hasPermission('system:dict:create')")
    @Log(title = "字典类型", businessType = Log.BusinessType.INSERT)
    public CommonResult<Boolean> create(@RequestBody DictTypeDO dictType) {
        dictTypeService.create(dictType);
        return CommonResult.success(true);
    }

    @PutMapping
    @Operation(summary = "修改字典类型")
    @PreAuthorize("@ss.hasPermission('system:dict:update')")
    @Log(title = "字典类型", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@RequestBody DictTypeDO dictType) {
        dictTypeService.update(dictType);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典类型（级联删除数据）")
    @PreAuthorize("@ss.hasPermission('system:dict:delete')")
    @Log(title = "字典类型", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        dictTypeService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制字典类型及数据")
    @PreAuthorize("@ss.hasPermission('system:dict:copy')")
    @Log(title = "字典类型", businessType = Log.BusinessType.INSERT)
    public CommonResult<Boolean> copy(@PathVariable Long id) {
        dictTypeService.copy(id);
        return CommonResult.success(true);
    }

    @GetMapping("/recycle/page")
    @Operation(summary = "字典类型回收站分页")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<PageResult<DictTypeDO>> recyclePage(PageParam pageParam,
                                                            @RequestParam(required = false) String dictName,
                                                            @RequestParam(required = false) String dictType) {
        return CommonResult.success(dictTypeService.recyclePage(pageParam, dictName, dictType));
    }

    @PutMapping("/restore")
    @Operation(summary = "恢复字典类型")
    @PreAuthorize("@ss.hasRecycleRestore('system:dict:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        dictTypeService.restore(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete-permanent")
    @Operation(summary = "彻底删除字典类型")
    @PreAuthorize("@ss.hasRecycleDelete('system:dict:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        dictTypeService.deletePermanent(id);
        return CommonResult.success(true);
    }
}
