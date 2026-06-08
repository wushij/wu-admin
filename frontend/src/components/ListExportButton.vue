<template>
  <el-button
    v-permission="permission"
    class="list-export-btn"
    :loading="exporting"
    @click="dialogVisible = true"
  >
    <el-icon v-if="!exporting" class="btn-icon"><Download /></el-icon>
    导出
  </el-button>

  <el-dialog
    v-model="dialogVisible"
    :title="`导出 · ${moduleLabel}`"
    width="440px"
    class="list-export-dialog"
    destroy-on-close
    append-to-body
    @closed="resetForm"
  >
    <div class="export-intro">
      <el-icon class="intro-icon"><Document /></el-icon>
      <div>
        <p class="intro-title">导出当前列表数据</p>
        <p class="intro-desc">筛选条件与列表查询保持一致，便于审计留档与汇报。</p>
      </div>
    </div>

    <el-form label-position="top" class="export-form">
      <el-form-item label="文件格式">
        <el-radio-group v-model="form.format" class="format-group">
          <el-radio-button value="xlsx">
            <span class="format-option">
              <el-icon><Grid /></el-icon>
              Excel (.xlsx)
            </span>
          </el-radio-button>
          <el-radio-button value="csv">
            <span class="format-option">
              <el-icon><Tickets /></el-icon>
              CSV (.csv)
            </span>
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="导出范围">
        <el-radio-group v-model="form.scope" class="scope-group">
          <el-radio value="filtered" border>
            <div class="scope-option">
              <span class="scope-name">当前筛选全部</span>
              <span class="scope-hint">按搜索条件导出，最多 {{ maxRows.toLocaleString() }} 条</span>
            </div>
          </el-radio>
          <el-radio value="page" border>
            <div class="scope-option">
              <span class="scope-name">仅当前页</span>
              <span class="scope-hint">导出表格正在查看的页码数据</span>
            </div>
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="export-tip"
    >
      <template #title>
        大量数据将分批写入，单次导出上限 {{ maxRows.toLocaleString() }} 条；导出操作会记入操作日志。
      </template>
    </el-alert>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="exporting" @click="handleExport">
        <el-icon v-if="!exporting"><Download /></el-icon>
        开始导出
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Download, Document, Grid, Tickets } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getExportLabel,
  getExportPath,
  buildExportFilename,
  type ExportModule,
} from '@/api/system/export'
import {
  downloadListExport,
  EXPORT_MAX_ROWS,
  type ExportFormat,
  type ExportScope,
} from '@/utils/exportDownload'

const props = defineProps<{
  module: ExportModule
  /** 与列表查询一致的筛选/分页参数 */
  queryParams: object
  /** 额外查询参数（如时间范围） */
  extraParams?: object
  /** v-permission 权限标识，与列表查询权限一致 */
  permission: string
}>()

const maxRows = EXPORT_MAX_ROWS
const moduleLabel = computed(() => getExportLabel(props.module))
const dialogVisible = ref(false)
const exporting = ref(false)

const form = reactive<{ format: ExportFormat; scope: ExportScope }>({
  format: 'xlsx',
  scope: 'filtered',
})

function resetForm() {
  form.format = 'xlsx'
  form.scope = 'filtered'
}

function mergeParams(): Record<string, string | number | boolean | null | undefined> {
  return {
    ...(props.queryParams as Record<string, string | number | boolean | null | undefined>),
    ...((props.extraParams ?? {}) as Record<string, string | number | boolean | null | undefined>),
  }
}

async function handleExport() {
  exporting.value = true
  try {
    await downloadListExport({
      url: getExportPath(props.module),
      params: mergeParams(),
      format: form.format,
      scope: form.scope,
      defaultFilename: buildExportFilename(props.module, form.format, form.scope),
    })
    ElMessage.success('导出成功，文件已开始下载')
    dialogVisible.value = false
  } catch (e) {
    if (e instanceof Error && e.message) {
      // downloadListExport 已 toast
    } else {
      ElMessage.error('导出失败，请稍后重试')
    }
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.list-export-btn .btn-icon {
  margin-right: 4px;
}
.export-intro {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 20px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
}
.intro-icon {
  font-size: 28px;
  color: var(--el-color-primary);
  flex-shrink: 0;
  margin-top: 2px;
}
.intro-title {
  margin: 0 0 4px;
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
}
.intro-desc {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.format-group {
  width: 100%;
  display: flex;
}
.format-group :deep(.el-radio-button) {
  flex: 1;
}
.format-group :deep(.el-radio-button__inner) {
  width: 100%;
  padding: 10px 12px;
}
.format-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.scope-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}
.scope-group :deep(.el-radio) {
  height: auto;
  margin-right: 0;
  padding: 12px 14px;
  border-radius: 8px;
  width: 100%;
}
.scope-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-left: 4px;
}
.scope-name {
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.scope-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.export-tip {
  margin-top: 4px;
}
.export-tip :deep(.el-alert__title) {
  font-size: 12px;
  line-height: 1.5;
}
</style>
