import request from '@/utils/request'

// 获取首页统计数据
export function getDashboardStats() {
  return request({
    url: '/dashboard/stats',
    method: 'get'
  })
}

// 记录访问
export function recordVisit() {
  return request({
    url: '/dashboard/visit',
    method: 'get'
  })
}
