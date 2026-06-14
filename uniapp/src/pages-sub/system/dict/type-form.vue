<template>
  <view class="module-form-page">
    <ModuleDarkHero
      :title="isCreate ? '新增字典类型' : '编辑字典类型'"
      :subtitle="isCreate ? '填写名称与编码，保存后即可维护字典项' : '修改名称、状态或备注'"
      icon="notes-o"
      theme="dict"
    />

    <ListLoading v-if="loading" />

    <template v-else>
      <view class="form-section card--elevated">
        <view class="form-section__head">
          <ModuleIcon icon="records-o" theme="dict" size="sm" />
          <view class="form-section__intro">
            <text class="form-section__title">类型信息</text>
          </view>
        </view>

        <view class="form-fields">
          <FormCell
            v-model="form.dictName"
            label="字典名称"
            editable
            boxed
            placeholder="如：系统状态"
          />
          <FormCell
            v-if="isCreate"
            v-model="form.dictType"
            label="字典类型"
            editable
            boxed
            placeholder="如 sys_order_status"
          />
          <FormCell v-else label="字典类型" boxed :display-value="form.dictType" muted />
          <FormCell label="状态" clickable boxed arrow @click="pickStatus">
            <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
          </FormCell>
          <FormCell v-model="form.remark" label="备注" editable boxed placeholder="选填" last />
        </view>
      </view>

      <PageFooter>
        <button class="page-footer__btn" :loading="saving" @click="save(typeId)">
          {{ isCreate ? '创建字典类型' : '保存' }}
        </button>
      </PageFooter>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { useDictTypeForm } from '@/composables/useDictTypeForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'

const typeId = ref(0)
const { loading, saving, isCreate, form, statusOptions, statusIndex, initCreate, load, onStatusChange, save } =
  useDictTypeForm()

const { resetBaseline } = useEditPageGuard(
  () => ({ ...form, statusIndex: statusIndex.value }),
  { loading },
)

function pickStatus() {
  uni.showActionSheet({
    itemList: statusOptions.map((s) => s.label),
    success: (res) => onStatusChange({ detail: { value: res.tapIndex } }),
  })
}

onLoad((options) => {
  if (options?.mode === 'create') {
    initCreate()
    resetBaseline()
    return
  }
  typeId.value = Number(options?.id)
  if (typeId.value) load(typeId.value)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';
@import '@/styles/module-form-page.scss';
</style>
