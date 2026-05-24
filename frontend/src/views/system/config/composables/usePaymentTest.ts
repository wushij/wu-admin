import { ref, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { testPayment, getPayOrderStatus } from '@/api/system/config'
import { getErrorMessage } from '@/utils/axiosError'

export function usePaymentTest(isDirty: () => boolean) {
  const paymentTesting = ref(false)
  const showPaymentModal = ref(false)
  const payOrderStatus = ref('PENDING')
  const payStatusRefreshing = ref(false)
  const paymentResult = ref({
    type: '' as 'wechat' | 'alipay' | '',
    orderNo: '',
    qrcode: '',
    payUrl: '',
  })
  let payPollTimer: ReturnType<typeof setInterval> | null = null

  function stopPayPolling() {
    if (payPollTimer) { clearInterval(payPollTimer); payPollTimer = null }
  }

  async function pollPayOrderStatus(manual = false) {
    if (!paymentResult.value.orderNo) return
    if (manual) payStatusRefreshing.value = true
    try {
      const res = await getPayOrderStatus(paymentResult.value.orderNo)
      payOrderStatus.value = res.data?.status || 'PENDING'
      if (payOrderStatus.value === 'PAID') {
        stopPayPolling()
        ElMessage.success('支付成功')
      } else if (manual) {
        ElMessage.info('尚未检测到支付成功，请确认已扫码付款后再试')
      }
    } catch {
      if (manual) ElMessage.warning('查询失败，请稍后重试')
    } finally {
      payStatusRefreshing.value = false
    }
  }

  watch(showPaymentModal, (visible) => {
    stopPayPolling()
    if (visible) {
      payOrderStatus.value = 'PENDING'
      pollPayOrderStatus()
      payPollTimer = setInterval(() => pollPayOrderStatus(), 2000)
    }
  })

  onUnmounted(stopPayPolling)

  async function handleTestPayment(type: 'wechat' | 'alipay') {
    if (isDirty()) { ElMessage.warning('请先保存支付配置，再生成测试订单'); return }
    paymentTesting.value = true
    try {
      const res = await testPayment(type)
      paymentResult.value = {
        type, orderNo: res.data.orderNo,
        qrcode: res.data.qrcode || '', payUrl: res.data.payUrl || '',
      }
      showPaymentModal.value = true
    } catch (e) {
      ElMessage.error(getErrorMessage(e) || '创建测试订单失败')
    } finally {
      paymentTesting.value = false
    }
  }

  return {
    paymentTesting, showPaymentModal, payOrderStatus, payStatusRefreshing,
    paymentResult, pollPayOrderStatus, handleTestPayment,
  }
}
