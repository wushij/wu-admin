<template>
  <el-form :model="draft" label-width="120px" class="config-form">
    <el-form-item label="开放注册">
      <el-switch v-model="draft.enabled" :disabled="!canEdit" />
    </el-form-item>
    <el-form-item label="注册验证码">
      <el-switch v-model="draft.captchaEnabled" :disabled="!canEdit || !draft.enabled" />
    </el-form-item>
    <el-form-item v-if="draft.enabled && draft.captchaEnabled" label="验证码类型">
      <el-radio-group v-model="draft.captchaType" :disabled="!canEdit">
        <el-radio label="image">图片</el-radio>
        <el-radio label="slider">滑块</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="密码最小长度">
      <el-input-number v-model="draft.minPasswordLength" :min="6" :max="32" :disabled="!canEdit || !draft.enabled" />
    </el-form-item>
    <el-form-item label="默认角色">
      <el-select v-model="draft.defaultRoleCode" :disabled="!canEdit || !draft.enabled" style="width: 260px">
        <el-option v-for="role in roleOptions" :key="role.code" :label="`${role.name}（${role.code}）`" :value="role.code" />
      </el-select>
    </el-form-item>
    <el-form-item label="注册需审核">
      <el-switch v-model="draft.needAudit" :disabled="!canEdit || !draft.enabled" />
    </el-form-item>
    <el-form-item v-if="draft.needAudit" label="审核人">
      <div class="config-auditor-field">
        <el-select
          v-model="draft.auditorUserIds"
          multiple
          filterable
          clearable
          class="config-auditor-select"
          :disabled="!canEdit || !draft.enabled"
          placeholder="请选择审核人"
        >
          <el-option v-for="user in userOptions" :key="user.id" :label="user.label" :value="user.id" />
        </el-select>
        <p class="config-auditor-field__hint">未选择时默认通知超级管理员</p>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'
import type { RoleOption, UserOption } from '../composables/useConfigDraft'

defineProps<{
  draft: ConfigGroupMap['register']
  canEdit: boolean
  roleOptions: RoleOption[]
  userOptions: UserOption[]
}>()
</script>
