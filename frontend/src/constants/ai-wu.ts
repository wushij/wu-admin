import { ChatDotRound, Cpu, Document, Timer } from '@element-plus/icons-vue'
import type { Component } from 'vue'

export interface AiWuQuickQuestion {
  icon: Component
  text: string
  desc: string
}

/** 空状态快捷提问（按当前账号权限由后端知识库检索后回答） */
export const AI_WU_QUICK_QUESTIONS: AiWuQuickQuestion[] = [
  {
    icon: ChatDotRound,
    text: '这个系统有哪些功能模块？',
    desc: '按你的权限梳理可用功能与入口',
  },
  {
    icon: Document,
    text: '怎么修改登录密码？',
    desc: '获取本系统真实操作路径指引',
  },
  {
    icon: Timer,
    text: '我是什么角色，属于哪个部门？',
    desc: '查看当前账号的角色与组织信息',
  },
  {
    icon: Cpu,
    text: '这个系统用了什么技术栈，有哪些核心优势？',
    desc: '了解前后端架构、安全机制与 AI 亮点',
  },
]
