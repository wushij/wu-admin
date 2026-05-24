<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="isCreate ? '新增菜单' : '编辑菜单'"
      :subtitle="isCreate ? '配置目录、菜单或按钮权限' : '修改菜单路由与权限标识'"
      icon="apps-o"
      theme="menu"
    />

    <ListLoading v-if="loading" />

    <template v-else>
      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="apps-o" theme="menu" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">菜单信息</text>
            <text class="form-section__desc">上级、名称与类型为必填项</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell label="上级菜单" clickable boxed arrow @click="pickParent">
            <text class="picker-value">{{ parentOptions[parentIndex]?.label || '请选择' }}</text>
          </FormCell>
          <FormCell
            v-model="form.name"
            label="菜单名称"
            editable
            boxed
            placeholder="请输入菜单名称"
          />
          <FormCell label="菜单类型" clickable boxed arrow last @click="pickType">
            <text class="picker-value">{{ typeOptions[typeIndex]?.label }}</text>
          </FormCell>
        </view>
      </view>

      <view v-if="form.type !== 3" class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="desktop-o" theme="menu" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">路由与图标</text>
            <text class="form-section__desc">前端路由与展示图标</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-model="form.path"
            label="路由地址"
            editable
            boxed
            placeholder="如 /system/user"
          />
          <FormCell
            v-if="form.type === 2"
            v-model="form.component"
            label="组件路径"
            editable
            boxed
            placeholder="如 system/user/index"
          />
          <FormCell
            v-model="form.icon"
            label="图标"
            editable
            boxed
            placeholder="如 user-o"
            last
          />
        </view>
      </view>

      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="setting-o" theme="menu" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">权限与状态</text>
            <text class="form-section__desc">权限标识用于接口鉴权</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-model="form.permission"
            label="权限标识"
            editable
            boxed
            placeholder="如 system:menu:list"
          />
          <FormCell
            v-model="sortText"
            label="排序"
            editable
            boxed
            input-type="number"
            placeholder="0"
          />
          <FormCell label="状态" clickable boxed arrow last @click="pickStatus">
            <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
          </FormCell>
        </view>
      </view>

      <PageFooter>
        <button class="page-footer__btn" :loading="saving" @click="save(menuId)">
          {{ isCreate ? '创建菜单' : '保存' }}
        </button>
      </PageFooter>
    </template>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useMenuForm } from '@/composables/useMenuForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'
import { showActionSheet } from '@/utils/app-dialog'

const menuId = ref(0)
const {
  loading,
  saving,
  isCreate,
  form,
  typeOptions,
  statusOptions,
  typeIndex,
  statusIndex,
  parentOptions,
  parentIndex,
  initCreate,
  loadMenu,
  onTypeChange,
  onStatusChange,
  onParentChange,
  save,
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
  set: (v) => {
    form.sort = Number(v) || 0
  },
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
@use '@/styles/common.scss' as *;
@use '@/styles/module-form-page.scss' as *;
</style>
