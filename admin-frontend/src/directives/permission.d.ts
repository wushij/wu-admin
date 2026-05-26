import type { App } from 'vue'

/**
 * 权限控制指令
 */
export const permission: any

/**
 * 角色控制指令
 */
export const role: any

/**
 * 注册全局指令
 */
export function setupPermissionDirectives(app: App): void
