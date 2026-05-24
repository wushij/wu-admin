<template>
  <el-tag v-if="showTag" :type="tagType" :size="tagSize" :effect="tagEffect">
    {{ label }}
  </el-tag>
  <span v-else class="dict-tag-plain">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import type { TagProps } from 'element-plus'
import { getDictLabel, getDictListClass, listClassToTagType, useDict } from '@/composables/useDict'

const props = defineProps({
  value: { type: [String, Number, Boolean], default: undefined },
  dictType: { type: String, required: true },
  size: { type: String, default: 'small' },
  effect: { type: String, default: 'light' },
  /** 无匹配时是否仍显示 el-tag */
  tag: { type: Boolean, default: true }
})

const { load } = useDict(props.dictType)

const label = computed(() => getDictLabel(props.dictType, props.value))
const tagType = computed(() => listClassToTagType(getDictListClass(props.dictType, props.value)) as TagProps['type'])
const tagSize = computed(() => props.size as TagProps['size'])
const tagEffect = computed(() => props.effect as TagProps['effect'])
const showTag = computed(() => props.tag && label.value !== '-')

onMounted(() => load())
watch(
  () => props.dictType,
  () => load(true)
)
watch(
  () => props.value,
  () => load()
)
</script>

<style scoped>
.dict-tag-plain {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
