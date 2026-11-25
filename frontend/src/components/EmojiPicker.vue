<template>
  <el-popover placement="top-start" :width="340" trigger="click" popper-class="emoji-popover">
    <template #reference>
      <span class="toolbar-btn-ref">
        <el-tooltip content="表情" placement="top" :show-after="1000">
          <el-button link class="toolbar-btn" aria-label="表情">
            <ChatToolbarIcons name="emoji" />
          </el-button>
        </el-tooltip>
      </span>
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
import ChatToolbarIcons from '@/components/chat/ChatToolbarIcons.vue'
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
.toolbar-btn-ref {
  display: inline-flex;
  align-items: center;
}

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
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: 4px;
}

.emoji-tab:hover {
  background: var(--color-bg-hover);
}

.emoji-tab.active {
  color: var(--el-color-primary);
  font-weight: 600;
  background: var(--color-bg-subtle);
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
  background: var(--color-bg-hover);
}
</style>
