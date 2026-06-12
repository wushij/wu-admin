<template>
  <view class="oper-log-detail page-padded">
    <ListLoading v-if="loading" />
    <template v-else-if="detail">
      <MonitorPanel title="基本信息">
        <DetailRow label="模块名称" :value="detail.title" />
        <DetailRow label="业务类型" :value="businessTypeLabel(detail.businessType)" />
        <DetailRow label="请求方式" :value="detail.requestMethod" />
        <DetailRow label="操作人员" :value="detail.operName" />
        <DetailRow label="操作地址" :value="detail.operIp" />
        <DetailRow label="耗时" :value="detail.costTime != null ? `${detail.costTime}ms` : undefined" />
        <DetailRow label="操作状态" :value="detail.status === 0 ? '正常' : '异常'" />
        <DetailRow label="操作时间" :value="formatDateTime(detail.operTime, true)" />
      </MonitorPanel>

      <MonitorPanel title="请求信息">
        <DetailRow label="请求地址" :value="detail.operUrl" />
        <DetailRow label="方法名称" :value="detail.method" />
      </MonitorPanel>

      <MonitorPanel v-if="detail.errorMsg" title="错误信息">
        <text class="oper-log-detail__error">{{ detail.errorMsg }}</text>
      </MonitorPanel>

      <MonitorPanel title="请求参数">
        <scroll-view scroll-y class="oper-log-detail__scroll">
          <text class="oper-log-detail__code">{{ paramText }}</text>
        </scroll-view>
      </MonitorPanel>

      <MonitorPanel title="返回参数">
        <scroll-view scroll-y class="oper-log-detail__scroll">
          <text class="oper-log-detail__code">{{ resultText }}</text>
        </scroll-view>
      </MonitorPanel>
    </template>
    <EmptyState v-else title="日志不存在或已失效" icon="records-o" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import DetailRow from '@/components/common/DetailRow/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { businessTypeLabel, formatOperLogJson, readOperLogDetail } from '@/composables/useOperLog'
import { formatDateTime } from '@/utils/format'
import type { OperLogVO } from '@/types/system'

const loading = ref(true)
const detail = ref<OperLogVO | null>(null)

const paramText = computed(() => {
  const text = formatOperLogJson(detail.value?.operParam)
  return text || '（空）'
})

const resultText = computed(() => {
  const text = formatOperLogJson(detail.value?.jsonResult)
  return text || '（空）'
})

onLoad((options) => {
  const id = Number(options?.id)
  uni.setNavigationBarTitle({ title: '日志详情' })
  detail.value = readOperLogDetail(Number.isFinite(id) ? id : undefined)
  loading.value = false
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.oper-log-detail {
  min-height: 100vh;
  box-sizing: border-box;
  padding-bottom: 32rpx;
}

.oper-log-detail__scroll {
  max-height: 360rpx;
}

.oper-log-detail__code {
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

.oper-log-detail__error {
  display: block;
  padding: 20rpx;
  border-radius: $radius-md;
  background: rgba(245, 108, 108, 0.08);
  font-size: $font-size-sm;
  line-height: 1.55;
  color: $color-danger;
  word-break: break-all;
}
</style>
