import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import globals from 'globals'

export default tseslint.config(
  { ignores: ['dist/**', 'node_modules/**', 'unpackage/**'] },
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
        // uni-app 运行时全局对象
        uni: 'readonly',
        wx: 'readonly',
        getApp: 'readonly',
        getCurrentPages: 'readonly',
        UniApp: 'readonly',
        UniHelper: 'readonly',
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
    },
  },
  {
    // TODO(拆分债务)：security 页当前 1016 行，超出 1000 行硬线。
    // 此处冻结现有体量以阻止继续增长，拆分完成后删除本段豁免。
    files: ['src/pages-sub/system/security/index.vue'],
    rules: {
      'max-lines': [
        'error',
        { max: 1050, skipBlankLines: true, skipComments: true },
      ],
    },
  },
)
