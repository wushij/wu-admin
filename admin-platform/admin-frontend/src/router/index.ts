import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

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
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'dashboard' }
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
        meta: { title: '角色管理', icon: 'peoples', permission: 'system:role:list' }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'tree-table', permission: 'system:menu:list' }
      },
      {
        path: 'system/dept',
        name: 'SystemDept',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理', icon: 'tree', permission: 'system:dept:list' }
      },
      {
        path: 'system/login-log',
        name: 'SystemLoginLog',
        component: () => import('@/views/system/login-log/index.vue'),
        meta: { title: '登录日志', icon: 'document', permission: 'system:loginLog:list' }
      },
      {
        path: 'system/ticket',
        name: 'SystemTicket',
        component: () => import('@/views/system/ticket/index.vue'),
        meta: { title: '工单管理', icon: 'Document', permission: 'system:ticket:list' }
      },
      {
        path: 'system/approval',
        name: 'SystemApproval',
        component: () => import('@/views/system/approval/index.vue'),
        meta: { title: '审批单中心', icon: 'Checked', permission: 'system:approval:list' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')
  const userStore = useUserStore()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - Admin Platform` : 'Admin Platform'
  
  if (to.meta.requiresAuth === false) {
    // 不需要认证的页面
    if (to.path === '/login' && token) {
      next('/')
    } else {
      next()
    }
  } else {
    // 需要认证的页面
    if (!token) {
      next('/login')
    } else {
      // 有 token 但没有菜单信息（刷新页面），重新获取用户信息
      if (!userStore.menus || userStore.menus.length === 0) {
        try {
          await userStore.refreshUserStore()
          next() // 获取成功后继续导航
        } catch (error) {
          console.error('获取用户信息失败:', error)
          next('/login') // 失败则跳转登录页
        }
      } else {
        next()
      }
    }
  }
})

export default router
