package com.admin.server.modules.system.api.post;

import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageParam;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.system.api.post.vo.PostRespVO;
import com.admin.server.modules.system.convert.PostConvert;
import com.admin.server.modules.system.dal.dataobject.post.PostDO;
import com.admin.server.modules.system.service.post.PostService;
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
    @PreAuthorize("@ss.hasRead('system:post:list')")
    public CommonResult<List<PostRespVO>> tree() {
        List<PostDO> list = postService.tree();
        return CommonResult.success(PostConvert.convertPostList(list));
    }

    @GetMapping("/list")
    @Operation(summary = "启用岗位扁平列表")
    @PreAuthorize("@ss.hasRead('system:post:list')")
    public CommonResult<List<PostRespVO>> list() {
        List<PostDO> list = postService.listEnabled();
        return CommonResult.success(PostConvert.convertPostList(list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "岗位详情")
    @PreAuthorize("@ss.hasRead('system:post:list')")
    public CommonResult<PostRespVO> detail(@PathVariable Long id) {
        PostDO post = postService.getById(id);
        return CommonResult.success(PostConvert.convertPost(post));
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

    @GetMapping("/recycle/page")
    @Operation(summary = "岗位回收站分页")
    @PreAuthorize("@ss.hasRecycleRead()")
    public CommonResult<PageResult<PostRespVO>> recyclePage(PageParam pageParam,
                                                        @RequestParam(required = false) String postName,
                                                        @RequestParam(required = false) Integer status) {
        PageResult<PostDO> page = postService.recyclePage(pageParam, postName, status);
        return CommonResult.success(PageResult.of(PostConvert.convertPostList(page.getList()), page.getTotal()));
    }

    @PutMapping("/restore")
    @Operation(summary = "恢复岗位")
    @PreAuthorize("@ss.hasRecycleRestore('system:post:delete')")
    public CommonResult<Boolean> restore(@RequestParam Long id) {
        postService.restore(id);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete-permanent")
    @Operation(summary = "彻底删除岗位")
    @PreAuthorize("@ss.hasRecycleDelete('system:post:delete')")
    public CommonResult<Boolean> deletePermanent(@RequestParam Long id) {
        postService.deletePermanent(id);
        return CommonResult.success(true);
    }
}
