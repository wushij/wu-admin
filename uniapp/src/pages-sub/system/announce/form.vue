<template>
  <view class="page-padded edit-page">
    <ListLoading v-if="loading" />
    <view v-else class="form-panel">
      <FormCell v-model="form.title" label="标题" editable placeholder="必填" />
      <FormCell label="类型" clickable arrow @click="pickType">
        <text class="picker-value">{{ typeOptions[typeIndex]?.label }}</text>
      </FormCell>
      <FormCell label="通知对象" clickable arrow @click="pickTarget">
        <text class="picker-value">{{ targetOptions[targetIndex]?.label }}</text>
      </FormCell>

      <view v-if="form.targetType === 1" class="target-panel">
        <text class="target-panel__title">选择用户（已选 {{ targetIds.length }}）</text>
        <view v-for="user in userRows" :key="user.id" class="target-row">
          <text class="target-row__label">{{ user.label }}</text>
          <switch :checked="isTargetSelected(user.id)" @change="onUserToggle(user.id, $event)" />
        </view>
        <EmptyState v-if="!userRows.length" title="暂无用户" icon="friends-o" />
      </view>

      <FormCell
        v-if="form.targetType === 2"
        label="选择部门"
        clickable
        arrow
        @click="pickDepts"
      >
        <text class="picker-value">{{ deptLabel }}</text>
      </FormCell>

      <view class="content-field">
        <text class="content-field__label">内容</text>
        <textarea v-model="form.content" class="content-field__input" placeholder="请输入通知内容" />
      </view>
    </view>
    <PageFooter>
      <button class="page-footer__btn page-footer__btn--ghost" :loading="saving" @click="save(announceId, false)">保存草稿</button>
      <button v-if="!isCreate || canPublish" class="page-footer__btn" :loading="saving" @click="save(announceId, true)">发布</button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { useAnnounceForm } from '@/composables/useAnnounceForm'
import { useModulePermission } from '@/composables/useModulePermission'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import { setTreeSelectDeptIds } from '@/utils/tree-select-init'
import { appendNavFromParam } from '@/utils/nav-from'

const announceId = ref(0)
const { hasPerm } = useModulePermission('system:announce:list')
const canPublish = computed(() => hasPerm('system:announce:publish'))

const {
  loading, saving, isCreate, form, typeOptions, targetOptions, typeIndex, targetIndex,
  targetIds, deptLabel, userRows,
  initCreate, load, loadUserOptions, onTypeChange, onTargetChange, isTargetSelected, toggleTarget, setDeptSelection, save,
} = useAnnounceForm()

const { resetBaseline } = useEditPageGuard(
  () => ({
    ...form,
    typeIndex: typeIndex.value,
    targetIndex: targetIndex.value,
    targetIds: [...targetIds.value].sort((a, b) => a - b),
    deptLabel: deptLabel.value,
  }),
  { loading },
)

function pickType() {
  uni.showActionSheet({
    itemList: typeOptions.map((o) => o.label),
    success: (res) => onTypeChange({ detail: { value: res.tapIndex } }),
  })
}

function pickTarget() {
  uni.showActionSheet({
    itemList: targetOptions.map((o) => o.label),
    success: (res) => onTargetChange({ detail: { value: res.tapIndex } }),
  })
}

function onUserToggle(id: number, e: { detail: { value: boolean } }) {
  toggleTarget(id, e.detail.value)
}

function pickDepts() {
  setTreeSelectDeptIds([...targetIds.value])
  uni.navigateTo({
    url: appendNavFromParam(
      `/pages-sub/system/tree-select?type=dept-multi&title=${encodeURIComponent('选择部门')}`,
    ),
  })
}

function applyPickerResults() {
  try {
    const raw = uni.getStorageSync('uniapp_page_picker_result')
    if (!raw) return
    uni.removeStorageSync('uniapp_page_picker_result')
    const result = JSON.parse(String(raw)) as {
      kind: string
      ids?: number[]
      labelText?: string
    }
    if (result.kind === 'dept-multi' && Array.isArray(result.ids)) {
      setDeptSelection(result.ids, result.labelText || '')
    }
  } catch {
    uni.removeStorageSync('uniapp_page_picker_result')
  }
}

onShow(() => {
  applyPickerResults()
})

onLoad(async (options) => {
  await loadUserOptions()
  if (options?.mode === 'create') {
    initCreate()
    resetBaseline()
    return
  }
  announceId.value = Number(options?.id)
  if (announceId.value) load(announceId.value)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.edit-page { min-height: 100vh; padding-bottom: calc(140rpx + env(safe-area-inset-bottom)); }

.target-panel {
  padding: 8rpx 0 16rpx;
  border-top: 1px solid $color-border-light;
}

.target-panel__title {
  display: block;
  padding: 16rpx 32rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.target-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 16rpx 32rpx;
  border-bottom: 1px solid $color-border-light;
}

.target-row__label {
  flex: 1;
  font-size: $font-size-base;
  color: $color-text-primary;
}

.content-field {
  padding: 24rpx 32rpx 32rpx;
}

.content-field__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-md;
  color: $color-text-primary;
}

.content-field__input {
  width: 100%;
  min-height: 280rpx;
  padding: 16rpx;
  border-radius: $radius-sm;
  background: $color-bg-muted;
  font-size: $font-size-base;
  box-sizing: border-box;
}
</style>
