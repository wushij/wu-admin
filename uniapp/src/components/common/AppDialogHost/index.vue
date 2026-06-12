<template>
  <AppConfirm
    :visible="dialogState.confirmVisible"
    :title="dialogState.confirmOptions.title"
    :content="dialogState.confirmOptions.content"
    :confirm-text="dialogState.confirmOptions.confirmText"
    :cancel-text="dialogState.confirmOptions.cancelText"
    :tone="dialogState.confirmOptions.tone"
    :editable="dialogState.confirmOptions.editable"
    :placeholder-text="dialogState.confirmOptions.placeholderText"
    :input-type="dialogState.confirmOptions.inputType"
    :show-cancel="dialogState.confirmOptions.showCancel"
    :content-align="dialogState.confirmOptions.contentAlign"
    v-model:input-value="dialogState.confirmInput"
    @confirm="onConfirm"
    @cancel="resolveConfirm(false)"
  />
  <AppActionSheet
    :visible="dialogState.actionSheetVisible"
    :title="dialogState.actionSheetOptions.title"
    :items="dialogState.actionSheetOptions.items"
    :scrollable="dialogState.actionSheetOptions.scrollable"
    @pick="resolveActionSheet"
    @cancel="cancelActionSheet"
  />
</template>

<script setup lang="ts">
import AppConfirm from '@/components/common/AppConfirm/index.vue'
import AppActionSheet from '@/components/common/AppActionSheet/index.vue'
import {
  dialogState,
  resolveConfirm,
  resolveActionSheet,
  cancelActionSheet,
} from '@/store/dialog'

function onConfirm() {
  resolveConfirm(true, dialogState.confirmInput)
}
</script>
