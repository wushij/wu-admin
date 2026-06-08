package cn.rbac.server.modules.system.service.post;

import cn.rbac.server.common.pojo.PageParam;
import cn.rbac.server.common.pojo.PageResult;
import cn.rbac.server.modules.system.dal.dataobject.post.PostDO;

import java.util.List;

public interface PostService {

    List<PostDO> tree();

    List<PostDO> listEnabled();

    PostDO getById(Long id);

    void create(PostDO post);

    void update(PostDO post);

    void delete(Long id);

    void move(Long id, Long parentId);

    PageResult<PostDO> recyclePage(PageParam pageParam, String postName, Integer status);

    void restore(Long id);

    void deletePermanent(Long id);
}
