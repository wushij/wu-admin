<template>
  <div class="tags-view">
    <div ref="scrollRef" class="tags-view-scroll">
      <button
        v-for="tag in visitedViews"
        :key="tag.path"
        type="button"
        class="tags-view-item"
        :class="{ active: isActive(tag), affix: tag.affix }"
        @click="go(tag)"
      >
        <span class="tags-view-title">{{ tag.title }}</span>
        <el-icon
          v-if="!tag.affix"
          class="tags-view-close"
          @click.stop="close(tag)"
        >
          <Close />
        </el-icon>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { Close } from '@element-plus/icons-vue'
import { useTagsViewStore, type TagView } from '@/store/tagsView'

const route = useRoute()
const router = useRouter()
const tagsStore = useTagsViewStore()
const { visitedViews } = storeToRefs(tagsStore)
const scrollRef = ref<HTMLElement | null>(null)

onMounted(() => {
  tagsStore.initTags()
  tagsStore.addView(route)
  scrollActiveIntoView()
})

watch(
  () => route.fullPath,
  () => {
    tagsStore.addView(route)
    scrollActiveIntoView(false)
  },
)

function isActive(tag: TagView) {
  return route.path === tag.path
}

function go(tag: TagView) {
  if (route.fullPath !== tag.fullPath) {
    router.push(tag.fullPath)
    return
  }
  // 已在当前页：仅确保标签可见，不触发重排
  scrollActiveIntoView(false)
}

function close(tag: TagView) {
  const next = tagsStore.delView(tag, route.path)
  if (next && route.path === tag.path) {
    router.push(next)
  }
}

async function scrollActiveIntoView(alignEnd = true) {
  await nextTick()
  const container = scrollRef.value
  if (!container) return
  const active = container.querySelector('.tags-view-item.active') as HTMLElement | null
  if (!active) return
  active.scrollIntoView({
    block: 'nearest',
    inline: alignEnd ? 'nearest' : 'center',
  })
}
</script>

<style scoped src="./layout-tags-view.css"></style>
