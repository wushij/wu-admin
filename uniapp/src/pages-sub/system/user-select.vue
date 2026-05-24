<template>
  <view class="page-padded user-select-page has-page-footer">
    <SearchBar v-model="keyword" placeholder="搜索用户名 / 昵称 / 手机号" />

    <view class="user-select-page__body">
      <ListLoading v-if="loading && !users.length" />

      <scroll-view scroll-y class="user-select-page__scroll">
        <view
          v-if="allowEmpty"
          class="user-row"
          :class="{ 'user-row--selected': isSelected(null) }"
          @tap.stop="selectId(null)"
        >
          <view class="user-row__check" :class="{ 'user-row__check--checked': isSelected(null) }">
            <text v-if="isSelected(null)">✓</text>
          </view>
          <text class="user-row__name">{{ emptyLabel }}</text>
        </view>

        <view
          v-for="user in filteredUsers"
          :key="user.id"
          class="user-row"
          :class="{ 'user-row--selected': isSelected(user.id) }"
          @tap.stop="selectId(user.id)"
        >
          <view class="user-row__check" :class="{ 'user-row__check--checked': isSelected(user.id) }">
            <text v-if="isSelected(user.id)">✓</text>
          </view>
          <view class="user-row__main">
            <text class="user-row__name">{{ userLabel(user) }}</text>
            <text v-if="user.mobile" class="user-row__sub">{{ user.mobile }}</text>
          </view>
        </view>

        <EmptyState v-if="!loading && !filteredUsers.length" title="暂无匹配用户" icon="friends-o" />
      </scroll-view>
    </view>

    <PageFooter>
      <button class="page-footer__btn" :disabled="confirming" @tap.stop="confirm">确定</button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import SearchBar from '@/components/common/SearchBar/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { useUserSelectPage } from '@/composables/useUserSelectPage'
import { setPagePickerResult } from '@/utils/page-picker-result'
import { commitLeaderPick } from '@/utils/leader-pick'

type PageOpts = Record<string, string | undefined>
type OpenerEventChannel = {
  emit: (event: string, data: unknown) => void
}

const confirming = ref(false)
const pickToken = ref('')
let openerChannel: OpenerEventChannel | null = null

const {
  loading,
  keyword,
  users,
  filteredUsers,
  selectedId,
  selectedIds,
  multiMode,
  allowEmpty,
  emptyLabel,
  initFromRoute,
  ensureUsers,
  selectId,
  isSelected,
  userLabel,
  resolveSelectedLabel,
  resolveMultiLabelText,
} = useUserSelectPage()

function getPageOptions(): PageOpts {
  const page = getCurrentPages().slice(-1)[0] as { options?: PageOpts } | undefined
  return page?.options || {}
}

function getLivePickToken(): string {
  return String(getPageOptions().pick || pickToken.value || '')
}

function refreshOpenerChannel() {
  const page = getCurrentPages().slice(-1)[0] as {
    getOpenerEventChannel?: () => OpenerEventChannel
  } | undefined
  openerChannel = page?.getOpenerEventChannel?.() ?? openerChannel
}

function bootstrap(options: PageOpts) {
  const session = String(options.pick || options.session || (options.multi === '1' ? 'multi' : ''))
  if (!session || confirming.value) return
  if (session !== pickToken.value) pickToken.value = session
  initFromRoute(options, session)
}

function confirm() {
  if (confirming.value) return
  confirming.value = true

  if (multiMode.value) {
    const ids = [...selectedIds.value]
    const labelText = resolveMultiLabelText(ids)
    setPagePickerResult({ kind: 'user-multi', ids, labelText })
    setTimeout(() => {
      uni.navigateBack({
        fail: () => {
          confirming.value = false
          uni.showToast({ title: '返回失败，请重试', icon: 'none' })
        },
        complete: () => {
          confirming.value = false
        },
      })
    }, 32)
    return
  }

  const pickedId = selectedId.value
  const label = resolveSelectedLabel(pickedId)
  const token = getLivePickToken()
  const payload = {
    kind: 'user-leader',
    id: pickedId,
    label,
  }

  if (token) {
    commitLeaderPick(token, { id: pickedId, label })
  }
  setPagePickerResult(payload)
  openerChannel?.emit('pickUserLeader', payload)

  // 略延迟返回，确保 storage / eventChannel 写入完成
  setTimeout(() => {
    uni.navigateBack({
      fail: () => {
        confirming.value = false
        uni.showToast({ title: '返回失败，请重试', icon: 'none' })
      },
      complete: () => {
        confirming.value = false
      },
    })
  }, 32)
}

onLoad((options) => {
  refreshOpenerChannel()
  pickToken.value = String(options?.pick || '')

  const title = options?.title ? decodeURIComponent(String(options.title)) : '选择负责人'
  uni.setNavigationBarTitle({ title })
  allowEmpty.value = options?.allowEmpty !== '0'
  emptyLabel.value = options?.emptyLabel ? decodeURIComponent(String(options.emptyLabel)) : '不设置'
  bootstrap(options as PageOpts)
  ensureUsers()
})

onShow(() => {
  if (confirming.value) return
  refreshOpenerChannel()
  bootstrap(getPageOptions())
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.user-select-page {
  height: 100vh;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.user-select-page__body {
  position: relative;
  height: calc(100% - 120rpx);
}

.user-select-page__scroll {
  height: 100%;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 8rpx;
  border-bottom: 1px solid $color-border-light;
}

.user-row--selected {
  background: rgba(79, 70, 229, 0.06);
}

.user-row__check {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  border: 2rpx solid $color-border;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24rpx;
  flex-shrink: 0;
}

.user-row__check--checked {
  background: #4f46e5;
  border-color: #4f46e5;
}

.user-row__main {
  flex: 1;
  min-width: 0;
}

.user-row__name {
  display: block;
  font-size: $font-size-base;
  color: $color-text-primary;
}

.user-row__sub {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}
</style>
