import { ref, reactive, computed } from 'vue'
import { getProfile, updateProfile, uploadAvatar } from '@/api/system/profile'
import { getConfig } from '@/api/system/auth'
import { fileDisplayUrl } from '@/api/system/file/index'
import { logger } from '@/utils/logger'
import { useUserStore } from '@/store/user'

export function useProfileForm() {
  const loading = ref(false)
  const saving = ref(false)
  const uploading = ref(false)
  const smsEnabled = ref(false)
  const avatarUrl = ref('')
  const form = reactive({
    userId: undefined as number | undefined,
    nickname: '',
    email: '',
    username: '',
    deptName: '',
    mobile: '',
    updateTime: '',
    roleNames: [] as string[],
    postNames: [] as string[],
  })

  const hasBoundMobile = computed(() => /^1[3-9]\d{9}$/.test((form.mobile || '').trim()))

  function maskMobile(mobile?: string) {
    const m = (mobile || '').trim()
    if (!/^1[3-9]\d{9}$/.test(m)) return m || '未绑定手机号'
    return `${m.slice(0, 3)} **** ${m.slice(-4)}`
  }

  async function load() {
    loading.value = true
    try {
      const [cfgRes, profileRes] = await Promise.all([getConfig(), getProfile()])
      smsEnabled.value = cfgRes.data?.login?.smsEnabled === true
      const data = profileRes.data
      form.userId = data.userId
      form.nickname = data.nickname || ''
      form.email = data.email || ''
      form.username = data.username || ''
      form.deptName = data.deptName || ''
      form.mobile = data.mobile || ''
      form.updateTime = data.updateTime || ''
      form.roleNames = data.roleNames || []
      form.postNames = data.postNames || []
      avatarUrl.value = data.avatar ? fileDisplayUrl(data.avatar) : ''
    } catch (e) {
      logger.error(e)
      uni.showToast({ title: '加载资料失败', icon: 'none' })
    } finally {
      loading.value = false
    }
  }

  async function syncUserStore() {
    try {
      await useUserStore().getUserInfo()
    } catch (e) {
      logger.warn('同步用户信息失败', e)
    }
  }

  async function pickAvatar() {
    try {
      const choose = await uni.chooseImage({ count: 1, sizeType: ['compressed'] })
      const filePath = choose.tempFilePaths?.[0]
      if (!filePath) return
      uploading.value = true
      const res = await uploadAvatar(filePath)
      const url = res.data?.url
      if (!url) {
        uni.showToast({ title: '上传失败', icon: 'none' })
        return
      }
      await updateProfile({ avatar: url })
      avatarUrl.value = fileDisplayUrl(url)
      useUserStore().patchUserInfo({ avatar: url })
      await syncUserStore()
      uni.showToast({ title: '头像已更新', icon: 'success' })
    } catch (e) {
      logger.error(e)
      uni.showToast({ title: '头像上传失败', icon: 'none' })
    } finally {
      uploading.value = false
    }
  }

  async function save() {
    if (!form.nickname.trim()) {
      uni.showToast({ title: '请输入昵称', icon: 'none' })
      return
    }
    saving.value = true
    try {
      await updateProfile({
        nickname: form.nickname.trim(),
        email: form.email.trim() || undefined,
      })
      useUserStore().patchUserInfo({ nickname: form.nickname.trim() })
      await syncUserStore()
      uni.showToast({ title: '保存成功', icon: 'success' })
    } catch (e) {
      logger.error(e)
    } finally {
      saving.value = false
    }
  }

  return {
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
  }
}
