<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="680px"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
    @closed="$emit('closed')"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="任务名称" prop="jobName">
        <el-input v-model="formData.jobName" placeholder="请输入任务名称" />
      </el-form-item>
      <el-form-item label="任务组名" prop="jobGroup">
        <el-select v-model="formData.jobGroup" allow-create filterable default-first-option style="width: 100%">
          <el-option label="SYSTEM（系统任务）" value="SYSTEM" />
          <el-option label="DEFAULT（默认）" value="DEFAULT" />
        </el-select>
      </el-form-item>
      <el-form-item label="调用目标" prop="invokeTarget">
        <el-input v-model="formData.invokeTarget" placeholder="beanName.methodName" />
      </el-form-item>
      <el-form-item label="Cron" prop="cronExpression">
        <div class="cron-row">
          <el-input v-model="formData.cronExpression" placeholder="0 30 2 * * ?" @blur="$emit('validate-cron')" />
          <el-select
            :model-value="cronPreset"
            placeholder="常用"
            clearable
            style="width: 140px"
            @update:model-value="$emit('apply-cron-preset', $event)"
          >
            <el-option v-for="p in cronPresets" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </div>
        <div v-if="cronPreview.hint" class="cron-preview">
          <el-icon><Clock /></el-icon>
          <span>{{ cronPreview.hint }}</span>
          <span v-if="cronPreview.nextFireTimes?.length" class="preview-times">
            下次：{{ cronPreview.nextFireTimes[0] }}
          </span>
        </div>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="错误策略">
            <el-select v-model="formData.misfirePolicy" style="width: 100%">
              <el-option label="立即执行" :value="1" />
              <el-option label="执行一次" :value="2" />
              <el-option label="放弃执行" :value="3" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="并发执行">
            <el-radio-group v-model="formData.concurrent">
              <el-radio :value="0">允许</el-radio>
              <el-radio :value="1">禁止</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="创建后启用">
        <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="$emit('submit')">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Clock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { SysJob } from '@/api/monitor/job'
import { cronPresets } from '../constants/cronPresets'

defineProps<{
  visible: boolean
  title: string
  formData: SysJob
  formRules: FormRules
  cronPreset: string | undefined
  cronPreview: { hint?: string; nextFireTimes?: string[] }
  submitting: boolean
}>()

defineEmits<{
  'update:visible': [value: boolean]
  closed: []
  'validate-cron': []
  'apply-cron-preset': [value: string | undefined]
  submit: []
}>()

const formRef = ref<FormInstance>()
defineExpose({ formRef })
</script>

<style scoped>
.cron-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.cron-row .el-input {
  flex: 1;
}
.cron-preview {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 12px;
  color: #52c41a;
}
.preview-times {
  color: #909399;
  margin-left: 8px;
}
</style>
