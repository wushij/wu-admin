<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="aimodel-page">
    <ModuleDarkHero
      title="AI 模型配置"
      :subtitle="`大模型供应商接入维护 · 共 ${total} 个`"
      icon="setting-o"
      theme="ai"
    >
      <template #extra>
        <view class="module-dark-hero__chip" @click.stop="goLog">
          <IconFont name="records-o" :size="24" color="rgba(255,255,255,0.9)" />
          <text>对话日志</text>
        </view>
      </template>
    </ModuleDarkHero>

    <SegmentTabs v-model="statusMode" :tabs="statusTabs" />
    <SearchBar v-model="keyword" placeholder="搜索配置名称" @search="onSearch" />

    <ListLoading v-if="loading && !list.length" />

    <scroll-view
      v-else
      scroll-y
      class="aimodel-page__scroll"
      @scrolltolower="loadMore"
    >
      <view
        v-for="item in list"
        :key="item.id"
        class="aimodel-card card--elevated"
        :class="{ 'is-disabled': item.status === 0 }"
      >
        <view class="aimodel-card__head">
          <view class="aimodel-card__badge" :style="providerBadgeStyle(item.provider)">
            <text>{{ providerMeta(item.provider).short }}</text>
          </view>
          <view class="aimodel-card__title-wrap">
            <view class="aimodel-card__name-row">
              <text class="aimodel-card__title">{{ item.name }}</text>
              <view v-if="item.isDefault === 1" class="default-badge">
                <text>★ 默认</text>
              </view>
            </view>
            <text class="aimodel-card__model">{{ providerMeta(item.provider).label }} · {{ item.modelName }}</text>
          </view>
          <view class="status-tag" :class="item.status === 1 ? 'is-active' : 'is-inactive'">
            <text>{{ item.status === 1 ? '已启用' : '已停用' }}</text>
          </view>
        </view>

        <view class="aimodel-card__body">
          <view class="info-row">
            <text class="info-label">接口地址</text>
            <text class="info-val ellipsis">{{ item.baseUrl || '—' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">API Key</text>
            <view class="info-val">
              <text class="key-pill" :class="{ 'is-configured': item.hasApiKey }">
                {{ item.hasApiKey ? (item.apiKeyMasked || '已配置') : '未配置' }}
              </text>
            </view>
          </view>
          <view class="info-row">
            <text class="info-label">模型参数</text>
            <text class="info-val">温度 {{ item.temperature ?? 0.7 }} · 最大 {{ item.maxTokens ?? 4096 }} Tokens</text>
          </view>

          <view
            v-if="testResults[item.id] !== undefined"
            class="test-badge"
            :class="testResults[item.id] >= 0 ? 'is-ok' : 'is-fail'"
          >
            <text>{{ testResults[item.id] >= 0 ? `连接正常 · ${testResults[item.id]}ms` : '连接失败' }}</text>
          </view>
        </view>

        <view class="aimodel-card__foot">
          <view class="action-btn" @click.stop="handleTest(item)">
            <text>测试</text>
          </view>
          <view v-if="item.isDefault !== 1 && item.status === 1" class="action-btn action-btn--warn" @click.stop="handleDefault(item)">
            <text>设为默认</text>
          </view>
          <view class="action-btn action-btn--primary" @click.stop="goEdit(item.id)">
            <text>编辑</text>
          </view>
          <view v-if="item.isDefault !== 1" class="action-btn action-btn--danger" @click.stop="handleDelete(item)">
            <text>删除</text>
          </view>
        </view>
      </view>

      <EmptyState v-if="empty" title="暂无模型配置" icon="setting-o" />
      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
    </scroll-view>

    <FabButton v-if="canCreate" @click="goCreate" />
    <AiWuAssistant />
    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListFooter from '@/components/common/ListFooter/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import FabButton from '@/components/common/FabButton/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { usePageList } from '@/composables/usePageList'
import { useModulePermission } from '@/composables/useModulePermission'
import {
  pageAiModel,
  deleteAiModel,
  setDefaultAiModel,
  testAiModel,
  type AiModelVO,
} from '@/api/system/ai-model'
import { providerMeta } from '@/constants/aiProviders'
import { showConfirm } from '@/store/dialog'

const { allowed, hasPerm } = useModulePermission('system:ai-model:list')
const canCreate = computed(() => hasPerm('system:ai-model:create'))
const canUpdate = computed(() => hasPerm('system:ai-model:update'))
const canDelete = computed(() => hasPerm('system:ai-model:delete'))
const canTest = computed(() => hasPerm('system:ai-model:test'))

const keyword = ref('')
const total = ref(0)
const statusMode = ref('all')
const statusTabs = [
  { key: 'all', label: '全部' },
  { key: '1', label: '启用' },
  { key: '0', label: '停用' },
]
/** id -> 延迟ms（-1 表示失败） */
const testResults = reactive<Record<number, number>>({})

const { list, loading, refreshing, finished, empty, refresh, loadMore } = usePageList<AiModelVO>(
  async (pageNo, pageSize) => {
    const res = await pageAiModel({
      pageNo,
      pageSize,
      name: keyword.value.trim() || undefined,
      status: statusMode.value === 'all' ? undefined : Number(statusMode.value),
    })
    total.value = res.data?.total || 0
    return { list: res.data?.list || [], total: total.value }
  },
)

function providerBadgeStyle(provider: string) {
  const meta = providerMeta(provider)
  return { background: meta.bg, color: meta.color }
}

function onItemMenu(item: AiModelVO) {
  const actions: string[] = []
  if (canTest.value) actions.push('测试连接')
  if (canUpdate.value) actions.push('编辑')
  if (canUpdate.value && item.isDefault !== 1) actions.push('设为默认')
  if (canDelete.value) actions.push('删除')
  if (!actions.length) return
  uni.showActionSheet({
    itemList: actions,
    success: async (res) => {
      const action = actions[res.tapIndex]
      if (action === '测试连接') await handleTest(item)
      else if (action === '编辑') goEdit(item.id)
      else if (action === '设为默认') await handleDefault(item)
      else if (action === '删除') handleDelete(item)
    },
  })
}

async function handleTest(item: AiModelVO) {
  uni.showLoading({ title: '测试中…', mask: true })
  try {
    const res = await testAiModel({ id: item.id })
    testResults[item.id] = res.data ?? 0
    uni.hideLoading()
    uni.showToast({ title: `连接正常 ${res.data}ms`, icon: 'success' })
  } catch {
    testResults[item.id] = -1
    uni.hideLoading()
  }
}

async function handleDefault(item: AiModelVO) {
  await setDefaultAiModel(item.id)
  uni.showToast({ title: '已设为默认', icon: 'success' })
  await refresh({ silent: true })
}

async function handleDelete(item: AiModelVO) {
  const res = await showConfirm({
    title: '删除模型配置',
    content: `确定要删除模型配置「${item.name}」吗？此操作不可撤销！`,
    confirmText: '删除',
    cancelText: '取消',
    tone: 'danger',
  })
  if (!res.confirmed) return
  await deleteAiModel(item.id)
  uni.showToast({ title: '已删除', icon: 'success' })
  await refresh({ silent: true })
}

function goCreate() {
  uni.navigateTo({ url: '/pages-sub/ai/model/form?mode=create' })
}

function goEdit(id: number) {
  uni.navigateTo({ url: `/pages-sub/ai/model/form?id=${id}` })
}

function goLog() {
  uni.navigateTo({ url: '/pages-sub/ai/log/index' })
}

function onSearch() {
  refresh({ silent: true })
}

async function onRefresh() {
  await refresh()
}

watch(statusMode, () => refresh({ silent: true }))

const skipNextShowRefresh = ref(true)
onMounted(() => refresh())
onShow(async () => {
  if (skipNextShowRefresh.value) {
    skipNextShowRefresh.value = false
    return
  }
  if (loading.value || refreshing.value) return
  await refresh({ silent: true })
})
onPullDownRefresh(async () => {
  try {
    await refresh()
  } finally {
    uni.stopPullDownRefresh()
  }
})
</script>

<style lang="scss" scoped>
@use '@/styles/mine.scss' as *;

.aimodel-page {
  @include mine-page-bg;
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
  padding: $page-padding-y $page-padding-x 0;
}

.aimodel-page__scroll {
  flex: 1;
  min-height: 0;
  margin-top: 8rpx;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.aimodel-card {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  padding: 28rpx 24rpx;
  margin-bottom: $card-gap;
  border-radius: 24rpx;
  background: #ffffff;
  border: 1rpx solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 4rpx 20rpx rgba(15, 23, 42, 0.04);
  transition: opacity 0.2s;

  &.is-disabled {
    opacity: 0.65;
  }
}

.aimodel-card__head {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.aimodel-card__badge {
  flex-shrink: 0;
  width: 80rpx;
  height: 80rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.06);

  text {
    font-size: 28rpx;
    font-weight: 800;
  }
}

.aimodel-card__title-wrap {
  flex: 1;
  min-width: 0;
}

.aimodel-card__name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.aimodel-card__title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.default-badge {
  padding: 2rpx 12rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  box-shadow: 0 2rpx 6rpx rgba(245, 158, 11, 0.3);
  flex-shrink: 0;

  text {
    font-size: 20rpx;
    color: #ffffff;
    font-weight: 600;
  }
}

.aimodel-card__model {
  display: block;
  margin-top: 6rpx;
  font-size: 23rpx;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-tag {
  padding: 4rpx 14rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 600;
  flex-shrink: 0;

  &.is-active {
    background: rgba(99, 102, 241, 0.1);
    color: #6366f1;
  }

  &.is-inactive {
    background: #f1f5f9;
    color: #94a3b8;
  }
}

.aimodel-card__body {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  padding: 18rpx 20rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 1rpx solid rgba(226, 232, 240, 0.8);
}

.info-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  font-size: 23rpx;

  .info-label {
    flex-shrink: 0;
    width: 110rpx;
    color: #64748b;
    font-weight: 500;
  }

  .info-val {
    flex: 1;
    min-width: 0;
    color: #334155;

    &.ellipsis {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.key-pill {
  display: inline-block;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  font-family: monospace;
  background: #e2e8f0;
  color: #64748b;

  &.is-configured {
    background: rgba(16, 185, 129, 0.1);
    color: #059669;
    font-weight: 600;
  }
}

.test-badge {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  font-weight: 500;
  margin-top: 4rpx;
  width: fit-content;

  &.is-ok {
    background: #f0fdf4;
    color: #16a34a;
    border: 1rpx solid #bbf7d0;
  }

  &.is-fail {
    background: #fef2f2;
    color: #dc2626;
    border: 1rpx solid #fecaca;
  }
}

.aimodel-card__foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14rpx;
  border-top: 1rpx dashed rgba(226, 232, 240, 0.8);
  padding-top: 18rpx;

  .action-btn {
    padding: 10rpx 20rpx;
    border-radius: 12rpx;
    background: #f1f5f9;
    color: #475569;

    text {
      font-size: 23rpx;
      font-weight: 500;
    }

    &--primary {
      background: rgba(99, 102, 241, 0.1);
      color: #6366f1;
    }

    &--warn {
      background: rgba(245, 158, 11, 0.1);
      color: #d97706;
    }

    &--danger {
      background: rgba(239, 68, 68, 0.1);
      color: #dc2626;
    }
  }
}
</style>
