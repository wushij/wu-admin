<template>

  <view class="profile-card card--elevated">

    <view class="profile-card__main">

      <view class="profile-card__avatar">

        <image

          v-if="avatarSrc && !avatarBroken"

          class="profile-card__img"

          :src="avatarSrc"

          mode="aspectFill"

          @error="avatarBroken = true"

        />

        <view v-else class="profile-card__fallback">

          <text>{{ avatarFallback }}</text>

        </view>

      </view>

      <view class="profile-card__info">

        <text class="profile-card__name">{{ nickname }}</text>

        <view v-if="primaryRole" class="profile-card__role">

          <text>{{ primaryRole }}</text>

        </view>

        <text v-if="orgLine" class="profile-card__org">{{ orgLine }}</text>

      </view>

    </view>

  </view>

</template>



<script setup lang="ts">

import { computed, ref, watch } from 'vue'

import { fileDisplayUrl } from '@/api/system/file/index'



const props = withDefaults(

  defineProps<{

    nickname: string

    avatar?: string

    roles?: string[]

    deptName?: string

    postNames?: string

  }>(),

  { roles: () => [] },

)



const avatarBroken = ref(false)



const avatarSrc = computed(() => (props.avatar ? fileDisplayUrl(props.avatar) : ''))

const avatarFallback = computed(() => (props.nickname || 'U').slice(0, 1).toUpperCase())

const primaryRole = computed(() => props.roles[0] || '')

const orgLine = computed(() => {

  const parts = [props.deptName, props.postNames].filter(Boolean)

  return parts.length ? parts.join(' · ') : ''

})



watch(

  () => props.avatar,

  () => {

    avatarBroken.value = false

  },

)

</script>



<style lang="scss" scoped>

@import '@/styles/variables.scss';



.profile-card {

  margin-bottom: $section-gap;

  padding: 32rpx;

  overflow: hidden;

}



.profile-card__main {

  display: flex;

  align-items: center;

  gap: 28rpx;

}



.profile-card__avatar {

  flex-shrink: 0;

  width: 112rpx;

  height: 112rpx;

  border-radius: 28rpx;

  overflow: hidden;

  box-shadow: 0 8rpx 24rpx rgba(79, 70, 229, 0.18);

}



.profile-card__img,

.profile-card__fallback {

  width: 100%;

  height: 100%;

}



.profile-card__fallback {

  display: flex;

  align-items: center;

  justify-content: center;

  background: linear-gradient(135deg, #6366f1, #8b5cf6);

  font-size: 44rpx;

  font-weight: $font-weight-bold;

  color: #fff;

}



.profile-card__info {

  flex: 1;

  min-width: 0;

}



.profile-card__name {

  display: block;

  font-size: $font-size-xl;

  font-weight: $font-weight-bold;

  color: $color-text-primary;

  line-height: 1.3;

}



.profile-card__role {

  display: inline-flex;

  margin-top: 12rpx;

  padding: 4rpx 16rpx;

  border-radius: $radius-full;

  background: $color-primary-muted;



  text {

    font-size: $font-size-xs;

    font-weight: $font-weight-semibold;

    color: $color-primary;

  }

}



.profile-card__org {

  display: block;

  margin-top: 12rpx;

  font-size: $font-size-sm;

  color: $color-text-secondary;

  line-height: 1.45;

}

</style>

