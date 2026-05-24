import type { IconName } from '@/constants/iconfont'

export interface MessageChannel {
  key: string
  title: string
  desc: string
  icon: IconName
  theme: string
  path: string
  permission?: string
  countKey: 'announceCount' | 'inboxCount' | 'chatCount'
}

/** Tab 顺序：公告 → 企业 IM → 业务消息 */
export const MESSAGE_CHANNELS: MessageChannel[] = [
  {
    key: 'announce',
    title: '系统公告',
    desc: '平台通知与维护公告',
    icon: 'bell',
    theme: 'notice',
    path: '/pages-sub/msg/announce/index',
    permission: 'system:announce:list',
    countKey: 'announceCount',
  },
  {
    key: 'chat',
    title: '企业 IM',
    desc: '私聊与群聊',
    icon: 'chat-o',
    theme: 'chat',
    path: '/pages-sub/msg/chat/index',
    permission: 'system:chat:list',
    countKey: 'chatCount',
  },
  {
    key: 'inbox',
    title: '站内信',
    desc: '业务提醒与审批消息',
    icon: 'notes-o',
    theme: 'inbox',
    path: '/pages-sub/msg/inbox/index',
    countKey: 'inboxCount',
  },
]
