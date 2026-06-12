<template>
  <view class="mine-account page-padded">
    <ListLoading v-if="loading" />
    <template v-else-if="profile">
      <MonitorPanel title="账号信息">
        <DetailRow label="账号 ID" :value="profile.userId" />
        <DetailRow label="登录账号" :value="profile.username" />
        <DetailRow label="昵称" :value="profile.nickname" />
        <DetailRow label="手机号" :value="profile.mobile || '未绑定'" />
        <DetailRow label="邮箱" :value="profile.email || '未填写'" />
        <DetailRow label="账号状态" :value="formatUserStatus(profile.status)" />
      </MonitorPanel>

      <MonitorPanel title="组织信息">
        <DetailRow label="部门" :value="profile.deptName" />
        <DetailRow label="岗位" :value="postText" />
        <DetailRow label="角色" :value="roleText" />
      </MonitorPanel>

      <MonitorPanel title="登录信息">
        <DetailRow label="最近登录" :value="formatDateTime(profile.lastLoginTime, true)" />
        <DetailRow label="登录 IP" :value="profile.lastLoginIp" />
        <DetailRow label="登录地点" :value="profile.lastLoginLocation" />
        <DetailRow label="注册时间" :value="formatDateTime(profile.createTime, true)" />
        <DetailRow label="资料更新" :value="formatDateTime(profile.updateTime, true)" />
      </MonitorPanel>

      <MonitorPanel title="安全提示">
        <view class="mine-account__tips">
          <text v-for="(tip, index) in securityTips" :key="index" class="mine-account__tip">{{ tip }}</text>
        </view>
      </MonitorPanel>
    </template>
    <EmptyState v-else title="加载失败" icon="contact-o" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MonitorPanel from '@/components/common/MonitorPanel/index.vue'
import DetailRow from '@/components/common/DetailRow/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import { getProfile } from '@/api/system/profile'
import { formatDateTime, formatUserStatus } from '@/utils/format'
import type { UserProfile } from '@/types/profile'

const loading = ref(true)
const profile = ref<UserProfile | null>(null)

const postText = computed(() => profile.value?.postNames?.join('、') || '—')
const roleText = computed(() => profile.value?.roleNames?.join('、') || '—')

const securityTips = [
  '请勿将账号密码告知他人或在公共设备勾选「记住我」。',
  '发现陌生登录记录时，请立即修改密码并联系管理员。',
  '部门、岗位、角色由管理员分配，如需调整请联系系统管理员。',
]

onMounted(async () => {
  try {
    const res = await getProfile()
    profile.value = res.data || null
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.mine-account {
  min-height: 100vh;
  box-sizing: border-box;
  padding-bottom: 32rpx;
}

.mine-account__tips {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.mine-account__tip {
  display: block;
  padding: 20rpx 24rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  font-size: $font-size-sm;
  color: $color-text-secondary;
  line-height: 1.55;
}
</style>
