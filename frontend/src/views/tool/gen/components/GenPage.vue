<template>
  <div class="app-container module-page gen-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.gen" />
            <span>代码生成</span>
          </div>
          <p class="module-hero-desc">从当前数据库导入表结构，配置字段后一键生成 CRUD 前后端代码</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">已导入表</div>
        </div>
      </div>
    </el-card>

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="表名">
          <el-input v-model="queryParams.tableName" placeholder="请输入表名" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>生成配置列表</span>
          <div class="header-actions">
            <el-button type="primary" v-permission="'tool:gen:import'" :icon="Upload" @click="openImport">导入表</el-button>
            <el-button
              type="success"
              v-permission="'tool:gen:code'"
              :icon="Cpu"
              :disabled="!selectedIds.length"
              @click="batchGenerate"
            >
              生成代码
            </el-button>
            <el-button
              type="danger"
              v-permission="'tool:gen:remove'"
              :icon="Delete"
              :disabled="!selectedIds.length"
              @click="batchDelete"
            >
              删除
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" header-align="center" />
        <el-table-column prop="tableName" label="表名" min-width="160" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="tableComment" label="表描述" min-width="140" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="className" label="实体类" width="140" align="center" header-align="center" />
        <el-table-column prop="businessName" label="业务名" width="120" align="center" header-align="center" />
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" header-align="center" />
        <el-table-column label="操作" width="460" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" v-permission="'tool:gen:preview'" @click="handlePreview(row)">预览</el-button>
              <el-button size="small" type="primary" v-permission="'tool:gen:edit'" @click="openEdit(row)">配置</el-button>
              <el-button size="small" type="success" v-permission="'tool:gen:code'" @click="openGenerate(row)">生成</el-button>
              <el-button size="small" type="warning" v-permission="'tool:gen:edit'" @click="handleSync(row)">同步</el-button>
              <el-button size="small" type="warning" v-permission="'tool:gen:code'" @click="handleRemoveCode(row)">移除</el-button>
              <el-button size="small" type="danger" v-permission="'tool:gen:remove'" @click="handleDelete(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 导入表 -->
    <el-dialog v-model="importVisible" title="导入数据库表" width="900px" destroy-on-close>
      <el-form inline>
        <el-form-item label="表名">
          <el-input v-model="importQuery.tableName" clearable @keyup.enter="loadDbTables" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadDbTables">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-table
        v-loading="importLoading"
        :data="dbTables"
        max-height="400"
        border
        stripe
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
        @selection-change="(rows: DatabaseTable[]) => selectedTableNames = rows.map(r => r.tableName)"
      >
        <el-table-column type="selection" width="48" align="center" header-align="center" />
        <el-table-column prop="tableName" label="表名" min-width="200" align="center" header-align="center" />
        <el-table-column prop="tableComment" label="表描述" min-width="200" align="center" header-align="center" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" header-align="center" />
      </el-table>
      <el-pagination
        v-model:current-page="importQuery.pageNo"
        v-model:page-size="importQuery.pageSize"
        :total="importTotal"
        layout="total, prev, pager, next"
        class="table-pagination"
        @current-change="loadDbTables"
      />
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedTableNames.length" @click="handleImport">
          导入 ({{ selectedTableNames.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑配置 -->
    <el-dialog v-model="editVisible" title="编辑生成配置" width="1100px" destroy-on-close top="5vh">
      <el-tabs>
        <el-tab-pane label="基本信息">
          <el-form :model="editForm" label-width="108px" class="edit-basic-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="表名"><el-input v-model="editForm.tableName" disabled /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="表描述"><el-input v-model="editForm.tableComment" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="实体类名"><el-input v-model="editForm.className" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="包路径"><el-input v-model="editForm.packageName" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="模块名"><el-input v-model="editForm.moduleName" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="业务名"><el-input v-model="editForm.businessName" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="功能名称"><el-input v-model="editForm.functionName" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="作者"><el-input v-model="editForm.author" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="上级菜单ID">
                  <el-input-number v-model="editForm.parentMenuId" :min="0" controls-position="right" class="full-width" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="字段配置">
          <el-table
            :data="editForm.columns"
            border
            stripe
            max-height="420"
            size="small"
            :header-cell-style="tableHeaderStyle"
            :cell-style="tableCellStyle"
          >
            <el-table-column prop="columnName" label="字段名" width="120" fixed align="center" header-align="center" />
            <el-table-column label="描述" width="140" align="center" header-align="center">
              <template #default="{ row }">
                <div class="cell-control"><el-input v-model="row.columnComment" size="small" /></div>
              </template>
            </el-table-column>
            <el-table-column label="Java类型" width="128" align="center" header-align="center">
              <template #default="{ row }">
                <div class="cell-control">
                  <el-select v-model="row.javaType" size="small">
                    <el-option v-for="t in JAVA_TYPES" :key="t" :label="t" :value="t" />
                  </el-select>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="javaField" label="Java字段" width="120" align="center" header-align="center" />
            <el-table-column label="插入" width="72" align="center" header-align="center">
              <template #default="{ row }">
                <div class="cell-control"><el-switch v-model="row.isInsert" :active-value="1" :inactive-value="0" size="small" /></div>
              </template>
            </el-table-column>
            <el-table-column label="编辑" width="72" align="center" header-align="center">
              <template #default="{ row }">
                <div class="cell-control"><el-switch v-model="row.isEdit" :active-value="1" :inactive-value="0" size="small" /></div>
              </template>
            </el-table-column>
            <el-table-column label="列表" width="72" align="center" header-align="center">
              <template #default="{ row }">
                <div class="cell-control"><el-switch v-model="row.isList" :active-value="1" :inactive-value="0" size="small" /></div>
              </template>
            </el-table-column>
            <el-table-column label="查询" width="72" align="center" header-align="center">
              <template #default="{ row }">
                <div class="cell-control"><el-switch v-model="row.isQuery" :active-value="1" :inactive-value="0" size="small" /></div>
              </template>
            </el-table-column>
            <el-table-column label="查询方式" width="108" align="center" header-align="center">
              <template #default="{ row }">
                <div class="cell-control">
                  <el-select v-model="row.queryType" size="small">
                    <el-option v-for="q in QUERY_TYPES" :key="q.value" :label="q.label" :value="q.value" />
                  </el-select>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="显示类型" width="116" align="center" header-align="center">
              <template #default="{ row }">
                <div class="cell-control">
                  <el-select v-model="row.htmlType" size="small">
                    <el-option v-for="h in HTML_TYPES" :key="h.value" :label="h.label" :value="h.value" />
                  </el-select>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="字典类型" min-width="150" align="center" header-align="center">
              <template #default="{ row }">
                <div class="cell-control">
                  <el-select v-model="row.dictType" size="small" clearable filterable>
                    <el-option v-for="d in dictTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
                  </el-select>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="布局配置">
          <el-form label-width="120px" class="layout-form">
            <el-form-item label="表单布局">
              <el-radio-group v-model="editForm.formLayout" class="layout-radio-group">
                <el-radio value="vertical">从上到下（单列）</el-radio>
                <el-radio value="grid">一行两列</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览代码（dialog 挂载到 body，样式见文件末尾非 scoped 块） -->
    <el-dialog
      v-model="previewVisible"
      title="预览代码"
      width="90vw"
      top="4vh"
      destroy-on-close
      align-center
      class="preview-code-dialog"
      body-class="preview-code-dialog-body"
    >
      <el-tabs v-model="previewTab" class="preview-code-tabs">
        <el-tab-pane v-for="(code, name) in previewCodes" :key="name" :label="name" :name="name">
          <pre class="preview-code-block"><code>{{ code }}</code></pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 生成方式 -->
    <el-dialog v-model="generateVisible" title="生成代码" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon class="mb-16">请选择生成方式</el-alert>
      <div class="generate-options">
        <div
          class="generate-option"
          :class="{ selected: generateType === 'project' }"
          @click="generateType = 'project'"
        >
          <el-icon :size="28" color="#14b8a6"><FolderOpened /></el-icon>
          <div>
            <div class="option-title">生成到项目</div>
            <div class="option-desc">写入后端 Java、前端 Vue/API，并自动创建菜单</div>
          </div>
        </div>
        <div
          class="generate-option"
          :class="{ selected: generateType === 'download' }"
          @click="generateType = 'download'"
        >
          <el-icon :size="28" color="#3b82f6"><Download /></el-icon>
          <div>
            <div class="option-title">下载代码包</div>
            <div class="option-desc">打包为 ZIP 下载，可手动复制到项目</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" :disabled="!generateType" @click="confirmGenerate">下一步</el-button>
      </template>
    </el-dialog>

    <!-- 确认文件列表 -->
    <el-dialog
      v-model="previewFilesVisible"
      :title="previewAction === 'generate' ? '确认生成' : '确认移除'"
      width="600px"
      destroy-on-close
    >
      <el-alert :type="previewAction === 'generate' ? 'info' : 'warning'" :closable="false" class="mb-16">
        {{ previewAction === 'generate' ? '以下文件将被写入项目：' : '以下文件将被删除：' }}
      </el-alert>
      <el-alert
        v-if="previewAction === 'generate' && hasModifiedPreviewFiles"
        type="warning"
        :closable="false"
        show-icon
        class="mb-16"
      >
        检测到已存在且内容已修改的文件，确认后将覆盖本地手工改动。
      </el-alert>
      <el-scrollbar max-height="300px" v-loading="previewFilesLoading">
        <ul class="file-list">
          <li
            v-for="f in previewFiles"
            :key="f"
            :class="{
              'file-new': f.startsWith('[新建]'),
              'file-overwrite': f.startsWith('[覆盖·无变化]'),
              'file-modified': f.includes('已修改'),
            }"
          >{{ f }}</li>
        </ul>
      </el-scrollbar>
      <template #footer>
        <el-button @click="previewFilesVisible = false">取消</el-button>
        <el-button
          :type="previewAction === 'generate' ? 'primary' : 'danger'"
          :loading="generateLoading"
          @click="executeGenerateOrRemove"
        >
          {{ previewAction === 'generate' ? '确认生成' : '确认移除' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 结果 -->
    <el-dialog v-model="resultVisible" :title="resultType === 'generate' ? '生成完成' : '移除完成'" width="600px">
      <el-alert :type="resultType === 'generate' ? 'success' : 'warning'" :closable="false" class="mb-16">
        共处理 {{ resultFiles.length }} 项
      </el-alert>
      <el-scrollbar max-height="280px">
        <ul class="file-list">
          <li v-for="f in resultFiles" :key="f">{{ f }}</li>
        </ul>
      </el-scrollbar>
      <el-alert v-if="resultType === 'generate'" type="info" :closable="false" class="mt-16">
        请重启后端以加载新 Java 代码；菜单已自动创建，重新登录即可看到新页面。
      </el-alert>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import {
  Search, Refresh, Upload, Delete, Cpu,
  FolderOpened, Download,
} from '@element-plus/icons-vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import { useGenPage } from '../composables/useGenPage'
import type { DatabaseTable } from '@/api/tool/gen'

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const {
  loading, total, tableData, selectedIds, queryParams,
  importVisible, importLoading, dbTables, selectedTableNames, importQuery, importTotal,
  editVisible, editForm, dictTypeOptions,
  previewVisible, previewTab, previewCodes,
  generateVisible, generateType, generateLoading,
  previewFilesVisible, previewFiles, previewAction, previewFilesLoading, hasModifiedPreviewFiles,
  resultVisible, resultType, resultFiles,
  JAVA_TYPES, QUERY_TYPES, HTML_TYPES,
  loadData, handleQuery, resetQuery, onSelectionChange,
  openImport, loadDbTables, handleImport,
  openEdit, saveEdit, handleDelete, batchDelete,
  handlePreview, openGenerate, batchGenerate, confirmGenerate, executeGenerateOrRemove,
  handleRemoveCode, handleSync,
} = useGenPage()

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.gen-page {
  .edit-basic-form {
    max-width: 920px;
    margin: 8px auto 0;

    :deep(.el-form-item__label) {
      justify-content: center;
      text-align: center;
    }

    :deep(.el-input),
    :deep(.el-input-number) {
      width: 100%;
    }

    .full-width {
      width: 100%;
    }
  }

  .layout-form {
    display: flex;
    justify-content: center;
    padding: 12px 0 8px;

    :deep(.el-form-item) {
      margin-bottom: 0;
    }

    :deep(.el-form-item__label) {
      justify-content: center;
      text-align: center;
    }
  }

  .layout-radio-group {
    justify-content: center;
  }

  .cell-control {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
  }

  .mb-16 { margin-bottom: 16px; }
  .mt-16 { margin-top: 16px; }

  .generate-options {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .generate-option {
    display: flex;
    align-items: flex-start;
    gap: 14px;
    padding: 16px;
    border: 2px solid #e5e7eb;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover { border-color: var(--theme-primary, #111827); }
    &.selected {
      border-color: var(--theme-primary, #111827);
      background: rgba(17, 24, 39, 0.04);
    }
  }

  .option-title { font-weight: 600; margin-bottom: 4px; }
  .option-desc { font-size: 12px; color: var(--el-text-color-secondary); }

  .file-list {
    margin: 0;
    padding-left: 20px;
    font-size: 13px;
    line-height: 1.8;
    word-break: break-all;

    .file-modified {
      color: var(--el-color-warning);
      font-weight: 600;
    }

    .file-overwrite {
      color: var(--el-text-color-secondary);
    }

    .file-new {
      color: var(--el-color-success);
    }
  }
}
</style>

<!-- dialog 挂到 body，scoped 样式无效，须用全局样式覆盖 App.vue 的 .el-dialog__body 滚动 -->
<style lang="scss">
.preview-code-dialog {
  margin-bottom: 0;
  overflow: hidden;
}

.el-dialog__body.preview-code-dialog-body {
  max-height: none !important;
  overflow: hidden !important;
  padding-top: 8px;
}

.preview-code-dialog-body .preview-code-tabs {
  .el-tabs__header {
    margin-bottom: 12px;
  }

  .el-tabs__content {
    padding: 0;
    overflow: hidden;
  }
}

.preview-code-dialog-body .preview-code-block {
  margin: 0;
  padding: 16px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 8px;
  max-height: calc(100vh - 180px);
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
}
</style>
