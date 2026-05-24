<template>
  <view class="group-form page-padded">
    <ListLoading v-if="loading" />
    <template v-else>
      <view class="form card">
        <view class="field">
          <text class="field__label">群名称</text>
          <input v-model="form.name" class="field__input" placeholder="请输入群名称" :maxlength="30" />
        </view>
        <view v-if="mode === 'edit'" class="field field--column field--last">
          <text class="field__label">群公告</text>
          <textarea v-model="form.announcement" class="field__textarea" placeholder="选填" />
        </view>
      </view>

      <text class="section-title">{{ mode === 'create' ? '选择成员' : '添加成员' }}</text>
      <view class="member-list card">
        <view
          v-for="user in users"
          :key="user.id"
          class="member-row"
          @click="toggleMember(user.id)"
        >
          <UserAvatar :src="user.avatar" :name="user.nickname || user.username" size="sm" />
          <text class="member-row__name">{{ user.nickname || user.username }}</text>
          <text class="member-row__check">{{ isSelected(user.id) ? '✓' : '' }}</text>
        </view>
      </view>

      <view class="group-form__footer">
        <button
          class="submit"
          :loading="saving"
          @click="mode === 'create' ? submitCreate() : submitEdit()"
        >
          {{ mode === 'create' ? '创建群聊' : '保存' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import ListLoading from '@/components/common/ListLoading/index.vue'
import UserAvatar from '@/components/business/UserAvatar/index.vue'
import { useGroupForm } from '@/composables/useGroupForm'
import { useEditPageGuard } from '@/composables/useEditPageGuard'

const {
  loading,
  saving,
  mode,
  form,
  users,
  selectedIds,
  toggleMember,
  isSelected,
  init,
  submitCreate,
  submitEdit,
} = useGroupForm()

const { resetBaseline } = useEditPageGuard(
  () => ({
    mode: mode.value,
    name: form.name,
    announcement: form.announcement,
    selectedIds: [...selectedIds.value].sort((a, b) => a - b),
  }),
  { loading },
)

onLoad(async (options) => {
  const m = options?.mode || 'create'
  uni.setNavigationBarTitle({ title: m === 'edit' ? '编辑群聊' : '创建群聊' })
  await init({ mode: m, id: options?.id })
  resetBaseline()
})
</script>

<style lang="scss" scoped>
.group-form {
  min-height: 100vh;
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.field {
  display: flex;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1px solid #f0f0f0;
}

.field--column {
  flex-direction: column;
  align-items: stretch;
}

.field--last {
  border-bottom: none;
}

.field__label {
  width: 160rpx;
  font-size: 30rpx;
}

.field__input {
  flex: 1;
  min-width: 0;
  text-align: left;
  font-size: 30rpx;
}

.field__input::placeholder {
  color: #c0c4cc;
}

.field__textarea {
  width: 100%;
  min-height: 160rpx;
  margin-top: 12rpx;
  padding: 16rpx;
  background: #f7f8fa;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.section-title {
  display: block;
  margin: 24rpx 0 16rpx;
  font-size: 28rpx;
  color: #606266;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 32rpx;
  border-bottom: 1px solid #f0f0f0;
}

.member-row__name {
  flex: 1;
  font-size: 30rpx;
}

.member-row__check {
  color: #6366f1;
  font-size: 32rpx;
}

.group-form__footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  background: #fff;
}

.submit {
  background: #010710;
  color: #fff;
  border-radius: 16rpx;
}
</style>
