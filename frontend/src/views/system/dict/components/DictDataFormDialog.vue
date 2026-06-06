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
      <el-form-item label="字典标签" prop="dictLabel">
        <el-input v-model="form.dictLabel" placeholder="界面展示文字" />
      </el-form-item>
      <el-form-item label="字典键值" prop="dictValue">
        <el-input v-model="form.dictValue" placeholder="存入数据库的值" />
      </el-form-item>
      <el-form-item label="显示排序" prop="sort">
        <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="回显样式">
        <el-select v-model="form.listClass" placeholder="列表/标签颜色" style="width: 100%">
          <el-option label="默认" value="default" />
          <el-option label="成功" value="success" />
          <el-option label="警告" value="warning" />
          <el-option label="错误" value="danger" />
          <el-option label="信息" value="info" />
          <el-option label="主色" value="primary" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否默认">
        <el-radio-group v-model="form.isDefault">
          <el-radio :label="1">是</el-radio>
          <el-radio :label="0">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态">
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
import type { DictDataSaveDTO } from '@/api/system/dict'
import { DICT_TYPE } from '@/constants/dict'

defineProps<{
  visible: boolean
  title: string
  form: DictDataSaveDTO
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
