<template>
  <view class="profile-page">
    <ListLoading v-if="loading" />

    <template v-else>
      <view class="profile-hero" @click="pickAvatar">
        <view class="profile-hero__pattern" />
        <view class="profile-hero__glow" />
        <view class="profile-hero__body">
          <view class="profile-hero__avatar">
            <image
              v-if="avatarUrl && !avatarBroken"
              class="profile-hero__img"
              :src="avatarUrl"
              mode="aspectFill"
              @error="avatarBroken = true"
            />
            <view v-else class="profile-hero__fallback">
              <text>{{ avatarFallback }}</text>
            </view>
            <view class="profile-hero__edit">
              <IconFont name="edit" :size="22" color="#ffffff" />
            </view>
          </view>
          <text class="profile-hero__name">{{ form.nickname || form.username || '用户' }}</text>
          <text class="profile-hero__hint">点击更换头像</text>
        </view>
      </view>

      <view class="profile-section card--elevated">
        <view class="profile-section__head">
          <ModuleIcon icon="contact-o" theme="indigo" size="sm" />
          <text class="profile-section__title">基本信息</text>
        </view>

        <view class="profile-fields">
          <view class="profile-field profile-field--readonly">
            <text class="profile-field__label">用户名</text>
            <text class="profile-field__value profile-field__value--muted">{{ form.username || '—' }}</text>
          </view>

          <view class="profile-field">
            <text class="profile-field__label">昵称</text>
            <input
              v-model="form.nickname"
              class="profile-field__input"
              placeholder="请输入昵称"
              maxlength="30"
            />
          </view>

          <view class="profile-field">
            <text class="profile-field__label">邮箱</text>
            <input
              v-model="form.email"
              class="profile-field__input"
              placeholder="选填"
              maxlength="64"
            />
          </view>

          <view class="profile-field profile-field--last">
            <text class="profile-field__label">手机号</text>
            <text v-if="mobileHint" class="profile-field__hint">{{ mobileHint }}</text>
            <view class="profile-field__mobile">
              <text class="profile-field__value">
                {{ hasBoundMobile ? maskMobile(form.mobile) : '未绑定' }}
              </text>
              <text v-if="smsEnabled" class="profile-field__link" @click.stop="goMobileBind">
                {{ hasBoundMobile ? '更换手机号' : '绑定手机号' }}
              </text>
            </view>
          </view>
        </view>
      </view>

      <view class="profile-section card--elevated">
        <view class="profile-section__head">
          <ModuleIcon icon="cluster-o" theme="dept" size="sm" />
          <text class="profile-section__title">组织信息</text>
        </view>

        <view class="profile-fields">
          <view class="profile-field profile-field--readonly">
            <text class="profile-field__label">所属部门</text>
            <text class="profile-field__value">{{ form.deptName || '未分配' }}</text>
          </view>

          <view class="profile-field profile-field--readonly profile-field--last">
            <text class="profile-field__label">岗位</text>
            <text class="profile-field__value">{{ postText }}</text>
          </view>
        </view>
      </view>
    </template>

    <PageFooter>
      <button class="profile-save-btn" :loading="saving || uploading" @click="save">保存资料</button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import IconFont from '@/components/common/IconFont/index.vue'
import ModuleIcon from '@/components/common/ModuleIcon/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import { useProfileForm } from '@/composables/useProfileForm'
import { appendNavFromParam } from '@/utils/nav-from'

const avatarBroken = ref(false)

const {
  loading,
  saving,
  uploading,
  smsEnabled,
  avatarUrl,
  form,
  hasBoundMobile,
  maskMobile,
  load,
  pickAvatar,
  save,
} = useProfileForm()

const avatarFallback = computed(() => (form.nickname || form.username || 'U').slice(0, 1).toUpperCase())
const postText = computed(() => (form.postNames?.length ? form.postNames.join('、') : '未分配'))

const mobileHint = computed(() => {
  if (!smsEnabled.value && !hasBoundMobile.value) {
    return '短信功能未启用，无法绑定手机号'
  }
  return ''
})

function goMobileBind() {
  if (!smsEnabled.value) {
    uni.showToast({ title: '短信功能未启用', icon: 'none' })
    return
  }
  uni.navigateTo({ url: appendNavFromParam('/pages-sub/mine/mobile-bind') })
}

onMounted(load)
onShow(load)
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/mine.scss';

.profile-page {
  @include mine-page-bg;
  min-height: 100vh;
  padding: $page-padding-y $page-padding-x calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.profile-hero {
  position: relative;
  margin-bottom: $section-gap;
  border-radius: $radius-xl;
  overflow: hidden;
  background: linear-gradient(135deg, #010710 0%, #0f1a2e 55%, #1a1040 100%);
  box-shadow: 0 12rpx 40rpx rgba(0, 0, 0, 0.14);
}

.profile-hero__pattern {
  position: absolute;
  inset: 0;
  opacity: 0.07;
  background-image: radial-gradient(rgba(255, 255, 255, 0.8) 1px, transparent 1px);
  background-size: 32rpx 32rpx;
  pointer-events: none;
}

.profile-hero__glow {
  position: absolute;
  top: -30%;
  right: -8%;
  width: 260rpx;
  height: 260rpx;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.28) 0%, transparent 70%);
  pointer-events: none;
}

.profile-hero__body {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx 40rpx;
}

.profile-hero__avatar {
  position: relative;
  padding: 6rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #a78bfa);
}

.profile-hero__img,
.profile-hero__fallback {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  display: block;
}

.profile-hero__fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4f46e5, #818cf8);
  color: #fff;
  font-size: 64rpx;
  font-weight: $font-weight-bold;
}

.profile-hero__edit {
  position: absolute;
  right: 4rpx;
  bottom: 4rpx;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(79, 70, 229, 0.92);
  border: 3rpx solid #fff;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
}

.profile-hero__name {
  margin-top: 24rpx;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: #fff;
  line-height: 1.3;
}

.profile-hero__hint {
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: rgba(255, 255, 255, 0.55);
}

.profile-section {
  padding: 28rpx 28rpx 8rpx;
  margin-bottom: $card-gap;
}

.profile-section__head {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 8rpx;
}

.profile-section__title {
  font-size: $font-size-md;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
}

.profile-fields {
  display: flex;
  flex-direction: column;
}

.profile-field {
  padding: 24rpx 0;
  border-bottom: 1px solid $color-border-light;

  &--last {
    border-bottom: none;
    padding-bottom: 16rpx;
  }
}

.profile-field__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-xs;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;
  letter-spacing: 0.02em;
}

.profile-field__hint {
  display: block;
  margin: -4rpx 0 12rpx;
  font-size: 20rpx;
  color: $color-text-placeholder;
  line-height: 1.4;
}

.profile-field__value {
  display: block;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  line-height: 1.45;
  word-break: break-all;

  &--muted {
    font-weight: $font-weight-semibold;
    color: $color-text-regular;
  }
}

.profile-field__input {
  width: 100%;
  min-height: 80rpx;
  padding: 0 24rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.03) 0%, $color-bg-muted 100%);
  font-size: $font-size-md;
  color: $color-text-primary;
  box-sizing: border-box;
}

.profile-field__input::placeholder {
  color: $color-text-placeholder;
}

.profile-field--readonly .profile-field__value {
  padding: 16rpx 24rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.profile-field__mobile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  padding: 16rpx 24rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
}

.profile-field__mobile .profile-field__value {
  flex: 1;
  min-width: 0;
  padding: 0;
  background: transparent;
}

.profile-field__link {
  flex-shrink: 0;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-primary;
}

.profile-save-btn {
  @include mine-primary-btn;
  width: 100%;
}
</style>
