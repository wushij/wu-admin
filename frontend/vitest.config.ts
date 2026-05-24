import { defineConfig, mergeConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import viteConfig from './vite.config'

export default mergeConfig(
  viteConfig,
  defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',
    include: ['tests/unit/**/*.{test,spec}.ts'],
    clearMocks: true,
  },
}),
)
