<template>
<view class="form-panel">
  <FormCell label="开放注册" switch-cell>
    <switch :checked="registerDraft.enabled" :disabled="!canEdit" @change="onRegisterSwitch('enabled', $event)" />
  </FormCell>
  <FormCell label="注册验证码" switch-cell>
    <switch :checked="registerDraft.captchaEnabled" :disabled="!canEdit || !registerDraft.enabled" @change="onRegisterSwitch('captchaEnabled', $event)" />
  </FormCell>
  <FormCell v-if="registerDraft.enabled && registerDraft.captchaEnabled" label="验证码类型">
    <ConfigRadioGroup
      v-model="registerDraft.captchaType"
      :options="captchaTypeOptions"
      :disabled="!canEdit"
    />
  </FormCell>
  <FormCell label="密码最小长度">
    <NumberStepper v-model="registerDraft.minPasswordLength" :min="6" :max="32" :disabled="!canEdit || !registerDraft.enabled" />
  </FormCell>
  <FormCell label="默认角色" clickable boxed arrow @click="pickDefaultRole">
    <text class="picker-value">{{ defaultRoleLabel }}</text>
  </FormCell>
  <FormCell label="注册需审核" switch-cell :last="!registerDraft.needAudit">
    <switch :checked="registerDraft.needAudit" :disabled="!canEdit || !registerDraft.enabled" @change="onRegisterSwitch('needAudit', $event)" />
  </FormCell>
  <FormCell
    v-if="registerDraft.needAudit"
    label="审核人"
    clickable
    boxed
    arrow
    last
    @click="pickRegisterAuditors"
  >
    <text class="picker-value">{{ registerAuditorLabel }}</text>
  </FormCell>
</view>
</template>

<script setup lang="ts">
import ConfigRadioGroup from '@/components/business/system/ConfigRadioGroup/index.vue'
import FormCell from '@/components/base/FormCell/index.vue'
import NumberStepper from '@/components/base/NumberStepper/index.vue'

import type { RegisterAdminConfig } from '@/types/config-types'

defineProps<{
  registerDraft: RegisterAdminConfig
  canEdit: boolean
  captchaTypeOptions: { label: string; value: string }[]
  defaultRoleLabel: string
  registerAuditorLabel: string
  onRegisterSwitch: (key: any, e: any) => void
  pickDefaultRole: () => void
  pickRegisterAuditors: () => void
}>()
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;


</style>
