<template>
  <el-popover placement="top-start" :width="340" trigger="click" popper-class="emoji-popover">
    <template #reference>
      <el-button link class="toolbar-btn" title="表情">
        <el-icon><Sunny /></el-icon>
      </el-button>
    </template>
    <div class="emoji-panel">
      <div class="emoji-tabs">
        <button
          v-for="cat in CHAT_EMOJI_CATEGORIES"
          :key="cat.key"
          type="button"
          class="emoji-tab"
          :class="{ active: activeTab === cat.key }"
          @click="activeTab = cat.key"
        >
          {{ cat.label }}
        </button>
      </div>
      <div class="emoji-grid">
        <span
          v-for="e in currentEmojis"
          :key="`${activeTab}-${e}`"
          class="emoji"
          @click="pick(e)"
        >{{ e }}</span>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Sunny } from '@element-plus/icons-vue'
import { CHAT_EMOJI_CATEGORIES } from '@/constants/chat-emojis'

const emit = defineEmits<{
  pick: [emoji: string]
}>()

const activeTab = ref(CHAT_EMOJI_CATEGORIES[0].key)

const currentEmojis = computed(() =>
  CHAT_EMOJI_CATEGORIES.find((c) => c.key === activeTab.value)?.emojis ?? [],
)

function pick(emoji: string) {
  emit('pick', emoji)
}
</script>

<style scoped>
.emoji-panel {
  user-select: none;
}

.emoji-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.emoji-tab {
  flex: 1;
  border: none;
  background: transparent;
  padding: 4px 0;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  border-radius: 4px;
}

.emoji-tab:hover {
  background: #f5f5f5;
}

.emoji-tab.active {
  color: var(--theme-primary, #111827);
  font-weight: 600;
  background: #f0f0f0;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 2px;
  max-height: 240px;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 2px;
}

.emoji {
  font-size: 22px;
  line-height: 1.2;
  cursor: pointer;
  text-align: center;
  padding: 4px 2px;
  border-radius: 4px;
}

.emoji:hover {
  background: #f0f0f0;
}
</style>
