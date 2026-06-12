<template>
  <view class="page-padded tree-select-page has-page-footer">
    <ListLoading v-if="loading" />
    <template v-else>
      <view class="tree-select-panel card--elevated">
        <view class="tree-select-panel__head">
          <view class="tree-select-panel__head-main">
            <text class="tree-select-panel__title">{{ panelTitle }}</text>
            <text class="tree-select-panel__hint">{{ panelHint }}</text>
          </view>
          <text
            v-if="multiple || selectedId != null || allowEmpty"
            class="tree-select-panel__picked"
          >
            {{ selectedLabel }}
          </text>
        </view>

        <scroll-view scroll-y class="tree-select-panel__scroll">
          <view
            v-if="allowEmpty && !multiple"
            class="tree-node"
            :class="{ 'tree-node--selected': isSelected(null) }"
          >
            <view class="tree-node__expand-slot" />
            <view class="tree-node__main" @tap="selectId(null)">
              <view
                class="tree-node__check"
                :class="{ 'tree-node__check--checked': isSelected(null) }"
              >
                <text v-if="isSelected(null)" class="tree-node__check-mark">✓</text>
              </view>
              <text class="tree-node__name">{{ emptyLabel }}</text>
            </view>
          </view>

          <view
            v-for="row in visibleRows"
            :key="row.id"
            class="tree-node"
            :class="{ 'tree-node--selected': isSelected(row.id) }"
          >
            <view class="tree-node__indent" :style="{ width: `${row.level * 36}rpx` }" />
            <view class="tree-node__expand-slot">
              <view
                v-if="row.hasChildren"
                class="tree-node__expand-btn"
                :class="{ 'tree-node__expand-btn--open': isExpanded(row.id) }"
                @tap.stop="toggleExpand(row.id)"
              >
                <IconFont name="arrow" :size="30" color="#64748b" />
              </view>
            </view>
            <view class="tree-node__main" @tap="onRowTap(row.id)">
              <view
                class="tree-node__check"
                :class="{
                  'tree-node__check--checked': isSelected(row.id),
                  'tree-node__check--square': multiple,
                }"
              >
                <text v-if="isSelected(row.id)" class="tree-node__check-mark">✓</text>
              </view>
              <view class="tree-node__text">
                <text class="tree-node__name">{{ row.name }}</text>
                <text v-if="row.extra" class="tree-node__sub">{{ row.extra }}</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </template>

    <PageFooter>
      <button class="page-footer__btn" @tap="confirm">确定</button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import ListLoading from '@/components/common/ListLoading/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import IconFont from '@/components/common/IconFont/index.vue'
import { useTreeSelectPage } from '@/composables/useTreeSelectPage'
import { setPagePickerResult } from '@/utils/page-picker-result'
import { getTreeSelectPostIds, getTreeSelectDeptIds } from '@/utils/tree-select-init'
import { getFromQueryParent } from '@/utils/nav-from'
import { registerPageShallowFallback, installH5ShallowStackTrapIfNeeded } from '@/utils/navigate-back'
import { pinNavParent } from '@/utils/nav-history'
import { scheduleSyncH5BackButton } from '@/store/h5-back-button'

const USER_LIST_URL = '/pages-sub/system/user/index'

const panelTitle = ref('选择部门')
const pickKind = ref('dept')

const {
  loading,
  multiple,
  allowEmpty,
  selectedId,
  selectedIds,
  selectedLabel,
  visibleRows,
  loadDept,
  loadDeptMulti,
  loadPostParent,
  loadPost,
  commitSelection,
  isExpanded,
  toggleExpand,
  selectId,
  onRowTap,
  isSelected,
  emptyLabel,
} = useTreeSelectPage()

const panelHint = computed(() => {
  if (multiple.value && pickKind.value === 'dept-multi') {
    return '点左侧按钮展开，点名称多选部门'
  }
  if (multiple.value) return '点左侧按钮展开，点名称选择岗位'
  return '点左侧按钮展开，点名称选择部门'
})

function confirm() {
  commitSelection()
  if (multiple.value && pickKind.value === 'dept-multi') {
    setPagePickerResult({
      kind: 'dept-multi',
      ids: [...selectedIds.value],
      labelText: selectedLabel.value,
    })
  } else if (multiple.value) {
    setPagePickerResult({
      kind: 'post',
      ids: [...selectedIds.value],
      labelText: selectedLabel.value,
    })
  } else if (pickKind.value === 'post-parent') {
    setPagePickerResult({
      kind: 'post-parent',
      id: selectedId.value,
      label: selectedLabel.value,
    })
  } else {
    setPagePickerResult({
      kind: pickKind.value,
      id: selectedId.value,
      label: selectedLabel.value,
    })
  }
  uni.navigateBack()
}

onShow(() => {
  const from = getFromQueryParent()
  const parent = from || USER_LIST_URL
  registerPageShallowFallback(parent)
  pinNavParent(parent)
  installH5ShallowStackTrapIfNeeded()
  scheduleSyncH5BackButton()
})

onLoad((options) => {
  pickKind.value = String(options?.type || 'dept')
  panelTitle.value = decodeURIComponent(String(options?.title || (pickKind.value === 'post' ? '选择岗位' : '选择部门')))
  uni.setNavigationBarTitle({ title: panelTitle.value })

  if (pickKind.value === 'post') {
    const initial = getTreeSelectPostIds()
    loadPost(initial)
    return
  }

  if (pickKind.value === 'dept-multi') {
    const initial = getTreeSelectDeptIds()
    loadDeptMulti(initial)
    return
  }

  if (pickKind.value === 'post-parent') {
    const rawSelected = options?.selectedId
    const selected =
      rawSelected != null && String(rawSelected) !== '' && Number.isFinite(Number(rawSelected)) && Number(rawSelected) > 0
        ? Number(rawSelected)
        : null
    const excludeId = Number(options?.excludeId) || undefined
    const emptyAllowed = options?.allowEmpty === '1' || options?.allowEmpty === 'true'
    const emptyLabel = options?.emptyLabel ? decodeURIComponent(String(options.emptyLabel)) : '顶级岗位'
    loadPostParent(selected, emptyAllowed, { excludeId, emptyLabel })
    return
  }

  const rawSelected = options?.selectedId
  const selected =
    rawSelected != null && String(rawSelected) !== '' && Number.isFinite(Number(rawSelected)) && Number(rawSelected) > 0
      ? Number(rawSelected)
      : null
  const emptyAllowed = options?.allowEmpty === '1' || options?.allowEmpty === 'true'
  const excludeId = Number(options?.excludeId) || undefined
  const emptyLabel = options?.emptyLabel ? decodeURIComponent(String(options.emptyLabel)) : '顶级部门'
  loadDept(selected, emptyAllowed, { excludeId, emptyLabel })
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/common.scss';

.tree-select-page {
  height: 100vh;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.tree-select-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tree-select-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
  padding: 24rpx 28rpx;
  border-bottom: 1px solid $color-border-light;
}

.tree-select-panel__head-main {
  flex: 1;
  min-width: 0;
}

.tree-select-panel__title {
  display: block;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.tree-select-panel__hint {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.tree-select-panel__picked {
  flex-shrink: 0;
  max-width: 280rpx;
  font-size: $font-size-xs;
  color: $color-primary;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-select-panel__scroll {
  height: calc(100vh - 320rpx);
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 12rpx;
  min-height: 96rpx;
  padding: 10rpx 20rpx 10rpx 8rpx;
  border-bottom: 1px solid $color-border-light;

  &--selected {
    background: rgba(99, 102, 241, 0.06);
  }

  &:last-child {
    border-bottom: none;
  }
}

.tree-node__indent {
  flex-shrink: 0;
}

.tree-node__expand-slot {
  width: 72rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tree-node__expand-btn {
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

.tree-node__main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 8rpx 0;
}

.tree-node__text {
  flex: 1;
  min-width: 0;
}

.tree-node__check {
  width: 40rpx;
  height: 40rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 2rpx solid $color-border;
  background: $color-bg-card;

  &--square {
    border-radius: 10rpx;
  }

  &--checked {
    border-color: $color-primary;
    background: $color-primary;
  }
}

.tree-node__check-mark {
  font-size: 24rpx;
  color: #fff;
  line-height: 1;
}

.tree-node__name {
  display: block;
  font-size: $font-size-base;
  color: $color-text-primary;
  line-height: 1.45;
}

.tree-node__sub {
  display: block;
  margin-top: 4rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}
</style>
