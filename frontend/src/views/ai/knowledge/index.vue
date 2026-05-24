<template>
  <div class="app-container module-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.aiKnowledge" />
            <span>AI 知识库</span>
          </div>
          <p class="module-hero-desc">维护 AI wu助手的项目问答知识，对话时按用户提问自动检索并注入，提升回答准确性</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">知识条目</div>
        </div>
      </div>
    </el-card>

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="标题/关键词">
          <el-input v-model="queryParams.keyword" placeholder="标题或关键词" clearable style="width: 200px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.category" placeholder="全部分类" clearable style="width: 140px">
            <el-option v-for="(label, key) in CATEGORY_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
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
          <span>知识列表</span>
          <el-button
            v-permission="'system:ai-knowledge:create'"
            type="primary"
            size="small"
            :icon="Plus"
            @click="openCreate"
          >
            新增知识
          </el-button>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        :header-cell-style="tableHeaderStyle"
        :cell-style="tableCellStyle"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" header-align="center" />
        <el-table-column prop="title" label="标题" min-width="180" align="left" header-align="center" show-overflow-tooltip />
        <el-table-column prop="keywords" label="关键词" min-width="180" align="left" header-align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ row.keywords || '—' }}</template>
        </el-table-column>
        <el-table-column label="分类" width="100" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ CATEGORY_LABELS[row.category] || row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="权重" width="80" align="center" header-align="center" />
        <el-table-column label="状态" width="90" align="center" header-align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" align="center" header-align="center" />
        <el-table-column label="操作" width="180" fixed="right" align="center" header-align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-permission="'system:ai-knowledge:update'"
                type="primary"
                size="small"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'system:ai-knowledge:delete'"
                type="danger"
                size="small"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-drawer v-model="editVisible" :title="form.id ? '编辑知识' : '新增知识'" size="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="knowledge-form">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="如：忘记密码如何找回" />
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input v-model="form.keywords" maxlength="255" show-word-limit placeholder="逗号分隔，如：忘记密码,重置密码,登录不了" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" style="width: 200px">
            <el-option v-for="(label, key) in CATEGORY_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            maxlength="4000"
            show-word-limit
            placeholder="支持 Markdown 排版；代码块请使用成对的 ``` 围栏"
          />
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="form.sort" :min="0" :max="9999" :step="5" />
          <span class="form-tip">数值越大越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import {
  pageAiKnowledge,
  getAiKnowledge,
  createAiKnowledge,
  updateAiKnowledge,
  deleteAiKnowledge,
  type AiKnowledgeVO,
  type AiKnowledgeSaveDTO,
  type AiKnowledgePageQuery,
} from '@/api/system/ai-knowledge'

const CATEGORY_LABELS: Record<string, string> = {
  faq: '常见问题',
  manual: '操作手册',
  module: '模块说明',
  other: '其它',
}

const tableHeaderStyle = { textAlign: 'center' as const }
const tableCellStyle = { textAlign: 'center' as const }

const loading = ref(false)
const total = ref(0)
const tableData = ref<AiKnowledgeVO[]>([])

const queryParams = reactive<AiKnowledgePageQuery>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  category: '',
  status: null,
})

const editVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const defaultForm = (): AiKnowledgeSaveDTO => ({
  id: undefined,
  title: '',
  keywords: '',
  content: '',
  category: 'faq',
  sort: 0,
  status: 1,
})
const form = reactive<AiKnowledgeSaveDTO>(defaultForm())

/** 校验正文中的 ``` 围栏成对闭合，与后端 AiKnowledgeSanitizer 逻辑对齐，前置拦截 */
function validateFence(_r: unknown, value: string, callback: (e?: Error) => void) {
  if (!value) {
    callback()
    return
  }
  const count = (value.match(/```/g) || []).length
  if (count % 2 !== 0) {
    callback(new Error('代码块围栏 ``` 未成对闭合'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [
    { required: true, message: '请输入正文', trigger: 'blur' },
    { validator: validateFence, trigger: 'blur' },
  ],
}

async function loadData() {
  loading.value = true
  try {
    const res = await pageAiKnowledge({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      category: queryParams.category || undefined,
      status: queryParams.status,
    })
    tableData.value = res.data?.list || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNo = 1
  loadData()
}

function resetQuery() {
  queryParams.keyword = ''
  queryParams.category = ''
  queryParams.status = null
  handleQuery()
}

function openCreate() {
  Object.assign(form, defaultForm())
  editVisible.value = true
}

async function openEdit(row: AiKnowledgeVO) {
  const res = await getAiKnowledge(row.id)
  const data = res.data
  Object.assign(form, {
    id: data.id,
    title: data.title,
    keywords: data.keywords || '',
    content: data.content,
    category: data.category || 'faq',
    sort: data.sort ?? 0,
    status: data.status ?? 1,
  })
  editVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateAiKnowledge(form)
      ElMessage.success('修改成功')
    } else {
      await createAiKnowledge(form)
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: AiKnowledgeVO) {
  await ElMessageBox.confirm(`确定要删除知识「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteAiKnowledge(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.knowledge-form {
  padding-right: 8px;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
