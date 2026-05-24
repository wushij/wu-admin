import type { IconName } from './iconfont'

export interface TabBarItem {
  pagePath: string
  text: string
  icon: IconName
  iconActive: IconName
}

export const TAB_BAR_ITEMS: TabBarItem[] = [
  {
    pagePath: '/pages/index/index',
    text: '首页',
    icon: 'home-o',
    iconActive: 'home',
  },
  {
    pagePath: '/pages/work/index',
    text: '工作台',
    icon: 'apps-o',
    iconActive: 'apps-o',
  },
  {
    pagePath: '/pages/message/index',
    text: '消息',
    icon: 'chat-o',
    iconActive: 'chat',
  },
  {
    pagePath: '/pages/mine/index',
    text: '我的',
    icon: 'user-o',
    iconActive: 'user',
  },
]
