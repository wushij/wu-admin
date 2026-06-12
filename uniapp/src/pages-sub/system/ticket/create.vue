<template>
  <view class="page-padded create-page">
    <view class="form-panel">
      <FormCell v-model="form.title" label="标题" editable placeholder="必填" />
      <view class="content-field">
        <text class="content-field__label">描述</text>
        <textarea v-model="form.description" class="content-field__input" placeholder="选填" />
      </view>
      <FormCell label="优先级" clickable arrow @click="pickPriority">
        <text class="picker-value">{{ priorityOptions[priorityIndex]?.label || '请选择' }}</text>
      </FormCell>
      <FormCell label="处理人" clickable arrow @click="pickAssignee">
        <text class="picker-value">{{ assigneeDisplay }}</text>
      </FormCell>
      <FormCell label="截止日期" last>
        <picker mode="date" :value="deadlineDate" @change="onDeadlineChange">
          <text class="picker-value">{{ deadlineDate || '选填' }}</text>
        </picker>
      </FormCell>
    </view>

    <PageFooter>
      <button class="page-footer__btn" :loading="saving" @click="submit">提交工单</button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { useTicketCreateForm } from '@/composables/useTicketCreateForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import type { AssigneeOptionVO } from '@/types/system'

const {
  saving,
  form,
  assignees,
  assigneeIndex,
  priorityOptions,
  priorityIndex,
  onAssigneeChange,
  onPriorityChange,
  onDeadlineChange,
  submit,
} = useTicketCreateForm()

useEditPageGuard(() => ({
  ...form,
  assigneeIndex: assigneeIndex.value,
  priorityIndex: priorityIndex.value,
}))

const deadlineDate = computed(() => (form.deadline ? form.deadline.slice(0, 10) : ''))

function assigneeLabel(item: AssigneeOptionVO) {
  if (item.id === 0) return '全员通知'
  return `${item.nickname || item.username}`
}

const assigneeDisplay = computed(() => {
  const item = assignees.value[assigneeIndex.value]
  if (!item) return '请选择'
  return assigneeLabel(item)
})

function pickPriority() {
  if (!priorityOptions.value.length) return
  uni.showActionSheet({
    itemList: priorityOptions.value.map((o) => o.label),
    success: (res) => onPriorityChange({ detail: { value: res.tapIndex } }),
  })
}

function pickAssignee() {
  if (!assignees.value.length) return
  uni.showActionSheet({
    itemList: assignees.value.map((a) => assigneeLabel(a)),
    success: (res) => onAssigneeChange({ detail: { value: res.tapIndex } }),
  })
}

</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.create-page {
  min-height: 100vh;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.content-field {
  padding: 24rpx 32rpx;
  border-bottom: 1px solid $color-border-light;
}

.content-field__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-md;
  color: $color-text-primary;
}

.content-field__input {
  width: 100%;
  min-height: 160rpx;
  padding: 16rpx;
  border-radius: $radius-sm;
  background: $color-bg-muted;
  font-size: $font-size-base;
  box-sizing: border-box;
}
</style>
