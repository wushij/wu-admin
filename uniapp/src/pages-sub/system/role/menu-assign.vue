<template>
  <view class="page-padded perm-page has-page-footer">
    <ListLoading v-if="loading" />
    <template v-else>
      <view class="perm-role-card card--elevated">
        <text class="perm-role-card__label">角色名称</text>
        <text class="perm-role-card__name">{{ roleName || '—' }}</text>
      </view>

      <view class="perm-panel card--elevated">
        <view class="perm-panel__head">
          <view class="perm-panel__head-main">
            <text class="perm-panel__title">菜单权限</text>
            <text class="perm-panel__hint">点左侧按钮展开，点勾选框选择权限</text>
          </view>
          <text class="perm-panel__count">已选 {{ checkedCount }} 项</text>
        </view>
        <scroll-view scroll-y class="perm-panel__scroll">
          <view
            v-for="row in visibleRows"
            :key="row.id"
            class="perm-node"
            :class="{ 'perm-node--child': row.level > 0 }"
          >
            <view class="perm-node__indent" :style="{ width: `${row.level * 36}rpx` }" />
            <view class="perm-node__expand-slot">
              <view
                v-if="row.hasChildren"
                class="perm-node__expand-btn"
                :class="{ 'perm-node__expand-btn--open': isExpanded(row.id) }"
                @tap.stop="toggleExpand(row.id)"
              >
                <IconFont name="arrow" :size="30" color="#64748b" />
              </view>
            </view>
            <view
              class="perm-node__check"
              :class="{
                'perm-node__check--checked': isChecked(row.id) && !isIndeterminate(row.id),
                'perm-node__check--half': isIndeterminate(row.id),
              }"
              @tap.stop="toggleCheck(row.id)"
            >
              <text v-if="isChecked(row.id) || isIndeterminate(row.id)" class="perm-node__check-mark">
                {{ isIndeterminate(row.id) ? '−' : '✓' }}
              </text>
            </view>
            <view class="perm-node__body">
              <text class="perm-node__name">{{ row.name }}</text>
              <text v-if="typeLabel(row.type)" class="perm-node__type">{{ typeLabel(row.type) }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </template>

    <PageFooter>
      <button class="page-footer__btn" :loading="saving" @click="save">确定</button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import ListLoading from '@/components/common/ListLoading/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import { useRoleMenuAssign } from '@/composables/useRoleMenuAssign'
import { useUnsavedLeaveGuard } from '@/composables/useUnsavedLeaveGuard'

const {
  loading,
  saving,
  roleName,
  visibleRows,
  checkedCount,
  isDirty,
  load,
  isExpanded,
  toggleExpand,
  isChecked,
  isIndeterminate,
  toggleCheck,
  save,
} = useRoleMenuAssign()

useUnsavedLeaveGuard(isDirty)

function typeLabel(type?: number) {
  if (type === 2) return '菜单'
  if (type === 3) return '按钮'
  if (type === 1) return '目录'
  return ''
}

onLoad((options) => {
  const id = Number(options?.id)
  const name = options?.name ? decodeURIComponent(String(options.name)) : ''
  if (id) load(id, name)
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.perm-page {
  height: 100vh;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.perm-role-card {
  padding: 28rpx 32rpx;
  margin-bottom: $card-gap;
}

.perm-role-card__label {
  display: block;
  margin-bottom: 10rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.perm-role-card__name {
  display: block;
  font-size: $font-size-lg;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.perm-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.perm-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
  padding: 24rpx 28rpx;
  border-bottom: 1px solid $color-border-light;
}

.perm-panel__head-main {
  flex: 1;
  min-width: 0;
}

.perm-panel__title {
  display: block;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.perm-panel__hint {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.perm-panel__count {
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.perm-panel__scroll {
  height: calc(100vh - 380rpx);
}

.perm-node {
  display: flex;
  align-items: center;
  gap: 8rpx;
  min-height: 88rpx;
  padding: 12rpx 24rpx 12rpx 8rpx;
  border-bottom: 1px solid $color-border-light;

  &:last-child {
    border-bottom: none;
  }
}

.perm-node__indent {
  flex-shrink: 0;
}

.perm-node__expand-slot {
  width: 72rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.perm-node__expand-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  background: $color-bg-muted;
  transition: transform 0.2s ease, background 0.15s ease;

  &--open {
    transform: rotate(90deg);
    background: rgba(99, 102, 241, 0.12);
  }

  &:active {
    background: rgba(99, 102, 241, 0.18);
  }
}

.perm-node__check {
  width: 36rpx;
  height: 36rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8rpx;
  border: 2rpx solid $color-border;
  background: $color-bg-card;

  &--checked,
  &--half {
    border-color: $color-primary;
    background: $color-primary;
  }
}

.perm-node__check-mark {
  font-size: 22rpx;
  color: #fff;
  line-height: 1;
}

.perm-node__body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.perm-node__name {
  font-size: $font-size-sm;
  color: $color-text-primary;
}

.perm-node__type {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  border-radius: $radius-full;
  font-size: 20rpx;
  color: $color-text-secondary;
  background: $color-bg-muted;
}
</style>
