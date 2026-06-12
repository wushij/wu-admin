<template>
  <view
    class="form-cell"
    :class="{
      'form-cell--last': last,
      'form-cell--clickable': clickable,
      'form-cell--wide': wide,
      'form-cell--boxed': boxed,
      'form-cell--switch': switchCell,
    }"
    @tap.stop="onTap"
  >
    <view class="form-cell__label-wrap">
      <text class="form-cell__label">{{ label }}</text>
      <text v-if="hint" class="form-cell__hint">{{ hint }}</text>
    </view>
    <view class="form-cell__body">
      <slot>
        <input
          v-if="editable"
          :value="modelValue"
          class="form-cell__input"
          :type="inputType"
          :password="password"
          :maxlength="maxlength"
          :placeholder="placeholder"
          :disabled="disabled"
          @input="onInput"
        />
        <text v-else class="form-cell__value" :class="{ 'form-cell__value--muted': muted }">{{ displayValue }}</text>
      </slot>
      <IconFont v-if="arrow" name="arrow" :size="28" color="#c0c4cc" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'

const props = withDefaults(
  defineProps<{
    label: string
    modelValue?: string
    displayValue?: string
    placeholder?: string
    editable?: boolean
    password?: boolean
    inputType?: 'text' | 'number' | 'digit'
    maxlength?: number
    muted?: boolean
    last?: boolean
    arrow?: boolean
    clickable?: boolean
    disabled?: boolean
    wide?: boolean
    hint?: string
    boxed?: boolean
    switchCell?: boolean
  }>(),
  {
    editable: false,
    password: false,
    inputType: 'text',
    muted: false,
    last: false,
    arrow: false,
    clickable: false,
    disabled: false,
    wide: false,
    boxed: false,
    switchCell: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  click: []
}>()

function onInput(e: { detail: { value: string } }) {
  emit('update:modelValue', e.detail.value)
}

const lastTapAt = ref(0)

function onTap() {
  if (!props.clickable) return
  const now = Date.now()
  if (now - lastTapAt.value < 350) return
  lastTapAt.value = now
  emit('click')
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.form-cell {
  display: flex;
  align-items: flex-start;
  gap: 24rpx;
  min-height: 104rpx;
  padding: 20rpx 32rpx;
  border-bottom: 1px solid $color-border-light;
}

.form-cell--wide .form-cell__label-wrap {
  width: 240rpx;
}

.form-cell__label-wrap {
  flex-shrink: 0;
  width: 168rpx;
  padding-top: 6rpx;
}

.form-cell__label {
  display: block;
  font-size: $font-size-md;
  color: $color-text-primary;
  line-height: 1.45;
}

.form-cell__hint {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.4;
}

.form-cell__body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12rpx;
  min-height: 64rpx;
}

.form-cell--last {
  border-bottom: none;
}

.form-cell--clickable:active {
  background: $color-bg-muted;
}

.form-cell__input,
.form-cell__value {
  flex: 1;
  text-align: right;
  font-size: $font-size-md;
  color: $color-text-primary;
}

.form-cell__value--muted {
  color: $color-text-secondary;
}

.form-cell__input::placeholder {
  color: $color-text-placeholder;
}

.form-cell--boxed .form-cell__input,
.form-cell--boxed .form-cell__value,
.form-cell--boxed :deep(.picker-value) {
  width: 100%;
  min-height: 72rpx;
  padding: 0 20rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: $color-bg-card;
  text-align: left;
  line-height: 72rpx;
  box-sizing: border-box;
}

.form-cell--boxed .form-cell__input {
  line-height: normal;
  height: 72rpx;
}

.form-cell--boxed .form-cell__body {
  justify-content: flex-start;
}

.form-cell--switch {
  align-items: center;
  min-height: 88rpx;
}

.form-cell--switch .form-cell__label-wrap {
  padding-top: 0;
}

.form-cell--switch .form-cell__body {
  justify-content: flex-end;
  min-height: auto;
}

.form-cell--switch .form-cell__body :deep(switch) {
  transform: scale(0.78);
  transform-origin: right center;
}
</style>
