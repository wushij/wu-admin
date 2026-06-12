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

/** 顺序对齐 PC：业务消息 → 企业 IM → 公告 */
export const MESSAGE_CHANNELS: MessageChannel[] = [
  {
    key: 'inbox',
    title: '站内信',
    desc: '业务提醒与审批消息',
    icon: 'notes-o',
    theme: 'inbox',
    path: '/pages-sub/msg/inbox/index',
    countKey: 'inboxCount',
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
    key: 'announce',
    title: '系统公告',
    desc: '平台通知与维护公告',
    icon: 'bell',
    theme: 'notice',
    path: '/pages-sub/msg/announce/index',
    permission: 'system:announce:list',
    countKey: 'announceCount',
  },
]
