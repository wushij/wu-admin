<template>
  <view class="module-form-page">
    <ModuleDarkHero
      title="新建工单"
      subtitle="填写标题并指定处理人"
      icon="records-o"
      theme="ticket"
    />

    <ListLoading v-if="!ready" />

    <template v-else>
      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="edit" theme="ticket" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">工单内容</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-model="form.title"
            label="标题"
            editable
            boxed
            placeholder="请输入工单标题"
          />

          <FormCell label="描述" boxed last class="form-cell--multiline">
            <textarea
              v-model="form.description"
              class="form-cell__textarea"
              placeholder="补充问题说明（选填）"
              :maxlength="2000"
            />
          </FormCell>
        </view>
      </view>

      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="cluster-o" theme="ticket" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">处理设置</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="优先级" clickable boxed arrow @click="pickPriority">
            <text class="picker-value" :class="{ 'picker-value--muted': !priorityLabel }">
              {{ priorityLabel || '请选择' }}
            </text>
          </FormCell>

          <FormCell label="处理人" clickable boxed arrow @click="pickAssignee">
            <text class="picker-value" :class="{ 'picker-value--muted': !assigneeDisplay }">
              {{ assigneeDisplay || '请选择' }}
            </text>
          </FormCell>

          <FormCell label="截止日期" boxed last>
            <picker mode="date" :value="deadlineDate" @change="onDeadlineChange">
              <text class="picker-value" :class="{ 'picker-value--muted': !deadlineDate }">
                {{ deadlineDate || '选填' }}
              </text>
            </picker>
          </FormCell>
        </view>
      </view>
    </template>

    <PageFooter v-if="ready">
      <button class="page-footer__btn" :loading="saving" @click="onSubmit">提交工单</button>
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
import { useTicketCreateForm } from '@/composables/useTicketCreateForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import { showActionSheet } from '@/utils/app-dialog'
import type { AssigneeOptionVO } from '@/types/system'

const {
  saving,
  ready,
  form,
  assignees,
  assigneeIndex,
  priorityOptions,
  priorityIndex,
  initPage,
  onAssigneeChange,
  onPriorityChange,
  onDeadlineChange,
  submit,
} = useTicketCreateForm()

const { leaveAfterSave } = useEditPageGuard(
  () => ({
    title: form.title,
    description: form.description,
    priority: form.priority,
    assigneeUserId: form.assigneeUserId,
    deadline: form.deadline,
    assigneeIndex: assigneeIndex.value,
    priorityIndex: priorityIndex.value,
  }),
  { loading: computed(() => !ready.value), fallbackUrl: '/pages-sub/system/ticket/index' },
)

const deadlineDate = computed(() => (form.deadline ? form.deadline.slice(0, 10) : ''))

const priorityLabel = computed(() => priorityOptions.value[priorityIndex.value]?.label || '')

function assigneeLabel(item: AssigneeOptionVO) {
  if (item.id === 0) return '全员通知'
  return `${item.nickname || item.username}`
}

const assigneeDisplay = computed(() => {
  const item = assignees.value[assigneeIndex.value]
  return item ? assigneeLabel(item) : ''
})

onLoad(() => {
  initPage()
})

async function pickPriority() {
  if (!priorityOptions.value.length) {
    uni.showToast({ title: '优先级加载中', icon: 'none' })
    return
  }
  try {
    const index = await showActionSheet({
      title: '优先级',
      items: priorityOptions.value.map((o) => ({ label: o.label })),
    })
    onPriorityChange({ detail: { value: index } })
  } catch {
    /* cancelled */
  }
}

async function pickAssignee() {
  if (!assignees.value.length) {
    uni.showToast({ title: '暂无可选处理人', icon: 'none' })
    return
  }
  try {
    const index = await showActionSheet({
      title: '处理人',
      items: assignees.value.map((a) => ({ label: assigneeLabel(a) })),
    })
    onAssigneeChange({ detail: { value: index } })
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
