<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="520px"
    destroy-on-close
    :close-on-click-modal="false"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item label="字典名称" prop="dictName">
        <el-input v-model="form.dictName" placeholder="如：系统状态" />
      </el-form-item>
      <el-form-item label="字典类型" prop="dictType">
        <el-input v-model="form.dictType" placeholder="小写+下划线，如 sys_order_status" :disabled="!!form.id" />
        <div v-if="!form.id" class="form-tip">创建后不可修改；业务代码用此编码引用字典</div>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <DictSelect
          v-model="form.status"
          :dict-type="DICT_TYPE.NORMAL_DISABLE"
          value-type="number"
          :clearable="false"
          width="160px"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="$emit('submit')">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormRules } from 'element-plus'
import type { DictTypeSaveDTO } from '@/api/system/dict'
import { DICT_TYPE } from '@/constants/dict'

defineProps<{
  visible: boolean
  title: string
  form: DictTypeSaveDTO
  rules: FormRules
  submitting: boolean
}>()

defineEmits<{
  'update:visible': [value: boolean]
  submit: []
}>()

const formRef = ref()
defineExpose({ formRef })
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  margin-top: 4px;
}
</style>
