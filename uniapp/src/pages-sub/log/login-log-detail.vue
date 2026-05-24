<template>
  <view class="login-log-detail page-padded">
    <ListLoading v-if="loading" />
    <template v-else-if="detail">
      <MonitorPanel title="登录信息">
        <DetailRow label="用户名" :value="detail.username" />
        <DetailRow label="登录状态" :value="loginStatusLabel(detail.status)" />
        <DetailRow label="登录时间" :value="formatDateTime(detail.loginTime, true)" />
        <DetailRow label="登录地点" :value="detail.loginLocation" />
        <DetailRow label="消息" :value="detail.msg" />
      </MonitorPanel>

      <MonitorPanel title="客户端信息">
        <DetailRow label="IP 地址" :value="detail.ipaddr" />
        <DetailRow label="浏览器" :value="detail.browser" />
        <DetailRow label="操作系统" :value="detail.os" />
      </MonitorPanel>
    </template>
    <EmptyState v-else title="日志不存在或已失效" icon="contact-o" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import DetailRow from '@/components/common/DetailRow/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { loginStatusLabel, readLoginLogDetail } from '@/composables/useLoginLog'
import { formatDateTime } from '@/utils/format'
import type { LoginLogVO } from '@/types/system'

const loading = ref(true)
const detail = ref<LoginLogVO | null>(null)

onLoad((options) => {
  const id = Number(options?.id)
  uni.setNavigationBarTitle({ title: '登录详情' })
  detail.value = readLoginLogDetail(Number.isFinite(id) ? id : undefined)
  loading.value = false
})
</script>

<style lang="scss" scoped>
.login-log-detail {
  min-height: 100vh;
  box-sizing: border-box;
  padding-bottom: 32rpx;
}
</style>
