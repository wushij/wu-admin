<template>
  <div class="app-container module-page ai-model-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.aiModel" />
            <span>AI 模型配置</span>
          </div>
          <p class="module-hero-desc">管理 AI wu助手接入的大模型供应商，支持 DeepSeek / OpenAI / 通义千问 / Kimi</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">模型总数</div>
        </div>
      </div>
    </el-card>

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="配置名称">
          <el-input v-model="queryParams.name" placeholder="请输入配置名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="queryParams.provider" placeholder="全部供应商" clearable style="width: 160px">
            <el-option v-for="p in PROVIDER_OPTIONS" :key="p.value" :label="p.label" :value="p.value" />
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
          <span>模型列表</span>
          <div class="header-actions">
            <el-button type="primary" :icon="Plus" v-permission="'system:ai-model:create'" @click="openDialog()">新增模型</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="model-grid">
        <el-empty v-if="!loading && list.length === 0" description="暂无模型配置，点击右上角新增模型" class="grid-empty" />
        <div v-for="item in list" :key="item.id" class="model-card" :class="{ 'is-disabled': item.status === 0 }">
          <div class="model-card-head">
            <div class="provider-badge" :style="{ background: providerMeta(item.provider).bg, color: providerMeta(item.provider).color }">
              {{ providerMeta(item.provider).short }}
            </div>
            <div class="model-card-title">
              <div class="name-row">
                <span class="name" :title="item.name">{{ item.name }}</span>
                <span v-if="item.isDefault === 1" class="default-badge">
                  <el-icon :size="11"><Star /></el-icon>
                  <span>默认</span>
                </span>
              </div>
              <div class="sub">{{ providerMeta(item.provider).label }} · {{ item.modelName }}</div>
            </div>
            <div class="status-switch-wrap">
              <el-switch
                :model-value="item.status === 1"
                :disabled="!canUpdate"
                @change="(v: boolean | string | number) => toggleStatus(item, Boolean(v))"
              />
              <span class="status-text" :class="{ active: item.status === 1 }">{{ item.status === 1 ? '启用' : '停用' }}</span>
            </div>
          </div>

          <div class="model-card-body">
            <div class="info-line">
              <span class="label">接口地址</span>
              <span class="value" :title="item.baseUrl">{{ item.baseUrl || '—' }}</span>
            </div>
            <div class="info-line">
              <span class="label">API Key</span>
              <div class="value">
                <span class="key-tag" :class="{ configured: item.hasApiKey }">
                  {{ item.hasApiKey ? (item.apiKeyMasked || '✓ 已配置') : '未配置' }}
                </span>
              </div>
            </div>
            <div class="info-line">
              <span class="label">模型参数</span>
              <span class="value">温度 {{ item.temperature ?? 0.7 }} · 最大 {{ item.maxTokens ?? 4096 }} Tokens</span>
            </div>
            <div v-if="testResults[item.id] !== undefined" class="test-result" :class="testResults[item.id]! >= 0 ? 'ok' : 'fail'">
              <el-icon v-if="testResults[item.id]! >= 0"><CircleCheckFilled /></el-icon>
              <el-icon v-else><CircleCloseFilled /></el-icon>
              <span>{{ testResults[item.id]! >= 0 ? `连接正常 · ${testResults[item.id]}ms` : '连接失败' }}</span>
            </div>
          </div>

          <div class="model-card-foot">
            <el-button
              size="small"
              class="action-btn"
              :loading="testingId === item.id"
              :icon="Connection"
              v-permission="'system:ai-model:test'"
              @click="handleTest(item)"
            >
              测试
            </el-button>
            <el-button
              v-if="item.isDefault !== 1"
              size="small"
              type="warning"
              plain
              class="action-btn"
              :icon="Star"
              :disabled="item.status !== 1"
              v-permission="'system:ai-model:update'"
              @click="handleSetDefault(item)"
            >
              设为默认
            </el-button>
            <el-button
              size="small"
              type="primary"
              plain
              class="action-btn"
              :icon="Edit"
              v-permission="'system:ai-model:update'"
              @click="openDialog(item)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              class="action-btn"
              :icon="Delete"
              :disabled="item.isDefault === 1"
              v-permission="'system:ai-model:delete'"
              @click="handleDelete(item)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[8, 16, 32]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑模型' : '新增模型'"
      width="640px"
      top="6vh"
      class="ai-model-dialog"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="供应商" prop="provider">
          <div class="provider-select">
            <div
              v-for="p in PROVIDER_OPTIONS"
              :key="p.value"
              class="provider-item"
              :class="{ active: form.provider === p.value }"
              @click="selectProvider(p.value)"
            >
              <span class="provider-item-badge" :style="{ background: providerMeta(p.value).bg, color: providerMeta(p.value).color }">
                {{ providerMeta(p.value).short }}
              </span>
              <span>{{ p.label }}</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="form.name" placeholder="如：DeepSeek 官方" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-select v-model="form.modelName" filterable allow-create default-first-option placeholder="选择或输入模型名称" style="width: 100%">
            <el-option v-for="m in providerModels" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="如：https://api.deepseek.com/v1" />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model="form.apiKey"
            type="password"
            show-password
            :placeholder="form.id && editHasKey ? '留空表示不修改已保存的 Key' : '请输入 API Key'"
            autocomplete="new-password"
          />
          <el-link
            v-if="providerMeta(form.provider).portalUrl"
            type="primary"
            :href="providerMeta(form.provider).portalUrl"
            target="_blank"
            class="key-portal-link"
          >
            获取 {{ providerMeta(form.provider).label }} API Key
          </el-link>
        </el-form-item>
        <el-form-item label="温度">
          <div class="slider-row">
            <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" style="flex: 1" />
            <span class="slider-value">{{ form.temperature?.toFixed(1) }}</span>
            <el-tooltip content="控制 AI 回答的随机性与创造力：0 偏严谨确定，1 偏均衡，2 偏富有想象力" placement="top">
              <el-icon style="color: var(--el-text-color-secondary); cursor: pointer;"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </el-form-item>
        <el-form-item label="最大 Tokens">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-input-number v-model="form.maxTokens" :min="128" :max="32768" :step="256" style="width: 180px" />
            <el-tooltip content="限制 AI 单次回答生成的最大文本长度（1000 Tokens ≈ 700 汉字），默认 4096" placement="top">
              <el-icon style="color: var(--el-text-color-secondary); cursor: pointer;"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="可选" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :loading="formTesting" v-permission="'system:ai-model:test'" @click="handleFormTest">测试连接</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Search,
  Refresh,
  Plus,
  CircleCheckFilled,
  CircleCloseFilled,
  QuestionFilled,
  Connection,
  Star,
  Edit,
  Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import { useUserStore } from '@/store/user'
import {
  pageAiModel,
  createAiModel,
  updateAiModel,
  deleteAiModel,
  setDefaultAiModel,
  testAiModel,
  type AiModelVO,
  type AiModelSaveDTO,
  type AiModelPageQuery,
  type AiProvider,
} from '@/api/system/ai-model'

/** 供应商元信息与默认配置（选择供应商时自动回填） */
const PROVIDER_META: Record<string, { label: string; short: string; color: string; bg: string; baseUrl: string; portalUrl: string; models: string[] }> = {
  deepseek: {
    label: 'DeepSeek',
    short: 'DS',
    color: '#4d6bfe',
    bg: 'rgba(77, 107, 254, 0.12)',
    baseUrl: 'https://api.deepseek.com/v1',
    portalUrl: 'https://platform.deepseek.com',
    models: ['deepseek-v4-flash', 'deepseek-v4-pro'],
  },
  openai: {
    label: 'OpenAI',
    short: 'GPT',
    color: '#10a37f',
    bg: 'rgba(16, 163, 127, 0.12)',
    baseUrl: 'https://api.openai.com/v1',
    portalUrl: 'https://platform.openai.com',
    models: ['gpt-4o', 'gpt-4o-mini'],
  },
  qwen: {
    label: '通义千问',
    short: 'QW',
    color: '#615ced',
    bg: 'rgba(97, 92, 237, 0.12)',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    portalUrl: 'https://modelstudio.console.alibabacloud.com',
    models: ['qwen3.7-plus', 'qwen3.7-max', 'qwen3.6-plus'],
  },
  kimi: {
    label: 'Kimi',
    short: 'KM',
    color: '#0f172a',
    bg: 'rgba(15, 23, 42, 0.08)',
    baseUrl: 'https://api.moonshot.cn/v1',
    portalUrl: 'https://platform.moonshot.cn',
    models: ['kimi-k2.7-code', 'kimi-k2.6'],
  },
}

const PROVIDER_OPTIONS = Object.entries(PROVIDER_META).map(([value, meta]) => ({ value: value as AiProvider, label: meta.label }))

function providerMeta(provider: string) {
  return PROVIDER_META[provider] || { label: provider, short: 'AI', color: '#64748b', bg: 'rgba(100, 116, 139, 0.12)', baseUrl: '', portalUrl: '', models: [] }
}

const userStore = useUserStore()
const canUpdate = computed(() =>
  (userStore.userInfo?.permissions || []).some((p: string) => p === '*:*:*' || p === 'system:ai-model:update'),
)

const loading = ref(false)
const total = ref(0)
const list = ref<AiModelVO[]>([])
const testingId = ref<number | null>(null)
/** id -> 延迟ms（-1 表示失败） */
const testResults = reactive<Record<number, number>>({})

const queryParams = reactive<AiModelPageQuery>({
  pageNo: 1,
  pageSize: 8,
  name: '',
  provider: '',
  status: null,
})

async function loadData() {
  loading.value = true
  try {
    const res = await pageAiModel({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      name: queryParams.name || undefined,
      provider: queryParams.provider || undefined,
      status: queryParams.status,
    })
    list.value = res.data?.list || []
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
  queryParams.name = ''
  queryParams.provider = ''
  queryParams.status = null
  handleQuery()
}

/* ---------- 新增 / 编辑 ---------- */
const dialogVisible = ref(false)
const submitting = ref(false)
const formTesting = ref(false)
const formRef = ref<FormInstance>()
const editHasKey = ref(false)

const defaultForm = (): AiModelSaveDTO & { temperature: number; maxTokens: number } => ({
  id: undefined,
  name: '',
  provider: 'deepseek',
  modelName: 'deepseek-v4-flash',
  baseUrl: PROVIDER_META.deepseek.baseUrl,
  apiKey: '',
  temperature: 0.7,
  maxTokens: 4096,
  systemPrompt: '',
  isDefault: 0,
  status: 1,
  remark: '',
})

const form = reactive(defaultForm())

const rules: FormRules = {
  provider: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'change' }],
  baseUrl: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
}

const providerModels = computed(() => providerMeta(form.provider).models)

function selectProvider(provider: AiProvider) {
  if (form.provider === provider) return
  form.provider = provider
  const meta = providerMeta(provider)
  // 切换供应商时自动回填官方默认地址与首个推荐模型
  form.baseUrl = meta.baseUrl
  form.modelName = meta.models[0] || ''
}

function openDialog(row?: AiModelVO) {
  Object.assign(form, defaultForm())
  editHasKey.value = false
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      provider: row.provider,
      modelName: row.modelName,
      baseUrl: row.baseUrl,
      apiKey: '',
      temperature: row.temperature ?? 0.7,
      maxTokens: row.maxTokens ?? 4096,
      systemPrompt: row.systemPrompt || '',
      isDefault: row.isDefault ?? 0,
      status: row.status ?? 1,
      remark: row.remark || '',
    })
    editHasKey.value = !!row.hasApiKey
  }
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const data: AiModelSaveDTO = { ...form, apiKey: form.apiKey || undefined }
    if (form.id) {
      await updateAiModel(data)
      ElMessage.success('修改成功')
    } else {
      await createAiModel(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

/* ---------- 卡片操作 ---------- */
async function handleTest(row: AiModelVO) {
  testingId.value = row.id
  try {
    const res = await testAiModel({ id: row.id })
    testResults[row.id] = Number(res.data) || 0
    ElMessage.success(`连接正常，延迟 ${res.data}ms`)
  } catch {
    testResults[row.id] = -1
  } finally {
    testingId.value = null
  }
}

async function handleFormTest() {
  const valid = await formRef.value?.validateField(['provider', 'modelName', 'baseUrl']).catch(() => false)
  if (valid === false) return
  formTesting.value = true
  try {
    const res = await testAiModel({
      id: form.id,
      provider: form.provider,
      modelName: form.modelName,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey || undefined,
    })
    ElMessage.success(`连接正常，延迟 ${res.data}ms`)
  } finally {
    formTesting.value = false
  }
}

async function handleSetDefault(row: AiModelVO) {
  await ElMessageBox.confirm(`确定将「${row.name}」设为默认模型吗？AI wu助手默认使用该模型对话。`, '提示', { type: 'warning' })
  await setDefaultAiModel(row.id)
  ElMessage.success('设置成功')
  loadData()
}

async function toggleStatus(row: AiModelVO, enabled: boolean) {
  if (row.isDefault === 1 && !enabled) {
    ElMessage.warning('默认模型不能停用，请先设置其他默认模型')
    return
  }
  await updateAiModel({
    id: row.id,
    name: row.name,
    provider: row.provider,
    modelName: row.modelName,
    baseUrl: row.baseUrl,
    temperature: row.temperature,
    maxTokens: row.maxTokens,
    systemPrompt: row.systemPrompt,
    isDefault: row.isDefault,
    status: enabled ? 1 : 0,
    remark: row.remark,
  })
  ElMessage.success(enabled ? '已启用' : '已停用')
  loadData()
}

async function handleDelete(row: AiModelVO) {
  await ElMessageBox.confirm(`确定要删除模型「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteAiModel(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
  min-height: 120px;
}

.grid-empty {
  grid-column: 1 / -1;
}

.model-card {
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--el-bg-color, #ffffff);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-5, #818cf8);
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  }

  &.is-disabled {
    opacity: 0.65;
  }
}

.model-card-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.provider-badge {
  width: 44px;
  height: 44px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 800;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.model-card-title {
  flex: 1;
  min-width: 0;

  .name-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .name {
    font-size: 15px;
    font-weight: 700;
    color: var(--el-text-color-primary, #0f172a);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .default-badge {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    padding: 2px 8px;
    border-radius: 20px;
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    color: #ffffff;
    font-size: 10.5px;
    font-weight: 600;
    box-shadow: 0 2px 6px rgba(245, 158, 11, 0.3);
    flex-shrink: 0;
  }

  .sub {
    margin-top: 3px;
    font-size: 12px;
    color: var(--el-text-color-secondary, #64748b);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.status-switch-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;

  .status-text {
    font-size: 12px;
    color: #94a3b8;
    font-weight: 500;

    &.active {
      color: var(--theme-primary, #6366f1);
      font-weight: 600;
    }
  }
}

.model-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--el-fill-color-lighter, rgba(248, 250, 252, 0.8));
  border: 1px solid rgba(226, 232, 240, 0.7);

  .info-line {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12px;
    line-height: 1.6;

    .label {
      flex-shrink: 0;
      width: 58px;
      color: var(--el-text-color-secondary, #64748b);
      font-weight: 500;
    }

    .value {
      flex: 1;
      min-width: 0;
      color: var(--el-text-color-regular, #334155);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.key-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-family: Consolas, Monaco, monospace;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;

  &.configured {
    background: rgba(16, 185, 129, 0.1);
    color: #059669;
    border-color: rgba(16, 185, 129, 0.25);
    font-weight: 600;
  }
}

.test-result {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 6px;
  margin-top: 2px;
  width: fit-content;

  &.ok {
    background: #f0fdf4;
    color: #16a34a;
    border: 1px solid #bbf7d0;
  }

  &.fail {
    background: #fef2f2;
    color: #dc2626;
    border: 1px solid #fecaca;
  }
}

.model-card-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px dashed rgba(226, 232, 240, 0.8);
  padding-top: 12px;

  .action-btn {
    border-radius: 8px;
    height: 28px;
    padding: 0 10px;
    font-size: 12px;
    transition: all 0.2s;
  }
}

/* 供应商选择块 */
.provider-select {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.provider-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1.5px solid var(--el-border-color-lighter);
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-blank);
  transition: border-color 0.2s, background-color 0.2s, color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
  user-select: none;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-fill-color-light);
  }

  &.active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);
    box-shadow: 0 0 0 1px var(--el-color-primary);
  }
}

.provider-item-badge {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10.5px;
  font-weight: 700;
  flex-shrink: 0;
}

.key-portal-link {
  margin-top: 4px;
  font-size: 12px;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;

  .slider-value {
    width: 32px;
    text-align: right;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

:deep(.ai-model-dialog) {
  border-radius: 16px;
  overflow: hidden;
  margin-top: 6vh !important;
  margin-bottom: 6vh !important;

  .el-dialog__header {
    margin-right: 0;
    padding: 16px 20px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .el-dialog__title {
    font-size: 16px;
    font-weight: 700;
    color: var(--el-text-color-primary);
  }

  .el-dialog__body {
    max-height: calc(82vh - 110px);
    overflow-y: auto;
    padding: 20px 24px 8px;

    &::-webkit-scrollbar {
      width: 5px;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--el-border-color-darker);
      border-radius: 10px;
    }
  }

  .el-dialog__footer {
    padding: 12px 20px 16px;
    border-top: 1px solid var(--el-border-color-lighter);
  }
}
</style>
