<template>
  <view class="retrieval-panel">
    <view class="retrieval-form card--elevated">
      <text class="retrieval-form__title">检索测试</text>
      <text class="retrieval-form__desc">
        输入问题，查看知识库实际召回结果与相关性得分，用于校验切片质量
      </text>

      <textarea
        v-model="query"
        class="retrieval-query"
        :maxlength="300"
        :auto-height="true"
        placeholder="例如：如何修改登录密码？"
        placeholder-class="retrieval-query__placeholder"
      />

      <view class="retrieval-params">
        <view class="retrieval-param">
          <text class="retrieval-param__label">TopK</text>
          <input v-model="topK" class="retrieval-param__input" type="number" placeholder="5" />
        </view>
        <view class="retrieval-param">
          <text class="retrieval-param__label">最低分</text>
          <input v-model="minScore" class="retrieval-param__input" type="number" placeholder="0.2" />
        </view>
        <view class="retrieval-submit" :class="{ 'is-loading': running }" @click="runRetrieval">
          <text>{{ running ? '检索中…' : '开始检索' }}</text>
        </view>
      </view>
    </view>

    <scroll-view v-if="result" scroll-y class="retrieval-result">
      <view class="retrieval-summary">
        <view class="tag" :class="matchModeMeta.cls">
          <text>{{ matchModeMeta.label }}</text>
        </view>
        <text class="retrieval-summary__text">命中 {{ result.items.length }} 条 · TopK {{ result.topK }}</text>
      </view>

      <view v-for="(item, index) in result.items" :key="item.chunkId" class="retrieval-item card--elevated">
        <view class="retrieval-item__head">
          <text class="retrieval-item__score">{{ (item.score * 100).toFixed(1) }}%</text>
          <text class="retrieval-item__doc">{{ item.documentName || '未命名文档' }}</text>
          <text class="retrieval-item__idx">[{{ index + 1 }}]</text>
        </view>
        <text v-if="item.heading" class="retrieval-item__heading">{{ item.heading }}</text>
        <text class="retrieval-item__content">{{ item.content }}</text>
      </view>

      <EmptyState
        v-if="result.items.length === 0"
        title="没有召回结果"
        description="可尝试降低最低分、提高 TopK，或确认文档已完成向量化"
        icon="search"
      />
    </scroll-view>

    <EmptyState
      v-else
      title="等待检索"
      description="输入问题后点击「开始检索」，查看该知识库的召回效果"
      icon="search"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import {
  retrieveKnowledgeTest,
  type KnowledgeRetrieveResult,
} from '@/api/system/ai-knowledge-base'

const props = defineProps<{
  knowledgeBaseId: number
}>()

const query = ref('')
const topK = ref('5')
const minScore = ref('0.2')
const running = ref(false)
const result = ref<KnowledgeRetrieveResult | null>(null)

const matchModeMeta = computed(() => {
  switch (result.value?.matchMode) {
    case 'VECTOR':
      return { label: '向量检索', cls: 'is-vector' }
    case 'KEYWORD':
      return { label: '关键词检索', cls: 'is-keyword' }
    default:
      return { label: '无匹配', cls: 'is-none' }
  }
})

async function runRetrieval() {
  const text = query.value.trim()
  if (!text) {
    uni.showToast({ title: '请输入检索内容', icon: 'none' })
    return
  }
  if (running.value) return
  running.value = true
  try {
    const res = await retrieveKnowledgeTest({
      knowledgeBaseId: props.knowledgeBaseId,
      query: text,
      topK: Number(topK.value) || 5,
      minScore: Number(minScore.value),
    })
    result.value = res.data || null
  } catch {
    /* 错误提示由请求层统一处理 */
  } finally {
    running.value = false
  }
}
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.retrieval-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.retrieval-form {
  margin: 16rpx 0;
  padding: 24rpx 26rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
}

.retrieval-form__title {
  font-size: 28rpx;
  font-weight: 700;
  color: $color-text-primary;
}

.retrieval-form__desc {
  display: block;
  margin-top: 8rpx;
  font-size: 21rpx;
  line-height: 1.5;
  color: $color-text-secondary;
}

.retrieval-query {
  width: 100%;
  min-height: 130rpx;
  margin-top: 16rpx;
  padding: 16rpx 18rpx;
  box-sizing: border-box;
  border-radius: $radius-md;
  border: 1px solid $color-border-light;
  background: $color-bg-page;
  font-size: 25rpx;
  line-height: 1.5;
  color: $color-text-primary;
}

.retrieval-query__placeholder {
  color: $color-text-placeholder;
}

.retrieval-params {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 16rpx;
}

.retrieval-param {
  display: flex;
  align-items: center;
  gap: 8rpx;
  height: 64rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  border: 1px solid $color-border-light;
  background: $color-bg-page;
}

.retrieval-param__label {
  font-size: 21rpx;
  color: $color-text-secondary;
}

.retrieval-param__input {
  width: 90rpx;
  font-size: 24rpx;
  color: $color-text-primary;
}

.retrieval-submit {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 72rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #1677ff 0%, #6366f1 100%);

  &:active {
    opacity: 0.9;
  }

  &.is-loading {
    opacity: 0.65;
  }

  text {
    font-size: 25rpx;
    font-weight: 600;
    color: #ffffff;
  }
}

.retrieval-result {
  flex: 1;
  min-height: 0;
}

.retrieval-summary {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.retrieval-summary__text {
  font-size: 21rpx;
  color: $color-text-secondary;
}

.retrieval-item {
  margin-bottom: 16rpx;
  padding: 22rpx 24rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
}

.retrieval-item__head {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.retrieval-item__score {
  font-size: 24rpx;
  font-weight: 700;
  color: #16a34a;
}

.retrieval-item__doc {
  flex: 1;
  min-width: 0;
  font-size: 23rpx;
  font-weight: 600;
  color: $color-text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.retrieval-item__idx {
  font-size: 20rpx;
  color: $color-text-placeholder;
}

.retrieval-item__heading {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #6366f1;
}

.retrieval-item__content {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  line-height: 1.6;
  color: $color-text-secondary;
}

.tag {
  padding: 4rpx 16rpx;
  border-radius: 999rpx;

  text {
    font-size: 20rpx;
    font-weight: 600;
  }

  &.is-vector {
    background: rgba(22, 119, 255, 0.1);

    text {
      color: #1677ff;
    }
  }

  &.is-keyword {
    background: rgba(217, 119, 6, 0.12);

    text {
      color: #d97706;
    }
  }

  &.is-none {
    background: rgba(100, 116, 139, 0.12);

    text {
      color: #64748b;
    }
  }
}
</style>
