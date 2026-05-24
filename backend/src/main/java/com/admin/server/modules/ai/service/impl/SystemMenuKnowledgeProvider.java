package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.system.dal.dataobject.permission.MenuDO;
import com.admin.server.modules.system.service.permission.PermissionService;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按用户权限生成轻量级菜单知识（L1 动态菜单注入）
 * <p>
 * 复用 PermissionService.getUserMenuList 的权限过滤结果，仅保留目录/菜单（type 1/2、status=1），
 * 输出「顶级：二级、二级…」的紧凑 Markdown 列表；普通用户看不到的菜单不会出现在提示词中，
 * 与 L3 角色分级同理，防止 AI 侧信道泄露无权功能。
 * </p>
 */
@Component
public class SystemMenuKnowledgeProvider {

    /** 单个顶级菜单下最多列出的二级菜单数（防个别目录过长撑爆容量） */
    private static final int MAX_CHILDREN_PER_TOP = 12;

    @Resource
    private PermissionService permissionService;

    /**
     * 生成用户可见菜单结构 Markdown（含二级明细）
     *
     * @return 形如 "- 系统管理：用户管理、角色管理、菜单管理" 的多行文本；无可见菜单时返回空串
     */
    public String buildMenuKnowledge(Long userId) {
        List<MenuDO> menus = loadVisibleMenus(userId);
        if (menus.isEmpty()) {
            return "";
        }
        Map<Long, List<String>> childrenByParent = new LinkedHashMap<>();
        List<MenuDO> topLevels = new ArrayList<>();
        for (MenuDO menu : menus) {
            long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
            if (parentId == 0L) {
                topLevels.add(menu);
            } else {
                childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(menu.getName());
            }
        }
        StringBuilder sb = new StringBuilder();
        for (MenuDO top : topLevels) {
            sb.append("- ").append(top.getName());
            List<String> children = childrenByParent.get(top.getId());
            if (CollUtil.isNotEmpty(children)) {
                List<String> limited = children.size() > MAX_CHILDREN_PER_TOP
                        ? children.subList(0, MAX_CHILDREN_PER_TOP)
                        : children;
                sb.append("：").append(String.join("、", limited));
                if (children.size() > MAX_CHILDREN_PER_TOP) {
                    sb.append(" 等");
                }
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    /**
     * 仅顶级菜单的降级版本（容量截断时使用）
     */
    public String buildTopLevelOnly(Long userId) {
        List<MenuDO> menus = loadVisibleMenus(userId);
        if (menus.isEmpty()) {
            return "";
        }
        List<String> tops = new ArrayList<>();
        for (MenuDO menu : menus) {
            long parentId = menu.getParentId() == null ? 0L : menu.getParentId();
            if (parentId == 0L && StrUtil.isNotBlank(menu.getName())) {
                tops.add(menu.getName());
            }
        }
        return tops.isEmpty() ? "" : "- " + String.join("、", tops);
    }

    /** 用户可见的目录/菜单（getUserMenuList 已过滤禁用与失效父级） */
    private List<MenuDO> loadVisibleMenus(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        List<MenuDO> menus = permissionService.getUserMenuList(userId);
        List<MenuDO> visible = new ArrayList<>(menus.size());
        for (MenuDO menu : menus) {
            boolean isDirOrMenu = menu.getType() != null && menu.getType() != 3;
            boolean enabled = menu.getStatus() == null || menu.getStatus() == 1;
            if (isDirOrMenu && enabled && StrUtil.isNotBlank(menu.getName())) {
                visible.add(menu);
            }
        }
        return visible;
    }
}
