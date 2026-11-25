<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑联网搜索引擎' : '新增联网搜索引擎'"
    width="620px"
    align-center
    destroy-on-close
    class="custom-web-search-dialog"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :validate-on-rule-change="false"
      label-width="100px"
      class="engine-form"
    >
      <el-form-item label="供应商" prop="preset">
        <div class="provider-select">
          <button
            v-for="p in PROVIDER_OPTIONS"
            :key="p.value"
            type="button"
            class="provider-item"
            :class="{ active: activeProviderKey === p.value }"
            @click="applyProvider(p.value)"
          >
            <span class="provider-item-badge" :style="{ background: p.bg }">
              <AiProviderIcon :provider="p.icon" :size="16" />
            </span>
            <span>{{ p.label }}</span>
          </button>
        </div>
      </el-form-item>

      <el-form-item v-if="activeProviderKey === 'custom'" label="接入协议" prop="protocol">
        <el-select v-model="form.protocol" placeholder="请选择接入协议" style="width: 100%" @change="handleProtocolChange">
          <el-option label="OpenAI 联网对话" value="openai_search" />
          <el-option label="标准 HTTP JSON" value="http_json" />
        </el-select>
      </el-form-item>

      <el-form-item label="引擎名称" prop="name">
        <el-input
          v-model="form.name"
          :placeholder="activeProviderMeta.namePlaceholder"
          maxlength="40"
          show-word-limit
        />
      </el-form-item>

      <el-form-item v-if="form.protocol === 'openai_search'" label="模型标识" prop="modelName">
        <el-input v-model="form.modelName" placeholder="如：qwen-plus、doubao-1.5-pro" maxlength="60" />
      </el-form-item>

      <el-form-item label="接口地址" prop="endpointUrl">
        <el-input
          v-model="form.endpointUrl"
          :placeholder="activeProviderMeta.urlPlaceholder"
          clearable
        />
      </el-form-item>

      <el-form-item label="API Key">
        <el-input
          v-model="form.apiKey"
          type="password"
          show-password
          autocomplete="new-password"
          :placeholder="isEdit ? '留空或保持掩码则沿用原密钥' : '请输入 API Key'"
        />
        <el-link
          v-if="activeProviderMeta.portalUrl"
          type="primary"
          :href="activeProviderMeta.portalUrl"
          target="_blank"
          class="key-portal-link"
        >
          获取 {{ activeProviderMeta.label }} API Key
        </el-link>
      </el-form-item>

      <div class="form-inline-row">
        <el-form-item label="优先级" prop="priority" class="inline-item">
          <el-input-number v-model="form.priority" :min="1" :max="999" controls-position="right" style="width: 120px" />
          <span class="field-inline-hint">越小越优先</span>
        </el-form-item>
        <el-form-item label="状态" class="inline-item">
          <el-radio-group v-model="form.enabled">
            <el-radio :value="true">启用</el-radio>
            <el-radio :value="false">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </div>

      <el-form-item label="备注">
        <el-input v-model="form.remark" placeholder="可选" maxlength="100" />
      </el-form-item>

      <el-collapse v-model="advancedCollapse" class="advanced-collapse">
        <el-collapse-item title="高级请求与字段映射（可选）" name="advanced">
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="鉴权请求头">
                <el-input v-model="form.authHeader" placeholder="默认 Authorization" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="鉴权前缀">
                <el-input v-model="form.authPrefix" placeholder="如：Bearer （可留空）" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="12">
            <el-col v-if="form.protocol === 'http_json'" :span="12">
              <el-form-item label="请求方式">
                <el-radio-group v-model="form.httpMethod">
                  <el-radio-button value="POST">POST</el-radio-button>
                  <el-radio-button value="GET">GET</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="超时(毫秒)">
                <el-input-number
                  v-model="form.timeoutMs"
                  :min="1000"
                  :max="30000"
                  :step="1000"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item v-if="form.protocol === 'openai_search'" label="扩展请求体" prop="extraBodyJson">
            <el-input
              v-model="form.extraBodyJson"
              type="textarea"
              :rows="2"
              placeholder='可选合法 JSON 对象，例如：{"enable_search": true}'
            />
          </el-form-item>

          <template v-else>
            <el-form-item :label="form.httpMethod === 'GET' ? 'Query 模板' : '请求体模板'">
              <el-input
                v-model="form.requestTemplate"
                type="textarea"
                :rows="2"
                :placeholder="form.httpMethod === 'GET'
                  ? '如：q={{query}}&format=json'
                  : '如：{&quot;query&quot;: &quot;{{query}}&quot;, &quot;limit&quot;: {{limit}}}'"
              />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="结果路径">
                  <el-input v-model="form.itemsPath" placeholder="默认 results" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="标题字段">
                  <el-input v-model="form.titleField" placeholder="默认 title" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="链接字段">
                  <el-input v-model="form.urlField" placeholder="默认 url" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="摘要字段">
                  <el-input v-model="form.snippetField" placeholder="默认 content" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>
        </el-collapse-item>
      </el-collapse>
    </el-form>

    <div v-if="testResult" class="test-probe-result" :class="testResult.success ? 'is-ok' : 'is-fail'">
      <div class="test-probe-result__summary">
        <span>{{ testResult.success ? '连接正常' : '连接未就绪' }} · {{ testResult.message }}</span>
        <el-tag size="small" type="info">{{ testResult.latencyMs }} ms</el-tag>
      </div>
      <div v-if="testResult.results?.length" class="test-probe-result__list">
        <div v-for="(item, idx) in testResult.results" :key="idx" class="test-probe-item">
          <div class="test-probe-item__title">{{ idx + 1 }}. {{ item.title }}</div>
          <div v-if="item.url" class="test-probe-item__url">{{ item.url }}</div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button :loading="testing" @click="handleTestSearch">测试连接</el-button>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import AiProviderIcon from '@/components/AiProviderIcon.vue'
import { testCustomWebSearchEngine } from '@/api/system/config'
import type { CustomWebSearchEngine, WebSearchTestResultVO } from '@/types/config'
import { getErrorMessage } from '@/utils/axiosError'

type SearchProviderKey = 'deepseek' | 'tavily_proxy' | 'custom'

interface ProviderOption {
  value: SearchProviderKey
  icon: string
  label: string
  bg: string
  portalUrl: string
  namePlaceholder: string
  urlPlaceholder: string
  defaults: Partial<CustomWebSearchEngine>
}

const PROVIDER_OPTIONS: ProviderOption[] = [
  {
    // 官方独立检索接口：POST https://api.deepseek.com/v1/tools/search
    // 请求 {query, count(1~50), ori_question, localization, need_content}，响应 results[]
    value: 'deepseek',
    icon: 'deepseek',
    label: 'DeepSeek',
    bg: 'rgba(77, 107, 254, 0.12)',
    portalUrl: 'https://platform.deepseek.com',
    namePlaceholder: '如：DeepSeek 联网搜索',
    urlPlaceholder: 'https://api.deepseek.com/v1/tools/search',
    defaults: {
      name: 'DeepSeek 联网搜索',
      preset: 'deepseek',
      protocol: 'http_json',
      httpMethod: 'POST',
      endpointUrl: 'https://api.deepseek.com/v1/tools/search',
      authHeader: 'Authorization',
      authPrefix: 'Bearer ',
      requestTemplate: '{"query": "{{query}}", "count": {{limit}}}',
      itemsPath: 'results',
      titleField: 'title',
      urlField: 'url',
      snippetField: 'snippet',
      timeoutMs: 15000,
    },
  },
  {
    value: 'tavily_proxy',
    icon: 'tavily',
    label: 'Tavily',
    bg: 'rgba(16, 66, 255, 0.12)',
    portalUrl: 'https://app.tavily.com',
    namePlaceholder: '如：Tavily 搜索代理',
    urlPlaceholder: 'https://api.tavily.com/search',
    defaults: {
      name: 'Tavily 联网搜索',
      preset: 'tavily_proxy',
      protocol: 'http_json',
      httpMethod: 'POST',
      endpointUrl: 'https://api.tavily.com/search',
      authHeader: 'Authorization',
      authPrefix: 'Bearer ',
      requestTemplate: '{"query": "{{query}}", "max_results": {{limit}}, "search_depth": "basic"}',
      itemsPath: 'results',
      titleField: 'title',
      urlField: 'url',
      snippetField: 'content',
      timeoutMs: 15000,
    },
  },
  {
    value: 'custom',
    icon: 'http',
    label: '自定义',
    bg: 'rgba(100, 116, 139, 0.12)',
    portalUrl: '',
    namePlaceholder: '如：公司联网检索网关',
    urlPlaceholder: '请输入公网 HTTPS/HTTP 检索接口地址',
    defaults: {
      name: '自定义联网网关',
      preset: 'custom',
      protocol: 'http_json',
      httpMethod: 'POST',
      endpointUrl: '',
      authHeader: 'Authorization',
      authPrefix: 'Bearer ',
      requestTemplate: '{"query": "{{query}}", "limit": {{limit}}}',
      itemsPath: 'results',
      titleField: 'title',
      urlField: 'url',
      snippetField: 'content',
      timeoutMs: 15000,
    },
  },
]

const props = defineProps<{
  modelValue: boolean
  editingEngine: CustomWebSearchEngine | null
  defaultPriority: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  save: [engine: CustomWebSearchEngine]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val),
})

const isEdit = computed(() => Boolean(props.editingEngine?.id))
const formRef = ref<FormInstance>()
const advancedCollapse = ref<string[]>([])
const testing = ref(false)
const testResult = ref<WebSearchTestResultVO | null>(null)

function createBlankForm(): CustomWebSearchEngine {
  return {
    id: `ws-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    name: 'DeepSeek 联网搜索',
    preset: 'deepseek',
    protocol: 'http_json',
    enabled: true,
    priority: props.defaultPriority || 10,
    endpointUrl: 'https://api.deepseek.com/v1/tools/search',
    httpMethod: 'POST',
    apiKey: '',
    authHeader: 'Authorization',
    authPrefix: 'Bearer ',
    modelName: '',
    extraBodyJson: '',
    requestTemplate: '{"query": "{{query}}", "count": {{limit}}}',
    itemsPath: 'results',
    titleField: 'title',
    urlField: 'url',
    snippetField: 'snippet',
    timeoutMs: 15000,
    remark: '',
  }
}

const form = reactive<CustomWebSearchEngine>(createBlankForm())

const activeProviderKey = computed<SearchProviderKey>(() => {
  const p = (form.preset || '').toLowerCase()
  if (p === 'deepseek') return 'deepseek'
  if (p === 'tavily' || p === 'tavily_proxy') return 'tavily_proxy'
  return 'custom'
})

const activeProviderMeta = computed(
  () => PROVIDER_OPTIONS.find((item) => item.value === activeProviderKey.value) ?? PROVIDER_OPTIONS[2],
)

const rules: FormRules = {
  name: [{ required: true, message: '请输入引擎名称', trigger: 'blur' }],
  endpointUrl: [{ required: true, message: '请输入公网 HTTPS/HTTP 接口地址', trigger: 'blur' }],
  extraBodyJson: [
    {
      validator: (_rule, val: string, cb) => {
        if (!val || !val.trim()) return cb()
        try {
          const parsed = JSON.parse(val)
          if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
            return cb(new Error('扩展参数必须是合法 JSON 对象，例如 {"enable_search": true}'))
          }
          return cb()
        } catch {
          return cb(new Error('扩展参数 JSON 格式有误'))
        }
      },
      trigger: 'blur',
    },
  ],
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    testResult.value = null
    advancedCollapse.value = []
    const fresh = createBlankForm()
    if (props.editingEngine) {
      Object.assign(form, fresh, JSON.parse(JSON.stringify(props.editingEngine)))
    } else {
      Object.assign(form, fresh)
    }
  },
  { immediate: true },
)

function applyProvider(key: SearchProviderKey) {
  const option = PROVIDER_OPTIONS.find((item) => item.value === key)
  if (!option) return
  const preserveId = form.id
  const preserveKey = form.apiKey
  const preservePriority = form.priority
  const preserveRemark = form.remark
  Object.assign(form, createBlankForm(), option.defaults, {
    id: preserveId,
    apiKey: preserveKey,
    priority: preservePriority,
    remark: preserveRemark,
  })
  testResult.value = null
}

function handleProtocolChange(protocol: 'openai_search' | 'http_json') {
  if (protocol === 'openai_search' && !form.modelName) {
    form.modelName = ''
  } else if (protocol === 'http_json' && !form.itemsPath) {
    form.httpMethod = form.httpMethod || 'POST'
    form.itemsPath = 'results'
    form.titleField = 'title'
    form.urlField = 'url'
    form.snippetField = 'content'
  }
}

async function handleTestSearch() {
  if (!form.endpointUrl?.trim()) {
    ElMessage.warning('请先填写接口地址再测试')
    return
  }
  testing.value = true
  testResult.value = null
  try {
    const res = await testCustomWebSearchEngine({ ...form }, 'Spring Boot 最新特性')
    testResult.value = res.data
    if (res.data?.success) {
      ElMessage.success(`连接正常（${res.data.latencyMs}ms）`)
    } else {
      ElMessage.warning(res.data?.message || '已连通但未解析出有效检索结果')
    }
  } catch (e) {
    // 失败提示由全局请求拦截器统一弹出，这里只在弹窗内展示失败原因，避免重复 Toast
    testResult.value = {
      success: false,
      engineName: form.name || '',
      latencyMs: 0,
      message: getErrorMessage(e) || '测试连接失败',
      results: [],
    }
  } finally {
    testing.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('save', JSON.parse(JSON.stringify(form)))
  visible.value = false
}

function handleClosed() {
  testResult.value = null
  advancedCollapse.value = []
}
</script>

<style scoped lang="scss">
.provider-select {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.provider-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  background: var(--color-bg-surface);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);

  &:hover {
    border-color: var(--el-color-primary-light-5);
  }

  &.active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
  }
}

.provider-item-badge {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-circle);
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

:global(html.dark) .provider-item-badge {
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.key-portal-link {
  margin-top: 4px;
  font-size: var(--text-caption);
}

.form-inline-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  margin-bottom: 18px;

  .inline-item {
    flex: 1;
    min-width: 0;
    margin-bottom: 0;
  }

  /* label 与右侧控件垂直居中，避免「状态」文字与单选组错位 */
  :deep(.el-form-item) {
    align-items: center;
  }

  :deep(.el-form-item__label) {
    height: auto;
    line-height: 1.5;
    padding-top: 0;
    padding-bottom: 0;
  }
}

.field-inline-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.advanced-collapse {
  margin-top: 4px;
  border-radius: var(--radius-md);
  overflow: hidden;

  :deep(.el-collapse-item__header) {
    height: 38px;
    font-size: 12.5px;
    color: var(--color-text-secondary);
    padding: 0 12px;
    background: var(--color-bg-subtle);
  }

  :deep(.el-collapse-item__wrap) {
    padding: 12px 12px 0;
    background: var(--color-bg-surface);
  }
}

.test-probe-result {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);

  &.is-ok {
    border-color: rgba(16, 185, 129, 0.4);
  }

  &.is-fail {
    border-color: rgba(245, 158, 11, 0.45);
  }

  &__summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    font-size: 12.5px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  &__list {
    margin-top: 8px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
}

.test-probe-item {
  padding-top: 4px;
  border-top: 1px solid var(--color-border);
  font-size: 12px;

  &__title {
    font-weight: 500;
    color: var(--color-text-primary);
  }

  &__url {
    color: var(--el-color-primary);
    word-break: break-all;
  }
}
</style>
