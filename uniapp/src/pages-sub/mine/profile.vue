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
      <view class="form-panel card--elevated">
        <FormCell label="用户名" :display-value="form.username || '—'" muted />
        <FormCell v-model="form.nickname" label="昵称" editable placeholder="请输入昵称" />
        <FormCell v-model="form.email" label="邮箱" editable placeholder="选填" />
        <FormCell
          label="手机号"
          :hint="!smsEnabled && !hasBoundMobile ? '短信功能未启用，无法绑定手机号' : undefined"
          last
        >
          <view class="mobile-row">
            <text class="mobile-row__value">
              {{ hasBoundMobile ? maskMobile(form.mobile) : '未绑定' }}
            </text>
            <text v-if="smsEnabled" class="mobile-row__link" @click.stop="goMobileBind">
              {{ hasBoundMobile ? '更换手机号' : '绑定手机号' }}
            </text>
          </view>
        </FormCell>
      </view>

      <text class="profile-page__section">组织信息</text>
      <view class="form-panel card--elevated">
        <FormCell label="所属部门" :display-value="form.deptName || '未分配'" muted />
        <FormCell label="岗位" :display-value="postText" muted last />
      </view>
    </template>

    <PageFooter>
      <button class="page-footer__btn" :loading="saving || uploading" @click="save">保存资料</button>
    </PageFooter>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import IconFont from '@/components/common/IconFont/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
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
@import '@/styles/common.scss';

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

.mobile-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  width: 100%;
}

.mobile-row__value {
  font-size: $font-size-md;
  color: $color-text-secondary;
  text-align: right;
}

.mobile-row__link {
  flex-shrink: 0;
  font-size: $font-size-sm;
  font-weight: $font-weight-semibold;
  color: $color-primary;
}
</style>
