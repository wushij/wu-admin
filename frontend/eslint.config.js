import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import globals from 'globals'

export default tseslint.config(
  { ignores: ['dist/**', 'node_modules/**'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  {
    files: ['**/*.{ts,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.es2021,
      },
      parserOptions: {
        parser: tseslint.parser,
      },
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],
      'no-empty': ['error', { allowEmptyCatch: true }],
      // 单文件体量硬线：超过 1000 行必须先按职责拆分（见 .agents/skills/senior-frontend 第 15 节）
      'max-lines': [
        'error',
        { max: 1000, skipBlankLines: true, skipComments: true },
      ],
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'off',
      'vue/no-deprecated-filter': 'off',
    },
  },
  {
    // 视图层隔离：views 之间禁止互相引用。
    // 共享 UI 下沉到 @/components，共享逻辑下沉到 @/composables，
    // 页面内部子组件一律使用相对路径 ./components/xxx.vue。
    files: ['src/views/**/*.{ts,vue}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/views', '@/views/*', '@/views/**'],
              message:
                '禁止跨视图引用：共享 UI 请下沉到 @/components，共享逻辑请下沉到 @/composables，页面内子组件请用相对路径。',
            },
          ],
        },
      ],
    },
  },
)
