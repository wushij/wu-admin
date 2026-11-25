<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑知识库' : '新建知识库'"
    width="560px"
    align-center
    destroy-on-close
    @closed="$emit('closed')"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
      :validate-on-rule-change="false"
    >
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" maxlength="128" show-word-limit placeholder="如：产品使用手册" />
      </el-form-item>

      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="可选，说明该知识库收录的内容范围"
        />
      </el-form-item>

      <el-form-item label="向量模型">
        <el-select v-model="form.embeddingModelId" style="width: 100%">
          <el-option :value="0" label="跟随默认向量模型" />
          <el-option
            v-for="model in embeddingModels"
            :key="model.id"
            :value="model.id"
            :label="`${model.name}（${model.modelName}${model.dimension ? ' · ' + model.dimension + ' 维' : ''}）`"
          />
        </el-select>
        <div v-if="!embeddingModels.length" class="form-hint is-warning">
          尚未配置向量模型，文档可以正常上传与切片，但无法生成向量；请先在「AI 模型配置」中新增类型为「向量模型」的配置。
        </div>
      </el-form-item>

      <div class="chunk-row">
        <el-form-item label="切片大小">
          <el-input-number v-model="form.chunkSize" :min="256" :max="4000" :step="100" />
        </el-form-item>
        <el-form-item label="重叠">
          <el-input-number v-model="form.chunkOverlap" :min="0" :max="2000" :step="16" />
        </el-form-item>
      </div>
      <div class="form-hint">切片大小按字符数计，默认 800；重叠默认 128。修改后已有切片需「重建索引」才会生效。</div>

      <div class="inline-row">
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" :step="1" />
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="$emit('submit')">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { EmbeddingModelOption, KnowledgeBaseSaveDTO } from '@/api/system/ai-knowledge-base'

const visible = defineModel<boolean>('visible', { default: false })
const form = defineModel<KnowledgeBaseSaveDTO>('form', { required: true })

defineProps<{
  rules: FormRules
  embeddingModels: EmbeddingModelOption[]
  submitting: boolean
}>()

defineEmits<{
  closed: []
  submit: []
}>()

const formRef = ref<FormInstance>()

defineExpose({ formRef })
</script>

<style scoped lang="scss">
.chunk-row {
  display: flex;
  gap: var(--space-3);

  :deep(.el-form-item) {
    flex: 1;
    min-width: 0;
  }
}

.inline-row {
  display: flex;
  gap: var(--space-3);

  :deep(.el-form-item) {
    flex: 1;
    min-width: 0;
  }
}

.form-hint {
  margin: -6px 0 14px 96px;
  font-size: var(--text-caption);
  line-height: 1.5;
  color: var(--color-text-secondary);

  &.is-warning {
    color: var(--color-warning);
  }
}
</style>
