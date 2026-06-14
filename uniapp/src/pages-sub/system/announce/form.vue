<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="isCreate ? '新增通知' : '编辑通知'"
      :subtitle="isCreate ? '撰写通知并选择发送对象' : '修改通知内容与发布范围'"
      icon="bell"
      theme="notice"
    />

    <ListLoading v-if="loading" />

    <template v-else>
      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="bell" theme="notice" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">基本信息</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-model="form.title"
            label="标题"
            editable
            boxed
            placeholder="请输入通知标题"
          />
          <FormCell label="类型" clickable boxed arrow @click="pickType">
            <text class="picker-value">{{ typeOptions[typeIndex]?.label }}</text>
          </FormCell>
          <FormCell
            label="通知对象"
            clickable
            boxed
            arrow
            :last="form.targetType === 3"
            @click="pickTarget"
          >
            <text class="picker-value">{{ targetOptions[targetIndex]?.label }}</text>
          </FormCell>
        </view>
      </view>

      <view v-if="form.targetType === 1" class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="friends-o" theme="notice" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">指定用户</text>
          </view>
        </view>

        <view class="form-fields">
          <view class="target-summary">已选 {{ targetIds.length }} 人</view>
          <view v-for="user in userRows" :key="user.id" class="target-row">
            <text class="target-row__label">{{ user.label }}</text>
            <switch :checked="isTargetSelected(user.id)" @change="onUserToggle(user.id, $event)" />
          </view>
          <EmptyState v-if="!userRows.length" title="暂无用户" icon="friends-o" />
        </view>
      </view>

      <view v-else-if="form.targetType === 2" class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="cluster-o" theme="notice" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">指定部门</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="选择部门" clickable boxed arrow last @click="pickDepts">
            <text
              class="picker-value"
              :class="{ 'picker-value--muted': deptLabel === '请选择部门' }"
            >
              {{ deptLabel }}
            </text>
          </FormCell>
        </view>
      </view>

      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="notes-o" theme="notice" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">通知内容</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="内容" boxed last class="form-cell--multiline">
            <textarea
              v-model="form.content"
              class="form-cell__textarea"
              placeholder="请输入通知内容"
              :maxlength="5000"
            />
          </FormCell>
        </view>
      </view>

      <PageFooter>
        <button
          class="page-footer__btn page-footer__btn--ghost"
          :loading="saving"
          @click="save(announceId, false)"
        >
          保存草稿
        </button>
        <button
          v-if="!isCreate || canPublish"
          class="page-footer__btn"
          :loading="saving"
          @click="save(announceId, true)"
        >
          发布
        </button>
      </PageFooter>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
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
  loading,
  saving,
  isCreate,
  form,
  typeOptions,
  targetOptions,
  typeIndex,
  targetIndex,
  targetIds,
  deptLabel,
  userRows,
  initCreate,
  load,
  loadUserOptions,
  onTypeChange,
  onTargetChange,
  isTargetSelected,
  toggleTarget,
  setDeptSelection,
  save,
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
@import '@/styles/module-form-page.scss';

.target-summary {
  padding: 16rpx 24rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  border-bottom: 1px solid $color-border-light;
}

.target-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  border-bottom: 1px solid $color-border-light;

  &:last-child {
    border-bottom: none;
  }
}

.target-row__label {
  flex: 1;
  min-width: 0;
  font-size: $font-size-base;
  color: $color-text-primary;
}
</style>
