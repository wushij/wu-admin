<template>
  <view class="mine-hero card--elevated" @click="emit('click')">
    <view class="mine-hero__profile">
      <view class="mine-hero__avatar-wrap">
        <view
          class="mine-hero__avatar"
          :class="{ 'mine-hero__avatar--photo': showPhoto }"
        >
          <image
            v-if="showPhoto"
            class="mine-hero__avatar-img"
            :src="avatarSrc"
            mode="aspectFill"
            @error="avatarBroken = true"
          />
          <text v-else class="mine-hero__fallback-text">{{ avatarFallback }}</text>
        </view>
      </view>

      <view class="mine-hero__info">
        <view class="mine-hero__top">
          <text class="mine-hero__name">{{ nickname }}</text>
          <view v-if="roleTags.length" class="mine-hero__roles">
            <text v-for="role in roleTags" :key="role" class="mine-hero__role">{{ role }}</text>
          </view>
        </view>
        <text v-if="orgLine" class="mine-hero__org">{{ orgLine }}</text>
      </view>

      <view class="mine-hero__edit">
        <IconFont name="edit" :size="32" color="#6366f1" />
      </view>
    </view>

    <view v-if="stats.length" class="mine-hero__stats">
      <view v-for="item in stats" :key="item.label" class="mine-hero__stat">
        <text class="mine-hero__stat-value">{{ item.value }}</text>
        <text class="mine-hero__stat-label">{{ item.label }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import { fileDisplayUrl } from '@/api/system/file/index'
import { formatListTime, formatUserStatus } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    nickname: string
    avatar?: string
    roles?: string[]
    deptName?: string
    postNames?: string
    lastLoginTime?: string
    lastLoginIp?: string
    status?: number
  }>(),
  { roles: () => [] },
)

const emit = defineEmits<{ click: [] }>()

const avatarBroken = ref(false)

const avatarSrc = computed(() => (props.avatar ? fileDisplayUrl(props.avatar) : ''))
const showPhoto = computed(() => !!avatarSrc.value && !avatarBroken.value)
const avatarFallback = computed(() => (props.nickname || 'U').slice(0, 1).toUpperCase())
const roleTags = computed(() => props.roles.slice(0, 2))
const orgLine = computed(() => {
  const parts = [props.deptName, props.postNames].filter(Boolean)
  return parts.length ? parts.join(' · ') : ''
})

const stats = computed(() => [
  {
    label: '最近登录',
    value: props.lastLoginTime ? formatListTime(props.lastLoginTime) : '—',
  },
  {
    label: '登录 IP',
    value: props.lastLoginIp || '—',
  },
  {
    label: '账号状态',
    value: formatUserStatus(props.status),
  },
])

watch(
  () => props.avatar,
  () => {
    avatarBroken.value = false
  },
)
</script>

<style lang="scss" scoped>

.mine-hero {
  margin-bottom: $section-gap;
  padding: 32rpx 32rpx 24rpx;
}

.mine-hero__profile {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding-left: 12rpx;
}

.mine-hero__avatar-wrap {
  flex-shrink: 0;
}

.mine-hero__info {
  flex: 1;
  min-width: 0;
}

.mine-hero__top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  min-width: 0;
}

.mine-hero__avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(79, 70, 229, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}

.mine-hero__avatar-img {
  width: 100%;
  height: 100%;
}

.mine-hero__avatar--photo {
  background-color: #f3f4f6;
  background-image: none;
}

.mine-hero__fallback-text {
  font-size: 44rpx;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1;
}

.mine-hero__name {
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.35;
}

.mine-hero__roles {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}

.mine-hero__role {
  padding: 4rpx 16rpx;
  border-radius: $radius-full;
  background: $color-primary-muted;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-primary;
}

.mine-hero__org {
  display: block;
  margin-top: 10rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.45;
}

.mine-hero__edit {
  flex-shrink: 0;
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  background: rgba(99, 102, 241, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.mine-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 1px solid $color-border-light;
}

.mine-hero__stat {
  min-width: 0;
  text-align: center;
}

.mine-hero__stat-value {
  display: block;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  word-break: break-all;
  line-height: 1.4;
}

.mine-hero__stat-label {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: $color-text-placeholder;
}
</style>
