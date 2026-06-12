<template>
  <view class="page-padded edit-page">
    <ListLoading v-if="loading" />
    <view v-else class="form-panel">
      <FormCell v-model="form.dictName" label="字典名称" editable placeholder="必填" />
      <FormCell v-model="form.dictType" label="字典类型" editable placeholder="必填" :disabled="!isCreate" />
      <FormCell label="状态" clickable arrow @click="pickStatus">
        <text class="picker-value">{{ statusOptions[statusIndex]?.label }}</text>
      </FormCell>
      <FormCell v-model="form.remark" label="备注" editable placeholder="选填" last />
    </view>
    <PageFooter>
      <button class="page-footer__btn" :loading="saving" @click="save(typeId)">保存</button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
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
@import '@/styles/common.scss';
.edit-page { min-height: 100vh; padding-bottom: calc(140rpx + env(safe-area-inset-bottom)); }
</style>
