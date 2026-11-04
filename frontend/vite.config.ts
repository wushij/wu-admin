import { defineConfig, type Plugin, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { readdir, readFile, stat, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { gzipSync } from 'node:zlib'

/**
 * 基于 Node 原生 node:zlib 的构建产物 Gzip 预压缩插件（替代已废弃的 vite-plugin-compression），
 * 配合生产 Nginx 的 `gzip_static on;` 直出预压缩文件，零外部依赖。
 */
function nativeGzipPlugin(thresholdBytes = 10240): Plugin {
  const compressibleRe = /\.(?:js|mjs|css|html|json|svg)$/i
  async function walkAndCompress(dir: string): Promise<void> {
    const entries = await readdir(dir, { withFileTypes: true })
    await Promise.all(
      entries.map(async (entry) => {
        const fullPath = join(dir, entry.name)
        if (entry.isDirectory()) {
          await walkAndCompress(fullPath)
          return
        }
        if (!entry.isFile() || !compressibleRe.test(entry.name)) return
        const info = await stat(fullPath)
        if (info.size <= thresholdBytes) return
        const raw = await readFile(fullPath)
        const compressed = gzipSync(raw, { level: 9 })
        if (compressed.length < raw.length) {
          await writeFile(`${fullPath}.gz`, compressed)
        }
      }),
    )
  }
  let outDir = 'dist'
  return {
    name: 'native-gzip-compression',
    apply: 'build',
    configResolved(config) {
      outDir = join(config.root, config.build.outDir)
    },
    async closeBundle() {
      await walkAndCompress(outDir)
    },
  }
}

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
  plugins: [vue(), nativeGzipPlugin()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: true,
    port: 3000,
    proxy: {
      // 所有 /api 请求代理到 Spring Boot 后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
      // Knife4j 页面引用 /webjars、/swagger-ui 等同源根路径（后端 context-path=/api）
      '/doc.html': knife4jProxy((path) => `/api${path}`),
      '/webjars': knife4jProxy((path) => `/api${path}`),
      '/swagger-ui': knife4jProxy((path) => `/api${path}`),
      '/v3/api-docs': knife4jProxy((path) => `/api${path}`),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1500,
    rolldownOptions: {
      output: {
        minify: {
          compress: {
            dropConsole: true,
            dropDebugger: true,
          },
        },
      },
    },
  },
})
