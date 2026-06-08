<template>
  <div>
    <el-collapse class="pay-collapse">
      <el-collapse-item title="微信支付" name="wechatPay">
        <el-form :model="draft.wechatPay" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
          <el-form-item label="启用"><el-switch v-model="draft.wechatPay.enabled" :disabled="!canEdit" /></el-form-item>
          <el-form-item label="商户号">
            <el-input v-model="draft.wechatPay.mchId" name="wxpay-mch-id" autocomplete="off" placeholder="请输入商户号" :disabled="!canEdit" />
          </el-form-item>
          <el-form-item label="AppID">
            <el-input v-model="draft.wechatPay.appId" name="wxpay-app-id" autocomplete="off" placeholder="微信支付 AppID（非登录账号）" :disabled="!canEdit" />
          </el-form-item>
          <el-form-item label="APIv3 密钥">
            <el-input v-model="draft.wechatPay.apiV3Key" name="wxpay-api-v3-key" type="password" show-password autocomplete="new-password" placeholder="32 位 APIv3 密钥" :disabled="!canEdit" />
          </el-form-item>
          <el-form-item label="商户私钥">
            <el-input v-model="draft.wechatPay.privateKey" name="wxpay-private-key" type="textarea" :rows="4" autocomplete="off" :disabled="!canEdit" />
          </el-form-item>
          <el-form-item label="证书序列号">
            <el-input v-model="draft.wechatPay.certSerialNo" name="wxpay-cert-serial" autocomplete="off" placeholder="证书序列号" :disabled="!canEdit" />
          </el-form-item>
          <el-form-item label="回调地址">
            <el-input v-model="draft.wechatPay.notifyUrl" name="wxpay-notify-url" autocomplete="off" :placeholder="defaultWechatNotifyUrl" :disabled="!canEdit" />
            <span class="unit">须公网 HTTPS，对应 POST /api/pay/notify/wechat</span>
          </el-form-item>
          <el-form-item v-if="draft.wechatPay.enabled && canEdit" label="测试支付">
            <el-button type="primary" :loading="paymentTesting" @click="$emit('testPayment', 'wechat')">生成测试订单</el-button>
          </el-form-item>
        </el-form>
      </el-collapse-item>
      <el-collapse-item title="支付宝支付" name="alipayPay">
        <el-form :model="draft.alipay" label-width="120px" class="config-form wide-form" autocomplete="off" @submit.prevent>
          <el-form-item label="启用"><el-switch v-model="draft.alipay.enabled" :disabled="!canEdit" /></el-form-item>
          <el-form-item label="AppID">
            <el-input v-model="draft.alipay.appId" name="alipay-app-id" autocomplete="off" placeholder="支付宝 AppID" :disabled="!canEdit" />
          </el-form-item>
          <el-form-item label="应用私钥">
            <el-input v-model="draft.alipay.privateKey" name="alipay-private-key" type="textarea" :rows="4" autocomplete="off" :disabled="!canEdit" />
          </el-form-item>
          <el-form-item label="支付宝公钥">
            <el-input v-model="draft.alipay.publicKey" name="alipay-public-key" type="textarea" :rows="4" autocomplete="off" :disabled="!canEdit" />
          </el-form-item>
          <el-form-item label="签名类型">
            <el-select v-model="draft.alipay.signType" style="width: 160px" :disabled="!canEdit">
              <el-option label="RSA2" value="RSA2" /><el-option label="RSA" value="RSA" />
            </el-select>
          </el-form-item>
          <el-form-item label="网关地址">
            <el-select v-model="draft.alipay.gatewayUrl" style="width: 100%; max-width: 480px" :disabled="!canEdit">
              <el-option label="正式环境" value="https://openapi.alipay.com/gateway.do" />
              <el-option label="沙箱环境" value="https://openapi-sandbox.dl.alipaydev.com/gateway.do" />
            </el-select>
          </el-form-item>
          <el-form-item label="回调地址">
            <el-input v-model="draft.alipay.notifyUrl" :placeholder="defaultAlipayNotifyUrl" :disabled="!canEdit" />
            <span class="unit">须公网 HTTPS，对应 POST /api/pay/notify/alipay</span>
          </el-form-item>
          <el-form-item v-if="draft.alipay.enabled && canEdit" label="测试支付">
            <el-button type="primary" :loading="paymentTesting" @click="$emit('testPayment', 'alipay')">生成测试订单</el-button>
          </el-form-item>
        </el-form>
      </el-collapse-item>
    </el-collapse>
    <el-alert type="info" :closable="false" show-icon class="pay-tip-alert">填写后务必点击页底「保存全部」。</el-alert>
    <el-alert type="info" :closable="false" show-icon class="pay-tip-alert">
      测试订单金额固定 0.01 元。保存支付配置后再生成订单；支付完成后弹窗会每 2 秒向微信/支付宝主动查单并更新状态。
    </el-alert>
  </div>
</template>

<script setup lang="ts">
import type { ConfigGroupMap } from '@/types/config'

defineProps<{
  draft: ConfigGroupMap['payment']
  canEdit: boolean
  paymentTesting: boolean
  defaultWechatNotifyUrl: string
  defaultAlipayNotifyUrl: string
}>()

defineEmits<{
  testPayment: [type: 'wechat' | 'alipay']
}>()
</script>
