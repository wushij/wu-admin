<template>
  <view class="recycle-detail-page" :class="{ 'has-page-footer': (canRestore || canDeletePermanent) && item }">
    <ListLoading v-if="loading" />

    <template v-else-if="item && moduleConfig">
      <view class="recycle-detail-hero card--elevated">
        <view class="recycle-detail-hero__head">
          <UserAvatar
            v-if="heroThumb?.type === 'avatar'"
            :src="String(item.avatar || '')"
            :name="heroTitle"
            size="lg"
          />
          <FileThumb
            v-else-if="heroThumb?.type === 'file' && heroThumb.file"
            :file="heroThumb.file"
            class="recycle-detail-hero__file-thumb"
          />
          <image
            v-else-if="heroThumb?.type === 'image'"
            class="recycle-detail-hero__image"
            :src="heroThumb.src"
            mode="aspectFill"
          />
          <view class="recycle-detail-hero__text">
            <view class="recycle-detail-hero__top">
              <view class="recycle-detail-hero__badge">{{ moduleConfig.label }}</view>
              <text class="recycle-detail-hero__status">已删除</text>
            </view>
            <text class="recycle-detail-hero__title">{{ heroTitle }}</text>
            <text v-if="heroSub" class="recycle-detail-hero__sub">{{ heroSub }}</text>
          </view>
        </view>
      </view>

      <view class="section card--elevated">
        <text class="section__title">详细信息</text>
        <view class="info-grid">
          <view
            v-for="field in moduleConfig.detailFields"
            :key="field.prop"
            class="info-item"
            :class="{ 'info-item--full': field.prop === 'updateTime' }"
          >
            <text class="info-item__label">{{ field.label }}</text>
            <DictTag
              v-if="isRecycleTagField(field)"
              :dict-type="field.dictType"
              :value="item[field.prop] as string | number"
            />
            <text v-else class="info-item__value">{{ formatRecycleFieldValue(item, field) }}</text>
          </view>
        </view>
      </view>

      <view v-if="moduleConfig.hint" class="recycle-detail-hint">
        <text>{{ moduleConfig.hint }}</text>
      </view>
    </template>

    <EmptyState v-else-if="!loading" title="记录不存在或已失效" icon="notes-o" />

    <PageFooter v-if="(canRestore || canDeletePermanent) && item">
      <button v-if="canRestore" class="page-footer__btn page-footer__btn--primary-outline" @click="onRestore">恢复</button>
      <button v-if="canDeletePermanent" class="page-footer__btn page-footer__btn--danger" @click="onDelete">清除</button>
    </PageFooter>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/common/EmptyState/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import UserAvatar from '@/components/business/UserAvatar/index.vue'
import FileThumb from '@/components/business/FileThumb/index.vue'
import PageFooter from '@/components/common/PageFooter/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { RECYCLE_MODULES } from '@/constants/recycle-modules'
import { usePermission } from '@/composables/usePermission'
import { resolveRecycleDetailItem } from '@/utils/recycle-detail-cache'
import { formatRecycleFieldValue, isRecycleTagField } from '@/utils/recycle-field'
import { resolveRecycleRowThumb } from '@/utils/recycle-thumb'
import { showConfirm } from '@/utils/app-dialog'
import { navigateToFallback } from '@/utils/navigate-back'

const { hasPerm } = usePermission()

const recycleType = ref('')
const item = ref<Record<string, unknown> | null>(null)
const loading = ref(true)

const moduleConfig = computed(() => RECYCLE_MODULES.find((m) => m.key === recycleType.value))

const canRestore = computed(() => {
  const mod = moduleConfig.value
  return mod ? hasPerm('system:recycle:restore') || hasPerm(mod.deletePerm) : false
})

const canDeletePermanent = computed(() => {
  const mod = moduleConfig.value
  return mod ? hasPerm('system:recycle:delete') || hasPerm(mod.deletePerm) : false
})

const heroTitle = computed(() => {
  const mod = moduleConfig.value
  const data = item.value
  if (!mod || !data) return '—'
  const raw = data[mod.titleField]
  if (raw != null && String(raw).trim()) return String(raw)
  const id = data.id
  return id != null ? `${mod.label} #${id}` : '—'
})

const heroSub = computed(() => {
  const mod = moduleConfig.value
  const data = item.value
  if (!mod?.subField || !data) return ''
  const raw = data[mod.subField]
  if (raw == null || !String(raw).trim()) return ''
  const label = mod.subFieldLabel || ''
  return label ? `${label}：${raw}` : String(raw)
})

const heroThumb = computed(() => resolveRecycleRowThumb(item.value || {}, moduleConfig.value))

async function onRestore() {
  const mod = moduleConfig.value
  const id = Number(item.value?.id)
  if (!mod || !id) return
  const ok = await showConfirm({
    title: '恢复确认',
    content: `确定恢复该${mod.label}吗？`,
    confirmText: '恢复',
  })
  if (!ok.confirmed) return
  await mod.restore(id)
  uni.showToast({ title: '已恢复', icon: 'success' })
  setTimeout(() => {
    navigateToFallback('/pages-sub/system/recycle/index')
  }, 400)
}

async function onDelete() {
  const mod = moduleConfig.value
  const id = Number(item.value?.id)
  if (!mod || !id) return
  const ok = await showConfirm({
    title: '彻底删除',
    content: '删除后无法恢复，是否继续？',
    confirmText: '删除',
    tone: 'danger',
  })
  if (!ok.confirmed) return
  await mod.deletePermanent(id)
  uni.showToast({ title: '已清除', icon: 'success' })
  setTimeout(() => {
    navigateToFallback('/pages-sub/system/recycle/index')
  }, 400)
}

onLoad(async (options) => {
  recycleType.value = String(options?.type || '')
  const id = Number(options?.id)
  loading.value = true
  try {
    item.value = id ? await resolveRecycleDetailItem(recycleType.value, id) : null
  } finally {
    loading.value = false
  }
  const mod = moduleConfig.value
  uni.setNavigationBarTitle({
    title: mod ? `${mod.label}回收详情` : '回收详情',
  })
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.recycle-detail-page {
  min-height: 100vh;
  padding: $page-padding-y $page-padding-x;
  box-sizing: border-box;
}

.recycle-detail-hero {
  padding: 28rpx 32rpx;
  margin-bottom: $section-gap;
}

.recycle-detail-hero__head {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.recycle-detail-hero__file-thumb :deep(.file-thumb) {
  width: 120rpx;
  height: 120rpx;
  border-radius: 20rpx;
}

.recycle-detail-hero__image {
  width: 120rpx;
  height: 120rpx;
  flex-shrink: 0;
  border-radius: 20rpx;
  background: $color-bg-muted;
  border: 1px solid $color-border-light;
}

.recycle-detail-hero__text {
  flex: 1;
  min-width: 0;
}

.recycle-detail-hero__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.recycle-detail-hero__badge {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-size: $font-size-xs;
  color: $color-primary;
  background: rgba(99, 102, 241, 0.1);
}

.recycle-detail-hero__status {
  font-size: $font-size-xs;
  color: $color-danger;
}

.recycle-detail-hero__title {
  display: block;
  font-size: $font-size-xl;
  font-weight: $font-weight-bold;
  color: $color-text-primary;
  line-height: 1.4;
}

.recycle-detail-hero__sub {
  display: block;
  margin-top: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-secondary;
}

.section {
  padding: 28rpx 32rpx;
}

.section__title {
  display: block;
  margin-bottom: 24rpx;
  font-size: $font-size-md;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
}

.info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 24rpx 16rpx;
}

.info-item {
  width: calc(50% - 8rpx);
  min-width: 0;
}

.info-item--full {
  width: 100%;
}

.info-item__label {
  display: block;
  margin-bottom: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.info-item__value {
  display: block;
  font-size: $font-size-base;
  color: $color-text-primary;
  line-height: 1.45;
  word-break: break-all;
}

.recycle-detail-hint {
  margin-top: $section-gap;
  padding: 20rpx 24rpx;
  border-radius: $radius-md;
  background: rgba(230, 162, 60, 0.12);
  border: 1px solid rgba(230, 162, 60, 0.22);
  font-size: $font-size-xs;
  color: $color-warning;
  line-height: 1.55;
}
</style>
