<template>
  <div class="app-container recycle-center-page">
    <section class="hero-panel">
      <div class="hero-glow hero-glow--left" />
      <div class="hero-glow hero-glow--right" />
      <div class="hero-main">
        <div class="hero-icon-wrap">
          <el-icon><Delete /></el-icon>
        </div>
        <div>
          <h2 class="hero-title">回收中心</h2>
          <p class="hero-subtitle">{{ visibleTypes.length }} 类业务数据 · 可恢复或彻底清除</p>
        </div>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <span class="hero-stat-num">{{ visiblePendingTotal }}</span>
          <span class="hero-stat-label">待处理</span>
        </div>
        <div class="hero-stat-divider" />
        <div class="hero-stat">
          <span class="hero-stat-num">{{ activePendingCount }}</span>
          <span class="hero-stat-label">当前分类</span>
        </div>
      </div>
    </section>

    <div class="type-cards">
      <button
        v-for="type in visibleTypes"
        :key="type.key"
        type="button"
        class="type-card"
        :class="[
          `accent-${type.accent}`,
          { active: activeType === type.key, 'has-pending': summary[type.key] > 0 },
        ]"
        @click="switchType(type.key)"
      >
        <span class="type-icon-wrap">
          <el-icon class="type-icon"><component :is="type.icon" /></el-icon>
        </span>
        <span class="type-body">
          <span class="type-label">{{ type.label }}</span>
          <span class="type-count">{{ summary[type.key] || 0 }} 条</span>
        </span>
        <span v-if="summary[type.key] > 0" class="type-dot" />
      </button>
    </div>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-header">
          <div class="table-header-left">
            <span
              v-if="currentType"
              class="table-header-icon"
              :class="`accent-${currentType.accent}`"
            >
              <el-icon><component :is="currentType.icon" /></el-icon>
            </span>
            <span>{{ currentType?.label }}回收列表</span>
          </div>
          <el-button type="primary" link class="refresh-btn" @click="handleRefresh">
            <span class="refresh-label-wrap">
              <el-icon class="refresh-icon" :class="{ 'refresh-icon--spinning': refreshing }">
                <Refresh />
              </el-icon>
              刷新
            </span>
          </el-button>
        </div>
      </template>

      <el-alert
        v-if="currentType?.hint"
        type="warning"
        :closable="false"
        show-icon
        class="type-hint"
        :title="currentType.hint"
      />

      <el-form v-if="currentType?.searchFields?.length" inline class="search-form">
        <el-form-item v-for="field in currentType.searchFields" :key="field.key" :label="field.label">
          <el-input
            v-model="searchForm[field.key]"
            :placeholder="field.placeholder"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        empty-text="暂无已删除数据"
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
      >
        <el-table-column
          v-for="col in currentType?.columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          show-overflow-tooltip
        >
          <template v-if="col.tag && col.dictType" #default="{ row }">
            <DictTag :value="row[col.prop]" :dict-type="col.dictType" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                v-if="canDelete"
                type="primary"
                plain
                size="small"
                @click="handleRestore(row)"
              >
                恢复
              </el-button>
              <el-button
                v-if="canDelete"
                type="danger"
                plain
                size="small"
                @click="handlePermanentDelete(row)"
              >
                清除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadList"
        @current-change="loadList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Delete, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DictTag from '@/components/DictTag.vue'
import { getRecycleSummary, type RecycleSummary } from '@/api/system/recycle'
import { useUserStore } from '@/store/user'
import { hasMenuPermission } from '@/directives/permission'
import { RECYCLE_TYPES, getRecycleType, type RecycleTypeKey } from './recycle-config'
import { preloadDicts } from '@/composables/useDict'
import { DICT_TYPE } from '@/constants/dict'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const summary = reactive<RecycleSummary>({
  user: 0,
  role: 0,
  menu: 0,
  dept: 0,
  post: 0,
  ticket: 0,
  approval: 0,
  dict: 0,
  dictData: 0,
  announce: 0,
  job: 0,
  file: 0,
  total: 0,
})

const activeType = ref<RecycleTypeKey>('user')
const loading = ref(false)
const refreshing = ref(false)
const tableData = ref<Record<string, unknown>[]>([])
const total = ref(0)
const searchForm = reactive<Record<string, string>>({})
const query = reactive({ pageNo: 1, pageSize: 10 })

const visibleTypes = computed(() =>
  RECYCLE_TYPES.filter((t) => hasMenuPermission(userStore.menus, t.permission)),
)

const currentType = computed(() => getRecycleType(activeType.value))

const activePendingCount = computed(() => summary[activeType.value] ?? 0)

/** 仅统计当前用户可见分类，避免顶部总数与卡片不一致 */
const visiblePendingTotal = computed(() =>
  visibleTypes.value.reduce((sum, type) => sum + (summary[type.key] ?? 0), 0),
)

const canDelete = computed(() => {
  const perm = currentType.value?.deletePermission
  return perm ? hasMenuPermission(userStore.menus, perm) : false
})

function initSearchForm() {
  Object.keys(searchForm).forEach((k) => delete searchForm[k])
  currentType.value?.searchFields?.forEach((f) => {
    searchForm[f.key] = ''
  })
}

function switchType(key: RecycleTypeKey) {
  if (activeType.value === key) return
  activeType.value = key
  query.pageNo = 1
  initSearchForm()
  router.replace({ query: { tab: key } })
  loadList()
}

async function loadSummary() {
  try {
    const res = await getRecycleSummary()
    Object.assign(summary, res.data)
  } catch (e) {
    console.error(e)
  }
}

async function handleRefresh() {
  if (refreshing.value) return
  refreshing.value = true
  const startedAt = Date.now()
  const minSpinMs = 500
  try {
    await Promise.all([loadSummary(), loadList()])
  } finally {
    const remain = minSpinMs - (Date.now() - startedAt)
    if (remain > 0) {
      await new Promise((resolve) => setTimeout(resolve, remain))
    }
    refreshing.value = false
  }
}

async function loadList() {
  const type = currentType.value
  if (!type) return
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNo: query.pageNo,
      pageSize: query.pageSize,
    }
    type.searchFields?.forEach((f) => {
      const val = searchForm[f.key]?.trim()
      if (val) params[f.key] = val
    })
    const res = await type.fetchPage(params)
    tableData.value = (res.data?.list || []) as Record<string, unknown>[]
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNo = 1
  loadList()
}

function resetSearch() {
  initSearchForm()
  handleSearch()
}

async function handleRestore(row: Record<string, unknown>) {
  const type = currentType.value
  if (!type || row.id == null) return
  await ElMessageBox.confirm(`确定恢复该${type.label}吗？`, '恢复确认', { type: 'info' })
  await type.restore(Number(row.id))
  ElMessage.success('已恢复')
  await loadSummary()
  await loadList()
}

async function handlePermanentDelete(row: Record<string, unknown>) {
  const type = currentType.value
  if (!type || row.id == null) return
  await ElMessageBox.confirm('彻底删除后不可恢复，是否继续？', '警告', { type: 'warning' })
  await type.deletePermanent(Number(row.id))
  ElMessage.success('已清除')
  await loadSummary()
  await loadList()
}

watch(visibleTypes, (types) => {
  if (!types.length) return
  const tab = route.query.tab as string
  const matched = types.find((t) => t.key === tab)
  if (matched) {
    activeType.value = matched.key
  } else if (!types.some((t) => t.key === activeType.value)) {
    activeType.value = types[0].key
  }
}, { immediate: true })

onMounted(async () => {
  await preloadDicts([
    DICT_TYPE.TICKET_STATUS,
    DICT_TYPE.APPROVAL_FORM_TYPE,
  ])
  await loadSummary()
  initSearchForm()
  await loadList()
})
</script>

<style scoped lang="scss">
.recycle-center-page {
  .hero-panel {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    padding: 28px 32px;
    margin-bottom: 20px;
    border-radius: 16px;
    overflow: hidden;
    color: #fff;
    background: linear-gradient(
      135deg,
      var(--theme-primary, #111827) 0%,
      var(--theme-primary-hover, #374151) 52%,
      var(--theme-primary-active, #4b5563) 100%
    );
    box-shadow: 0 10px 28px rgba(0, 0, 0, 0.12);
  }

  .hero-glow {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.14) 0%, transparent 70%);
  }

  .hero-glow--left {
    width: 220px;
    height: 220px;
    top: -80px;
    left: -40px;
  }

  .hero-glow--right {
    width: 280px;
    height: 280px;
    bottom: -120px;
    right: -60px;
    opacity: 0.7;
  }

  .hero-main {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 18px;
    min-width: 0;
  }

  .hero-icon-wrap {
    width: 56px;
    height: 56px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.16);
    backdrop-filter: blur(8px);
    border: 1px solid rgba(255, 255, 255, 0.22);
    color: #fff;
    font-size: 26px;
    flex-shrink: 0;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
  }

  .hero-title {
    margin: 0 0 6px;
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 0.02em;
  }

  .hero-subtitle {
    margin: 0;
    font-size: 13px;
    opacity: 0.82;
    line-height: 1.5;
  }

  .hero-stats {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 20px;
    flex-shrink: 0;
    padding: 12px 22px;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.12);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.18);
  }

  .hero-stat {
    text-align: center;
    min-width: 64px;
  }

  .hero-stat-num {
    display: block;
    font-size: 30px;
    font-weight: 700;
    line-height: 1.1;
  }

  .hero-stat-label {
    display: block;
    margin-top: 4px;
    font-size: 12px;
    opacity: 0.78;
  }

  .hero-stat-divider {
    width: 1px;
    height: 36px;
    background: rgba(255, 255, 255, 0.22);
  }

  .type-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(148px, 1fr));
    gap: 12px;
    margin-bottom: 20px;
  }

  .type-card {
    position: relative;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 14px 14px 12px;
    border-radius: 14px;
    border: 1px solid var(--el-border-color-lighter);
    background: var(--el-bg-color);
    cursor: pointer;
    text-align: left;
    transition:
      border-color 0.22s,
      box-shadow 0.22s,
      transform 0.18s,
      background 0.22s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
    }

    &.active {
      border-color: transparent;
      background: var(--el-bg-color);
      box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
      transform: translateY(-2px);

      &::after {
        opacity: 1;
      }

      .type-label {
        color: var(--el-text-color-primary);
        font-weight: 600;
      }
    }

    &.has-pending:not(.active) {
      border-color: var(--el-border-color);
    }

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: inherit;
      padding: 2px;
      background: var(--accent-gradient, linear-gradient(135deg, #6366f1, #8b5cf6));
      mask:
        linear-gradient(#fff 0 0) content-box,
        linear-gradient(#fff 0 0);
      mask-composite: exclude;
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.22s;
    }
  }

  .type-icon-wrap {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    background: var(--accent-gradient, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: #fff;
    box-shadow: 0 4px 12px var(--accent-shadow, rgba(99, 102, 241, 0.35));
  }

  .type-icon {
    font-size: 18px;
  }

  .type-body {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    flex: 1;
  }

  .type-label {
    font-size: 14px;
    font-weight: 500;
    color: var(--el-text-color-regular);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .type-count {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .type-dot {
    position: absolute;
    top: 10px;
    right: 10px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--accent-gradient, #6366f1);
    box-shadow: 0 0 0 3px var(--el-bg-color);
  }

  .accent-user {
    --accent-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    --accent-shadow: rgba(102, 126, 234, 0.35);
  }

  .accent-role {
    --accent-gradient: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    --accent-shadow: rgba(240, 147, 251, 0.35);
  }

  .accent-menu {
    --accent-gradient: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    --accent-shadow: rgba(79, 172, 254, 0.35);
  }

  .accent-dept {
    --accent-gradient: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
    --accent-shadow: rgba(67, 233, 123, 0.35);
  }

  .accent-post {
    --accent-gradient: linear-gradient(135deg, #0ea5e9 0%, #06b6d4 100%);
    --accent-shadow: rgba(14, 165, 233, 0.35);
  }

  .accent-ticket {
    --accent-gradient: linear-gradient(135deg, #f7971e 0%, #ffd200 100%);
    --accent-shadow: rgba(247, 151, 30, 0.35);
  }

  .accent-approval {
    --accent-gradient: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%);
    --accent-shadow: rgba(161, 140, 209, 0.35);
  }

  .accent-dict {
    --accent-gradient: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
    --accent-shadow: rgba(99, 102, 241, 0.35);
  }

  .accent-dictData {
    --accent-gradient: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
    --accent-shadow: rgba(139, 92, 246, 0.35);
  }

  .accent-announce {
    --accent-gradient: linear-gradient(135deg, #ec4899 0%, #f472b6 100%);
    --accent-shadow: rgba(236, 72, 153, 0.35);
  }

  .accent-job {
    --accent-gradient: linear-gradient(135deg, #14b8a6 0%, #2dd4bf 100%);
    --accent-shadow: rgba(20, 184, 166, 0.35);
  }

  .accent-file {
    --accent-gradient: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    --accent-shadow: rgba(99, 102, 241, 0.35);
  }

  .table-card {
    border-radius: 14px;
    border: 1px solid var(--el-border-color-lighter);
    overflow: hidden;

    :deep(.el-card__header) {
      padding: 16px 20px;
      background: linear-gradient(180deg, var(--el-fill-color-lighter) 0%, var(--el-bg-color) 100%);
      border-bottom: 1px solid var(--el-border-color-lighter);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }
  }

  .table-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
    font-size: 15px;
  }

  .table-header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .table-header-icon {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 16px;
    background: var(--accent-gradient, linear-gradient(135deg, #6366f1, #8b5cf6));
  }

  .refresh-btn {
    padding: 0;
    height: auto;
    vertical-align: middle;
  }

  .refresh-label-wrap {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    line-height: 1;
  }

  .refresh-icon {
    font-size: 14px;
    transform-origin: center center;
  }

  .refresh-icon--spinning {
    animation: recycle-refresh-spin 0.75s linear infinite;
  }

  @keyframes recycle-refresh-spin {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }

  .type-hint {
    margin-bottom: 12px;
  }

  .search-form {
    margin-bottom: 12px;
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }

  .action-buttons {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
  }
}
</style>
