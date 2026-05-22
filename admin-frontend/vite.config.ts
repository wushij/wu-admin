import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

/**
 * Knife4j 调试：将 /system、/auth 等 API 路径转发到网关 /api 前缀。
 * 浏览器直接访问 /system/role 等前端路由（Accept: text/html）须走 SPA，不能代理。
 */
function gatewayApiProxy(): ProxyOptions {
  return {
    target: 'http://localhost:8080',
    changeOrigin: true,
    rewrite: (path) => `/api${path}`,
    bypass(req) {
      const accept = req.headers.accept ?? ''
      if (accept.includes('text/html')) {
        return '/index.html'
      }
    },
  }
}

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
    host: true,
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
      // Knife4j 调试：OpenAPI 路径无 /api 前缀，需代理到网关（与 app.api-docs.server-url 一致）
      '/system': gatewayApiProxy(),
      '/auth': gatewayApiProxy(),
      '/monitor': gatewayApiProxy(),
      '/files': gatewayApiProxy(),
      '/dashboard': gatewayApiProxy(),
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1500
  }
})
