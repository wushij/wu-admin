import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { MESSAGE_CHANNELS } from '@/constants/messageChannels'
import { usePermission } from '@/composables/usePermission'
import { useMessageStore } from '@/store/message'
import { getAppVersionName } from '@/utils/app-version'
import type { IconName } from '@/constants/iconfont'

export type MineMenuTheme = 'indigo' | 'cyan' | 'violet' | 'slate' | 'amber' | 'emerald' | 'rose' | 'notice' | 'default'

export interface MineMenuItem {
  key: string
  icon: IconName
  label: string
  desc?: string
  path: string
  theme?: MineMenuTheme
  permission?: string
  badge?: number
}

export interface MineMenuGroup {
  key: string
  title: string
  items: MineMenuItem[]
}

export function useMineMenu() {
  const { hasPerm, filterByPerm } = usePermission()
  const messageStore = useMessageStore()
  const { inboxCount, announceCount, chatCount } = storeToRefs(messageStore)
  const versionName = getAppVersionName()

  const accountGroup = computed<MineMenuGroup>(() => ({
    key: 'account',
    title: '账号与安全',
    items: [
      {
        key: 'profile',
        icon: 'contact-o',
        label: '编辑资料',
        desc: '昵称、邮箱、头像',
        path: '/pages-sub/mine/profile',
        theme: 'indigo',
      },
      {
        key: 'password',
        icon: 'shield-o',
        label: '修改密码',
        desc: '密码与短信重置',
        path: '/pages-sub/mine/password',
        theme: 'violet',
      },
      {
        key: 'login-logs',
        icon: 'clock-o',
        label: '登录记录',
        desc: '最近登录活动',
        path: '/pages-sub/mine/login-logs',
        theme: 'cyan',
      },
      {
        key: 'account',
        icon: 'manager-o',
        label: '账号概览',
        desc: '注册时间、最近登录 IP',
        path: '/pages-sub/mine/account',
        theme: 'amber',
      },
      {
        key: 'about',
        icon: 'info-o',
        label: '关于应用',
        desc: `版本 ${versionName}`,
        path: '/pages-sub/mine/about',
        theme: 'violet',
      },
    ],
  }))

  const messageGroup = computed<MineMenuGroup>(() => {
    const countMap = {
      inboxCount: inboxCount.value,
      announceCount: announceCount.value,
      chatCount: chatCount.value,
    }
    const items: MineMenuItem[] = filterByPerm(
      MESSAGE_CHANNELS.map((channel) => ({
        key: channel.key,
        icon: channel.icon,
        label: channel.title,
        desc: channel.desc,
        path: channel.path,
        permission: channel.permission,
        theme: (channel.theme === 'notice' ? 'notice' : channel.theme === 'chat' ? 'emerald' : 'amber') as MineMenuTheme,
        badge: countMap[channel.countKey] || 0,
      })),
    )
    return {
      key: 'message',
      title: '消息中心',
      items,
    }
  })

  const menuGroups = computed(() =>
    [accountGroup.value, messageGroup.value].filter((group) => group.items.length > 0),
  )

  return {
    menuGroups,
    hasPerm,
  }
}
