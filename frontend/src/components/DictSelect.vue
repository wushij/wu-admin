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
  valueType: { type: String as PropType<DictValueType>, default: 'auto' }
})

const emit = defineEmits(['update:modelValue'])

const { options, loading, load } = useDict(toRef(props, 'dictType'), {
  valueType: props.valueType
})

const selectStyle = computed(() => ({
  width: props.width === '100%' ? '100%' : props.width
}))

onMounted(() => load())
watch(
  () => props.dictType,
  () => load(true)
)
</script>
