import { defineConfig, type Plugin } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import path from 'node:path'

const ALIYUN_CLOUDAUTH_SCRIPT_RE =
  /[ \t]*<script\b[^>]*cn-shanghai-aliyun-cloudauth[^>]*><\/script>\r?\n?/gi

/**
 * 剔除 @dcloudio/uni-h5-vite 在无实人认证业务时误注入的阿里云脚本，
 * 避免线上 nginx 严格 CSP (script-src 'self') 拦截外链报控制台错误。
 */
function stripAliyunCloudAuthPlugin(): Plugin {
  return {
    name: 'strip-uni-aliyun-cloudauth-script',
    apply: 'build',
    transformIndexHtml: {
      order: 'post',
      handler(html) {
        return html.replace(ALIYUN_CLOUDAUTH_SCRIPT_RE, '')
      },
    },
  }
}

export default defineConfig({
  plugins: [uni(), stripAliyunCloudAuthPlugin()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
        silenceDeprecations: ['legacy-js-api'],
        additionalData: `@use "@/styles/variables.scss" as *;\n@use "@/styles/mixins.scss" as *;\n`,
      },
    },
  },
  server: {
    host: true,
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
})
