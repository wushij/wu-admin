<template>
  <view
    class="chat-avatar-shell"
    :class="{ 'chat-avatar-shell--round': round, 'chat-avatar-shell--square': !round }"
    :style="shellStyle"
  >
    <view
      class="chat-avatar"
      :class="{
        'chat-avatar--round': round,
        'chat-avatar--square': !round,
      }"
      :style="avatarStyle"
    >
      <image
        v-if="displaySrc && !imgBroken"
        class="chat-avatar__img"
        :src="displaySrc"
        mode="aspectFill"
        @error="onImgError"
      />
      <text v-else class="chat-avatar__text">{{ fallback }}</text>
    </view>
    <view v-if="online" class="chat-avatar__dot" />
  </view>
</template>



<script setup lang="ts">

import { computed, ref, watch } from 'vue'

import { fileDisplayUrl } from '@/api/system/file/index'

import { avatarFallback as buildAvatarFallback } from '@/utils/chat-avatar'
import { groupAvatarColor } from '@/utils/group-avatar'



const props = withDefaults(

  defineProps<{

    src?: string

    name?: string

    online?: boolean

    round?: boolean

    /** 群聊名称，用于无头像时的配色（对齐 PC groupAvatarStyle） */

    groupName?: string

  }>(),

  { round: true },

)



const imgBroken = ref(false)



const displaySrc = computed(() => (props.src ? fileDisplayUrl(props.src) : ''))

const fallback = computed(() => buildAvatarFallback(props.name || props.groupName))



const avatarStyle = computed(() => {

  if (displaySrc.value && !imgBroken.value) return {}

  if (props.groupName) {

    return { background: groupAvatarColor(props.groupName) }

  }

  return {}

})

const shellStyle = computed(() => {
  const style: Record<string, string> = {}
  if (props.groupName && (!displaySrc.value || imgBroken.value)) {
    style.background = groupAvatarColor(props.groupName)
  } else if (!displaySrc.value || imgBroken.value) {
    style.background = 'linear-gradient(135deg, #6366f1, #818cf8)'
  }
  return style
})



watch(

  () => props.src,

  () => {

    imgBroken.value = false

  },

)



function onImgError() {

  imgBroken.value = true

}

</script>



<style lang="scss" scoped>

.chat-avatar-shell {
  position: relative;
  flex-shrink: 0;
  width: 88rpx;
  height: 88rpx;
}

.chat-avatar-shell--round {
  border-radius: 50%;
}

.chat-avatar-shell--square {
  border-radius: 16rpx;
}

.chat-avatar {

  width: 100%;

  height: 100%;

  display: flex;

  align-items: center;

  justify-content: center;

  overflow: hidden;

}



.chat-avatar--round {
  border-radius: 50%;
}

.chat-avatar--square {
  border-radius: 16rpx;
}



.chat-avatar__img {

  width: 100%;

  height: 100%;

}



.chat-avatar__text {

  color: #fff;

  font-size: 34rpx;

  font-weight: 600;

}



.chat-avatar__dot {
  position: absolute;
  right: 4rpx;
  bottom: 4rpx;
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #07c160;
  border: 2rpx solid #fff;
  z-index: 2;
}

</style>

