<template>
  <view class="cache-key-page page-padded">
    <ListLoading v-if="loading" />
    <template v-else-if="detail">
      <MonitorPanel title="键信息">
        <DetailRow label="键名" :value="detail.key" />
        <DetailRow label="类型" :value="detail.type" />
        <DetailRow label="有效期" :value="formatCacheTtl(detail.ttl)" />
      </MonitorPanel>
      <MonitorPanel title="键值">
        <scroll-view scroll-y class="cache-key-page__scroll">
          <text class="cache-key-page__value">{{ valueText }}</text>
        </scroll-view>
      </MonitorPanel>
    </template>
    <EmptyState v-else title="读取失败或键不存在" icon="balance-list-o" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import DetailRow from '@/components/common/DetailRow/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { getCacheValue } from '@/api/monitor/cache'
import { formatCacheTtl, formatCacheValue } from '@/composables/useCacheMonitor'
import type { CacheValueDetail } from '@/types/system'

const loading = ref(true)
const detail = ref<CacheValueDetail | null>(null)

const valueText = computed(() => {
  if (!detail.value) return ''
  const text = formatCacheValue(detail.value.value)
  return text || '（空）'
})

onLoad(async (options) => {
  const key = decodeURIComponent(String(options?.key || ''))
  if (!key) {
    loading.value = false
    return
  }
  uni.setNavigationBarTitle({ title: '缓存详情' })
  try {
    const res = await getCacheValue(key)
    detail.value = res.data || null
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>

.cache-key-page {
  min-height: 100vh;
  box-sizing: border-box;
}

.cache-key-page__scroll {
  max-height: 60vh;
}

.cache-key-page__value {
  display: block;
  padding: 20rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 24rpx;
  line-height: 1.55;
  color: $color-text-primary;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
