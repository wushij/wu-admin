import { ref, reactive, computed, onUnmounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  changePassword, sendProfilePasswordSmsCode, resetPasswordBySms,
  sendProfileMobileBindSmsCode, bindProfileMobile,
  sendProfileEmailBindCode, bindProfileEmail,
  sendProfilePasswordEmailCode, resetPasswordByEmail,
} from '@/api/system/profile'
import { sliderVerifyToRequest } from '@/types/slider-captcha'
import type { SliderVerifyPayload } from '@/types/slider-captcha'

export function useProfileSecurity(
  getMinPwdLen: () => number,
  getSmsEnabled: () => boolean,
  getHasBoundMobile: () => boolean,
  _getMaskedMobile: () => string,
  getProfileMobile: () => string,
  getHasBoundEmail: () => boolean,
  _getMaskedEmail: () => string,
  reloadProfile: () => Promise<void>,
  getInfoFormRef: () => FormInstance | undefined,
  getInfoForm: () => { bindMobile: string; bindSmsCode: string; bindEmail: string; bindEmailCode: string },
  getPwdFormRef: () => FormInstance | undefined,
  getSmsPwdFormRef: () => FormInstance | undefined,
  getEmailPwdFormRef?: () => FormInstance | undefined,
) {
  const savingPwd = ref(false)
  const securityMode = ref<'password' | 'sms' | 'email'>('password')
  const sliderTarget = ref<'resetPwd' | 'bindMobile'>('resetPwd')
  const showSliderModal = ref(false)
  const sendingSmsCode = ref(false)
  const sendingBindSmsCode = ref(false)
  const sendingEmailResetCode = ref(false)
  const savingSmsPwd = ref(false)
  const savingEmailPwd = ref(false)
  const bindingMobile = ref(false)
  const smsCountdown = ref(0)
  const emailResetCountdown = ref(0)
  const bindSmsCountdown = ref(0)
  let smsTimer: ReturnType<typeof setInterval> | null = null
  let emailResetTimer: ReturnType<typeof setInterval> | null = null
  let bindSmsTimer: ReturnType<typeof setInterval> | null = null

  const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const smsPwdForm = reactive({ smsCode: '', newPassword: '', confirmPassword: '' })
  const emailPwdForm = reactive({ emailCode: '', newPassword: '', confirmPassword: '' })

  const canUseSmsReset = computed(() => getSmsEnabled() && getHasBoundMobile())
  const canUseEmailReset = computed(() => getHasBoundEmail())
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

  const emailPwdRules = computed<FormRules>(() => ({
    emailCode: [
      { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
      { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' },
    ],
    newPassword: [
      { required: true, message: '请输入新密码', trigger: 'blur' },
      { min: getMinPwdLen(), message: `密码长度不能少于 ${getMinPwdLen()} 位`, trigger: 'blur' },
    ],
    confirmPassword: [
      { required: true, message: '请确认新密码', trigger: 'blur' },
      {
        validator: (_rule: any, value: string, callback: any) => {
          if (value !== emailPwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
          else callback()
        },
        trigger: 'blur',
      },
    ],
  }))

  const sendingBindEmailCode = ref(false)
  const bindingEmail = ref(false)
  const bindEmailCountdown = ref(0)
  let bindEmailTimer: ReturnType<typeof setInterval> | null = null

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
    bindEmail: [
      { required: true, message: '请输入邮箱地址', trigger: 'blur' },
      { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
    ],
    bindEmailCode: [
      { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
      { pattern: /^\d{6}$/, message: '请输入 6 位数字验证码', trigger: 'blur' },
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

  function startEmailResetCountdown(seconds = 60) {
    if (emailResetTimer) { clearInterval(emailResetTimer); emailResetTimer = null }
    emailResetCountdown.value = seconds
    emailResetTimer = setInterval(() => {
      if (emailResetCountdown.value <= 1) { emailResetCountdown.value = 0; if (emailResetTimer) { clearInterval(emailResetTimer); emailResetTimer = null } }
      else emailResetCountdown.value -= 1
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

  function openEmailResetMode(setActiveTab: (tab: string) => void, setEmailBindEditing: () => void) {
    if (!getHasBoundEmail()) {
      ElMessage.warning('请先在「基本资料」中绑定邮箱')
      setActiveTab('info')
      setEmailBindEditing()
      return
    }
    securityMode.value = 'email'
    resetEmailPwdForm()
  }

  function closeSmsResetMode() { securityMode.value = 'password'; resetSmsPwdForm() }
  function closeEmailResetMode() { securityMode.value = 'password'; resetEmailPwdForm() }

  function resetSmsPwdForm() {
    smsPwdForm.smsCode = ''; smsPwdForm.newPassword = ''; smsPwdForm.confirmPassword = ''
    getSmsPwdFormRef()?.clearValidate()
  }

  function resetEmailPwdForm() {
    emailPwdForm.emailCode = ''; emailPwdForm.newPassword = ''; emailPwdForm.confirmPassword = ''
    getEmailPwdFormRef?.()?.clearValidate()
  }

  function resetPwdForm() {
    pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = ''
    getPwdFormRef()?.clearValidate()
  }

  function handleSendResetSmsCode() {
    if (!canUseSmsReset.value || sendingSmsCode.value || smsCountdown.value > 0) return
    sliderTarget.value = 'resetPwd'
    showSliderModal.value = true
  }

  async function handleSendResetEmailCode() {
    if (!canUseEmailReset.value || sendingEmailResetCode.value || emailResetCountdown.value > 0) return
    sendingEmailResetCode.value = true
    try {
      await sendProfilePasswordEmailCode()
      ElMessage.success('验证码已发送至您的邮箱，请注意查收')
      startEmailResetCountdown()
    } finally {
      sendingEmailResetCode.value = false
    }
  }

  async function handleEmailResetPassword() {
    const valid = await getEmailPwdFormRef?.()?.validate().catch(() => false)
    if (!valid) return
    savingEmailPwd.value = true
    try {
      await resetPasswordByEmail({
        emailCode: emailPwdForm.emailCode.trim(),
        newPassword: emailPwdForm.newPassword,
        confirmPassword: emailPwdForm.confirmPassword,
      })
      ElMessage.success('密码已通过邮箱验证成功重置，请妥善保管新密码')
      resetEmailPwdForm()
      securityMode.value = 'password'
    } catch { /* request 拦截器已提示 */ }
    finally { savingEmailPwd.value = false }
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

  async function handleBindMobile(onSuccess?: () => void) {
    if (!getSmsEnabled()) { ElMessage.warning('短信功能未启用，请联系管理员'); return }
    try {
      await getInfoFormRef()?.validateField('bindMobile')
      await getInfoFormRef()?.validateField('bindSmsCode')
    } catch { return }
    bindingMobile.value = true
    try {
      await bindProfileMobile({ mobile: getInfoForm().bindMobile.trim(), smsCode: getInfoForm().bindSmsCode.trim() })
      ElMessage.success(getHasBoundMobile() ? '手机号已更换' : '手机号已绑定')
      if (onSuccess) onSuccess()
      await reloadProfile()
    } catch { /* request 拦截器已提示 */ }
    finally { bindingMobile.value = false }
  }

  async function handleChangePassword() {
    const valid = await getPwdFormRef()?.validate().catch(() => false)
    if (!valid) return
    savingPwd.value = true
    try {
      await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword, confirmPassword: pwdForm.confirmPassword })
      ElMessage.success('密码修改成功，请妥善保管新密码')
      resetPwdForm()
    } finally { savingPwd.value = false }
  }

  async function handleSmsResetPassword() {
    const valid = await getSmsPwdFormRef()?.validate().catch(() => false)
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

  function startBindEmailCountdown(seconds = 60) {
    if (bindEmailTimer) { clearInterval(bindEmailTimer); bindEmailTimer = null }
    bindEmailCountdown.value = seconds
    bindEmailTimer = setInterval(() => {
      if (bindEmailCountdown.value <= 1) { bindEmailCountdown.value = 0; if (bindEmailTimer) { clearInterval(bindEmailTimer); bindEmailTimer = null } }
      else bindEmailCountdown.value -= 1
    }, 1000)
  }

  async function handleSendBindEmailCode() {
    try {
      await getInfoFormRef()?.validateField('bindEmail')
    } catch { return }
    const email = getInfoForm().bindEmail.trim()
    sendingBindEmailCode.value = true
    try {
      await sendProfileEmailBindCode({ email })
      ElMessage.success('验证码已发送至新邮箱，请注意查收')
      startBindEmailCountdown()
    } finally {
      sendingBindEmailCode.value = false
    }
  }

  async function handleBindEmail(onSuccess?: () => void) {
    try {
      await getInfoFormRef()?.validateField('bindEmail')
      await getInfoFormRef()?.validateField('bindEmailCode')
    } catch { return }
    bindingEmail.value = true
    try {
      await bindProfileEmail({
        email: getInfoForm().bindEmail.trim(),
        code: getInfoForm().bindEmailCode.trim(),
      })
      ElMessage.success('邮箱绑定成功')
      if (onSuccess) onSuccess()
      await reloadProfile()
    } finally {
      bindingEmail.value = false
    }
  }

  onUnmounted(() => {
    if (smsTimer) { clearInterval(smsTimer); smsTimer = null }
    if (emailResetTimer) { clearInterval(emailResetTimer); emailResetTimer = null }
    if (bindSmsTimer) { clearInterval(bindSmsTimer); bindSmsTimer = null }
    if (bindEmailTimer) { clearInterval(bindEmailTimer); bindEmailTimer = null }
  })

  return {
    savingPwd, securityMode, sliderTarget, showSliderModal,
    sendingSmsCode, sendingBindSmsCode, sendingBindEmailCode, sendingEmailResetCode, savingSmsPwd, savingEmailPwd, bindingMobile, bindingEmail,
    smsCountdown, emailResetCountdown, bindSmsCountdown, bindEmailCountdown,
    pwdForm, smsPwdForm, emailPwdForm, canUseSmsReset, canUseEmailReset, showMobileBindFields,
    pwdRules, smsPwdRules, emailPwdRules, infoRules,
    openSmsResetMode, closeSmsResetMode, openEmailResetMode, closeEmailResetMode, resetSmsPwdForm, resetEmailPwdForm, resetPwdForm,
    handleSendResetSmsCode, handleSendResetEmailCode, handleSendBindSmsCode, handleSendBindEmailCode, onSliderSuccess,
    handleBindMobile, handleBindEmail, handleChangePassword, handleSmsResetPassword, handleEmailResetPassword,
  }
}
