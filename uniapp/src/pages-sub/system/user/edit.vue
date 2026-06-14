<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="isCreate ? '新增用户' : '编辑用户'"
      :subtitle="isCreate ? '创建账号并分配组织与角色' : '修改资料与权限配置'"
      icon="friends-o"
      theme="user"
    />

    <ListLoading v-if="loading" />

    <template v-else-if="user">
      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="user-o" theme="user" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">账号信息</text>
            <text class="form-section__desc">用户名与昵称为必填项</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-if="isCreate"
            v-model="form.username"
            label="用户名"
            editable
            boxed
            placeholder="请输入用户名"
          />
          <FormCell v-else label="用户名" boxed :display-value="user.username" muted />
          <FormCell
            v-if="isCreate"
            v-model="form.password"
            label="密码"
            editable
            boxed
            password
            placeholder="请设置登录密码"
          />
          <FormCell v-model="form.nickname" label="昵称" editable boxed placeholder="请输入昵称" />
          <FormCell
            v-model="form.mobile"
            label="手机号"
            editable
            boxed
            input-type="digit"
            :maxlength="11"
            placeholder="选填"
          />
          <FormCell
            v-model="form.email"
            label="邮箱"
            editable
            boxed
            placeholder="选填"
            :last="!isCreate"
          />
        </view>
      </view>

      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="cluster-o" theme="user" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">组织与权限</text>
            <text class="form-section__desc">部门、岗位与角色可按需分配</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="部门" clickable boxed arrow @click="pickDept">
            <text class="picker-value" :class="{ 'picker-value--muted': deptLabel === '未分配' }">
              {{ deptLabel }}
            </text>
          </FormCell>
          <FormCell label="岗位" clickable boxed arrow @click="pickPost">
            <text class="picker-value" :class="{ 'picker-value--muted': postLabel === '未分配' }">
              {{ postLabel }}
            </text>
          </FormCell>
          <FormCell label="角色" clickable boxed arrow @click="pickRole">
            <text
              class="picker-value"
              :class="{ 'picker-value--muted': !roleOptions[roleIndex]?.name }"
            >
              {{ roleOptions[roleIndex]?.name || '未分配' }}
            </text>
          </FormCell>
          <FormCell label="状态" clickable boxed arrow @click="pickStatus">
            <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
          </FormCell>
          <FormCell v-model="form.remark" label="备注" editable boxed placeholder="选填" last />
        </view>
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
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { useUserEditForm } from '@/composables/useUserEditForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import { setTreeSelectPostIds } from '@/utils/tree-select-init'
import { appendNavFromParam } from '@/utils/nav-from'
import { registerPageShallowFallback, installH5ShallowStackTrapIfNeeded } from '@/utils/navigate-back'
import { pinNavParent } from '@/utils/nav-history'
import { scheduleSyncH5BackButton } from '@/store/h5-back-button'

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
  pinNavParent(backFallbackUrl.value)
  installH5ShallowStackTrapIfNeeded()
  scheduleSyncH5BackButton()
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

function applyBackFallback(url: string) {
  backFallbackUrl.value = url
  registerPageShallowFallback(url)
  pinNavParent(url)
}

onLoad((options) => {
  if (options?.mode === 'create') {
    applyBackFallback(USER_LIST_URL)
    initCreate()
    return
  }
  userId.value = Number(options?.id)
  if (userId.value) {
    applyBackFallback(`/pages-sub/system/user/detail?id=${userId.value}`)
    loadUser(userId.value)
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;
@use '@/styles/module-form-page.scss' as *;
</style>
