import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useSiteStore } from '@/store/site'
import { hasMenuPermission } from '@/directives/permission'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限访问', requiresAuth: false }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'dashboard' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'User' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'user', permission: 'system:user:list' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'UserFilled', permission: 'system:role:list' }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu', permission: 'system:menu:list' }
      },
      {
        path: 'system/org',
        name: 'SystemOrg',
        component: () => import('@/views/system/org/index.vue'),
        meta: { title: '组织管理', icon: 'OfficeBuilding', permission: 'system:dept:list' }
      },
      {
        path: 'system/dept',
        redirect: '/system/org'
      },
      {
        path: 'system/dict',
        name: 'SystemDict',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', icon: 'Collection', permission: 'system:dict:list' }
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '系统配置', icon: 'Tools', permission: 'system:config:list' }
      },
      {
        path: 'system/recycle',
        name: 'SystemRecycle',
        component: () => import('@/views/system/recycle/index.vue'),
        meta: { title: '回收中心', icon: 'Delete', permission: 'system:recycle:list' }
      },
      {
        path: 'system/oper-log',
        name: 'SystemOperLog',
        component: () => import('@/views/system/oper-log/index.vue'),
        meta: { title: '操作日志', icon: 'EditPen', permission: 'system:operLog:list' }
      },
      {
        path: 'system/login-log',
        name: 'SystemLoginLog',
        component: () => import('@/views/system/login-log/index.vue'),
        meta: { title: '登录日志', icon: 'Promotion', permission: 'system:loginLog:list' }
      },
      {
        path: 'system/file',
        name: 'SystemFile',
        component: () => import('@/views/system/file/index.vue'),
        meta: { title: '文件列表', icon: 'DocumentCopy', permission: 'sys:file:list' }
      },
      {
        path: 'system/ticket',
        name: 'SystemTicket',
        component: () => import('@/views/system/ticket/index.vue'),
        meta: { title: '工单管理', icon: 'Tickets', permission: 'system:ticket:list' }
      },
      {
        path: 'system/approval',
        name: 'SystemApproval',
        component: () => import('@/views/system/approval/index.vue'),
        meta: { title: '审批单中心', icon: 'Checked', permission: 'system:approval:list' }
      },
      {
        path: 'ai/model',
        name: 'AiModel',
        component: () => import('@/views/ai/model/index.vue'),
        meta: { title: 'AI 模型配置', icon: 'MagicStick', permission: 'system:ai-model:list' }
      },
      {
        path: 'ai/log',
        name: 'AiLog',
        component: () => import('@/views/ai/log/index.vue'),
        meta: { title: 'AI 对话日志', icon: 'ChatDotRound', permission: 'system:ai-log:list' }
      },
      {
        path: 'ai/knowledge',
        name: 'AiKnowledge',
        component: () => import('@/views/ai/knowledge/index.vue'),
        meta: { title: 'AI 知识库', icon: 'Notebook', permission: 'system:ai-knowledge:list' }
      },
      {
        path: 'system/ai-model',
        redirect: '/ai/model'
      },
      {
        path: 'system/ai-log',
        redirect: '/ai/log'
      },
      {
        path: 'monitor/api-access',
        name: 'MonitorApiAccess',
        component: () => import('@/views/monitor/api-access/index.vue'),
        meta: { title: 'API访问统计', icon: 'DataLine', permission: 'monitor:apiAccess:list' }
      },
      {
        path: 'monitor/online',
        name: 'MonitorOnline',
        component: () => import('@/views/monitor/online/index.vue'),
        meta: { title: '在线用户', icon: 'User', permission: 'monitor:online:list' }
      },
      {
        path: 'monitor/job',
        name: 'MonitorJob',
        component: () => import('@/views/monitor/job/index.vue'),
        meta: { title: '定时任务', icon: 'Timer', permission: 'monitor:job:list' }
      },
      {
        path: 'monitor/cache',
        name: 'MonitorCache',
        component: () => import('@/views/monitor/cache/index.vue'),
        meta: { title: '缓存监控', icon: 'Coin', permission: 'monitor:cache:list' }
      },
      {
        path: 'monitor/server',
        name: 'MonitorServer',
        component: () => import('@/views/monitor/server/index.vue'),
        meta: { title: '服务监控', icon: 'Cpu', permission: 'monitor:server:list' }
      },
      {
        path: 'tool/api-doc',
        name: 'ToolApiDoc',
        component: () => import('@/views/tool/api-doc/index.vue'),
        meta: { title: '接口文档', icon: 'Connection', permission: 'tool:apiDoc:view' }
      },
      {
        path: 'tool/gen',
        name: 'ToolGen',
        component: () => import('@/views/tool/gen/index.vue'),
        meta: { title: '代码生成', icon: 'SetUp', permission: 'tool:gen:list' }
      },
      {
        path: 'message/notice',
        name: 'MessageNotice',
        component: () => import('@/views/message/notice/index.vue'),
        meta: { title: '系统通知', icon: 'Notification', permission: 'system:announce:list' }
      },
      {
        path: 'message/chat',
        name: 'MessageChat',
        component: () => import('@/views/message/chat/index.vue'),
        meta: { title: '企业IM', icon: 'ChatDotRound', permission: 'system:chat:list' }
      }
    ]
  },
  // 兜底：未匹配的路径统一进入 404
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/** 路由页访问仅以启用菜单树为准，避免 permissions 别名导致停用菜单仍可直链进入 */
function hasRoutePermission(userStore: ReturnType<typeof useUserStore>, required?: unknown): boolean {
  if (!required) return true
  const requiredPerm = String(required)
  if (!requiredPerm) return true
  return hasMenuPermission(userStore.menus || [], requiredPerm)
}

router.beforeEach(async (to, _from, next) => {
  const siteStore = useSiteStore()
  await siteStore.ensureConfigLoaded()

  const userStore = useUserStore()

  document.title = to.meta.title ? `${to.meta.title} - Admin Platform` : 'Admin Platform'

  if (to.meta.requiresAuth === false) {
    if (to.path === '/login' && userStore.isLoggedIn) {
      next('/')
    } else {
      next()
    }
    return
  }

  if (!userStore.isLoggedIn || !userStore.menus?.length) {
    try {
      await userStore.refreshUserStore()
    } catch (error) {
      console.error('获取用户信息失败:', error)
      next('/login')
      return
    }
  }

  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }

  const required = to.matched
    .slice()
    .reverse()
    .find((record) => record.meta.permission)?.meta.permission

  if (required && !hasRoutePermission(userStore, required)) {
    next('/403')
    return
  }

  next()
})

export default router