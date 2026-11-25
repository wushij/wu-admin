import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/admin-page.scss'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import VueParticles from '@tsparticles/vue3'
import { loadSlim } from '@tsparticles/slim'

import App from './App.vue'
import router from './router'
import { setupPermissionDirectives } from './directives/permission'
import { setupGlobalTooltip } from './utils/tooltip-defaults'
import { getCurrentTheme, applyTheme } from './utils/theme'
import DictSelect from './components/DictSelect.vue'
import DictTag from './components/DictTag.vue'
import { useSiteStore } from './store/site'

const app = createApp(App)
const pinia = createPinia()

// 注册Pinia
app.use(pinia)

// 注册路由
app.use(router)

// 注册Element Plus，并覆盖业务模板 <el-tooltip> 的默认行为（1s 延迟 + 点击/失焦立即收起）
app.use(ElementPlus)
setupGlobalTooltip(app)

// 注册 VueParticles 插件
app.use(VueParticles, {
  init: async (engine) => {
    await loadSlim(engine)
  },
})

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册权限控制指令
setupPermissionDirectives(app)

// 字典组件（业务表单与列表统一使用）
app.component('DictSelect', DictSelect)
app.component('DictTag', DictTag)

// 初始化主题（含亮/暗双模式）
const currentTheme = getCurrentTheme()
applyTheme(currentTheme)

async function bootstrap() {
  const siteStore = useSiteStore()
  await siteStore.loadConfig()
  if (siteStore.disableDevtool) {
    const mod = await import('disable-devtool')
    mod.default()
  }
  app.mount('#app')
}

bootstrap()
