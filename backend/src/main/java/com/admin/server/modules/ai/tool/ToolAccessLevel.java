package com.admin.server.modules.ai.tool;

/**
 * 工具访问级别（设计方案 §5.1 角色分级）
 * <ul>
 *   <li>ADMIN：系统级运行数据，仅管理员角色或具备对应监控权限者可用；</li>
 *   <li>USER：仅限当前用户本人范围的数据，登录即可。</li>
 * </ul>
 */
public enum ToolAccessLevel {
    ADMIN,
    USER
}
