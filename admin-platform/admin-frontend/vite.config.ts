import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

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
      '/doc.html': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
      '/webjars': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
      '/swagger-ui': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
      '/v3/api-docs': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
      },
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1500
  }
})
