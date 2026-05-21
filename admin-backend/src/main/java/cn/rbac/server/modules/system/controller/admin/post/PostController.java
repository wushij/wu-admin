package cn.rbac.server.modules.system.controller.admin.post;

import cn.rbac.server.common.pojo.CommonResult;
import cn.rbac.server.framework.log.annotation.Log;
import cn.rbac.server.modules.system.dal.dataobject.post.PostDO;
import cn.rbac.server.modules.system.service.post.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Tag(name = "岗位管理")
@RestController
@RequestMapping("/system/post")
public class PostController {

    @Resource
    private PostService postService;

    @GetMapping("/tree")
    @Operation(summary = "岗位树")
    @PreAuthorize("@ss.hasPermission('system:post:list')")
    public CommonResult<List<PostDO>> tree() {
        return CommonResult.success(postService.tree());
    }

    @GetMapping("/list")
    @Operation(summary = "启用岗位扁平列表")
    public CommonResult<List<PostDO>> list() {
        return CommonResult.success(postService.listEnabled());
    }

    @GetMapping("/{id}")
    @Operation(summary = "岗位详情")
    @PreAuthorize("@ss.hasPermission('system:post:list')")
    public CommonResult<PostDO> detail(@PathVariable Long id) {
        return CommonResult.success(postService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增岗位")
    @PreAuthorize("@ss.hasPermission('system:post:create')")
    @Log(title = "岗位管理", businessType = Log.BusinessType.INSERT)
    public CommonResult<Boolean> create(@RequestBody PostDO post) {
        postService.create(post);
        return CommonResult.success(true);
    }

    @PutMapping
    @Operation(summary = "修改岗位")
    @PreAuthorize("@ss.hasPermission('system:post:update')")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@RequestBody PostDO post) {
        postService.update(post);
        return CommonResult.success(true);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除岗位")
    @PreAuthorize("@ss.hasPermission('system:post:delete')")
    @Log(title = "岗位管理", businessType = Log.BusinessType.DELETE)
    public CommonResult<Boolean> delete(@PathVariable Long id) {
        postService.delete(id);
        return CommonResult.success(true);
    }

    @PostMapping("/{id}/move")
    @Operation(summary = "拖拽移动岗位")
    @PreAuthorize("@ss.hasPermission('system:post:update')")
    @Log(title = "岗位管理", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> move(@PathVariable Long id, @RequestParam Long parentId) {
        postService.move(id, parentId);
        return CommonResult.success(true);
    }
}
