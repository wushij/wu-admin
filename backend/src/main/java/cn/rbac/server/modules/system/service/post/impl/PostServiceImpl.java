package cn.rbac.server.modules.system.service.post.impl;

import cn.rbac.server.common.pojo.BusinessException;
import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.post.PostDO;
import cn.rbac.server.modules.system.dal.dataobject.user.UserPostDO;
import cn.rbac.server.modules.system.dal.mysql.post.PostMapper;
import cn.rbac.server.modules.system.dal.mysql.user.UserPostMapper;
import cn.rbac.server.modules.system.service.post.PostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, PostDO> implements PostService {

    @Resource
    private UserPostMapper userPostMapper;

    @Override
    public List<PostDO> tree() {
        List<PostDO> list = list(new LambdaQueryWrapper<PostDO>().orderByAsc(PostDO::getSort));
        fillUserCount(list);
        return buildTree(list, 0L);
    }

    @Override
    public List<PostDO> listEnabled() {
        return list(new LambdaQueryWrapper<PostDO>()
                .eq(PostDO::getStatus, 1)
                .orderByAsc(PostDO::getSort));
    }

    @Override
    public PostDO getById(Long id) {
        PostDO post = super.getById(id);
        if (post == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        return post;
    }

    @Override
    public void create(PostDO post) {
        assertPostCodeUnique(post.getPostCode(), null);
        if (post.getParentId() == null) {
            post.setParentId(0L);
        }
        if (post.getStatus() == null) {
            post.setStatus(1);
        }
        save(post);
    }

    @Override
    public void update(PostDO post) {
        if (super.getById(post.getId()) == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        assertPostCodeUnique(post.getPostCode(), post.getId());
        Long parentId = post.getParentId();
        if (parentId != null && parentId.equals(post.getId())) {
            throw new BusinessException("上级岗位不能选择自己");
        }
        updateById(post);
    }

    @Override
    public void delete(Long id) {
        long childCount = count(new LambdaQueryWrapper<PostDO>().eq(PostDO::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("存在子岗位，无法删除");
        }
        long userCount = userPostMapper.selectCount(new LambdaQueryWrapper<UserPostDO>().eq(UserPostDO::getPostId, id));
        if (userCount > 0) {
            throw new BusinessException("岗位下仍有 " + userCount + " 名关联用户，无法删除");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, Long parentId) {
        PostDO post = super.getById(id);
        if (post == null) {
            throw new BusinessException(404, "岗位不存在");
        }
        parentId = parentId == null ? 0L : parentId;
        if (id.equals(parentId)) {
            throw new BusinessException("不能移动到自身");
        }
        if (parentId > 0 && isDescendant(id, parentId)) {
            throw new BusinessException("不能移动到子岗位下");
        }
        post.setParentId(parentId);
        updateById(post);
    }

    @Override
    public PageResult<PostDO> recyclePage(PageParam pageParam, String postName, Integer status) {
        Page<PostDO> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        Page<PostDO> deletedPage = (Page<PostDO>) baseMapper.selectDeletedPage(page, postName, status);
        return PageResult.of(deletedPage.getRecords(), deletedPage.getTotal());
    }

    @Override
    public void restore(Long id) {
        int rows = baseMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站岗位不存在");
        }
    }

    @Override
    public void deletePermanent(Long id) {
        int rows = baseMapper.deletePhysicalById(id);
        if (rows == 0) {
            throw new BusinessException(404, "回收站岗位不存在");
        }
    }

    private boolean isDescendant(Long ancestorId, Long nodeId) {
        PostDO node = super.getById(nodeId);
        while (node != null && node.getParentId() != null && node.getParentId() > 0) {
            if (node.getParentId().equals(ancestorId)) {
                return true;
            }
            node = super.getById(node.getParentId());
        }
        return false;
    }

    private void assertPostCodeUnique(String postCode, Long excludeId) {
        LambdaQueryWrapper<PostDO> wrapper = new LambdaQueryWrapper<PostDO>().eq(PostDO::getPostCode, postCode);
        if (excludeId != null) {
            wrapper.ne(PostDO::getId, excludeId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("岗位编码已存在");
        }
    }

    private void fillUserCount(List<PostDO> posts) {
        if (posts.isEmpty()) {
            return;
        }
        List<UserPostDO> links = userPostMapper.selectList(null);
        Map<Long, Long> countMap = links.stream()
                .collect(Collectors.groupingBy(UserPostDO::getPostId, Collectors.counting()));
        posts.forEach(p -> p.setUserCount(countMap.getOrDefault(p.getId(), 0L)));
    }

    private List<PostDO> buildTree(List<PostDO> list, Long parentId) {
        List<PostDO> tree = new ArrayList<>();
        for (PostDO post : list) {
            Long pid = post.getParentId() == null ? 0L : post.getParentId();
            if (pid.equals(parentId)) {
                post.setChildren(buildTree(list, post.getId()));
                tree.add(post);
            }
        }
        return tree;
    }
}
