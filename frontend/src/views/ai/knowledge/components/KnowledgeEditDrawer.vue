<template>
  <el-drawer
    :model-value="modelValue"
    :title="form.id ? '编辑 Markdown 文档' : '新建 Markdown 文档'"
    size="920px"
    destroy-on-close
    class="knowledge-edit-drawer"
    @update:model-value="(val) => $emit('update:modelValue', val)"
  >
    <div class="edit-drawer-content">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="knowledge-edit-form">
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="文档名称 / 标题" prop="title">
              <el-input
                v-model="form.title"
                maxlength="128"
                show-word-limit
                placeholder="如：企业请假制度、工单报修流程、系统登录指南"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="知识分类" prop="category">
              <el-select v-model="form.category" style="width: 100%">
                <el-option v-for="(label, key) in CATEGORY_LABELS" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item prop="keywords">
              <template #label>
                <div class="label-with-tool">
                  <span>检索关键词（逗号分隔）</span>
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="$emit('auto-keywords')"
                  >
                    <el-icon><MagicStick /></el-icon> 智能提炼关键词
                  </el-button>
                </div>
              </template>
              <el-input
                v-model="form.keywords"
                maxlength="255"
                show-word-limit
                placeholder="逗号分隔，如：忘记密码,找回密码,重置密码,登录不了"
              />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="排序权重">
              <el-input-number v-model="form.sort" :min="0" :max="9999" :step="5" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- Markdown 快捷模板工具栏 -->
        <div class="editor-quick-toolbar">
          <span class="toolbar-label">快速插入模板：</span>
          <el-button-group size="small">
            <el-button @click="$emit('insert-template', 'faq')">FAQ 问答模板</el-button>
            <el-button @click="$emit('insert-template', 'manual')">操作指南模板</el-button>
            <el-button @click="$emit('insert-template', 'rule')">业务规则模板</el-button>
            <el-button @click="$emit('insert-fence')">``` 代码块</el-button>
          </el-button-group>
          <span class="editor-mode-toggle">
            <el-radio-group
              :model-value="editorLayout"
              size="small"
              @update:model-value="(val) => $emit('update:editorLayout', val as 'split' | 'editOnly' | 'previewOnly')"
            >
              <el-radio-button value="split">双栏对照</el-radio-button>
              <el-radio-button value="editOnly">仅编辑</el-radio-button>
              <el-radio-button value="previewOnly">仅预览</el-radio-button>
            </el-radio-group>
          </span>
        </div>

        <!-- 正文编辑与实时预览分栏 -->
        <div class="editor-preview-split" :class="`layout-${editorLayout}`">
          <div v-show="editorLayout !== 'previewOnly'" class="pane-editor">
            <div class="pane-header">Markdown 源码录入</div>
            <el-form-item prop="content" class="content-form-item">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="18"
                maxlength="4000"
                show-word-limit
                placeholder="支持完整 Markdown 排版（标题、有序/无序列表、表格、引用块）；代码块请务必使用成对闭合的 ```"
                class="markdown-textarea"
              />
            </el-form-item>
          </div>

          <div v-show="editorLayout !== 'editOnly'" class="pane-preview">
            <div class="pane-header">实时排版预览效果</div>
            <div class="preview-scroll-body">
              <AiWuMarkdown
                v-if="form.content && form.content.trim()"
                :content="form.content"
              />
              <div v-else class="preview-placeholder">
                输入 Markdown 内容后，右侧将实时渲染排版样式
              </div>
            </div>
          </div>
        </div>

        <!-- 底部校验状态 -->
        <div class="editor-bottom-status">
          <div v-if="fenceCount % 2 !== 0" class="status-warning">
            <el-icon><Warning /></el-icon>
            <span>注意：正文中的代码块围栏 ``` 当前未成对闭合（当前数量为 {{ fenceCount }}），保存前请检查</span>
          </div>
          <div v-else class="status-ok">
            <el-icon><CircleCheck /></el-icon>
            <span>代码块围栏检查通过（成对闭合）</span>
          </div>
        </div>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="$emit('submit')">保存生效</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { MagicStick, Warning, CircleCheck } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import AiWuMarkdown from '@/components/AiWu/AiWuMarkdown.vue'
import type { AiKnowledgeSaveDTO } from '@/api/system/ai-knowledge'
import { CATEGORY_LABELS } from '../constants/knowledge'

defineProps<{
  modelValue: boolean
  form: AiKnowledgeSaveDTO
  rules: FormRules
  saving: boolean
  fenceCount: number
  editorLayout: 'split' | 'editOnly' | 'previewOnly'
}>()

const formRef = defineModel<FormInstance | undefined>('formRef')
defineExpose({ formRef })

defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'update:editorLayout', val: 'split' | 'editOnly' | 'previewOnly'): void
  (e: 'auto-keywords'): void
  (e: 'insert-template', type: 'faq' | 'manual' | 'rule'): void
  (e: 'insert-fence'): void
  (e: 'submit'): void
}>()
</script>

<style scoped lang="scss">
.knowledge-edit-drawer {
  .label-with-tool {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
  }

  .editor-quick-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    background: var(--el-fill-color-light);
    padding: 8px 12px;
    border-radius: 6px;

    .toolbar-label {
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }

    .editor-mode-toggle {
      margin-left: auto;
    }
  }

  .editor-preview-split {
    display: grid;
    gap: 16px;

    &.layout-split {
      grid-template-columns: 1fr 1fr;
    }

    &.layout-editOnly {
      grid-template-columns: 1fr;
    }

    &.layout-previewOnly {
      grid-template-columns: 1fr;
    }

    .pane-header {
      font-size: 12px;
      font-weight: 600;
      color: var(--el-text-color-secondary);
      margin-bottom: 6px;
    }

    .pane-preview {
      display: flex;
      flex-direction: column;

      .preview-scroll-body {
        border: 1px solid var(--el-border-color);
        border-radius: 4px;
        padding: 12px 16px;
        min-height: 400px;
        max-height: 460px;
        overflow-y: auto;
        background: var(--el-bg-color);

        .preview-placeholder {
          font-size: 13px;
          color: var(--el-text-color-placeholder);
          padding-top: 40px;
          text-align: center;
        }
      }
    }
  }

  .editor-bottom-status {
    margin-top: 10px;
    font-size: 12px;

    .status-warning {
      color: var(--el-color-warning);
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .status-ok {
      color: var(--el-color-success);
      display: flex;
      align-items: center;
      gap: 6px;
    }
  }
}
</style>
