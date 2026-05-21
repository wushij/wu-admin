package cn.rbac.server.modules.system.controller.admin.dict;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.modules.system.dal.dataobject.dict.DictDataDO;
import cn.rbac.server.modules.system.service.dict.DictDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "字典数据")
@RestController
@RequestMapping("/system/dict-data")
public class DictDataController {

    @Resource
    private DictDataService dictDataService;

    @GetMapping("/page")
    @Operation(summary = "字典数据分页")
    @PreAuthorize("@ss.hasPermission('system:dict:list')")
    public CommonResult<PageResult<DictDataDO>> page(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String dictLabel,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(dictDataService.page(pageNo, pageSize, dictType, dictLabel, status));
    }

    @GetMapping("/type/{dictType}")
    @Operation(summary = "按类型查询启用字典数据")
    public CommonResult<List<DictDataDO>> listByType(@PathVariable String dictType) {
        return CommonResult.success(dictDataService.listByDictType(dictType));
    }

    @GetMapping("/batch")
    @Operation(summary = "批量按类型查询启用字典数据")
    public CommonResult<Map<String, List<DictDataDO>>> batchByTypes(@RequestParam String types) {
        List<String> typeList = Arrays.stream(types.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return CommonResult.success(dictDataService.batchByTypes(typeList));
    }

    @GetMapping("/manage/{dictType}")
    @Operation(summary = "管理端按类型查询全部字典数据")
    @PreAuthorize("@ss.hasPermission('system:dict:list')")
    public CommonResult<List<DictDataDO>> listForManage(@PathVariable String dictType) {
        return CommonResult.success(dictDataService.listByDictTypeForManage(dictType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "字典数据详情")
    @PreAuthorize("@ss.hasPermission('system:dict:list')")
    public CommonResult<DictDataDO> detail(@PathVariable Long id) {
        return CommonResult.success(dictDataService.getById(id));
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
}
