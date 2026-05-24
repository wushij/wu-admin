<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder"
    :clearable="clearable"
    :disabled="disabled"
    :multiple="multiple"
    :filterable="filterable"
    :collapse-tags="collapseTags"
    :style="selectStyle"
    :loading="loading"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-option
      v-for="opt in options"
      :key="String(opt.value)"
      :label="opt.label"
      :value="opt.value"
    />
  </el-select>
</template>

<script setup lang="ts">
import { computed, onMounted, toRef, watch, type PropType } from 'vue'
import { useDict } from '@/composables/useDict'

type DictValueType = 'auto' | 'number' | 'string'

const props = defineProps({
  modelValue: { type: [String, Number, Array, Boolean], default: undefined },
  dictType: { type: String, required: true },
  placeholder: { type: String, default: '请选择' },
  clearable: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false },
  multiple: { type: Boolean, default: false },
  filterable: { type: Boolean, default: false },
  collapseTags: { type: Boolean, default: false },
  width: { type: String, default: '100%' },
  /** number | string | auto — 与表单字段类型对齐 */
  valueType: { type: String as PropType<DictValueType>, default: 'auto' },
  /** 加载后若当前值为空，自动选中 isDefault=1 的项 */
  applyDefault: { type: Boolean, default: false },
  /** 排除的键值（如创建表单不展示某些选项） */
  excludeValues: { type: Array as PropType<Array<string | number>>, default: () => [] },
})

const emit = defineEmits(['update:modelValue'])

const { options: rawOptions, loading, load } = useDict(toRef(props, 'dictType'), {
  valueType: props.valueType
})

const options = computed(() => {
  if (!props.excludeValues?.length) return rawOptions.value
  const excluded = new Set(props.excludeValues.map((v) => String(v)))
  return rawOptions.value.filter((o) => !excluded.has(String(o.value)))
})

const selectStyle = computed(() => ({
  width: props.width === '100%' ? '100%' : props.width
}))

onMounted(async () => {
  await load()
  tryApplyDefault()
})
watch(
  () => props.dictType,
  async () => {
    await load(true)
    tryApplyDefault()
  }
)

function tryApplyDefault() {
  if (!props.applyDefault) return
  const empty = props.modelValue === undefined || props.modelValue === null || props.modelValue === ''
  if (!empty) return
  const def = options.value.find((o) => o.raw?.isDefault === 1)
  if (def) emit('update:modelValue', def.value)
}
</script>
