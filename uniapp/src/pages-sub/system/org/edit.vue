<template>
  <view class="page-padded edit-page">
    <ListLoading v-if="loading" />
    <view v-else class="form-panel card--elevated">
      <FormCell label="上级" clickable arrow @click="pickParent">
        <text class="picker-value">{{ parentLabel }}</text>
      </FormCell>

      <template v-if="entity === 'dept'">
        <FormCell v-model="deptForm.name" label="部门名称" editable placeholder="必填" />
        <FormCell label="负责人" clickable arrow @click="pickLeader">
          <text class="picker-value">{{ leaderLabel }}</text>
        </FormCell>
        <FormCell v-model="deptForm.phone" label="联系电话" editable placeholder="选填" />
        <FormCell v-model="deptForm.email" label="邮箱" editable placeholder="选填" />
        <FormCell v-model="deptSortText" label="排序" editable input-type="number" />
      </template>

      <template v-else>
        <FormCell
          v-model="postForm.postCode"
          label="岗位编码"
          editable
          placeholder="必填"
          :disabled="!isCreate"
        />
        <FormCell v-model="postForm.postName" label="岗位名称" editable placeholder="必填" />
        <FormCell v-model="postSortText" label="排序" editable input-type="number" />
        <FormCell v-model="postForm.remark" label="备注" editable placeholder="选填" />
      </template>

      <FormCell label="状态" clickable arrow @click="pickStatus">
        <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
      </FormCell>
    </view>

    <PageFooter>
      <button class="page-footer__btn" :loading="saving" @tap="onSave">
        {{ isCreate ? '创建' : '保存' }}
      </button>
    </PageFooter>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useOrgForm, type OrgEntity } from '@/composables/useOrgForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import { showActionSheet } from '@/utils/app-dialog'
import { consumePagePickerResult, clearPagePickerResult } from '@/utils/page-picker-result'
import { appendNavFromParam } from '@/utils/nav-from'
import { beginLeaderPick, consumeLeaderPick, clearLeaderPick } from '@/utils/leader-pick'
import { registerPageShallowFallback } from '@/utils/navigate-back'

const ORG_INDEX_URL = '/pages-sub/system/org/index'

const recordId = ref(0)
const pendingLeaderPick = ref('')
const {
  loading,
  saving,
  isCreate,
  entity,
  deptForm,
  postForm,
  parentLabel,
  excludeParentId,
  leaderLabel,
  statusOptions,
  statusIndex,
  initCreate,
  loadEdit,
  setParentSelection,
  setLeaderSelection,
  onStatusChange,
  save,
} = useOrgForm()

const { resetBaseline, leaveAfterSave } = useEditPageGuard(
  () => ({
    entity: entity.value,
    deptForm: { ...deptForm },
    postForm: { ...postForm },
    parentLabel: parentLabel.value,
    leaderLabel: leaderLabel.value,
    statusIndex: statusIndex.value,
  }),
  { loading },
)

const deptSortText = computed({
  get: () => String(deptForm.sort ?? 0),
  set: (v) => { deptForm.sort = Number(v) || 0 },
})

const postSortText = computed({
  get: () => String(postForm.sort ?? 0),
  set: (v) => { postForm.sort = Number(v) || 0 },
})

function applyLeaderPick(result: { id?: number | null; label?: string }) {
  setLeaderSelection(result.id ?? null, result.label)
  resetBaseline()
}

function consumeLeaderPickResult() {
  const picked = consumeLeaderPick(pendingLeaderPick.value || undefined)
  if (picked) {
    pendingLeaderPick.value = ''
    applyLeaderPick(picked)
    return true
  }
  return false
}

function pickLeader() {
  clearPagePickerResult()
  clearLeaderPick()
  const initialId = deptForm.leaderUserId && deptForm.leaderUserId > 0 ? deptForm.leaderUserId : ''
  pendingLeaderPick.value = beginLeaderPick()
  const pick = pendingLeaderPick.value
  uni.navigateTo({
    url: appendNavFromParam(
      `/pages-sub/system/user-select?pick=${pick}&initialId=${initialId}&allowEmpty=1&emptyLabel=${encodeURIComponent('不设置')}&title=${encodeURIComponent('选择负责人')}`,
    ),
    events: {
      pickUserLeader(result: { id?: number | null; label?: string }) {
        pendingLeaderPick.value = ''
        applyLeaderPick(result)
      },
    },
  })
}

function pickParent() {
  const isDept = entity.value === 'dept'
  const parentId = isDept ? deptForm.parentId : postForm.parentId
  const selectedId = parentId && parentId > 0 ? parentId : ''
  const excludeId = excludeParentId.value ? `&excludeId=${excludeParentId.value}` : ''
  const type = isDept ? 'dept-parent' : 'post-parent'
  const emptyLabel = encodeURIComponent(isDept ? '顶级部门' : '顶级岗位')
  uni.navigateTo({
    url: appendNavFromParam(
      `/pages-sub/system/tree-select?type=${type}&allowEmpty=1&selectedId=${selectedId}${excludeId}&emptyLabel=${emptyLabel}&title=${encodeURIComponent(isDept ? '选择上级部门' : '选择上级岗位')}`,
    ),
  })
}

function applyPickerResults() {
  const result = consumePagePickerResult()
  if (!result) return
  if (result.kind === 'dept-parent' && entity.value === 'dept') {
    setParentSelection(result.id ?? null, result.label)
  } else if (result.kind === 'post-parent' && entity.value === 'post') {
    setParentSelection(result.id ?? null, result.label)
  } else if (result.kind === 'user-leader' && entity.value === 'dept') {
    applyLeaderPick(result)
  }
}

async function pickStatus() {
  try {
    const index = await showActionSheet({
      title: '状态',
      items: statusOptions.map((s) => ({ label: s.label })),
    })
    onStatusChange(index)
  } catch {
    /* cancelled */
  }
}

async function onSave() {
  const ok = await save(recordId.value)
  if (!ok) return
  leaveAfterSave()
}

onShow(() => {
  registerPageShallowFallback(ORG_INDEX_URL)
  if (consumeLeaderPickResult()) return
  applyPickerResults()
})

onLoad(async (options) => {
  const ent = (options?.entity === 'post' ? 'post' : 'dept') as OrgEntity
  if (options?.mode === 'create') {
    const parentId = Number(options?.parentId) || undefined
    await initCreate(ent, parentId)
    resetBaseline()
    return
  }
  recordId.value = Number(options?.id)
  if (recordId.value) await loadEdit(ent, recordId.value)
})
</script>

<style lang="scss" scoped>
@import '@/styles/common.scss';

.edit-page {
  min-height: 100vh;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.picker-value {
  color: $color-text-primary;
}
</style>
