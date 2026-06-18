import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  changePassword, sendProfilePasswordSmsCode, resetPasswordBySms,
  sendProfileMobileBindSmsCode, bindProfileMobile,
} from '@/api/system/profile'
import { sliderVerifyToRequest } from '@/types/slider-captcha'
import type { SliderVerifyPayload } from '@/types/slider-captcha'

export function useProfileSecurity(
  getMinPwdLen: () => number,
  getSmsEnabled: () => boolean,
  getHasBoundMobile: () => boolean,
  _getMaskedMobile: () => string,
  getProfileMobile: () => string,
  reloadProfile: () => Promise<void>,
  getInfoFormRef: () => FormInstance | undefined,
  getInfoForm: () => { bindMobile: string; bindSmsCode: string },
) {
  const savingPwd = ref(false)
  const pwdFormRef = ref<FormInstance>()
  const smsPwdFormRef = ref<FormInstance>()
  const securityMode = ref<'password' | 'sms'>('password')
  const sliderTarget = ref<'resetPwd' | 'bindMobile'>('resetPwd')
  const showSliderModal = ref(false)
  const sendingSmsCode = ref(false)
  const sendingBindSmsCode = ref(false)
  const savingSmsPwd = ref(false)
  const bindingMobile = ref(false)
  const smsCountdown = ref(0)
  const bindSmsCountdown = ref(0)
  let smsTimer: ReturnType<typeof setInterval> | null = null
  let bindSmsTimer: ReturnType<typeof setInterval> | null = null

  const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const smsPwdForm = reactive({ smsCode: '', newPassword: '', confirmPassword: '' })

  const canUseSmsReset = computed(() => getSmsEnabled() && getHasBoundMobile())
  const showMobileBindFields = computed(() => getSmsEnabled() && (!getHasBoundMobile() || false))

  const pwdRules = computed<FormRules>(() => ({
    oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: getMinPwdLen(), message: `密码长度不能少于 ${getMinPwdLen()} 位`, trigger: 'blur' },
    ],
    confirmPassword: [
      { required: true, message: '请确认新密码', trigger: 'blur' },
      {
        validator: (_rule: any, value: string, callback: any) => {
          if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
          else callback()
        },
        trigger: 'blur',
      },
    ],
  }))

  const smsPwdRules = computed<FormRules>(() => ({
    smsCode: [
      { required: true, message: '请输入短信验证码', trigger: 'blur' },
      { pattern: /^\d{4,6}$/, message: '请输入正确的验证码', trigger: 'blur' },
    ],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: getMinPwdLen(), message: `密码长度不能少于 ${getMinPwdLen()} 位`, trigger: 'blur' },
    ],
    confirmPassword: [
      { required: true, message: '请确认新密码', trigger: 'blur' },
      {
        validator: (_rule: any, value: string, callback: any) => {
          if (value !== smsPwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
          else callback()
        },
        trigger: 'blur',
      },
    ],
  }))

  const infoRules: FormRules = {
    nickname: [
      { required: true, message: '请输入昵称', trigger: 'blur' },
      { min: 2, max: 30, message: '昵称长度 2～30 个字符', trigger: 'blur' },
    ],
    email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
    bindMobile: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    ],
    bindSmsCode: [
      { required: true, message: '请输入短信验证码', trigger: 'blur' },
      { pattern: /^\d{4,6}$/, message: '请输入正确的验证码', trigger: 'blur' },
    ],
  }

  function startSmsCountdown(seconds = 60) {
    if (smsTimer) { clearInterval(smsTimer); smsTimer = null }
    smsCountdown.value = seconds
    smsTimer = setInterval(() => {
      if (smsCountdown.value <= 1) { smsCountdown.value = 0; if (smsTimer) { clearInterval(smsTimer); smsTimer = null } }
      else smsCountdown.value -= 1
    }, 1000)
  }

  function startBindSmsCountdown(seconds = 60) {
    if (bindSmsTimer) { clearInterval(bindSmsTimer); bindSmsTimer = null }
    bindSmsCountdown.value = seconds
    bindSmsTimer = setInterval(() => {
      if (bindSmsCountdown.value <= 1) { bindSmsCountdown.value = 0; if (bindSmsTimer) { clearInterval(bindSmsTimer); bindSmsTimer = null } }
      else bindSmsCountdown.value -= 1
    }, 1000)
  }

  function openSmsResetMode(setActiveTab: (tab: string) => void, setMobileBindEditing: () => void) {
    if (!getSmsEnabled()) { ElMessage.warning('短信功能未启用，请联系管理员'); return }
    if (!getHasBoundMobile()) {
      ElMessage.warning('请先在「基本资料」中绑定手机号')
      setActiveTab('info')
      if (getSmsEnabled()) setMobileBindEditing()
      return
    }
    securityMode.value = 'sms'
    resetSmsPwdForm()
  }

  function closeSmsResetMode() { securityMode.value = 'password'; resetSmsPwdForm() }

  function resetSmsPwdForm() {
    smsPwdForm.smsCode = ''; smsPwdForm.newPassword = ''; smsPwdForm.confirmPassword = ''
    smsPwdFormRef.value?.clearValidate()
  }

  function resetPwdForm() {
    pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = ''
    pwdFormRef.value?.clearValidate()
  }

  function handleSendResetSmsCode() {
    if (!canUseSmsReset.value || sendingSmsCode.value || smsCountdown.value > 0) return
    sliderTarget.value = 'resetPwd'
    showSliderModal.value = true
  }

  async function handleSendBindSmsCode() {
    if (!getSmsEnabled() || sendingBindSmsCode.value || bindSmsCountdown.value > 0) return
    try { await getInfoFormRef()?.validateField('bindMobile') } catch { return }
    const phone = getInfoForm().bindMobile.trim()
    if (!phone) {
      ElMessage.warning('请输入手机号')
      return
    }
    if (getHasBoundMobile() && phone === getProfileMobile()) {
      ElMessage.warning('新手机号不能与当前绑定的号码相同')
      return
    }
    sliderTarget.value = 'bindMobile'
    showSliderModal.value = true
  }

  async function onSliderSuccess(payload: SliderVerifyPayload) {
    const slider = sliderVerifyToRequest(payload)
    if (sliderTarget.value === 'bindMobile') {
      sendingBindSmsCode.value = true
      try {
        await sendProfileMobileBindSmsCode({ mobile: getInfoForm().bindMobile.trim(), ...slider })
        ElMessage.success('验证码已发送')
        startBindSmsCountdown()
      } catch { /* request 拦截器已提示 */ }
      finally { sendingBindSmsCode.value = false }
      return
    }
    sendingSmsCode.value = true
    try {
      await sendProfilePasswordSmsCode(slider)
      ElMessage.success('验证码已发送')
      startSmsCountdown()
    } catch { /* request 拦截器已提示 */ }
    finally { sendingSmsCode.value = false }
  }

  async function handleBindMobile() {
    if (!getSmsEnabled()) { ElMessage.warning('短信功能未启用，请联系管理员'); return }
    try {
      await getInfoFormRef()?.validateField('bindMobile')
      await getInfoFormRef()?.validateField('bindSmsCode')
    } catch { return }
    bindingMobile.value = true
    try {
      await bindProfileMobile({ mobile: getInfoForm().bindMobile.trim(), smsCode: getInfoForm().bindSmsCode.trim() })
      ElMessage.success(getHasBoundMobile() ? '手机号已更换' : '手机号已绑定')
      await reloadProfile()
    } catch { /* request 拦截器已提示 */ }
    finally { bindingMobile.value = false }
  }

  async function handleChangePassword() {
    const valid = await pwdFormRef.value?.validate().catch(() => false)
    if (!valid) return
    savingPwd.value = true
    try {
      await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword, confirmPassword: pwdForm.confirmPassword })
      ElMessage.success('密码修改成功，请妥善保管新密码')
      resetPwdForm()
    } finally { savingPwd.value = false }
  }

  async function handleSmsResetPassword() {
    const valid = await smsPwdFormRef.value?.validate().catch(() => false)
    if (!valid) return
    savingSmsPwd.value = true
    try {
      await resetPasswordBySms({ smsCode: smsPwdForm.smsCode.trim(), newPassword: smsPwdForm.newPassword, confirmPassword: smsPwdForm.confirmPassword })
      ElMessage.success('密码已重置，请使用新密码登录')
      resetSmsPwdForm()
      securityMode.value = 'password'
    } catch { /* request 拦截器已提示 */ }
    finally { savingSmsPwd.value = false }
  }

  onUnmounted(() => {
    if (smsTimer) { clearInterval(smsTimer); smsTimer = null }
    if (bindSmsTimer) { clearInterval(bindSmsTimer); bindSmsTimer = null }
  })

  return {
    savingPwd, pwdFormRef, smsPwdFormRef, securityMode, sliderTarget, showSliderModal,
    sendingSmsCode, sendingBindSmsCode, savingSmsPwd, bindingMobile,
    smsCountdown, bindSmsCountdown,
    pwdForm, smsPwdForm, canUseSmsReset, showMobileBindFields,
    pwdRules, smsPwdRules, infoRules,
    openSmsResetMode, closeSmsResetMode, resetSmsPwdForm, resetPwdForm,
    handleSendResetSmsCode, handleSendBindSmsCode, onSliderSuccess,
    handleBindMobile, handleChangePassword, handleSmsResetPassword,
  }
}
