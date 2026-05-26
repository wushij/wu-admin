import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/admin-page.scss'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import VueParticles from '@tsparticles/vue3'
import { loadSlim } from '@tsparticles/slim'

import App from './App.vue'
import router from './router'
import { setupPermissionDirectives } from './directives/permission'
import { getCurrentTheme, applyTheme } from './utils/theme'

const app = createApp(App)

// 注册Pinia
app.use(createPinia())

// 注册路由
app.use(router)

// 注册Element Plus
app.use(ElementPlus)

// 注册 VueParticles 插件
app.use(VueParticles, {
  init: async (engine) => {
    await loadSlim(engine)
  }
})

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册权限控制指令
setupPermissionDirectives(app)

// 初始化主题
const currentTheme = getCurrentTheme()
applyTheme(currentTheme)

app.mount('#app')
