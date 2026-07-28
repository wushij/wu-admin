<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="isCreate ? '新增模型' : '编辑模型'"
      :subtitle="isCreate ? '选择供应商并填写接入信息' : '修改接入信息，Key 留空表示不变'"
      icon="setting-o"
      theme="ai"
    />

    <ListLoading v-if="loading" />

    <template v-else>
      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="setting-o" theme="ai" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">接入信息</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="供应商" clickable boxed arrow @click="pickProvider">
            <text class="picker-value">{{ providerMeta(form.provider).label }}</text>
          </FormCell>
          <FormCell
            v-model="form.name"
            label="配置名称"
            editable
            boxed
            placeholder="如：DeepSeek 官方"
          />
          <FormCell label="模型名称" clickable boxed arrow @click="pickModel">
            <text class="picker-value">{{ form.modelName || '选择或输入模型' }}</text>
          </FormCell>
          <FormCell
            v-model="form.baseUrl"
            label="接口地址"
            editable
            boxed
            placeholder="如：https://api.deepseek.com/v1"
          />
          <FormCell
            v-model="form.apiKey"
            label="API Key"
            editable
            password
            boxed
            :placeholder="!isCreate && hasKey ? '留空表示不修改' : '请输入 API Key'"
            last
          />
        </view>
      </view>

      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="chart-trending-o" theme="ai" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">参数与状态</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-model="temperatureText"
            label="温度"
            editable
            boxed
            input-type="digit"
            placeholder="0 ~ 2，默认 0.7"
          />
          <FormCell
            v-model="maxTokensText"
            label="最大 Tokens"
            editable
            boxed
            input-type="number"
            placeholder="128 ~ 32768，默认 2048"
          />
          <FormCell
            v-model="form.remark"
            label="备注"
            editable
            boxed
            placeholder="选填"
          />
          <FormCell label="设为默认" switch-cell>
            <switch :checked="form.isDefault === 1" color="#6366f1" @change="onDefaultChange" />
          </FormCell>
          <FormCell label="启用" switch-cell last>
            <switch :checked="form.status === 1" color="#6366f1" @change="onStatusChange" />
          </FormCell>
        </view>
      </view>

      <PageFooter>
        <button
          class="page-footer__btn page-footer__btn--primary-outline"
          :loading="testing"
          @click="handleTest"
        >
          测试连接
        </button>
        <button class="page-footer__btn" :loading="saving" @click="handleSave">
          {{ isCreate ? '创建' : '保存' }}
        </button>
      </PageFooter>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import {
  getAiModel,
  createAiModel,
  updateAiModel,
  testAiModel,
  type AiModelSaveDTO,
} from '@/api/system/ai-model'
import { AI_PROVIDER_KEYS, AI_PROVIDER_META, providerMeta } from '@/constants/aiProviders'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const isCreate = ref(true)
const hasKey = ref(false)
const modelId = ref(0)

const form = reactive<Required<Omit<AiModelSaveDTO, 'id'>> & { id?: number }>({
  id: undefined,
  name: '',
  provider: 'deepseek',
  modelName: AI_PROVIDER_META.deepseek.models[0],
  baseUrl: AI_PROVIDER_META.deepseek.baseUrl,
  apiKey: '',
  temperature: 0.7,
  maxTokens: 2048,
  systemPrompt: '',
  isDefault: 0,
  status: 1,
  remark: '',
})

/** 数字字段以文本双向绑定，保存时转换 */
const temperatureText = ref('0.7')
const maxTokensText = ref('2048')

const providerLabels = computed(() => AI_PROVIDER_KEYS.map((k) => AI_PROVIDER_META[k].label))

function pickProvider() {
  uni.showActionSheet({
    itemList: providerLabels.value,
    success: (res) => {
      const provider = AI_PROVIDER_KEYS[res.tapIndex]
      if (!provider || provider === form.provider) return
      form.provider = provider
      // 切换供应商时自动回填官方默认地址与首个推荐模型
      const meta = providerMeta(provider)
      form.baseUrl = meta.baseUrl
      form.modelName = meta.models[0] || ''
    },
  })
}

function pickModel() {
  const models = providerMeta(form.provider).models
  const items = [...models, '手动输入']
  uni.showActionSheet({
    itemList: items,
    success: (res) => {
      if (res.tapIndex < models.length) {
        form.modelName = models[res.tapIndex]
        return
      }
      uni.showModal({
        title: '模型名称',
        editable: true,
        placeholderText: '请输入模型名称',
        content: form.modelName,
        success: (r) => {
          if (r.confirm && r.content?.trim()) {
            form.modelName = r.content.trim()
          }
        },
      })
    },
  })
}

function onDefaultChange(e: { detail: { value: boolean } }) {
  form.isDefault = e.detail.value ? 1 : 0
}

function onStatusChange(e: { detail: { value: boolean } }) {
  form.status = e.detail.value ? 1 : 0
}

function validate(): string | null {
  if (!form.name.trim()) return '请输入配置名称'
  if (!form.modelName.trim()) return '请选择或输入模型名称'
  if (!form.baseUrl.trim()) return '请输入接口地址'
  const t = Number(temperatureText.value)
  if (Number.isNaN(t) || t < 0 || t > 2) return '温度需在 0 ~ 2 之间'
  const mt = Number(maxTokensText.value)
  if (!Number.isInteger(mt) || mt < 128 || mt > 32768) return '最大 Tokens 需在 128 ~ 32768 之间'
  return null
}

function buildPayload(): AiModelSaveDTO {
  return {
    id: form.id,
    name: form.name.trim(),
    provider: form.provider,
    modelName: form.modelName.trim(),
    baseUrl: form.baseUrl.trim(),
    apiKey: form.apiKey || undefined,
    temperature: Number(temperatureText.value),
    maxTokens: Number(maxTokensText.value),
    systemPrompt: form.systemPrompt,
    isDefault: form.isDefault,
    status: form.status,
    remark: form.remark,
  }
}

async function handleSave() {
  const err = validate()
  if (err) {
    uni.showToast({ title: err, icon: 'none' })
    return
  }
  saving.value = true
  try {
    if (isCreate.value) {
      await createAiModel(buildPayload())
      uni.showToast({ title: '已创建', icon: 'success' })
    } else {
      await updateAiModel(buildPayload())
      uni.showToast({ title: '已保存', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 600)
  } finally {
    saving.value = false
  }
}

async function handleTest() {
  const err = validate()
  if (err) {
    uni.showToast({ title: err, icon: 'none' })
    return
  }
  testing.value = true
  try {
    const res = await testAiModel({
      id: form.id,
      provider: form.provider,
      modelName: form.modelName.trim(),
      baseUrl: form.baseUrl.trim(),
      apiKey: form.apiKey || undefined,
    })
    uni.showToast({ title: `连接正常 ${res.data}ms`, icon: 'success' })
  } finally {
    testing.value = false
  }
}

async function load(id: number) {
  loading.value = true
  try {
    const res = await getAiModel(id)
    const data = res.data
    if (!data) return
    Object.assign(form, {
      id: data.id,
      name: data.name,
      provider: data.provider,
      modelName: data.modelName,
      baseUrl: data.baseUrl,
      apiKey: '',
      systemPrompt: data.systemPrompt || '',
      isDefault: data.isDefault ?? 0,
      status: data.status ?? 1,
      remark: data.remark || '',
    })
    temperatureText.value = String(data.temperature ?? 0.7)
    maxTokensText.value = String(data.maxTokens ?? 2048)
    hasKey.value = !!data.hasApiKey
  } finally {
    loading.value = false
  }
}

onLoad((options) => {
  if (options?.mode === 'create') {
    isCreate.value = true
    return
  }
  modelId.value = Number(options?.id)
  if (modelId.value) {
    isCreate.value = false
    load(modelId.value)
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';
@import '@/styles/module-form-page.scss';
</style>
