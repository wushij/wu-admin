import request from '@/utils/request'

// 获取部门列表
export function getDeptList(params) {
  return request({
    url: '/system/dept/list',
    method: 'get',
    params
  })
}

// 获取部门详情
export function getDept(id) {
  return request({
    url: '/system/dept/get',
    method: 'get',
    params: { id }
  })
}

// 新增部门
export function createDept(data) {
  return request({
    url: '/system/dept/create',
    method: 'post',
    data
  })
}

// 修改部门
export function updateDept(data) {
  return request({
    url: '/system/dept/update',
    method: 'put',
    data
  })
}

// 删除部门
export function deleteDept(id) {
  return request({
    url: '/system/dept/delete',
    method: 'delete',
    params: { id }
  })
}

// 更新部门状态
export function updateDeptStatus(id, status) {
  return request({
    url: '/system/dept/update-status',
    method: 'put',
    params: { id, status }
  })
}

// 部门回收站分页
export function getRecycleDeptPage(params) {
  return request({
    url: '/system/dept/recycle/page',
    method: 'get',
    params
  })
}

// 恢复部门
export function restoreDept(id) {
  return request({
    url: '/system/dept/restore',
    method: 'put',
    params: { id }
  })
}

// 彻底删除部门
export function deleteDeptPermanent(id) {
  return request({
    url: '/system/dept/delete-permanent',
    method: 'delete',
    params: { id }
  })
}
