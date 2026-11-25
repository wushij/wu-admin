<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑模型' : '新增模型'"
    width="640px"
    align-center
    class="ai-model-dialog"
    destroy-on-close
    @closed="$emit('closed')"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      :validate-on-rule-change="false"
      label-width="100px"
    >
      <el-form-item label="模型类型">
        <div class="model-type-picker">
          <button
            v-for="t in MODEL_TYPE_OPTIONS"
            :key="t.value"
            type="button"
            class="type-option-card"
            :class="{ 'is-active': form.modelType === t.value }"
            @click="handleTypeChange(t.value)"
          >
            <span class="type-option-card__badge">
              <el-icon :size="14">
                <component :is="t.value === 'embedding' ? Histogram : ChatDotRound" />
              </el-icon>
            </span>
            <span class="type-option-card__label">{{ t.label }}</span>
            <span v-if="form.modelType === t.value" class="type-option-card__check">
              <el-icon :size="10"><Check /></el-icon>
            </span>
          </button>
        </div>
      </el-form-item>

      <!-- 向量模型统一走「自定义接入」的 OpenAI 兼容端点，不展示对话供应商 -->
      <el-form-item v-if="!isEmbedding" label="供应商" prop="provider">
        <div class="provider-select">
          <button
            v-for="p in PROVIDER_OPTIONS"
            :key="p.value"
            type="button"
            class="provider-item"
            :class="{ active: form.provider === p.value }"
            @click="$emit('selectProvider', p.value)"
          >
            <span class="provider-item-badge" :style="{ background: providerMeta(p.value).bg }">
              <AiProviderIcon :provider="p.value" :size="16" />
            </span>
            <span>{{ p.label }}</span>
          </button>
        </div>
      </el-form-item>

      <el-form-item v-if="form.provider === 'custom'" label="接入协议" prop="protocol">
        <el-select
          v-model="form.protocol"
          placeholder="请选择接入协议"
          style="width: 100%"
          @change="$emit('protocolChange')"
        >
          <el-option v-for="p in PROTOCOL_OPTIONS" :key="p.value" :label="p.label" :value="p.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="配置名称" prop="name">
        <el-input
          v-model="form.name"
          :placeholder="
            form.provider === 'custom'
              ? '如：公司网关 / 本地 Ollama'
              : '如：' + providerMeta(form.provider).label + ' 官方'
          "
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item prop="modelName">
        <template #label>
          <span class="model-name-label">
            模型名称
            <el-tooltip content="点击「获取模型」从接口拉取可用模型；拉取失败时也可直接手动输入" placement="top" :show-after="1000">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </span>
        </template>
        <div class="model-name-row">
          <el-select
            v-model="form.modelName"
            class="model-name-select"
            filterable
            allow-create
            default-first-option
            clearable
            :validate-event="false"
            placeholder="选择或输入模型名称"
          >
            <el-option v-for="m in modelOptions" :key="m" :label="m" :value="m" />
          </el-select>
          <el-button
            :loading="fetchingModels"
            :icon="Refresh"
            v-permission="'system:ai-model:test'"
            @click="$emit('fetchModels')"
          >
            获取模型
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="接口地址" prop="baseUrl">
        <el-input v-model="form.baseUrl" :placeholder="baseUrlPlaceholder" />
      </el-form-item>

      <el-form-item v-if="form.provider === 'custom'" label="路径模板">
        <el-input
          v-model="form.pathTemplate"
          maxlength="500"
          placeholder="留空使用协议默认路径，支持 {model} 占位"
        />
      </el-form-item>

      <el-form-item v-if="form.provider === 'custom'" label="额外请求头">
        <el-input
          v-model="form.extraHeaders"
          type="textarea"
          :rows="2"
          maxlength="1000"
          placeholder='可选，JSON 对象，如 {"HTTP-Referer":"https://example.com"}'
        />
      </el-form-item>

      <el-form-item prop="apiKey">
        <template #label>
          <span class="api-key-label" :class="{ required: !form.id }">API Key</span>
        </template>
        <el-input
          v-model="form.apiKey"
          type="password"
          show-password
          :placeholder="form.id && editHasKey ? '留空表示不修改已保存的 Key' : '请输入 API Key'"
          autocomplete="new-password"
        />
        <el-link
          v-if="providerMeta(form.provider).portalUrl"
          type="primary"
          :href="providerMeta(form.provider).portalUrl"
          target="_blank"
          class="key-portal-link"
        >
          获取 {{ providerMeta(form.provider).label }} API Key
        </el-link>
      </el-form-item>

      <el-form-item v-if="!isEmbedding" label="温度">
        <div class="slider-row">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" style="flex: 1" />
          <span class="slider-value motion-tabular">{{ form.temperature?.toFixed(1) }}</span>
          <el-tooltip content="控制 AI 回答的随机性与创造力：0 偏严谨确定，1 偏均衡，2 偏富有想象力" placement="top" :show-after="1000">
            <el-icon class="hint-q"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </el-form-item>

      <el-form-item v-if="!isEmbedding" label="最大 Tokens">
        <div class="tokens-row">
          <el-input-number v-model="form.maxTokens" :min="128" :max="32768" :step="256" style="width: 180px" />
          <el-tooltip content="限制 AI 单次回答生成的最大文本长度（1000 Tokens ≈ 700 汉字），默认 8192；推理类模型建议 16384 以上" placement="top" :show-after="1000">
            <el-icon class="hint-q"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </el-form-item>

      <template v-if="isEmbedding">
        <el-form-item label="向量维度" prop="dimension">
          <div class="dimension-row">
            <el-input-number
              v-model="form.dimension"
              :min="1"
              :max="8192"
              :step="1"
              style="width: 180px"
            />
            <el-select
              class="dimension-preset"
              placeholder="常用模型维度"
              clearable
              :model-value="''"
              @change="applyDimensionPreset"
            >
              <el-option
                v-for="preset in EMBEDDING_MODEL_PRESETS"
                :key="preset.value"
                :label="`${preset.label} · ${preset.dimension} 维`"
                :value="preset.value"
              />
            </el-select>
            <el-tooltip content="必须与向量接口实际返回维度一致，否则向量化会失败；常见 bge-large-zh 为 1024 维，text-embedding-3-small 为 1536 维" placement="top" :show-after="1000">
              <el-icon class="hint-q"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </el-form-item>

        <el-form-item label="批量大小">
          <div class="tokens-row">
            <el-input-number v-model="form.batchSize" :min="1" :max="128" :step="1" style="width: 180px" />
            <span class="dimension-hint">单次请求向量化的文本条数，默认 16</span>
          </div>
        </el-form-item>
      </template>

      <el-form-item v-if="!isEmbedding" label="系统提示词">
        <el-input
          v-model="form.systemPrompt"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          placeholder="可选，留空使用内置人设"
        />
      </el-form-item>

      <div class="form-inline-row">
        <el-form-item label="设为默认" class="inline-item">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" :disabled="editIsDefault" />
        </el-form-item>
        <el-form-item label="状态" class="inline-item">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </div>

      <el-form-item label="备注">
        <el-input v-model="form.remark" placeholder="可选" maxlength="200" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :loading="formTesting" v-permission="'system:ai-model:test'" @click="$emit('formTest')">
        测试连接
      </el-button>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="$emit('submit')">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ChatDotRound, Check, Histogram, QuestionFilled, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { AiModelSaveDTO, AiProvider } from '@/api/system/ai-model'
import AiProviderIcon from '@/components/AiProviderIcon.vue'
import {
  EMBEDDING_MODEL_PRESETS,
  MODEL_TYPE_OPTIONS,
  PROVIDER_OPTIONS,
  PROTOCOL_OPTIONS,
  guessEmbeddingDimension,
  providerMeta,
} from '../constants/aiModelMeta'

const visible = defineModel<boolean>('visible', { default: false })
const form = defineModel<AiModelSaveDTO & { temperature: number; maxTokens: number }>('form', {
  required: true,
})
const formRef = ref<FormInstance>()

/** 当前是否为向量模型：向量模型不需要温度、最大 Tokens 与系统提示词 */
const isEmbedding = computed(() => form.value.modelType === 'embedding')

/**
 * 切换模型类型：
 * - 向量模型固定走「自定义 + OpenAI 兼容」接入，清空对话模型的厂商与模型名，避免残留 DeepSeek 等配置；
 * - 切回对话模型时恢复默认采样参数。
 */
function handleTypeChange(next: string) {
  if (form.value.modelType === next) return
  form.value.modelType = next
  if (next === 'embedding') {
    form.value.provider = 'custom'
    form.value.protocol = 'openai'
    form.value.modelName = ''
    form.value.baseUrl = ''
    form.value.pathTemplate = ''
    form.value.dimension ||= 1536
    form.value.batchSize ||= 16
  } else {
    if (form.value.provider === 'custom') form.value.provider = 'deepseek'
    form.value.temperature ??= 0.7
    form.value.maxTokens ??= 8192
  }
  formRef.value?.clearValidate(['provider', 'modelName', 'baseUrl', 'dimension'])
}

/** 编辑既有配置时只补默认值，不改动已保存的厂商与模型名 */
watch(isEmbedding, (embedding) => {
  if (embedding) {
    form.value.dimension ||= 1536
    form.value.batchSize ||= 16
    return
  }
  form.value.temperature ??= 0.7
  form.value.maxTokens ??= 8192
})

/** 选择常用向量模型预设时同步维度，避免手填错维度导致向量化失败 */
function applyDimensionPreset(modelName: string) {
  if (!modelName) return
  form.value.modelName = modelName
  const dimension = guessEmbeddingDimension(modelName)
  if (dimension > 0) form.value.dimension = dimension
}

watch(
  () => form.value.modelName,
  (name) => {
    if (!isEmbedding.value || !name) return
    const dimension = guessEmbeddingDimension(name)
    if (dimension > 0 && form.value.dimension !== dimension) {
      form.value.dimension = dimension
    }
  },
)

defineProps<{
  rules: FormRules
  modelOptions: string[]
  baseUrlPlaceholder: string
  editHasKey: boolean
  editIsDefault: boolean
  fetchingModels: boolean
  formTesting: boolean
  submitting: boolean
}>()

defineEmits<{
  closed: []
  selectProvider: [provider: AiProvider]
  protocolChange: []
  fetchModels: []
  formTest: []
  submit: []
}>()

defineExpose({ formRef })
</script>

<style scoped lang="scss">
/* 模型类型：EduMind 风格胶囊分段（图标徽标 + 文案 + 选中勾选） */
.model-type-picker {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  width: 100%;
}

.type-option-card {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-pill);
  background: var(--color-bg-surface);
  cursor: pointer;
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);

  &:hover:not(.is-active) {
    border-color: var(--el-color-primary-light-5);
    color: var(--color-text-primary);
  }

  &.is-active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    font-weight: 600;
    box-shadow: 0 0 0 3px var(--el-color-primary-light-8);

    .type-option-card__badge {
      background: var(--el-color-primary);
      color: #ffffff;
    }
  }
}

.type-option-card__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: var(--radius-circle);
  background: var(--color-bg-subtle);
  color: var(--color-text-secondary);
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.type-option-card__label {
  line-height: 1.2;
}

.type-option-card__check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  border-radius: var(--radius-circle);
  background: var(--el-color-primary);
  color: #ffffff;
}

.dimension-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
}

.dimension-preset {
  width: 220px;
}

.dimension-hint {
  font-size: var(--text-caption);
  color: var(--color-text-secondary);
}

.model-name-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;

  .el-icon {
    color: var(--color-text-secondary);
    cursor: pointer;
  }
}

.model-name-row {
  display: flex;
  gap: var(--space-2);
  width: 100%;

  .model-name-select {
    flex: 1;
    min-width: 0;
  }
}

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

:global(html.dark .provider-item-badge),
:global(html[data-color-mode='dark'] .provider-item-badge) {
  /* 供应商品牌色偏暗，暗色下统一提亮底色，保证图标清晰可见 */
  background: rgba(148, 163, 184, 0.18) !important;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.14);
}

.key-portal-link {
  margin-top: 4px;
  font-size: var(--text-caption);
}

.api-key-label.required::before {
  content: '*';
  margin-right: 4px;
  color: var(--color-danger);
}

.form-inline-row {
  display: flex;
  gap: var(--space-4);

  .inline-item {
    flex: 1;
    min-width: 0;
    margin-bottom: 18px;
  }
}

.slider-row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  width: 100%;

  .slider-value {
    width: 32px;
    text-align: right;
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}

.tokens-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.hint-q {
  color: var(--color-text-secondary);
  cursor: pointer;
}
</style>
