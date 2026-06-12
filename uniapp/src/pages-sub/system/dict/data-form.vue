<template>
  <view class="page-padded edit-page">
    <view class="form-panel">
      <FormCell v-model="form.dictLabel" label="字典标签" editable placeholder="必填" />
      <FormCell v-model="form.dictValue" label="字典键值" editable placeholder="必填" />
      <FormCell v-model="sortText" label="排序" editable input-type="number" />
      <FormCell label="状态" clickable arrow @click="pickStatus">
        <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
      </FormCell>
      <FormCell v-model="form.remark" label="备注" editable placeholder="选填" last />
    </view>
    <PageFooter>
      <button class="page-footer__btn" :loading="saving" @click="save(dataId)">保存</button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import FormCell from '@/components/common/FormCell/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { listDictDataForManage } from '@/api/system/dict'
import { useDictDataForm } from '@/composables/useDictDataForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'

const dataId = ref(0)
const dictType = ref('')
const { saving, isCreate, form, statusOptions, statusIndex, initCreate, loadFromItem, onStatusChange, save } =
  useDictDataForm()

const { resetBaseline } = useEditPageGuard(() => ({
  ...form,
  statusIndex: statusIndex.value,
}))

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

onLoad(async (options) => {
  dictType.value = String(options?.dictType || '')
  if (options?.mode === 'create') {
    initCreate(dictType.value)
    resetBaseline()
    return
  }
  dataId.value = Number(options?.dataId)
  if (dataId.value && dictType.value) {
    const res = await listDictDataForManage(dictType.value)
    const item = (res.data || []).find((d) => d.id === dataId.value)
    if (item) loadFromItem(dictType.value, item)
    resetBaseline()
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/common.scss';
.edit-page { min-height: 100vh; padding-bottom: calc(140rpx + env(safe-area-inset-bottom)); }
</style>
