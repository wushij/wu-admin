<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="isCreate ? '新建知识库' : '编辑知识库'"
      :subtitle="isCreate ? '配置切分策略与向量模型，上传文档后自动索引' : '调整知识库配置，重新索引后生效'"
      icon="notes-o"
      theme="ai"
    />

    <ListLoading v-if="loading" />

    <template v-else>
      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="notes-o" theme="ai" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">基础信息</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-model="form.name"
            label="知识库名称"
            editable
            boxed
            :maxlength="60"
            placeholder="如：产品操作手册"
          />
          <FormCell label="描述" boxed>
            <textarea
              v-model="form.description"
              class="kb-textarea"
              :maxlength="300"
              :auto-height="false"
              placeholder="简要说明该知识库收录的内容范围"
              placeholder-class="kb-textarea__placeholder"
            />
          </FormCell>
        </view>
      </view>

      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="cluster-o" theme="ai" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">切分与向量</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="向量模型" clickable boxed arrow @click="pickEmbeddingModel">
            <text class="picker-value" :class="{ 'picker-value--muted': !embeddingLabel }">
              {{ embeddingLabel || '跟随默认向量模型' }}
            </text>
          </FormCell>
          <FormCell
            v-model="form.chunkSize"
            label="切片大小"
            editable
            boxed
            input-type="number"
            placeholder="默认 800"
            hint="建议 500-1500 字符"
          />
          <FormCell
            v-model="form.chunkOverlap"
            label="切片重叠"
            editable
            boxed
            input-type="number"
            placeholder="默认 100"
            hint="需小于切片大小"
          />
          <FormCell label="排序" editable boxed input-type="number" v-model="form.sort" placeholder="数值越小越靠前" />
          <FormCell label="启用状态" switch-cell last>
            <switch :checked="form.status === 1" color="#6366f1" @change="onStatusChange" />
          </FormCell>
        </view>
      </view>

      <view class="form-tip">
        <text>
          修改切分参数或向量模型后，已有切片不会自动重算，需在知识库详情页执行「重建索引」。
        </text>
      </view>

      <view class="form-footer">
        <button class="form-footer__btn" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </view>
    </template>

    <AppDialogHost />
    <AppActionSheet
      :visible="modelSheetVisible"
      title="选择向量模型"
      :items="modelItems"
      @pick="onPickEmbeddingModel"
      @cancel="modelSheetVisible = false"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/base/ModuleIcon/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import ListLoading from '@/components/base/ListLoading/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import AppActionSheet from '@/components/base/AppActionSheet/index.vue'
import {
  getKnowledgeBase,
  createKnowledgeBase,
  updateKnowledgeBase,
  listEmbeddingModels,
  type EmbeddingModelOption,
} from '@/api/system/ai-knowledge-base'
import type { ActionSheetItem } from '@/store/dialog'

const loading = ref(false)
const saving = ref(false)
const isCreate = ref(true)
let knowledgeBaseId = 0

const form = reactive({
  name: '',
  description: '',
  embeddingModelId: '' as string,
  chunkSize: '',
  chunkOverlap: '',
  sort: '',
  status: 1,
})

const models = ref<EmbeddingModelOption[]>([])
const modelSheetVisible = ref(false)

const embeddingLabel = computed(() => {
  if (!form.embeddingModelId) return ''
  const model = models.value.find((item) => String(item.id) === form.embeddingModelId)
  return model ? `${model.name}（${model.modelName}）` : '已选择向量模型'
})

const modelItems = computed<ActionSheetItem[]>(() => [
  { label: '跟随默认向量模型' },
  ...models.value.map((model) => ({
    label: `${model.name}（${model.modelName}）${model.dimension ? ` · ${model.dimension}维` : ''}`,
  })),
])

onLoad(async (query) => {
  const id = Number(query?.id || 0)
  knowledgeBaseId = id
  isCreate.value = !id
  loading.value = true
  try {
    const [modelRes] = await Promise.all([listEmbeddingModels()])
    models.value = modelRes.data || []
    if (!id) return
    const detail = await getKnowledgeBase(id)
    const data = detail.data
    if (!data) {
      uni.showToast({ title: '知识库不存在', icon: 'none' })
      return
    }
    form.name = data.name || ''
    form.description = data.description || ''
    form.embeddingModelId = data.embeddingModelId ? String(data.embeddingModelId) : ''
    form.chunkSize = data.chunkSize != null ? String(data.chunkSize) : ''
    form.chunkOverlap = data.chunkOverlap != null ? String(data.chunkOverlap) : ''
    form.sort = data.sort != null ? String(data.sort) : ''
    form.status = data.status ?? 1
  } catch {
    /* 错误提示由请求层统一处理 */
  } finally {
    loading.value = false
  }
})

function pickEmbeddingModel() {
  modelSheetVisible.value = true
}

function onPickEmbeddingModel(index: number) {
  modelSheetVisible.value = false
  if (index <= 0) {
    form.embeddingModelId = ''
    return
  }
  const model = models.value[index - 1]
  if (model) {
    form.embeddingModelId = String(model.id)
  }
}

function onStatusChange(event: { detail: { value: boolean } }) {
  form.status = event.detail.value ? 1 : 0
}

function validate(): string | null {
  if (!form.name.trim()) return '请填写知识库名称'
  const chunkSize = form.chunkSize ? Number(form.chunkSize) : undefined
  const chunkOverlap = form.chunkOverlap ? Number(form.chunkOverlap) : undefined
  if (chunkSize != null && (!Number.isFinite(chunkSize) || chunkSize < 100 || chunkSize > 4000)) {
    return '切片大小需在 100-4000 之间'
  }
  if (chunkOverlap != null && (!Number.isFinite(chunkOverlap) || chunkOverlap < 0)) {
    return '切片重叠不能为负数'
  }
  if (chunkSize != null && chunkOverlap != null && chunkOverlap >= chunkSize) {
    return '切片重叠必须小于切片大小'
  }
  return null
}

async function handleSave() {
  if (saving.value) return
  const error = validate()
  if (error) {
    uni.showToast({ title: error, icon: 'none' })
    return
  }
  saving.value = true
  try {
    const payload = {
      id: isCreate.value ? undefined : knowledgeBaseId,
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      embeddingModelId: form.embeddingModelId ? Number(form.embeddingModelId) : undefined,
      chunkSize: form.chunkSize ? Number(form.chunkSize) : undefined,
      chunkOverlap: form.chunkOverlap ? Number(form.chunkOverlap) : undefined,
      sort: form.sort ? Number(form.sort) : undefined,
      status: form.status,
    }
    if (isCreate.value) {
      await createKnowledgeBase(payload)
      uni.showToast({ title: '已创建知识库', icon: 'success' })
    } else {
      await updateKnowledgeBase(payload)
      uni.showToast({ title: '已保存修改', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 400)
  } catch {
    /* 错误提示由请求层统一处理 */
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/variables.scss' as *;
@use '@/styles/common.scss' as *;
@use '@/styles/module-form-page.scss' as *;

.kb-textarea {
  width: 100%;
  height: 132rpx;
  font-size: 26rpx;
  line-height: 1.5;
  color: $color-text-primary;
}

.kb-textarea__placeholder {
  color: $color-text-placeholder;
}

.picker-value {
  font-size: 26rpx;
  color: $color-text-primary;

  &--muted {
    color: $color-text-placeholder;
  }
}

.form-tip {
  margin: 8rpx 12rpx 160rpx;
  padding: 18rpx 22rpx;
  border-radius: $radius-md;
  background: rgba(22, 119, 255, 0.06);

  text {
    font-size: 22rpx;
    line-height: 1.55;
    color: #3b82f6;
  }
}

.form-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx $page-padding-x calc(18rpx + env(safe-area-inset-bottom));
  background: $color-bg-card;
  border-top: 1px solid $color-border-light;
}

.form-footer__btn {
  height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #1677ff 0%, #6366f1 100%);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  line-height: 88rpx;

  &::after {
    border: none;
  }

  &[disabled] {
    opacity: 0.6;
  }
}
</style>
