<template>

  <view class="announce-page page-padded">

    <view class="announce-head">

      <view class="announce-head__main">

        <text class="announce-head__title">系统公告</text>

        <text class="announce-head__hint">平台通知与重要公告</text>

      </view>

      <text v-if="list.length" class="announce-head__action" @click="onReadAll">全部已读</text>

    </view>



    <ListLoading v-if="loading && !list.length" variant="message" />



    <scroll-view

      v-else

      scroll-y

      class="announce-page__scroll"

      @scrolltolower="loadMore"

    >

      <view

        v-for="item in list"

        :key="item.id"

        class="announce-item"

        :class="{ 'announce-item--unread': item.isRead === 0 }"

        @click="goDetail(item.id)"

      >

        <MessageListIcon icon="bell" theme="notice" />

        <view class="announce-item__body">

          <view class="announce-item__top">

            <text class="announce-item__title">{{ item.title }}</text>

            <text class="announce-item__time">{{ formatListTime(item.createTime) }}</text>

          </view>

          <text class="announce-item__content">{{ summarizeText(item.content) }}</text>

          <view v-if="item.createName" class="announce-item__meta">

            <text class="announce-item__author">{{ item.createName }}</text>

          </view>

        </view>

      </view>



      <EmptyState v-if="empty && !loading" title="暂无公告" icon="bell" />

      <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />

    </scroll-view>

  </view>

</template>



<script setup lang="ts">

import { onMounted } from 'vue'

import { onPullDownRefresh } from '@dcloudio/uni-app'

import ListFooter from '@/components/common/ListFooter/index.vue'

import ListLoading from '@/components/common/ListLoading/index.vue'

import EmptyState from '@/components/common/EmptyState/index.vue'

import MessageListIcon from '@/components/business/MessageListIcon/index.vue'

import { usePageList } from '@/composables/usePageList'

import { useNoticeWs } from '@/composables/useNoticeWs'

import { getMyAnnounce, readAllAnnounce } from '@/api/message'

import { useMessageStore } from '@/store/message'

import { formatListTime, summarizeText } from '@/utils/format'



const { list, loading, finished, empty, refresh, loadMore } = usePageList(

  async (pageNo, pageSize) => {

    const res = await getMyAnnounce({ pageNo, pageSize })

    return { list: res.data?.list || [], total: res.data?.total || 0 }

  },

)



useNoticeWs(refresh)



async function onReadAll() {

  await readAllAnnounce()

  list.value.forEach((item) => {

    item.isRead = 1

  })

  await useMessageStore().refreshSummary()

  uni.showToast({ title: '已全部标记已读', icon: 'success' })

}



function goDetail(id: number) {

  uni.navigateTo({ url: `/pages-sub/msg/announce/detail?id=${id}` })

}



onMounted(refresh)



onPullDownRefresh(async () => {

  await refresh()

  uni.stopPullDownRefresh()

})

</script>



<style lang="scss" scoped>

@import '@/styles/variables.scss';



.announce-page {

  height: 100vh;

  display: flex;

  flex-direction: column;

  box-sizing: border-box;

  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));

}



.announce-head {

  display: flex;

  align-items: flex-start;

  justify-content: space-between;

  gap: 20rpx;

  margin-bottom: 20rpx;

}



.announce-head__main {

  flex: 1;

  min-width: 0;

}



.announce-head__title {

  display: block;

  font-size: $font-size-lg;

  font-weight: $font-weight-semibold;

  color: $color-text-primary;

}



.announce-head__hint {

  display: block;

  margin-top: 6rpx;

  font-size: $font-size-xs;

  color: $color-text-secondary;

}



.announce-head__action {

  flex-shrink: 0;

  padding-top: 4rpx;

  font-size: $font-size-sm;

  color: $color-primary;

  font-weight: $font-weight-semibold;

}



.announce-page__scroll {

  flex: 1;

  min-height: 0;

}



.announce-item {

  display: flex;

  align-items: flex-start;

  gap: 20rpx;

  margin-bottom: 16rpx;

  padding: 28rpx 24rpx;

  border-radius: $radius-lg;

  background: $color-bg-card;

  box-shadow: $shadow-card;

}



.announce-item--unread {

  background: rgba(245, 158, 11, 0.06);

  border: 1px solid rgba(245, 158, 11, 0.16);

}



.announce-item__body {

  flex: 1;

  min-width: 0;

}



.announce-item__top {

  display: flex;

  align-items: flex-start;

  justify-content: space-between;

  gap: 16rpx;

}



.announce-item__title {

  flex: 1;

  min-width: 0;

  font-size: $font-size-base;

  font-weight: $font-weight-semibold;

  color: $color-text-primary;

  line-height: 1.4;

  overflow: hidden;

  text-overflow: ellipsis;

  display: -webkit-box;

  -webkit-line-clamp: 2;

  -webkit-box-orient: vertical;

}



.announce-item--unread .announce-item__title {

  color: #b45309;

}



.announce-item__time {

  flex-shrink: 0;

  font-size: $font-size-xs;

  color: $color-text-secondary;

  white-space: nowrap;

}



.announce-item__content {

  display: block;

  margin-top: 12rpx;

  font-size: $font-size-sm;

  color: $color-text-secondary;

  line-height: 1.55;

  overflow: hidden;

  text-overflow: ellipsis;

  display: -webkit-box;

  -webkit-line-clamp: 2;

  -webkit-box-orient: vertical;

}



.announce-item__meta {

  margin-top: 14rpx;

}



.announce-item__author {

  display: inline-block;

  padding: 4rpx 14rpx;

  border-radius: $radius-full;

  font-size: $font-size-xs;

  color: $color-text-secondary;

  background: $color-bg-muted;

}

</style>

