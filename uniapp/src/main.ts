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
