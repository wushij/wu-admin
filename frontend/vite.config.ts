import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 开发环境：/api 与 Knife4j 资源代理到本地 backend（8080，context-path=/api）
function knife4jProxy(rewrite?: ProxyOptions['rewrite']): ProxyOptions {
  return {
    target: 'http://127.0.0.1:8080',
    changeOrigin: true,
    rewrite,
    configure: (proxy) => {
      proxy.on('proxyRes', (proxyRes) => {
        delete proxyRes.headers['x-frame-options']
      })
    },
  }
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    host: true,
    port: 3000,
    proxy: {
      // 所有 /api 请求代理到 Spring Boot 后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Knife4j 页面引用 /webjars、/swagger-ui 等同源根路径（后端 context-path=/api）
      '/doc.html': knife4jProxy((path) => `/api${path}`),
      '/webjars': knife4jProxy((path) => `/api${path}`),
      '/swagger-ui': knife4jProxy((path) => `/api${path}`),
      '/v3/api-docs': knife4jProxy((path) => `/api${path}`),
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1500
  }
})
