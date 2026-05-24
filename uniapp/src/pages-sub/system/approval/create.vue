<template>
  <view class="module-form-page">
    <ModuleDarkHero
      title="提交审批单"
      subtitle="选择类型并指定审批人"
      icon="completed"
      theme="approval"
    />

    <ListLoading v-if="!ready" />

    <template v-else>
      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="edit" theme="approval" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">审批内容</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="审批类型" clickable boxed arrow @click="pickFormType">
            <text class="picker-value" :class="{ 'picker-value--muted': !formTypeLabel }">
              {{ formTypeLabel || '请选择' }}
            </text>
          </FormCell>

          <FormCell
            v-model="form.title"
            label="标题"
            editable
            boxed
            placeholder="请输入审批标题"
          />

          <FormCell label="内容" boxed last class="form-cell--multiline">
            <textarea
              v-model="form.content"
              class="form-cell__textarea"
              placeholder="请输入审批说明（选填）"
              :maxlength="2000"
            />
          </FormCell>
        </view>
      </view>

      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="friends-o" theme="approval" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">指定审批人</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="审批人" clickable boxed arrow last @click="pickApprover">
            <text class="picker-value" :class="{ 'picker-value--muted': !approverDisplay }">
              {{ approverDisplay || '请选择' }}
            </text>
          </FormCell>
        </view>
      </view>
    </template>

    <PageFooter v-if="ready">
      <button class="page-footer__btn" :loading="saving" @click="onSubmit">提交审批单</button>
    </PageFooter>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useApprovalCreateForm } from '@/composables/useApprovalCreateForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import { showActionSheet } from '@/utils/app-dialog'

const {
  saving,
  ready,
  form,
  approvers,
  approverIndex,
  formTypeIndex,
  availableFormTypes,
  initPage,
  onFormTypeChange,
  onApproverChange,
  approverLabel,
  submit,
} = useApprovalCreateForm()

const { leaveAfterSave } = useEditPageGuard(
  () => ({
    formType: form.formType,
    title: form.title,
    approverUserId: form.approverUserId,
    content: form.content,
    formTypeIndex: formTypeIndex.value,
    approverIndex: approverIndex.value,
  }),
  { loading: computed(() => !ready.value), fallbackUrl: '/pages-sub/system/approval/index' },
)

const formTypeLabel = computed(() => availableFormTypes.value[formTypeIndex.value]?.label || '')

const approverDisplay = computed(() => {
  if (form.approverUserId == null) return ''
  const hit = approvers.value.find((u) => Number(u.id) === Number(form.approverUserId))
  return hit ? approverLabel(hit) : ''
})

onLoad(() => {
  initPage()
})

async function pickFormType() {
  if (!ready.value) {
    uni.showToast({ title: '审批类型加载中', icon: 'none' })
    return
  }
  if (!availableFormTypes.value.length) {
    uni.showToast({ title: '暂无可用审批类型', icon: 'none' })
    return
  }
  try {
    const index = await showActionSheet({
      title: '审批类型',
      items: availableFormTypes.value.map((o) => ({ label: o.label })),
    })
    onFormTypeChange({ detail: { value: index } })
  } catch {
    /* cancelled */
  }
}

async function pickApprover() {
  if (!approvers.value.length) {
    uni.showToast({ title: '暂无可选审批人', icon: 'none' })
    return
  }
  try {
    const index = await showActionSheet({
      title: '审批人',
      items: approvers.value.map((u) => ({ label: approverLabel(u) })),
    })
    onApproverChange({ detail: { value: index } })
  } catch {
    /* cancelled */
  }
}

async function onSubmit() {
  const ok = await submit()
  if (ok) leaveAfterSave()
}
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;
@use '@/styles/module-form-page.scss' as *;
</style>
