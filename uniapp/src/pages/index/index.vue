<template>
  <PageTabShell>
    <WelcomeHero
      :nickname="nickname"
      :avatar="heroAvatar"
      :stats="heroStats"
    />

    <ListLoading v-if="loading" variant="dashboard" />

    <FadeIn v-else :show="true">
      <view v-if="visibleCards.length" class="section-block">
        <text class="section-head">核心指标</text>
        <view class="stats-grid">
          <DataCard
            v-for="card in visibleCards"
            :key="card.key"
            :title="card.title"
            :value="statValue(stats, card.valueKey)"
            :icon="card.icon"
            :theme="card.theme"
            :footer="card.footer?.(stats)"
            @click="onCardTap(card)"
          />
        </view>
      </view>

      <view v-else class="empty-hint card--elevated">
        <EmptyState title="暂无可见统计" description="当前账号无 Dashboard 数据权限" icon="info-o" />
      </view>

      <view v-if="hasTodoSection" class="section-block">
        <text class="section-head">待办提醒</text>
        <view class="todo-panel card--elevated">
          <ApprovalPendingCard
            v-if="showApprovalTodo"
            :count="stats.approvalPendingCount"
            @click="goApproval"
          />
          <DashboardTodoCard
            v-if="showTicketTodo"
            :open-count="stats.ticketOpenCount"
            :overdue-count="stats.ticketOverdueCount"
            @click="goTicket"
          />
        </view>
      </view>

      <RecentLoginList
        v-if="recentLogins.length"
        :rows="recentLogins"
      />
    </FadeIn>
  </PageTabShell>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import PageTabShell from '@/components/common/PageTabShell/index.vue'
import WelcomeHero from '@/components/business/WelcomeHero/index.vue'
import DashboardTodoCard from '@/components/business/DashboardTodoCard/index.vue'
import ApprovalPendingCard from '@/components/business/ApprovalPendingCard/index.vue'
import RecentLoginList from '@/components/business/RecentLoginList/index.vue'
import DataCard from '@/components/common/DataCard/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FadeIn from '@/components/common/FadeIn/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { fileDisplayUrl } from '@/api/system/file/index'
import { useUserStore } from '@/store/user'
import { useDashboard } from '@/composables/useDashboard'
import { usePermission } from '@/composables/usePermission'
import { useTabBarPage } from '@/composables/useTabBarPage'
import { mobileStatCards, statValue, type StatCardConfig } from '@/constants/statCards'

useTabBarPage(0)

const userStore = useUserStore()
const { hasPerm } = usePermission()
const { loading, stats, recentLogins, refresh } = useDashboard()

const nickname = computed(() => userStore.userInfo.nickname || userStore.userInfo.username || '用户')
const heroAvatar = computed(() =>
  userStore.userInfo.avatar ? fileDisplayUrl(userStore.userInfo.avatar) : '',
)

const heroStats = computed(() => [
  { label: '在线', value: stats.value.onlineCount ?? 0 },
  { label: '今日访问', value: stats.value.todayVisits ?? 0 },
  { label: '今日登录', value: stats.value.todayLoginSuccess ?? 0 },
])

const showApprovalTodo = computed(
  () => hasPerm('system:approval:list') && (stats.value.approvalPendingCount ?? 0) > 0,
)

const showTicketTodo = computed(
  () => hasPerm('system:ticket:list') && (stats.value.ticketOpenCount ?? 0) > 0,
)

const hasTodoSection = computed(() => showApprovalTodo.value || showTicketTodo.value)

const visibleCards = computed(() =>
  mobileStatCards.filter((card) => !card.permission || hasPerm(card.permission)),
)

function onCardTap(card: StatCardConfig) {
  if (!card.path) return
  if (card.permission && !hasPerm(card.permission)) {
    uni.showToast({ title: '暂无权限', icon: 'none' })
    return
  }
  uni.navigateTo({ url: card.path })
}

function goTicket() {
  if (!hasPerm('system:ticket:list')) {
    uni.showToast({ title: '暂无工单权限', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages-sub/system/ticket/index' })
}

function goApproval() {
  uni.navigateTo({ url: '/pages-sub/system/approval/index' })
}

onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo.permissions?.length) {
    await userStore.getUserInfo().catch(() => {})
  }
  refresh()
})

onShow(() => {
  if (userStore.isLoggedIn) {
    userStore.getUserInfo().catch(() => {})
    refresh()
  }
})

onPullDownRefresh(async () => {
  await refresh()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>

.section-head {
  display: block;
  margin-bottom: 16rpx;
  padding: 0 4rpx;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
}

.empty-hint {
  margin-bottom: $section-gap;
}

.todo-panel {
  padding: 8rpx 24rpx 12rpx;
  overflow: hidden;
}
</style>
