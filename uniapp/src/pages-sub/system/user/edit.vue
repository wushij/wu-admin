<template>
  <view class="page-padded edit-page">
    <ListLoading v-if="loading" />
    <template v-else-if="user">
      <view class="form-panel">
        <FormCell
          v-if="isCreate"
          v-model="form.username"
          label="用户名"
          editable
          placeholder="必填"
        />
        <FormCell v-else label="用户名" :display-value="user.username" muted />
        <FormCell
          v-if="isCreate"
          v-model="form.password"
          label="密码"
          editable
          password
          placeholder="必填"
        />
        <FormCell v-model="form.nickname" label="昵称" editable placeholder="必填" />
        <FormCell v-model="form.mobile" label="手机号" editable input-type="digit" :maxlength="11" placeholder="选填" />
        <FormCell v-model="form.email" label="邮箱" editable placeholder="选填" />
        <FormCell label="部门" clickable arrow @click="pickDept">
          <text class="picker-value">{{ deptLabel }}</text>
        </FormCell>
        <FormCell label="岗位" clickable arrow @click="pickPost">
          <text class="picker-value">{{ postLabel }}</text>
        </FormCell>
        <FormCell label="角色" clickable arrow @click="pickRole">
          <text class="picker-value">{{ roleOptions[roleIndex]?.name || '未分配' }}</text>
        </FormCell>
        <FormCell label="状态" clickable arrow @click="pickStatus">
          <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
        </FormCell>
        <FormCell v-model="form.remark" label="备注" editable placeholder="选填" last />
      </view>

      <PageFooter>
        <button class="page-footer__btn" :loading="saving" @click="handleSave(userId)">
          {{ isCreate ? '创建用户' : '保存' }}
        </button>
      </PageFooter>
    </template>
    <EmptyState v-else title="用户不存在" icon="friends-o" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { useUserEditForm } from '@/composables/useUserEditForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import { setTreeSelectPostIds } from '@/utils/tree-select-init'
import { appendNavFromParam } from '@/utils/nav-from'
import { registerPageShallowFallback } from '@/utils/navigate-back'

const USER_LIST_URL = '/pages-sub/system/user/index'

const userId = ref(0)
const backFallbackUrl = ref(USER_LIST_URL)

const {
  loading,
  saving,
  isCreate,
  user,
  form,
  deptLabel,
  postLabel,
  roleOptions,
  statusOptions,
  roleIndex,
  statusIndex,
  initCreate,
  loadUser,
  setDeptSelection,
  setPostSelection,
  onRoleChange,
  onStatusChange,
  save,
} = useUserEditForm({ backFallback: () => backFallbackUrl.value })

const { markClean } = useEditPageGuard(
  () => ({
    nickname: form.nickname,
    mobile: form.mobile,
    email: form.email,
    remark: form.remark,
    deptId: form.deptId,
    postIds: [...form.postIds].sort((a, b) => a - b),
    roleId: form.roleId,
    status: form.status,
    password: form.password,
    roleIndex: roleIndex.value,
    statusIndex: statusIndex.value,
  }),
  { loading },
)

async function handleSave(id: number) {
  await save(id, markClean)
}

function pickDept() {
  uni.navigateTo({
    url: appendNavFromParam(
      `/pages-sub/system/tree-select?type=dept&allowEmpty=1&selectedId=${form.deptId ?? ''}&title=${encodeURIComponent('选择部门')}`,
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
      id?: number | null
      label?: string
      ids?: number[]
      labelText?: string
    }
    if (result.kind === 'dept' && result.label != null) {
      setDeptSelection(result.id ?? null, result.label)
    } else if (result.kind === 'post' && Array.isArray(result.ids)) {
      setPostSelection(result.ids, result.labelText || '')
    }
  } catch {
    uni.removeStorageSync('uniapp_page_picker_result')
  }
}

function pickPost() {
  setTreeSelectPostIds(Array.isArray(form.postIds) ? [...form.postIds] : [])
  uni.navigateTo({
    url: appendNavFromParam(
      `/pages-sub/system/tree-select?type=post&title=${encodeURIComponent('选择岗位')}`,
    ),
  })
}

onShow(() => {
  registerPageShallowFallback(backFallbackUrl.value)
  applyPickerResults()
})

function pickRole() {
  if (!roleOptions.value.length) return
  uni.showActionSheet({
    itemList: roleOptions.value.map((r) => r.name),
    success: (res) => onRoleChange({ detail: { value: res.tapIndex } }),
  })
}

function pickStatus() {
  uni.showActionSheet({
    itemList: statusOptions.map((s) => s.label),
    success: (res) => onStatusChange({ detail: { value: res.tapIndex } }),
  })
}

onLoad((options) => {
  if (options?.mode === 'create') {
    backFallbackUrl.value = USER_LIST_URL
    initCreate()
    return
  }
  userId.value = Number(options?.id)
  if (userId.value) {
    backFallbackUrl.value = `/pages-sub/system/user/detail?id=${userId.value}`
    loadUser(userId.value)
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/common.scss';

.edit-page {
  min-height: 100vh;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}
</style>
