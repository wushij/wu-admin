import request from '@/utils/request'

export function listConfigGroups() {
  return request({
    url: '/system/config-group/list',
    method: 'get'
  })
}

export function getConfigGroup(groupCode) {
  return request({
    url: `/system/config-group/${groupCode}`,
    method: 'get'
  })
}

export function updateConfigGroup(groupCode, configValue) {
  return request({
    url: `/system/config-group/${groupCode}`,
    method: 'put',
    data: { configValue }
  })
}
