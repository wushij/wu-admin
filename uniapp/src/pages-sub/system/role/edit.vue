<template>
  <view class="page-padded edit-page">
    <ListLoading v-if="loading" />
    <view v-else class="form-panel">
      <FormCell v-model="form.name" label="角色名称" editable placeholder="必填" />
      <FormCell v-model="form.code" label="角色编码" editable placeholder="必填" :disabled="!isCreate" />
      <FormCell v-model="sortText" label="排序" editable input-type="number" />
      <FormCell label="状态" clickable arrow @click="pickStatus">
        <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
      </FormCell>
      <FormCell v-model="form.remark" label="备注" editable placeholder="选填" last />
    </view>
    <PageFooter>
      <button class="page-footer__btn" :loading="saving" @click="save(roleId)">
        {{ isCreate ? '创建角色' : '保存' }}
      </button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { useRoleForm } from '@/composables/useRoleForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'

const roleId = ref(0)
const {
  loading, saving, isCreate, form, statusOptions, statusIndex,
  initCreate, loadRole, onStatusChange, save,
} = useRoleForm()

const { resetBaseline } = useEditPageGuard(
  () => ({
    name: form.name,
    code: form.code,
    sort: form.sort,
    remark: form.remark,
    statusIndex: statusIndex.value,
  }),
  { loading },
)

const sortText = computed({
  get: () => String(form.sort),
  set: (v) => { form.sort = Number(v) || 0 },
})

function pickStatus() {
  uni.showActionSheet({
    itemList: statusOptions.map((s) => s.label),
    success: (res) => onStatusChange({ detail: { value: res.tapIndex } }),
  })
}

onLoad((options) => {
  if (options?.mode === 'create') {
    initCreate().then(() => resetBaseline())
    return
  }
  roleId.value = Number(options?.id)
  if (roleId.value) loadRole(roleId.value)
})
</script>

<style lang="scss" scoped>
@import '@/styles/common.scss';
.edit-page { min-height: 100vh; padding-bottom: calc(140rpx + env(safe-area-inset-bottom)); }
</style>
