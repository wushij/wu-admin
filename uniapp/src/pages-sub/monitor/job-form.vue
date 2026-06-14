<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="isCreate ? '新增任务' : '编辑任务'"
      :subtitle="isCreate ? '配置调用目标与 Cron 表达式' : '修改任务调度参数'"
      icon="clock-o"
      theme="job"
    />

    <view class="form-section card--elevated">
      <view class="form-section__head">
        <ModuleIcon icon="clock-o" theme="job" size="sm" />
        <view class="form-section__intro">
          <text class="form-section__title">任务信息</text>
        </view>
      </view>

      <view class="form-fields">
        <FormCell
          v-model="form.jobName"
          label="任务名称"
          editable
          boxed
          placeholder="请输入任务名称"
        />
        <FormCell
          v-model="form.jobGroup"
          label="任务分组"
          editable
          boxed
          placeholder="DEFAULT"
          last
        />
      </view>
    </view>

    <view class="form-section card--elevated">
      <view class="form-section__head">
        <ModuleIcon icon="setting-o" theme="job" size="sm" />
        <view class="form-section__intro">
          <text class="form-section__title">调度配置</text>
        </view>
      </view>

      <view class="form-fields">
        <FormCell
          v-model="form.invokeTarget"
          label="调用目标"
          editable
          boxed
          placeholder="如 demoJob.run()"
        />
        <FormCell
          v-model="form.cronExpression"
          label="Cron 表达式"
          editable
          boxed
          placeholder="0 0/5 * * * ?"
          last
        />
      </view>
    </view>

    <view class="form-section card--elevated">
      <view class="form-section__head">
        <ModuleIcon icon="notes-o" theme="job" size="sm" />
        <view class="form-section__intro">
          <text class="form-section__title">状态与备注</text>
        </view>
      </view>

      <view class="form-fields">
        <FormCell label="状态" clickable boxed arrow @click="pickStatus">
          <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
        </FormCell>
        <FormCell v-model="form.remark" label="备注" editable boxed placeholder="选填" last />
      </view>
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
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { useJobForm } from '@/composables/useJobForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'

const jobId = ref<number | undefined>()
const {
  saving,
  isCreate,
  form,
  statusOptions,
  statusIndex,
  initCreate,
  loadFromJob,
  onStatusChange,
  save,
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
@import '@/styles/variables.scss';
@import '@/styles/common.scss';
@import '@/styles/module-form-page.scss';
</style>
