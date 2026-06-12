import { ref, reactive } from 'vue'
import { createJob, updateJob } from '@/api/monitor/job'
import type { SysJob } from '@/types/system'

export function useJobForm() {
  const loading = ref(false)
  const saving = ref(false)
  const isCreate = ref(false)

  const form = reactive<SysJob>({
    jobName: '',
    jobGroup: 'DEFAULT',
    invokeTarget: '',
    cronExpression: '',
    status: 1,
    remark: '',
    concurrent: 1,
    misfirePolicy: 1,
  })

  const statusOptions = [
    { label: '运行', value: 1 },
    { label: '暂停', value: 0 },
  ]
  const statusIndex = ref(0)

  function initCreate() {
    isCreate.value = true
    form.jobName = ''
    form.jobGroup = 'DEFAULT'
    form.invokeTarget = ''
    form.cronExpression = ''
    form.status = 1
    form.remark = ''
    statusIndex.value = 0
    uni.setNavigationBarTitle({ title: '新增任务' })
  }

  function loadFromJob(job: SysJob) {
    isCreate.value = false
    form.jobName = job.jobName
    form.jobGroup = job.jobGroup || 'DEFAULT'
    form.invokeTarget = job.invokeTarget
    form.cronExpression = job.cronExpression
    form.status = job.status ?? 1
    form.remark = job.remark || ''
    form.concurrent = job.concurrent ?? 1
    form.misfirePolicy = job.misfirePolicy ?? 1
    statusIndex.value = form.status === 0 ? 1 : 0
  }

  function onStatusChange(e: { detail: { value: number } }) {
    statusIndex.value = Number(e.detail.value)
    form.status = statusOptions[statusIndex.value]?.value ?? 1
  }

  async function save(id?: number) {
    if (!form.jobName.trim() || !form.invokeTarget.trim() || !form.cronExpression.trim()) {
      uni.showToast({ title: '请填写必填项', icon: 'none' })
      return
    }
    saving.value = true
    try {
      const payload = { ...form, jobName: form.jobName.trim(), invokeTarget: form.invokeTarget.trim() }
      if (isCreate.value) await createJob(payload)
      else await updateJob({ ...payload, id })
      uni.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 400)
    } finally {
      saving.value = false
    }
  }

  return {
    loading,
    saving,
    isCreate,
    form,
    statusOptions,
    statusIndex,
    initCreate,
    loadFromJob,
    onStatusChange,
    save,
  }
}

