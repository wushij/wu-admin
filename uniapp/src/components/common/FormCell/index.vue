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
        <view
          v-if="editable"
          class="form-cell__input-wrap"
          :class="{
            'form-cell__input-wrap--password': password,
            'form-cell__input-wrap--boxed': password && boxed,
          }"
        >
          <input
            :value="modelValue"
            class="form-cell__input"
            :class="{ 'form-cell__input--masked': useMask }"
            :type="resolvedInputType"
            :password="useNativePassword"
            :maxlength="maxlength"
            :placeholder="placeholder"
            :disabled="disabled"
            autocomplete="new-password"
            @input="onInput"
          />
          <view
            v-if="password"
            class="form-cell__input-toggle"
            @tap.stop="togglePasswordVisible"
          >
            <PasswordEyeIcon :slashed="passwordVisible" />
          </view>
        </view>
        <text v-else class="form-cell__value" :class="{ 'form-cell__value--muted': muted }">{{ displayValue }}</text>
      </slot>
      <IconFont v-if="arrow" name="arrow" :size="28" color="#c0c4cc" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import IconFont from '@/components/common/IconFont/index.vue'
import PasswordEyeIcon from '@/components/common/PasswordEyeIcon/index.vue'

const isH5 = process.env.UNI_PLATFORM === 'h5'

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
const passwordVisible = ref(false)

const useMask = computed(
  () => props.password && isH5 && !passwordVisible.value && !!props.modelValue,
)
const useNativePassword = computed(
  () => props.password && !isH5 && !!props.modelValue && !passwordVisible.value,
)

const resolvedInputType = computed(() => {
  if (!props.password) return props.inputType
  if (isH5) return 'text'
  if (!props.modelValue) return 'text'
  return passwordVisible.value ? 'text' : 'password'
})

function togglePasswordVisible() {
  passwordVisible.value = !passwordVisible.value
}

function onTap() {
  if (!props.clickable) return
  const now = Date.now()
  if (now - lastTapAt.value < 350) return
  lastTapAt.value = now
  emit('click')
}
</script>

<style lang="scss" scoped>

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

.form-cell__input-wrap {
  flex: 1;
  min-width: 0;
  position: relative;
}

.form-cell__input-wrap--password .form-cell__input {
  padding-right: 72rpx;
}

.form-cell__input-toggle {
  position: absolute;
  right: 12rpx;
  top: 50%;
  transform: translateY(-50%);
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.form-cell__input-toggle :deep(.password-eye-icon) {
  width: 36rpx;
  height: 36rpx;
}

.form-cell__input,
.form-cell__value {
  width: 100%;
  font-size: $font-size-md;
  color: $color-text-primary;
  box-sizing: border-box;
  text-align: right;
}

.form-cell__input-wrap .form-cell__input {
  text-align: right;
}

.form-cell__value--muted {
  color: $color-text-secondary;
}

.form-cell__input::placeholder {
  color: $color-text-placeholder;
}

.form-cell__input--masked {
  -webkit-text-security: disc;
  text-security: disc;
}

.form-cell--boxed .form-cell__input-wrap:not(.form-cell__input-wrap--password) .form-cell__input {
  width: 100%;
  min-height: 72rpx;
  padding: 0 24rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-full;
  background: $color-bg-card;
  text-align: left;
  line-height: normal;
  height: 72rpx;
  font-size: $font-size-base;
  box-sizing: border-box;
}

.form-cell--boxed .form-cell__input-wrap--boxed {
  width: 100%;
  min-height: 72rpx;
  padding: 0 20rpx 0 24rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-full;
  background: $color-bg-card;
  box-sizing: border-box;
}

.form-cell--boxed .form-cell__input-wrap--boxed .form-cell__input {
  width: 100%;
  min-height: 70rpx;
  height: 70rpx;
  padding: 0 56rpx 0 0;
  border: none;
  background: transparent;
  text-align: left;
  font-size: $font-size-base;
}

.form-cell--boxed .form-cell__input-wrap--boxed .form-cell__input-toggle {
  right: 20rpx;
}

.form-cell--boxed .form-cell__input::placeholder {
  font-size: $font-size-sm;
  color: $color-text-placeholder;
}

.form-cell--boxed .form-cell__value,
.form-cell--boxed :deep(.picker-value) {
  width: 100%;
  min-height: 72rpx;
  padding: 0 24rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-full;
  background: $color-bg-card;
  text-align: left;
  font-size: $font-size-base;
  line-height: 72rpx;
  box-sizing: border-box;
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

/* #ifdef H5 */
.form-cell__input-wrap--password .form-cell__input::-ms-reveal,
.form-cell__input-wrap--password .form-cell__input::-ms-clear {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
}

.form-cell__input-wrap--password .form-cell__input::-webkit-credentials-auto-fill-button,
.form-cell__input-wrap--password .form-cell__input::-webkit-contacts-auto-fill-button,
.form-cell__input-wrap--password .form-cell__input::-webkit-textfield-decoration-container {
  display: none !important;
  visibility: hidden !important;
  pointer-events: none !important;
  width: 0 !important;
  height: 0 !important;
}
/* #endif */
</style>

