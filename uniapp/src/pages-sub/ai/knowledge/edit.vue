<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="isCreate ? '新增知识' : '编辑知识'"
      :subtitle="isCreate ? '录入可被 AI 检索的知识条目' : '修改后立即生效'"
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
            v-model="form.title"
            label="标题"
            editable
            boxed
            :maxlength="120"
            placeholder="如：如何修改登录密码"
          />
          <FormCell
            v-model="form.keywords"
            label="关键词"
            editable
            boxed
            :maxlength="200"
            placeholder="逗号分隔，提升召回率"
          />
          <FormCell label="分类" clickable boxed arrow @click="pickCategory">
            <text class="picker-value">{{ categoryLabel }}</text>
          </FormCell>
          <FormCell label="排序">
            <NumberStepper
              v-model="sortValue"
              :min="0"
              :max="9999"
            />
          </FormCell>
          <FormCell label="启用" switch-cell last>
            <switch :checked="form.status === 1" color="#6366f1" @change="onStatusChange" />
          </FormCell>
        </view>
      </view>

      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="edit" theme="ai" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">知识内容</text>
            <text class="form-section__desc">填写用户提问时希望命中的标准答案</text>
          </view>
        </view>

        <view class="knowledge-editor">
          <textarea
            v-model="form.content"
            class="knowledge-content"
            :maxlength="-1"
            placeholder="请输入知识内容，例如：进入「我的 - 修改密码」，输入原密码与新密码后保存……"
          />
          <view class="knowledge-content__foot">
            <text class="knowledge-content__hint">支持 Markdown · 建议分步骤描述</text>
            <text class="knowledge-content__count">{{ form.content.length }} 字</text>
          </view>
        </view>
      </view>

      <PageFooter>
        <button class="page-footer__btn page-footer__btn--primary-outline" @click="goBack">
          取消
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
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/base/ModuleIcon/index.vue'
import ListLoading from '@/components/base/ListLoading/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import NumberStepper from '@/components/base/NumberStepper/index.vue'
import PageFooter from '@/components/base/PageFooter/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import {
  getAiKnowledge,
  createAiKnowledge,
  updateAiKnowledge,
  type AiKnowledgeSaveDTO,
} from '@/api/system/ai-knowledge'
import { showActionSheet } from '@/utils/app-dialog'
import { showSuccessToast } from '@/utils/app-toast'

const knowledgeId = ref(0)
const loading = ref(false)
const saving = ref(false)
const isCreate = computed(() => knowledgeId.value <= 0)

const form = reactive<AiKnowledgeSaveDTO>({
  title: '',
  keywords: '',
  content: '',
  category: 'faq',
  sort: 0,
  status: 1,
})

const categoryOptions = [
  { label: '常见问题', value: 'faq' },
  { label: '操作手册', value: 'manual' },
  { label: '功能模块', value: 'module' },
  { label: '其他', value: 'other' },
]

const categoryLabel = computed(
  () => categoryOptions.find((item) => item.value === form.category)?.label || '常见问题',
)

const sortValue = computed<number>({
  get: () => form.sort ?? 0,
  set: (val: number) => {
    form.sort = val
  },
})

async function pickCategory() {
  try {
    const index = await showActionSheet({
      title: '选择知识分类',
      items: categoryOptions.map((item) => ({ label: item.label })),
    })
    const option = categoryOptions[index]
    if (option) form.category = option.value
  } catch {
    /* 取消选择 */
  }
}

function onStatusChange(event: { detail: { value: boolean } }) {
  form.status = event.detail.value ? 1 : 0
}

async function loadDetail(id: number) {
  loading.value = true
  try {
    const res = await getAiKnowledge(id)
    const data = res.data
    if (!data) return
    form.title = data.title || ''
    form.keywords = data.keywords || ''
    form.content = data.content || ''
    form.category = data.category || 'faq'
    form.sort = data.sort ?? 0
    form.status = data.status ?? 1
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const title = form.title.trim()
  const content = form.content.trim()
  if (!title) {
    uni.showToast({ title: '请输入标题', icon: 'none' })
    return
  }
  if (!content) {
    uni.showToast({ title: '请输入知识内容', icon: 'none' })
    return
  }
  saving.value = true
  try {
    const payload: AiKnowledgeSaveDTO = {
      title,
      keywords: form.keywords?.trim() || undefined,
      content,
      category: form.category,
      sort: form.sort,
      status: form.status,
    }
    if (isCreate.value) {
      await createAiKnowledge(payload)
      showSuccessToast('创建成功')
    } else {
      await updateAiKnowledge({ ...payload, id: knowledgeId.value })
      showSuccessToast('保存成功')
    }
    setTimeout(goBack, 400)
  } finally {
    saving.value = false
  }
}

function goBack() {
  uni.navigateBack({
    delta: 1,
    fail: () => {
      uni.redirectTo({ url: '/pages-sub/ai/knowledge/index' })
    },
  })
}

onLoad((options) => {
  const id = Number(options?.id || 0)
  if (id > 0) {
    knowledgeId.value = id
    void loadDetail(id)
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;
@use '@/styles/module-form-page.scss' as *;

.knowledge-editor {
  padding: 8rpx 0 12rpx;
}

.knowledge-content {
  width: 100%;
  min-height: 420rpx;
  padding: 20rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: $color-bg-muted;
  font-size: $font-size-sm;
  line-height: 1.65;
  color: $color-text-primary;
  box-sizing: border-box;
}

.knowledge-content__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10rpx;
}

.knowledge-content__hint {
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}

.knowledge-content__count {
  font-size: $font-size-xs;
  color: $color-text-placeholder;
}
</style>
