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

        <!-- 供应商可视化快捷切换 -->
        <view class="provider-grid">
          <view
            v-for="k in AI_PROVIDER_KEYS"
            :key="k"
            class="provider-tile"
            :class="{ active: form.provider === k }"
            @click="onSelectProvider(k)"
          >
            <view class="provider-tile__icon-wrap" :style="{ background: providerMeta(k).bg }">
              <AiProviderIcon :provider="k" :size="20" />
            </view>
            <text class="provider-tile__label">{{ providerMeta(k).label }}</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="供应商" clickable boxed arrow @click="pickProvider">
            <view class="picker-value-wrap">
              <AiProviderIcon :provider="form.provider" :size="16" />
              <text class="picker-value">{{ providerMeta(form.provider).label }}</text>
            </view>
          </FormCell>
          <FormCell
            v-if="form.provider === 'custom'"
            label="接入协议"
            clickable
            boxed
            arrow
            @click="pickProtocol"
          >
            <text class="picker-value">{{ protocolMeta(form.protocol).label }}</text>
          </FormCell>
          <FormCell
            v-model="form.name"
            label="配置名称"
            editable
            boxed
            :placeholder="namePlaceholder"
          />
          <FormCell label="模型名称" clickable boxed @click="openModelSheet">
            <view class="model-value-row">
              <text class="model-value" :class="{ 'model-value--muted': !form.modelName }">
                {{ form.modelName || '选择或输入模型' }}
              </text>
              <text
                class="model-fetch-btn"
                :class="{ 'is-loading': fetchingModels }"
                @tap.stop="handleFetchModels"
              >
                {{ fetchingModels ? '获取中…' : '获取模型' }}
              </text>
            </view>
          </FormCell>
          <FormCell
            v-model="form.baseUrl"
            label="接口地址"
            editable
            boxed
            :placeholder="baseUrlPlaceholder"
          />
          <FormCell
            v-if="form.provider === 'custom'"
            v-model="form.pathTemplate"
            label="路径模板"
            editable
            boxed
            placeholder="留空按默认"
          />
          <FormCell
            v-if="form.provider === 'custom'"
            v-model="form.extraHeaders"
            label="额外请求头"
            editable
            boxed
            placeholder="可选 JSON"
          />
          <FormCell
            v-model="form.apiKey"
            :label="isCreate ? 'API Key *' : 'API Key'"
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
            placeholder="128 ~ 32768，默认 8192"
          />
          <FormCell
            v-model="form.remark"
            label="备注"
            editable
            boxed
            placeholder="选填"
          />
          <view class="form-textarea-cell">
            <text class="form-textarea-cell__label">系统提示词</text>
            <textarea
              v-model="form.systemPrompt"
              class="form-textarea-cell__input"
              :maxlength="2000"
              placeholder="可选，留空使用内置人设"
            />
          </view>
          <FormCell label="设为默认" switch-cell>
            <switch
              :checked="form.isDefault === 1"
              color="#6366f1"
              :disabled="editIsDefault"
              @change="onDefaultChange"
            />
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

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/base/ModuleIcon/index.vue'
import ListLoading from '@/components/base/ListLoading/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import PageFooter from '@/components/base/PageFooter/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import { showActionSheet, showConfirm } from '@/utils/app-dialog'
import {
  getAiModel,
  createAiModel,
  updateAiModel,
  testAiModel,
  listAiModels,
  type AiModelSaveDTO,
} from '@/api/system/ai-model'
import { AI_PROVIDER_KEYS, AI_PROVIDER_META, providerMeta, AI_PROTOCOL_KEYS, AI_PROTOCOL_META, protocolMeta } from '@/constants/aiProviders'
import AiProviderIcon from '@/components/business/ai/AiProviderIcon/index.vue'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const isCreate = ref(true)
const hasKey = ref(false)
/** 编辑的是否为当前默认模型：默认模型不允许在表单里直接取消默认 */
const editIsDefault = ref(false)
const modelId = ref(0)
const fetchingModels = ref(false)
/** 「获取模型」拉回来的模型列表，与内置推荐合并后展示 */
const fetchedModels = ref<string[]>([])

const form = reactive<Required<Omit<AiModelSaveDTO, 'id'>> & { id?: number }>({
  id: undefined,
  name: '',
  provider: 'deepseek',
  protocol: 'openai',
  modelName: AI_PROVIDER_META.deepseek.models[0],
  baseUrl: AI_PROVIDER_META.deepseek.baseUrl,
  pathTemplate: '',
  extraHeaders: '',
  apiKey: '',
  temperature: 0.7,
  maxTokens: 8192,
  systemPrompt: '',
  isDefault: 0,
  status: 1,
  remark: '',
})

/** 数字字段以文本双向绑定，保存时转换 */
const temperatureText = ref('0.7')
const maxTokensText = ref('8192')

const namePlaceholder = computed(() => {
  return form.provider === 'custom'
    ? '如：公司网关'
    : `如：${providerMeta(form.provider).label} 官方`
})

const baseUrlPlaceholder = computed(() => {
  return form.provider === 'custom'
    ? '请输入接口地址'
    : `如：${providerMeta(form.provider).baseUrl || 'https://api.example.com/v1'}`
})

function onSelectProvider(provider: string) {
  if (form.provider === provider) return
  form.provider = provider
  const meta = providerMeta(provider)
  form.baseUrl = meta.baseUrl
  form.modelName = meta.models[0] || ''
  // 协议、路径模板、额外请求头只对「自定义」有意义，切换时重置避免串配置
  form.protocol = 'openai'
  form.pathTemplate = ''
  form.extraHeaders = ''
  // 拉取结果属于上一家供应商，切换后必须失效
  fetchedModels.value = []
}

async function pickProtocol() {
  const items = AI_PROTOCOL_KEYS.map((k) => ({ label: AI_PROTOCOL_META[k].label }))
  let index: number
  try {
    index = await showActionSheet({ title: '接入协议', items })
  } catch {
    return
  }
  const protocol = AI_PROTOCOL_KEYS[index]
  if (!protocol) return
  form.protocol = protocol
  // 协议只决定请求体格式，地址由用户维护：为空时回填供应商默认地址
  if (!form.baseUrl.trim()) {
    form.baseUrl = providerMeta(form.provider).baseUrl
  }
}

async function pickProvider() {
  const items = AI_PROVIDER_KEYS.map((k) => ({ label: AI_PROVIDER_META[k].label }))
  let index: number
  try {
    index = await showActionSheet({ title: '供应商', items })
  } catch {
    return
  }
  const provider = AI_PROVIDER_KEYS[index]
  if (!provider) return
  onSelectProvider(provider)
}

/** 模型选择：首项「从接口获取模型」，中间为已拉取与内置模型，末项手动输入 */
async function openModelSheet() {
  if (fetchingModels.value) return
  const preset = providerMeta(form.provider).models
  const models = Array.from(new Set([...fetchedModels.value, ...preset].filter(Boolean)))
  const items = [
    { label: '从接口获取模型' },
    ...models.map((label) => ({ label })),
    { label: '手动输入' },
  ]
  let index: number
  try {
    index = await showActionSheet({ title: '模型名称', items })
  } catch {
    return
  }
  if (index === 0) {
    await handleFetchModels()
    return
  }
  if (index === items.length - 1) {
    await inputModelName()
    return
  }
  form.modelName = models[index - 1]
}

async function inputModelName() {
  const { confirmed, content } = await showConfirm({
    title: '手动输入模型名称',
    content: '',
    editable: true,
    placeholderText: '如：deepseek-chat',
    inputValue: form.modelName,
    confirmText: '确定',
  })
  if (confirmed && content?.trim()) {
    form.modelName = content.trim()
  }
}

/** 拉取供应商可用模型：编辑时 Key 留空会自动沿用库中已保存的密钥 */
async function handleFetchModels() {
  if (!form.baseUrl.trim()) {
    uni.showToast({ title: '请先填写接口地址', icon: 'none' })
    return
  }
  if (isCreate.value && !form.apiKey.trim()) {
    uni.showToast({ title: '请先填写 API Key', icon: 'none' })
    return
  }
  if (fetchingModels.value) return
  fetchingModels.value = true
  try {
    const res = await listAiModels({
      id: form.id,
      provider: form.provider,
      protocol: form.protocol,
      baseUrl: form.baseUrl.trim(),
      pathTemplate: form.pathTemplate.trim(),
      extraHeaders: form.extraHeaders.trim(),
      apiKey: form.apiKey || undefined,
    })
    const models = (res.data || []).filter((item) => !!item)
    if (models.length === 0) {
      uni.showToast({ title: '接口未返回可用模型，请手动输入', icon: 'none' })
      return
    }
    fetchedModels.value = models
  } finally {
    fetchingModels.value = false
  }
  uni.showToast({ title: `获取到 ${fetchedModels.value.length} 个模型`, icon: 'none' })
  // 拉取完成后立即弹出列表，省去用户再点一次
  await openModelSheet()
}

function onDefaultChange(e: { detail: { value: boolean } }) {
  if (editIsDefault.value && !e.detail.value) {
    uni.showToast({ title: '默认模型需先将其他模型设为默认', icon: 'none' })
    return
  }
  form.isDefault = e.detail.value ? 1 : 0
}

function onStatusChange(e: { detail: { value: boolean } }) {
  form.status = e.detail.value ? 1 : 0
}

function validate(): string | null {
  if (!form.name.trim()) return '请输入配置名称'
  if (!form.modelName.trim()) return '请选择或输入模型名称'
  if (!form.baseUrl.trim()) return '请输入接口地址'
  // 新增时云厂商必须填写密钥；编辑时留空表示沿用已保存密钥；自定义供应商允许免鉴权（如本地 Ollama）
  if (isCreate.value && !form.apiKey.trim()) return '请输入 API Key'
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
    protocol: form.protocol,
    modelName: form.modelName.trim(),
    baseUrl: form.baseUrl.trim(),
    pathTemplate: form.pathTemplate.trim(),
    extraHeaders: form.extraHeaders.trim(),
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
      protocol: form.protocol,
      modelName: form.modelName.trim(),
      baseUrl: form.baseUrl.trim(),
      pathTemplate: form.pathTemplate.trim(),
      extraHeaders: form.extraHeaders.trim(),
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
      protocol: data.protocol || 'openai',
      modelName: data.modelName,
      baseUrl: data.baseUrl,
      pathTemplate: data.pathTemplate || '',
      extraHeaders: data.extraHeaders || '',
      apiKey: '',
      systemPrompt: data.systemPrompt || '',
      isDefault: data.isDefault ?? 0,
      status: data.status ?? 1,
      remark: data.remark || '',
    })
    editIsDefault.value = data.isDefault === 1
    temperatureText.value = String(data.temperature ?? 0.7)
    maxTokensText.value = String(data.maxTokens ?? 8192)
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

.provider-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14rpx;
  margin: 12rpx 0 24rpx;
}

.form-textarea-cell {
  padding: 20rpx 32rpx;
  border-bottom: 1px solid $color-border-light;

  &__label {
    display: block;
    margin-bottom: 12rpx;
    font-size: $font-size-md;
    color: $color-text-primary;
  }

  &__input {
    width: 100%;
    min-height: 160rpx;
    padding: 16rpx 20rpx;
    box-sizing: border-box;
    font-size: $font-size-base;
    line-height: 1.6;
    color: $color-text-primary;
    background: $color-bg-card;
    border: 1px solid $color-border-light;
    border-radius: 16rpx;
  }
}

.provider-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 16rpx 6rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 2rpx solid #e2e8f0;
  transition: all 0.2s ease;

  &.active {
    background: rgba(79, 70, 229, 0.06);
    border-color: #4f46e5;
    box-shadow: 0 4rpx 12rpx rgba(79, 70, 229, 0.12);

    .provider-tile__label {
      color: #4f46e5;
      font-weight: 700;
    }
  }
}

.provider-tile__icon-wrap {
  width: 52rpx;
  height: 52rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.provider-tile__label {
  font-size: 22rpx;
  color: #475569;
  font-weight: 600;
  text-align: center;
  line-height: 1.2;
}

.model-value-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  width: 100%;
  min-height: 72rpx;
  padding: 0 12rpx 0 24rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-full;
  background: $color-bg-card;
  box-sizing: border-box;
}

.model-value {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: $font-size-base;
  color: $color-text-primary;
}

.model-value--muted {
  color: $color-text-placeholder;
}

.model-fetch-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 56rpx;
  padding: 0 20rpx;
  border-radius: $radius-full;
  background: rgba(79, 70, 229, 0.08);
  color: #4f46e5;
  font-size: $font-size-sm;
  font-weight: 600;

  &:active {
    background: rgba(79, 70, 229, 0.16);
  }

  &.is-loading {
    opacity: 0.6;
  }
}

.picker-value-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
}
</style>
