import { get } from '@/utils/request'

/**
 * API 访问统计
 */
export const getApiAccessPage = (params) => {
  return get('/monitor/api-access/page', params)
}

export const getApiAccessStatistics = (params) => {
  return get('/monitor/api-access/statistics', params)
}
