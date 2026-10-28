<template>
  <div class="security-config-pane">
    <el-form :model="draft" label-width="140px" class="config-form security-form">
      <el-divider content-position="left">前端安全</el-divider>
      <el-form-item label="禁止前端调试">
        <el-switch :model-value="draft.disableDevtool" :disabled="!canEdit" @update:model-value="(v) => draft.disableDevtool = !!v" />
        <span class="unit">开启后将限制打开开发者工具（F12），降低随意查看源码与调试的风险</span>
      </el-form-item>
      <el-divider content-position="left">会话安全</el-divider>
      <el-form-item label="禁止多端同时在线">
        <el-switch :model-value="forbidConcurrentLogin" :disabled="!canEdit" @update:model-value="(v) => $emit('update:forbidConcurrentLogin', !!v)" />
        <span class="unit">开启后，同一账号再次登录会先踢掉之前的会话，只保留最新一次登录</span>
      </el-form-item>
      <el-divider content-position="left">API 安全防线与防重放</el-divider>
      <el-form-item label="SM2 密码加密">
        <el-switch :model-value="draft.sm2EncryptEnabled ?? true" :disabled="!canEdit" @update:model-value="(v) => draft.sm2EncryptEnabled = !!v" />
        <span class="unit">是否启用登录/注册/重置密码非对称加密传输（国密 SM2 椭圆公钥加密，防止明文口令嗅探，默认开启）</span>
      </el-form-item>
      <el-form-item label="SM4 数据加密">
        <el-switch :model-value="draft.sm4EncryptEnabled ?? false" :disabled="!canEdit" @update:model-value="(v) => draft.sm4EncryptEnabled = !!v" />
        <span class="unit">是否启用接口请求/响应数据加密（国密 SM4-GCM 认证加密，IV[12] + 密文 + Tag[16]）</span>
      </el-form-item>
      <el-form-item label="数字签名验签">
        <el-switch :model-value="draft.sm2SignEnabled ?? false" :disabled="!canEdit" @update:model-value="(v) => draft.sm2SignEnabled = !!v" />
        <span class="unit">是否启用接口签名验签（国密 HMAC-SM3 高性能签名防篡改）</span>
      </el-form-item>
      <el-form-item label="时间戳校验">
        <el-switch :model-value="draft.timestampEnabled ?? true" :disabled="!canEdit" @update:model-value="(v) => draft.timestampEnabled = !!v" />
        <span class="unit">校验请求时间，防止过期请求（5 分钟时间窗口）</span>
      </el-form-item>
      <el-form-item label="Nonce 校验">
        <el-switch :model-value="draft.nonceEnabled ?? true" :disabled="!canEdit" @update:model-value="(v) => draft.nonceEnabled = !!v" />
        <span class="unit">校验随机数，防止高频重放攻击（Redis 单次随机数查重）</span>
      </el-form-item>
      <el-divider content-position="left">行为风控与分级处置</el-divider>
      <el-form-item label="启用风控">
        <el-switch :model-value="draft.riskEnabled ?? true" :disabled="!canEdit" @update:model-value="(v) => draft.riskEnabled = !!v" />
        <span class="unit">总开关，关闭即全部失效：不再累加风险分，已有的账号/IP 封禁、挑战、蜜罐封禁一并不再生效</span>
      </el-form-item>
      <el-form-item label="滑动窗口">
        <el-input-number :model-value="draft.riskWindowMinutes ?? 60" :min="5" :max="1440" :step="10" :disabled="!canEdit || riskOff" @update:model-value="(v) => draft.riskWindowMinutes = Number(v) || 60" />
        <span class="unit">分钟。只统计窗口内的事件，攻击停止后分数自动回落，默认 60</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="关注阈值">
        <el-input-number :model-value="draft.riskNoticeScore ?? 30" :min="10" :max="2000" :step="10" :disabled="!canEdit || riskOff" @update:model-value="(v) => draft.riskNoticeScore = Number(v) || 30" />
        <span class="unit">达到后记录事件便于回溯，默认 30</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="警告阈值">
        <el-input-number :model-value="draft.riskWarnScore ?? 50" :min="10" :max="3000" :step="10" :disabled="!canEdit || riskOff" @update:model-value="(v) => draft.riskWarnScore = Number(v) || 50" />
        <span class="unit">达到后按配置推送告警，默认 50</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="挑战阈值">
        <el-input-number :model-value="draft.riskChallengeScore ?? 70" :min="20" :max="4000" :step="10" :disabled="!canEdit || challengeOff" @update:model-value="(v) => draft.riskChallengeScore = Number(v) || 70" />
        <span class="unit">达到后进入挑战档，默认 70</span>
        <el-tag v-if="challengeOff" size="small" type="info" class="inactive-tag">未启用挑战：不会拦截写操作</el-tag>
      </el-form-item>
      <el-form-item label="临时封禁阈值">
        <el-input-number :model-value="draft.riskBlockScore ?? 90" :min="20" :max="5000" :step="10" :disabled="!canEdit || riskOff" @update:model-value="(v) => draft.riskBlockScore = Number(v) || 90" />
        <span class="unit">风险分达到后临时封禁，默认 90</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="最高档阈值">
        <el-input-number :model-value="draft.riskBanScore ?? 120" :min="40" :max="10000" :step="10" :disabled="!canEdit || riskOff" @update:model-value="(v) => draft.riskBanScore = Number(v) || 120" />
        <span class="unit">风险分达到后按最高档封禁，必须依次高于前四个阈值，默认 120</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="临时封禁时长">
        <el-input-number :model-value="draft.riskBlockSeconds ?? 3600" :min="60" :max="604800" :step="600" :disabled="!canEdit || riskOff" @update:model-value="(v) => draft.riskBlockSeconds = Number(v) || 3600" />
        <span class="unit">秒，默认 3600（1 小时）</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="最高档封禁时长">
        <el-input-number :model-value="draft.riskBanSeconds ?? 86400" :min="60" :max="2592000" :step="3600" :disabled="!canEdit || riskOff" @update:model-value="(v) => draft.riskBanSeconds = Number(v) || 86400" />
        <span class="unit">秒，默认 86400（24 小时）</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="启用挑战">
        <el-switch :model-value="draft.riskChallengeEnabled ?? false" :disabled="!canEdit || riskOff" @update:model-value="(v) => draft.riskChallengeEnabled = !!v" />
        <span class="unit">开启后高风险主体发起写操作需先完成滑块验证（携带 X-Slider-Ticket 重放原请求），默认关闭</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="行为识别">
        <el-switch :model-value="draft.riskBehaviorEnabled ?? true" :disabled="!canEdit || riskOff" @update:model-value="(v) => draft.riskBehaviorEnabled = !!v" />
        <span class="unit">识别翻页遍历与批量导出行为</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="翻页遍历阈值">
        <el-input-number :model-value="draft.riskPageScanThreshold ?? 8" :min="3" :max="200" :step="1" :disabled="!canEdit || behaviorOff" @update:model-value="(v) => draft.riskPageScanThreshold = Number(v) || 8" />
        <span class="unit">60 秒内访问的不同页码数达到该值判定为遍历，默认 8</span>
        <el-tag v-if="behaviorOff" size="small" type="info" class="inactive-tag">当前不生效：行为识别或风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="批量导出阈值">
        <el-input-number :model-value="draft.riskExportThreshold ?? 5" :min="2" :max="100" :step="1" :disabled="!canEdit || behaviorOff" @update:model-value="(v) => draft.riskExportThreshold = Number(v) || 5" />
        <span class="unit">60 秒内导出/下载次数达到该值判定为批量拉取，默认 5</span>
        <el-tag v-if="behaviorOff" size="small" type="info" class="inactive-tag">当前不生效：行为识别或风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="告警 Webhook">
        <el-input :model-value="draft.riskAlertWebhook ?? ''" :disabled="!canEdit || riskOff" placeholder="https://..." clearable @update:model-value="(v) => draft.riskAlertWebhook = String(v || '')" />
        <span class="unit">封禁、挑战、警告事件会 POST 到该地址；留空表示不推送</span>
        <el-tag v-if="riskOff" size="small" type="info" class="inactive-tag">当前不生效：风控总开关已关闭</el-tag>
      </el-form-item>
      <el-divider content-position="left">蜜罐诱捕</el-divider>
      <el-form-item label="启用蜜罐">
        <el-switch :model-value="draft.honeypotEnabled ?? false" :disabled="!canEdit" @update:model-value="(v) => draft.honeypotEnabled = !!v" />
        <span class="unit">开启后 /api/system/internal/ 下的诱饵路径对匿名请求开放，命中即追加风险分并封禁来源 IP（记分与封禁依赖风控总开关）</span>
      </el-form-item>
      <el-form-item label="命中风险分">
        <el-input-number :model-value="draft.honeypotRiskDelta ?? 80" :min="20" :max="500" :disabled="!canEdit || honeypotRiskOff" @update:model-value="(v) => draft.honeypotRiskDelta = Number(v) || 80" />
        <span class="unit">每次命中追加的风险分，默认 80</span>
        <el-tag v-if="honeypotRiskOff" size="small" type="info" class="inactive-tag">当前不生效：蜜罐或风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="封禁时长">
        <el-input-number :model-value="draft.honeypotBlockSeconds ?? 86400" :min="60" :max="604800" :step="3600" :disabled="!canEdit || honeypotRiskOff" @update:model-value="(v) => draft.honeypotBlockSeconds = Number(v) || 86400" />
        <span class="unit">命中后封禁来源 IP 的秒数，默认 86400（24 小时）</span>
        <el-tag v-if="honeypotRiskOff" size="small" type="info" class="inactive-tag">当前不生效：蜜罐或风控总开关已关闭</el-tag>
      </el-form-item>
      <el-form-item label="响应延迟">
        <el-input-number :model-value="draft.honeypotTarpitMs ?? 300" :min="0" :max="3000" :step="100" :disabled="!canEdit || honeypotOff" @update:model-value="(v) => draft.honeypotTarpitMs = Number(v) || 0" />
        <span class="unit">命中后随机延迟的上限（毫秒），用于消耗扫描器并发，默认 300</span>
        <el-tag v-if="honeypotOff" size="small" type="info" class="inactive-tag">当前不生效：蜜罐已关闭</el-tag>
      </el-form-item>
    </el-form>
    <el-alert type="info" :closable="false" show-icon title="保存全部后立即生效：接口安全开关将即时作用于系统所有 REST 接口；时间戳与 Nonce 建议保持开启。" class="security-alert" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ConfigGroupMap } from '@/types/config'

const props = defineProps<{
  draft: ConfigGroupMap['security']
  canEdit: boolean
  forbidConcurrentLogin: boolean
}>()

defineEmits<{
  'update:forbidConcurrentLogin': [value: boolean]
}>()

// 开关联动：上级开关关闭时，依赖它的配置项置灰并提示「当前不生效」，避免保存成功却不起作用而误导运维
const riskOff = computed(() => !(props.draft.riskEnabled ?? true))
const challengeOff = computed(() => riskOff.value || !(props.draft.riskChallengeEnabled ?? false))
const behaviorOff = computed(() => riskOff.value || !(props.draft.riskBehaviorEnabled ?? true))
const honeypotOff = computed(() => !(props.draft.honeypotEnabled ?? false))
// 蜜罐的记分与封禁同时依赖蜜罐开关和风控总开关
const honeypotRiskOff = computed(() => honeypotOff.value || riskOff.value)
</script>

<style scoped>
.config-form.security-form {
  max-width: 980px;
}

.security-form :deep(.el-form-item__content) {
  flex-wrap: nowrap;
}

.security-form .unit {
  margin-left: 10px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary, #909399);
  white-space: nowrap;
  flex-shrink: 0;
}

.security-form .inactive-tag {
  margin-left: 8px;
  flex-shrink: 0;
}

.security-alert {
  max-width: 980px;
  margin-top: 16px;
}
</style>
