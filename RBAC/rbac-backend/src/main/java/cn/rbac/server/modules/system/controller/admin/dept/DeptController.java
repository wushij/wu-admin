package cn.rbac.server.modules.system.controller.admin.dept;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.dept.DeptDO;
import cn.rbac.server.modules.system.dal.mysql.dept.DeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "部门管理")
@RestController
@RequestMapping("/system/dept")
public class DeptController {
    
    @Resource
    private DeptMapper deptMapper;
    
    @Operation(summary = "获取部门列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('system:dept:list')")
    public CommonResult<List<DeptDO>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<DeptDO> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(DeptDO::getName, name);
        }
        if (status != null) {
            wrapper.eq(DeptDO::getStatus, status);
        }
        List<DeptDO> allDepts = deptMapper.selectList(wrapper);
        // 构建树形结构
        return CommonResult.success(buildDeptTree(allDepts));
    }
    
    /**
     * 构建部门树形结构
     */
    private List<DeptDO> buildDeptTree(List<DeptDO> allDepts) {
        // 按 parentId 分组
        Map<Long, List<DeptDO>> parentMap = allDepts.stream()
                .collect(Collectors.groupingBy(dept -> dept.getParentId() == null ? 0L : dept.getParentId()));
        // 设置 children
        allDepts.forEach(dept -> dept.setChildren(parentMap.get(dept.getId())));
        // 返回顶级节点（parentId 为 null 或 0）
        return parentMap.getOrDefault(0L, new ArrayList<>());
    }
    
    @Operation(summary = "获取部门详情")
    @GetMapping("/get")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<DeptDO> get(@RequestParam Long id) {
        return CommonResult.success(deptMapper.selectById(id));
    }
    
    @Operation(summary = "新增部门")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermission('system:dept:create')")
    public CommonResult<Long> create(@RequestBody DeptCreateReqVO reqVO) {
        DeptDO dept = new DeptDO();
        dept.setName(reqVO.getName());
        dept.setParentId(reqVO.getParentId());
        dept.setSort(reqVO.getSort());
        dept.setLeaderName(reqVO.getLeaderName());
        dept.setPhone(reqVO.getPhone());
        dept.setEmail(reqVO.getEmail());
        dept.setStatus(1);
        deptMapper.insert(dept);
        return CommonResult.success(dept.getId());
    }
    
    @Operation(summary = "修改部门")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> update(@RequestBody DeptUpdateReqVO reqVO) {
        DeptDO dept = deptMapper.selectById(reqVO.getId());
        dept.setName(reqVO.getName());
        dept.setParentId(reqVO.getParentId());
        dept.setSort(reqVO.getSort());
        dept.setLeaderName(reqVO.getLeaderName());
        dept.setPhone(reqVO.getPhone());
        dept.setEmail(reqVO.getEmail());
        dept.setStatus(reqVO.getStatus());
        deptMapper.updateById(dept);
        return CommonResult.success(true);
    }
    
    @Operation(summary = "删除部门")
    @DeleteMapping("/delete")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        deptMapper.deleteById(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "部门回收站分页")
    @GetMapping("/recycle/page")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<PageResult<DeptDO>> recyclePage(PageParam pageParam,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<DeptDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<DeptDO> deletedPage = (Page<DeptDO>) deptMapper.selectDeletedPage(page, name, status);
        return CommonResult.success(PageResult.of(deletedPage.getRecords(), deletedPage.getTotal()));
    }

    @Operation(summary = "恢复部门")
    @PutMapping("/restore")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        int rows = deptMapper.restoreById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站部门不存在");
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "彻底删除部门")
    @DeleteMapping("/delete-permanent")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        int rows = deptMapper.deletePhysicalById(id);
        if (rows == 0) {
            return CommonResult.error(404, "回收站部门不存在");
        }
        return CommonResult.success(true);
    }
    
    @Operation(summary = "更新部门状态")
    @PutMapping("/update-status")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        DeptDO dept = deptMapper.selectById(id);
        dept.setStatus(status);
        deptMapper.updateById(dept);
        return CommonResult.success(true);
    }
    
    @Data
    public static class DeptCreateReqVO {
        private String name;
        private Long parentId;
        private Integer sort;
        private String leaderName;
        private String phone;
        private String email;
    }
    
    @Data
    public static class DeptUpdateReqVO {
        private Long id;
        private String name;
        private Long parentId;
        private Integer sort;
        private String leaderName;
        private String phone;
        private String email;
        private Integer status;
    }
}
