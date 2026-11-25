<template>
  <view v-if="citations.length" class="aiwu-citations">
    <view class="aiwu-citations__head" @click="expanded = !expanded">
      <view class="aiwu-citations__badge">
        <IconFont name="search" :size="24" color="#6366f1" />
        <text>实时联网检索参考 ({{ citations.length }})</text>
      </view>
      <text class="aiwu-citations__sub">{{ expanded ? '收起' : '展开查看' }}</text>
    </view>

    <view v-if="expanded" class="aiwu-citations__cards">
      <view
        v-for="(cite, idx) in citations"
        :key="`${cite.url}-${idx}`"
        class="aiwu-citation-card"
        @click="openCitation(cite.url)"
      >
        <view class="aiwu-citation-card__top">
          <text class="aiwu-citation-card__idx">[{{ idx + 1 }}]</text>
          <text class="aiwu-citation-card__tag">联网</text>
          <text class="aiwu-citation-card__title">{{ cite.title }}</text>
        </view>
        <text class="aiwu-citation-card__excerpt">
          {{ resolveCitationPreviewText(cite.snippet, cite.url) }}
        </text>
        <text class="aiwu-citation-card__source">{{ cite.source || '实时检索' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import IconFont from '@/components/base/IconFont/index.vue'
import { resolveCitationPreviewText } from '@/utils/ai/citation-preview'
import type { WebSearchCitation } from '@/api/ai'

defineProps<{
  citations: WebSearchCitation[]
}>()

const expanded = ref(false)

function openCitation(url: string) {
  if (!url) return
  // #ifdef H5
  window.open(url, '_blank', 'noopener,noreferrer')
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: url,
    showToast: false,
    success: () => uni.showToast({ title: '参考链接已复制', icon: 'none' }),
  })
  // #endif
}
</script>
