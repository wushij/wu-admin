<template>
  <div class="app-container module-page config-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.config" />
            <span>系统配置</span>
          </div>
          <p class="module-hero-desc">平台运行参数集中维护，修改后请点击底部「保存全部」生效</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ configTabCount }}</div>
          <div class="stat-label">配置分组</div>
        </div>
      </div>
    </el-card>

    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>配置详情</span>
          <span v-if="isDirty" class="dirty-hint">有未保存的修改</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基础信息" name="site">
          <SiteConfigTab :draft="draft.site" :can-edit="canEdit" />
        </el-tab-pane>
        <el-tab-pane label="会话令牌" name="session">
          <SessionConfigTab :draft="draft.session" :can-edit="canEdit" />
        </el-tab-pane>
        <el-tab-pane label="文件存储" name="file">
          <FileConfigTab :draft="draft.file" :can-edit="canEdit" :platform-max-file-mb="platformMaxFileMb" />
        </el-tab-pane>
        <el-tab-pane label="接口限流" name="rateLimit">
          <RateLimitConfigTab :draft="draft.rateLimit" :can-edit="canEdit" />
        </el-tab-pane>
        <el-tab-pane label="登录认证" name="login">
          <LoginConfigTab :draft="draft.login" :can-edit="canEdit" :sms-enabled="draft.sms.enabled" />
        </el-tab-pane>
        <el-tab-pane label="注册认证" name="register">
          <RegisterConfigTab :draft="draft.register" :can-edit="canEdit" :role-options="roleOptions" :user-options="userOptions" />
        </el-tab-pane>
        <el-tab-pane label="第三方配置" name="thirdParty">
          <ThirdPartyConfigTab :draft="draft.thirdParty" :can-edit="canEdit" />
        </el-tab-pane>
        <el-tab-pane label="支付配置" name="payment">
          <PaymentConfigTab
            :draft="draft.payment" :can-edit="canEdit" :payment-testing="paymentTesting"
            :default-wechat-notify-url="defaultWechatNotifyUrl" :default-alipay-notify-url="defaultAlipayNotifyUrl"
            @test-payment="handleTestPayment"
          />
        </el-tab-pane>
        <el-tab-pane label="短信配置" name="sms">
          <SmsConfigTab
            :draft="draft.sms" :can-edit="canEdit"
            :sms-testing="smsTesting"
            v-model:test-sms-phone="testSmsPhone"
            v-model:test-sms-template="testSmsTemplate"
            :recent-sms-logs="recentSmsLogs" :sms-status-text="smsStatusText" :sms-status-tag-type="smsStatusTagType"
            @test-sms="handleTestSms" @show-all-sms-logs="handleShowAllSmsLogs"
          />
        </el-tab-pane>
        <el-tab-pane label="安全配置" name="security">
          <SecurityConfigTab :draft="draft.security" :can-edit="canEdit" v-model:forbid-concurrent-login="forbidConcurrentLogin" />
        </el-tab-pane>
      </el-tabs>

      <div v-if="canEdit" class="footer-actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存全部</el-button>
      </div>
    </el-card>

    <!-- 测试支付弹窗 -->
    <el-dialog v-model="showPaymentModal" title="测试支付" width="420px" :lock-scroll="false" @closed="stopPayPolling">
      <div class="payment-test-modal">
        <div class="payment-info">
          <p>支付方式：{{ paymentResult.type === 'wechat' ? '微信支付' : '支付宝' }}</p>
          <p>订单号：{{ paymentResult.orderNo }}</p>
          <p>金额：<span class="amount">¥ 0.01</span></p>
          <p>
            支付状态：
            <el-tag :type="payOrderStatus === 'PAID' ? 'success' : 'warning'" size="small">
              {{ payOrderStatus === 'PAID' ? '已支付' : '待支付' }}
            </el-tag>
          </p>
        </div>
        <div v-if="paymentResult.qrcode" class="qrcode-container">
          <img :src="paymentResult.qrcode" alt="支付二维码" class="qrcode-img" />
          <p class="qrcode-tip">请使用{{ paymentResult.type === 'wechat' ? '微信' : '支付宝' }}扫码支付</p>
        </div>
        <div class="payment-actions">
          <el-button :loading="payStatusRefreshing" @click="pollPayOrderStatus(true)">刷新支付状态</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 短信记录弹窗 -->
    <el-dialog v-model="showSmsLogsModal" title="短信发送记录" width="860px" :lock-scroll="false" @opened="loadSmsLogs">
      <div class="sms-logs-toolbar">
        <el-input v-model="smsLogsSearch.phone" placeholder="手机号" clearable style="width: 180px" @keyup.enter="handleSearchSmsLogs" />
        <el-select v-model="smsLogsSearch.status" placeholder="发送状态" clearable style="width: 120px">
          <el-option label="成功" :value="1" /><el-option label="失败" :value="2" /><el-option label="发送中" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearchSmsLogs">搜索</el-button>
        <el-button @click="handleResetSmsLogsSearch">重置</el-button>
      </div>
      <el-table v-loading="smsLogsLoading" :data="smsLogsData" size="small" stripe max-height="420">
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="content" label="验证码" width="90" />
        <el-table-column prop="provider" label="服务商" width="88" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="smsStatusTagType(row.status)" size="small">{{ smsStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultMsg" label="结果信息" min-width="140" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发送时间" width="168" />
      </el-table>
      <div class="sms-logs-pagination">
        <el-pagination
          v-model:current-page="smsLogsPagination.page" v-model:page-size="smsLogsPagination.size"
          :total="smsLogsPagination.total" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next" background
          @current-change="loadSmsLogs" @size-change="handleSmsLogsSizeChange"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import { ElMessageBox } from 'element-plus'

const configTabCount = 10
import SiteConfigTab from './components/SiteConfigTab.vue'
import SessionConfigTab from './components/SessionConfigTab.vue'
import FileConfigTab from './components/FileConfigTab.vue'
import RateLimitConfigTab from './components/RateLimitConfigTab.vue'
import LoginConfigTab from './components/LoginConfigTab.vue'
import RegisterConfigTab from './components/RegisterConfigTab.vue'
import ThirdPartyConfigTab from './components/ThirdPartyConfigTab.vue'
import PaymentConfigTab from './components/PaymentConfigTab.vue'
import SmsConfigTab from './components/SmsConfigTab.vue'
import SecurityConfigTab from './components/SecurityConfigTab.vue'
import { useConfigDraft } from './composables/useConfigDraft'
import { usePaymentTest } from './composables/usePaymentTest'
import { useSmsTest } from './composables/useSmsTest'

const {
  canEdit, activeTab, loading, saving, roleOptions, userOptions, platformMaxFileMb,
  draft, isDirty, forbidConcurrentLogin,
  loadAll, handleReset, handleSave,
} = useConfigDraft()

const defaultWechatNotifyUrl = computed(() => `${window.location.origin}/api/pay/notify/wechat`)
const defaultAlipayNotifyUrl = computed(() => `${window.location.origin}/api/pay/notify/alipay`)

const {
  paymentTesting, showPaymentModal, payOrderStatus, payStatusRefreshing,
  paymentResult, pollPayOrderStatus, handleTestPayment,
} = usePaymentTest(() => isDirty.value)

function stopPayPolling() { /* handled by composable onUnmounted */ }

const {
  smsTesting, testSmsPhone, testSmsTemplate, recentSmsLogs,
  showSmsLogsModal, smsLogsLoading, smsLogsData, smsLogsPagination, smsLogsSearch,
  smsStatusText, smsStatusTagType,
  loadRecentSmsLogs, handleTestSms, handleShowAllSmsLogs,
  loadSmsLogs, handleSearchSmsLogs, handleResetSmsLogsSearch, handleSmsLogsSizeChange,
  syncTemplateFromConfig,
} = useSmsTest(() => isDirty.value, () => draft.sms.provider)

watch(activeTab, (tab) => {
  if (tab === 'sms') {
    loadRecentSmsLogs()
    syncTemplateFromConfig(draft.sms.templateVerifyCode)
  }
})

function confirmLeave() {
  return ElMessageBox.confirm('当前有未保存的修改，确定离开吗？', '提示', {
    confirmButtonText: '离开', cancelButtonText: '继续编辑', type: 'warning'
  })
}

onBeforeRouteLeave(async () => {
  if (!isDirty.value) return true
  try { await confirmLeave(); return true } catch { return false }
})

onMounted(() => {
  loadAll()
  if (activeTab.value === 'sms') loadRecentSmsLogs()
})
</script>

<style scoped>
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.dirty-hint { font-size: 13px; color: #e6a23c; }
.footer-actions { margin-top: 20px; padding-top: 16px; border-top: 1px solid #ebeef5; }
.payment-test-modal { text-align: center; }
.payment-test-modal .payment-info { margin-bottom: 20px; text-align: left; padding: 16px; background: #f9fafb; border-radius: 8px; }
.payment-test-modal .payment-info p { margin: 8px 0; color: #374151; }
.payment-test-modal .amount { font-size: 24px; font-weight: 600; color: #ef4444; }
.payment-test-modal .qrcode-container { padding: 20px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; display: inline-block; }
.payment-test-modal .qrcode-img { width: 200px; height: 200px; }
.payment-test-modal .qrcode-tip { margin-top: 12px; color: #6b7280; font-size: 14px; }
.payment-test-modal .payment-actions { margin-top: 20px; display: flex; justify-content: center; }
.sms-logs-toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.sms-logs-pagination { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>

<style src="./styles/config-shared.css"></style>
