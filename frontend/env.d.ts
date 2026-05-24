/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

/** 全局自定义指令（与 directives/permission.ts 一致，消除模板里 v-permission 告警） */
declare module '@vue/runtime-core' {
  interface GlobalDirectives {
    permission: import('vue').Directive<HTMLElement, string | string[]>
    role: import('vue').Directive<HTMLElement, string | string[]>
  }
}

import 'axios'

declare module 'axios' {
  export interface AxiosRequestConfig {
    silent403?: boolean
  }
}

export {}
