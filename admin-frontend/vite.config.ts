import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

/** 开发环境代理 Knife4j 时移除 X-Frame-Options，避免 iframe 内嵌被 DENY 拦截 */
function knife4jProxy(target: string, rewrite?: ProxyOptions['rewrite']): ProxyOptions {
  return {
    target,
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
    port: 3000,
    proxy: {
      // 所有 /api 请求代理到网关
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Knife4j 静态资源（HTML 内引用 /webjars、/swagger-ui 等同源路径，不能只用 /api/doc.html）
      '/doc.html': knife4jProxy('http://127.0.0.1:8081'),
      '/webjars': knife4jProxy('http://127.0.0.1:8081'),
      '/swagger-ui': knife4jProxy('http://127.0.0.1:8081'),
      '/v3/api-docs': knife4jProxy('http://127.0.0.1:8081'),
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1500
  }
})
