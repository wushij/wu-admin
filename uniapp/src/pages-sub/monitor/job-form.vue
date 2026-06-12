<template>
  <view class="page-padded edit-page">
    <view class="form-panel">
      <FormCell v-model="form.jobName" label="任务名称" editable placeholder="必填" />
      <FormCell v-model="form.jobGroup" label="任务分组" editable placeholder="DEFAULT" />
      <FormCell v-model="form.invokeTarget" label="调用目标" editable placeholder="bean.method()" />
      <FormCell v-model="form.cronExpression" label="Cron 表达式" editable placeholder="0 0/5 * * * ?" />
      <FormCell label="状态" clickable arrow @click="pickStatus">
        <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
      </FormCell>
      <FormCell v-model="form.remark" label="备注" editable placeholder="选填" last />
    </view>
    <PageFooter>
      <button class="page-footer__btn" :loading="saving" @click="save(jobId)">
        {{ isCreate ? '创建任务' : '保存' }}
      </button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { useJobForm } from '@/composables/useJobForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'

const jobId = ref<number | undefined>()
const {
  saving, isCreate, form, statusOptions, statusIndex,
  initCreate, loadFromJob, onStatusChange, save,
} = useJobForm()

const { resetBaseline } = useEditPageGuard(() => ({
  ...form,
  statusIndex: statusIndex.value,
}))

function pickStatus() {
  uni.showActionSheet({
    itemList: statusOptions.map((s) => s.label),
    success: (res) => onStatusChange({ detail: { value: res.tapIndex } }),
  })
}

onLoad((options) => {
  if (options?.mode === 'create') {
    initCreate()
    resetBaseline()
    return
  }
  const id = Number(options?.id)
  if (!id) return
  jobId.value = id
  try {
    const cached = uni.getStorageSync('job_edit_cache')
    if (cached) {
      loadFromJob(JSON.parse(String(cached)))
      uni.removeStorageSync('job_edit_cache')
    }
  } catch {
    /* ignore */
  }
  resetBaseline()
})
</script>

<style lang="scss" scoped>
@import '@/styles/common.scss';
.edit-page { min-height: 100vh; padding-bottom: calc(140rpx + env(safe-area-inset-bottom)); }
</style>
