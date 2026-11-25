// 必须最先执行：把 H5 运行时 API 挂回全局 uni，供 luch-request 等第三方库使用
import '@/plugins/h5-global-uni'
import { createSSRApp } from 'vue'
import App from './App.vue'
import pinia from './plugins/pinia'
import { installGlobalErrorHandler } from '@/plugins/global-error-handler'
export function createApp() {
  const app = createSSRApp(App)
  app.use(pinia)
  installGlobalErrorHandler(app)
  return {
    app,
  }
}
