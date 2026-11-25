<template>
  <article
    class="model-card motion-hover-lift"
    :class="{
      'is-disabled': item.status === 0,
      'motion-border-beam': item.isDefault === 1,
    }"
  >
    <div class="model-card-head">
      <div class="provider-badge" :style="{ background: providerMeta(item.provider).bg }">
        <AiProviderIcon :provider="item.provider" :size="26" />
      </div>
      <div class="model-card-title">
        <div class="name-row">
          <span class="name" :title="item.name">{{ item.name }}</span>
          <span class="type-tag" :class="item.modelType === 'embedding' ? 'is-vector' : 'is-chat'">
            {{ item.modelType === 'embedding' ? '向量' : '对话' }}
          </span>
          <div class="status-switch-wrap">
            <el-switch
              :model-value="item.status === 1"
              :disabled="!canUpdate"
              @change="(v: boolean | string | number) => $emit('toggleStatus', item, Boolean(v))"
            />
            <span class="status-text" :class="{ active: item.status === 1 }">
              {{ item.status === 1 ? '启用' : '停用' }}
            </span>
          </div>
        </div>
        <div class="sub">{{ providerMeta(item.provider).label }} · {{ item.modelName }}</div>
      </div>
    </div>

    <div class="model-card-body">
      <div class="info-line">
        <span class="label">接口地址</span>
        <span class="value" :title="item.baseUrl">{{ item.baseUrl || '—' }}</span>
      </div>
      <div class="info-line">
        <span class="label">API Key</span>
        <div class="value">
          <span class="key-tag" :class="{ configured: item.hasApiKey }">
            <el-icon v-if="item.hasApiKey && !item.apiKeyMasked" :size="12" aria-hidden="true">
              <Check />
            </el-icon>
            <span>{{ item.hasApiKey ? (item.apiKeyMasked || '已配置') : '未配置' }}</span>
          </span>
        </div>
      </div>
      <div class="info-line">
        <span class="label">{{ item.modelType === 'embedding' ? '向量参数' : '模型参数' }}</span>
        <span class="value motion-tabular">
          <template v-if="item.modelType === 'embedding'">
            {{ item.dimension || '—' }} 维 · 批量 {{ item.batchSize ?? 16 }}
          </template>
          <template v-else>
            温度 {{ item.temperature ?? 0.7 }} · 最大 {{ item.maxTokens ?? 8192 }} Tokens
          </template>
        </span>
      </div>
      <div v-if="testLatency !== undefined" class="test-result" :class="testLatency >= 0 ? 'ok' : 'fail'">
        <el-icon v-if="testLatency >= 0"><CircleCheckFilled /></el-icon>
        <el-icon v-else><CircleCloseFilled /></el-icon>
        <span class="motion-tabular">
          {{ testLatency >= 0 ? `连接正常 · ${testLatency}ms` : '连接失败' }}
        </span>
      </div>
    </div>

    <div class="model-card-foot">
      <span v-if="item.isDefault === 1" class="default-badge">
        <el-icon :size="11"><Star /></el-icon>
        <span>默认</span>
      </span>
      <el-button
        size="small"
        class="btn-pearl-teal"
        :loading="testing"
        :icon="Connection"
        v-permission="'system:ai-model:test'"
        @click="$emit('test', item)"
      >
        测试
      </el-button>
      <el-button
        v-if="item.isDefault !== 1"
        size="small"
        type="warning"
        plain
        :icon="Star"
        :disabled="item.status !== 1"
        v-permission="'system:ai-model:update'"
        @click="$emit('setDefault', item)"
      >
        设为默认
      </el-button>
      <el-button
        size="small"
        type="primary"
        plain
        :icon="Edit"
        v-permission="'system:ai-model:update'"
        @click="$emit('edit', item)"
      >
        编辑
      </el-button>
      <el-button
        size="small"
        type="danger"
        plain
        :icon="Delete"
        :disabled="item.isDefault === 1"
        v-permission="'system:ai-model:delete'"
        @click="$emit('delete', item)"
      >
        删除
      </el-button>
    </div>
  </article>
</template>

<script setup lang="ts">
import {
  Check,
  CircleCheckFilled,
  CircleCloseFilled,
  Connection,
  Star,
  Edit,
  Delete,
} from '@element-plus/icons-vue'
import AiProviderIcon from '@/components/AiProviderIcon.vue'
import type { AiModelVO } from '@/api/system/ai-model'
import { providerMeta } from '../constants/aiModelMeta'

defineProps<{
  item: AiModelVO
  canUpdate: boolean
  testing: boolean
  testLatency?: number
}>()

defineEmits<{
  toggleStatus: [item: AiModelVO, enabled: boolean]
  test: [item: AiModelVO]
  setDefault: [item: AiModelVO]
  edit: [item: AiModelVO]
  delete: [item: AiModelVO]
}>()
</script>

<style scoped lang="scss">
.model-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  background: var(--color-bg-surface);

  &.is-disabled {
    opacity: 0.65;
  }
}

.model-card-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.provider-badge {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

:global(html.dark) .provider-badge {
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.model-card-title {
  flex: 1;
  min-width: 0;

  .name-row {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    min-width: 0;
  }

  .name {
    flex: 1;
    min-width: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .type-tag {
    flex-shrink: 0;
    padding: 1px 8px;
    border-radius: var(--radius-pill);
    font-size: 10.5px;
    font-weight: 600;
    line-height: 1.6;

    &.is-chat {
      background: var(--color-primary-muted);
      color: var(--color-primary);
      border: 1px solid var(--color-border-subtle);
    }

    &.is-vector {
      background: var(--color-success-soft);
      color: var(--color-success);
      border: 1px solid var(--color-success-border);
    }
  }

  .sub {
    margin-top: 2px;
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.default-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: auto;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  background: var(--color-warning-soft);
  color: var(--color-warning);
  border: 1px solid var(--color-warning-border);
  font-size: 11px;
  font-weight: 600;
}

.status-switch-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;

  .status-text {
    font-size: var(--text-caption);
    color: var(--color-text-muted);
    font-weight: 500;

    &.active {
      color: var(--el-color-primary);
      font-weight: 600;
    }
  }
}

.model-card-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);

  .info-line {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12.5px;
    line-height: 1.5;

    .label {
      flex-shrink: 0;
      width: 58px;
      color: var(--color-text-secondary);
      font-weight: 500;
    }

    .value {
      flex: 1;
      min-width: 0;
      color: var(--color-text-primary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.key-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-family: var(--font-mono);
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);

  &.configured {
    background: var(--color-success-soft);
    color: var(--color-success);
    border-color: var(--color-success-border);
    font-weight: 600;
  }
}

.test-result {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  margin-top: 2px;
  width: fit-content;

  &.ok {
    background: var(--color-success-soft);
    color: var(--color-success);
    border: 1px solid var(--color-success-border);
  }

  &.fail {
    background: var(--color-danger-soft);
    color: var(--color-danger);
    border: 1px solid var(--color-danger-border);
  }
}

.model-card-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
  border-top: 1px solid var(--color-border-subtle);
  padding-top: var(--space-3);

  > .el-button {
    margin-left: 0;
  }
}
</style>
