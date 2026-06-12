<template>
  <view class="profile-page">
    <ListLoading v-if="loading" />

    <template v-else>
      <view class="avatar-card" @click="pickAvatar">
        <view class="avatar-card__ring">
          <image
            v-if="avatarUrl && !avatarBroken"
            class="avatar-card__img"
            :src="avatarUrl"
            mode="aspectFill"
            @error="avatarBroken = true"
          />
          <view v-else class="avatar-card__fallback">
            <text>{{ avatarFallback }}</text>
          </view>
        </view>
        <view class="avatar-card__action">
          <IconFont name="edit" :size="28" color="#4f46e5" />
          <text>更换头像</text>
        </view>
      </view>

      <text class="profile-page__section">基本信息</text>
      <view class="form-card">
        <view class="field field--readonly">
          <text class="field__label">用户名</text>
          <text class="field__value field__value--muted">{{ form.username || '—' }}</text>
        </view>
        <view class="field">
          <text class="field__label">昵称</text>
          <input v-model="form.nickname" class="field__input" placeholder="请输入昵称" />
        </view>
        <view class="field">
          <text class="field__label">邮箱</text>
          <input v-model="form.email" class="field__input" placeholder="选填" />
        </view>
        <view class="field field--last field--readonly">
          <text class="field__label">手机号</text>
          <text class="field__value field__value--muted">{{ hasMobile ? maskMobile(form.mobile) : '未绑定' }}</text>
        </view>
      </view>

      <text class="profile-page__section">组织信息</text>
      <view class="form-card">
        <view class="field field--readonly">
          <text class="field__label">所属部门</text>
          <text class="field__value field__value--muted">{{ form.deptName || '未分配' }}</text>
        </view>
        <view class="field field--last field--readonly">
          <text class="field__label">岗位</text>
          <text class="field__value field__value--muted">
            {{ form.postNames?.length ? form.postNames.join('、') : '未分配' }}
          </text>
        </view>
      </view>

      <template v-if="canBindMobile">
        <text class="profile-page__section">绑定手机号</text>
        <view class="form-card">
          <view class="field">
            <text class="field__label">手机号</text>
            <input
              v-model="form.bindMobile"
              class="field__input"
              type="number"
              :maxlength="11"
              placeholder="11 位手机号"
            />
          </view>
          <view class="field field--last">
            <text class="field__label">验证码</text>
            <view class="field__row">
              <input
                v-model="form.bindSmsCode"
                class="field__input field__input--grow"
                :maxlength="6"
                placeholder="短信验证码"
              />
              <button
                class="field__sms-btn"
                :disabled="bindSmsCountdown > 0 || sendingBindSms"
                :loading="sendingBindSms"
                @click="sendBindSmsCode"
              >
                {{ bindSmsCountdown > 0 ? `${bindSmsCountdown}s` : '获取' }}
              </button>
            </view>
          </view>
        </view>
        <button class="profile-page__bind" :loading="saving" @click="bindMobile">确认绑定</button>
      </template>
    </template>

    <view class="profile-page__footer">
      <button class="profile-page__submit" :loading="saving || uploading" @click="save">保存资料</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import IconFont from '@/components/common/IconFont/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import { useProfileForm } from '@/composables/useProfileForm'

const avatarBroken = ref(false)

const {
  loading,
  saving,
  uploading,
  sendingBindSms,
  bindSmsCountdown,
  avatarUrl,
  form,
  hasMobile,
  canBindMobile,
  maskMobile,
  load,
  pickAvatar,
  sendBindSmsCode,
  bindMobile,
  save,
} = useProfileForm()

const avatarFallback = computed(() => (form.nickname || form.username || 'U').slice(0, 1).toUpperCase())

onMounted(load)
onShow(load)
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
@import '@/styles/mine.scss';

.profile-page {
  @include mine-page-bg;
  min-height: 100vh;
  padding: 24rpx 24rpx calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.avatar-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 32rpx 36rpx;
  margin-bottom: 8rpx;
  @include mine-card;
}

.avatar-card__ring {
  padding: 8rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5, #a78bfa);
}

.avatar-card__img,
.avatar-card__fallback {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  display: block;
}

.avatar-card__fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #818cf8);
  color: #fff;
  font-size: 64rpx;
  font-weight: $font-weight-bold;
}

.avatar-card__action {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 20rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-primary;
}

.profile-page__section {
  @include mine-section-title;
  margin-top: 24rpx;
}

.form-card {
  padding: 8rpx 0;
  @include mine-card;
}

.field {
  padding: 28rpx 32rpx;
  border-bottom: 1px solid $color-border-light;

  &--last {
    border-bottom: none;
  }

  &--readonly {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24rpx;
  }
}

.field__label {
  display: block;
  margin-bottom: 16rpx;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-text-secondary;

  .field--readonly & {
    margin-bottom: 0;
    flex-shrink: 0;
  }
}

.field__value {
  font-size: $font-size-md;
  color: $color-text-primary;
  text-align: right;

  &--muted {
    color: $color-text-secondary;
  }
}

.field__input {
  width: 100%;
  height: 80rpx;
  padding: 0 24rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  font-size: $font-size-md;
  color: $color-text-primary;
  box-sizing: border-box;
}

.field__input--grow {
  flex: 1;
}

.field__row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.field__sms-btn {
  flex-shrink: 0;
  min-width: 140rpx;
  height: 80rpx;
  line-height: 80rpx;
  margin: 0;
  padding: 0 20rpx;
  border-radius: $radius-md;
  background: $color-primary-muted;
  color: $color-primary;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
}

.profile-page__bind {
  width: 100%;
  height: 80rpx;
  line-height: 80rpx;
  margin-top: 20rpx;
  border-radius: $radius-lg;
  background: $color-primary-muted;
  color: $color-primary;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
}

.profile-page__footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
}

.profile-page__submit {
  width: 100%;
  @include mine-primary-btn;
}
</style>
