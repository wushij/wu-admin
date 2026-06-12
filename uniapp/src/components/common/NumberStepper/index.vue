<template>
  <view class="number-stepper" :class="{ 'number-stepper--disabled': disabled }">
    <view
      class="number-stepper__btn"
      :class="{ 'number-stepper__btn--disabled': disabled || atMin }"
      @click="decrease"
    >
      <text class="number-stepper__symbol">−</text>
    </view>
    <input
      class="number-stepper__input"
      type="number"
      :value="String(innerValue)"
      :disabled="disabled"
      @input="onInput"
      @blur="commitInput"
    />
    <view
      class="number-stepper__btn"
      :class="{ 'number-stepper__btn--disabled': disabled || atMax }"
      @click="increase"
    >
      <text class="number-stepper__symbol">+</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: number
    min?: number
    max?: number
    step?: number
    disabled?: boolean
  }>(),
  {
    min: 0,
    max: 999999,
    step: 1,
    disabled: false,
  },
)

const emit = defineEmits<{ 'update:modelValue': [value: number] }>()

const innerValue = ref(clamp(props.modelValue))

watch(
  () => props.modelValue,
  (v) => {
    innerValue.value = clamp(v)
  },
)

const atMin = computed(() => innerValue.value <= props.min)
const atMax = computed(() => innerValue.value >= props.max)

function clamp(value: number) {
  const num = Number.isFinite(value) ? value : props.min
  return Math.min(props.max, Math.max(props.min, num))
}

function emitValue(value: number) {
  const next = clamp(value)
  innerValue.value = next
  emit('update:modelValue', next)
}

function decrease() {
  if (props.disabled || atMin.value) return
  emitValue(innerValue.value - props.step)
}

function increase() {
  if (props.disabled || atMax.value) return
  emitValue(innerValue.value + props.step)
}

function onInput(e: { detail: { value: string } }) {
  const raw = e.detail.value
  if (raw === '' || raw === '-') return
  const num = Number(raw)
  if (Number.isFinite(num)) innerValue.value = num
}

function commitInput() {
  emitValue(innerValue.value)
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.number-stepper {
  display: inline-flex;
  align-items: stretch;
  width: 240rpx;
  height: 72rpx;
  border: 1px solid #dcdfe6;
  border-radius: $radius-md;
  overflow: hidden;
  background: $color-bg-card;
}

.number-stepper__btn {
  width: 72rpx;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-right: 1px solid #dcdfe6;

  &:last-child {
    border-right: none;
    border-left: 1px solid #dcdfe6;
  }
}

.number-stepper__symbol {
  font-size: 32rpx;
  line-height: 1;
  color: $color-text-regular;
}

.number-stepper__btn--disabled .number-stepper__symbol {
  color: #c0c4cc;
}

.number-stepper__input {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 8rpx;
  border: none;
  background: #fff;
  text-align: center;
  font-size: $font-size-md;
  color: $color-text-primary;
}

.number-stepper--disabled {
  opacity: 0.65;
}
</style>
