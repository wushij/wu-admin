package com.admin.server.modules.system.convert;

import com.admin.server.modules.system.api.post.vo.PostRespVO;
import com.admin.server.modules.system.dal.dataobject.post.PostDO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 岗位数据转换器
 */
public final class PostConvert {

    private PostConvert() {
    }

    /**
     * 岗位数据递归复制，防止 children 集合泛型在 BeanUtils 复制时丢失类型
     */
    public static PostRespVO convertPost(PostDO post) {
        if (post == null) {
            return null;
        }
        PostRespVO vo = new PostRespVO();
        vo.setId(post.getId());
        vo.setParentId(post.getParentId());
        vo.setPostCode(post.getPostCode());
        vo.setPostName(post.getPostName());
        vo.setSort(post.getSort());
        vo.setStatus(post.getStatus());
        vo.setRemark(post.getRemark());
        vo.setUserCount(post.getUserCount());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
        if (post.getChildren() != null) {
            List<PostRespVO> childVOs = new ArrayList<>(post.getChildren().size());
            for (PostDO child : post.getChildren()) {
                childVOs.add(convertPost(child));
            }
            vo.setChildren(childVOs);
        }
        return vo;
    }

    public static List<PostRespVO> convertPostList(List<PostDO> postList) {
        if (postList == null || postList.isEmpty()) {
            return Collections.emptyList();
        }
        List<PostRespVO> list = new ArrayList<>(postList.size());
        for (PostDO post : postList) {
            list.add(convertPost(post));
        }
        return list;
    }
}
