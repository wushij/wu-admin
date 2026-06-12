<template>

  <view class="page-padded create-page">

    <view class="form-panel">

      <FormCell label="审批类型" clickable arrow @click="pickFormType">

        <text class="picker-value">{{ availableFormTypes[formTypeIndex]?.label || '请选择' }}</text>

      </FormCell>

      <FormCell v-model="form.title" label="标题" editable placeholder="必填" />

      <FormCell label="审批人" clickable arrow @click="pickApprover">

        <text class="picker-value">{{ approvers[approverIndex] ? approverLabel(approvers[approverIndex]) : '请选择' }}</text>

      </FormCell>

      <view class="content-field">

        <text class="content-field__label">内容</text>

        <textarea v-model="form.content" class="content-field__input" placeholder="请输入审批说明" />

      </view>

    </view>



    <PageFooter>

      <button class="page-footer__btn" :loading="saving" @click="onSubmit">提交审批单</button>

    </PageFooter>



    <AppDialogHost />

  </view>

</template>



<script setup lang="ts">

import FormCell from '@/components/common/FormCell/index.vue'

import PageFooter from '@/components/common/PageFooter/index.vue'

import AppDialogHost from '@/components/common/AppDialogHost/index.vue'

import { useApprovalCreateForm } from '@/composables/useApprovalCreateForm'

import { useEditPageGuard } from '@/composables/useEditPageGuard'

import { showActionSheet } from '@/utils/app-dialog'



const {

  saving,

  form,

  approvers,

  approverIndex,

  formTypeIndex,

  availableFormTypes,

  formTypesLoading,

  onFormTypeChange,

  onApproverChange,

  approverLabel,

  submit,

} = useApprovalCreateForm()



const { leaveAfterSave } = useEditPageGuard(() => ({

  ...form,

  approverIndex: approverIndex.value,

  formTypeIndex: formTypeIndex.value,

}))



async function pickFormType() {

  if (formTypesLoading.value) {

    uni.showToast({ title: '审批类型加载中', icon: 'none' })

    return

  }

  if (!availableFormTypes.value.length) {

    uni.showToast({ title: '暂无可用审批类型', icon: 'none' })

    return

  }

  try {

    const index = await showActionSheet({

      title: '审批类型',

      items: availableFormTypes.value.map((o) => ({ label: o.label })),

    })

    onFormTypeChange({ detail: { value: index } })

  } catch {

    /* cancelled */

  }

}



async function pickApprover() {

  if (!approvers.value.length) {

    uni.showToast({ title: '暂无可选审批人', icon: 'none' })

    return

  }

  try {

    const index = await showActionSheet({

      title: '审批人',

      items: approvers.value.map((u) => ({ label: approverLabel(u) })),

    })

    onApproverChange({ detail: { value: index } })

  } catch {

    /* cancelled */

  }

}



async function onSubmit() {

  const ok = await submit()

  if (ok) leaveAfterSave()

}

</script>



<style lang="scss" scoped>

@import '@/styles/variables.scss';

@import '@/styles/common.scss';



.create-page {

  min-height: 100vh;

  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));

  box-sizing: border-box;

}



.content-field {

  padding: 24rpx 32rpx 32rpx;

}



.content-field__label {

  display: block;

  margin-bottom: 12rpx;

  font-size: $font-size-md;

  color: $color-text-primary;

}



.content-field__input {

  width: 100%;

  min-height: 200rpx;

  padding: 16rpx;

  border-radius: $radius-sm;

  background: $color-bg-muted;

  font-size: $font-size-base;

  box-sizing: border-box;

}

</style>

