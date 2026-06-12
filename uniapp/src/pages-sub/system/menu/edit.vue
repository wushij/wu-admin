<template>
  <view class="page-padded edit-page">
    <ListLoading v-if="loading" />
    <view v-else class="form-panel">
      <FormCell label="上级菜单" clickable arrow @click="pickParent">
        <text class="picker-value">{{ parentOptions[parentIndex]?.label }}</text>
      </FormCell>
      <FormCell v-model="form.name" label="菜单名称" editable placeholder="必填" />
      <FormCell label="菜单类型" clickable arrow @click="pickType">
        <text class="picker-value">{{ typeOptions[typeIndex]?.label }}</text>
      </FormCell>
      <FormCell v-if="form.type !== 3" v-model="form.path" label="路由地址" editable placeholder="选填" />
      <FormCell v-if="form.type === 2" v-model="form.component" label="组件路径" editable placeholder="选填" />
      <FormCell v-if="form.type !== 3" v-model="form.icon" label="图标" editable placeholder="选填" />
      <FormCell v-model="form.permission" label="权限标识" editable placeholder="如 system:user:list" />
      <FormCell v-model="sortText" label="排序" editable input-type="number" />
      <FormCell label="状态" clickable arrow @click="pickStatus">
        <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
      </FormCell>
    </view>
    <PageFooter>
      <button class="page-footer__btn" :loading="saving" @tap="save(menuId)">
        {{ isCreate ? '创建菜单' : '保存' }}
      </button>
    </PageFooter>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useMenuForm } from '@/composables/useMenuForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import { showActionSheet } from '@/utils/app-dialog'

const menuId = ref(0)
const {
  loading, saving, isCreate, form, typeOptions, statusOptions, typeIndex, statusIndex,
  parentOptions, parentIndex, initCreate, loadMenu, onTypeChange, onStatusChange, onParentChange, save,
} = useMenuForm()

const { resetBaseline } = useEditPageGuard(
  () => ({
    ...form,
    typeIndex: typeIndex.value,
    statusIndex: statusIndex.value,
    parentIndex: parentIndex.value,
  }),
  { loading },
)

const sortText = computed({
  get: () => String(form.sort ?? 0),
  set: (v) => { form.sort = Number(v) || 0 },
})

async function pickType() {
  try {
    const index = await showActionSheet({
      title: '菜单类型',
      items: typeOptions.map((t) => ({ label: t.label })),
    })
    onTypeChange({ detail: { value: index } })
  } catch {
    /* cancelled */
  }
}

async function pickStatus() {
  try {
    const index = await showActionSheet({
      title: '状态',
      items: statusOptions.map((s) => ({ label: s.label })),
    })
    onStatusChange({ detail: { value: index } })
  } catch {
    /* cancelled */
  }
}

async function pickParent() {
  try {
    const index = await showActionSheet({
      title: '选择上级菜单',
      scrollable: true,
      items: parentOptions.value.map((p) => ({ label: p.label })),
    })
    onParentChange({ detail: { value: index } })
  } catch {
    /* cancelled */
  }
}

onLoad(async (options) => {
  if (options?.mode === 'create') {
    const parentId = Number(options?.parentId) || undefined
    await initCreate(parentId)
    const type = Number(options?.type)
    if (type === 2 || type === 3) {
      form.type = type
      typeIndex.value = type === 3 ? 2 : 1
    }
    resetBaseline()
    return
  }
  menuId.value = Number(options?.id)
  if (menuId.value) loadMenu(menuId.value)
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
