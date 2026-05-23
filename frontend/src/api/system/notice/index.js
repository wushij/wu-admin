import request from '@/utils/request'

export function getUnreadNoticeCount() {
  return request({
    url: '/system/notice/unread-count',
    method: 'get'
  })
}

export function getMyNoticeList() {
  return request({
    url: '/system/notice/my-list',
    method: 'get'
  })
}

export function readNotice(id) {
  return request({
    url: '/system/notice/read',
    method: 'put',
    data: { id }
  })
}

export function readAllNotice() {
  return request({
    url: '/system/notice/read-all',
    method: 'put'
  })
}
