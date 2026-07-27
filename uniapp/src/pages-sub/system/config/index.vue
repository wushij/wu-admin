<template>
  <PermissionBlock v-if="!allowed" />
  <view v-else class="page-padded config-page">
    <ModuleDarkHero title="系统配置" subtitle="修改后请点击「保存全部」生效" icon="setting-o" theme="config" />
    <SegmentTabs v-model="tab" :tabs="tabs" scroll />

    <ListLoading v-if="loading" />

    <scroll-view v-else scroll-y class="config-page__scroll">
      <!-- 站点 -->
      <view v-if="tab === 'site'" class="form-panel">
        <FormCell v-model="siteDraft.platformName" label="平台名称" editable boxed :disabled="!canEdit" />
        <FormCell v-model="siteDraft.platformSubtitle" label="平台副标题" editable boxed :disabled="!canEdit" />
        <FormCell v-model="siteDraft.loginWelcome" label="登录页标题" editable boxed :disabled="!canEdit" />
        <FormCell v-model="siteDraft.registerTitle" label="注册页标题" editable boxed :disabled="!canEdit" />
        <FormCell v-model="siteDraft.copyright" label="页脚版权" editable boxed placeholder="选填" :disabled="!canEdit" last />
      </view>

      <!-- 会话 -->
      <view v-else-if="tab === 'session'" class="form-panel form-panel--session">
        <FormCell label="Token 有效期" last>
          <NumberStepper v-model="sessionDraft.tokenExpireHours" :min="1" :max="720" :disabled="!canEdit" />
          <text class="config-unit">小时</text>
        </FormCell>
      </view>

      <!-- 文件 -->
      <view v-else-if="tab === 'file'" class="form-panel form-panel--file">
        <FormCell label="单文件上限">
          <NumberStepper v-model="fileDraft.maxSizeMb" :min="1" :max="platformMaxFileMb" :disabled="!canEdit" />
          <text class="config-unit">MB（最高 {{ platformMaxFileMb }}）</text>
        </FormCell>
        <FormCell label="允许扩展名" class="form-cell--stacked" last>
          <textarea
            v-model="fileDraft.allowedExtensions"
            class="config-boxed-textarea"
            :disabled="!canEdit"
            placeholder="逗号分隔，如 jpg,png,pdf"
          />
        </FormCell>
      </view>

      <!-- 限流 -->
      <view v-else-if="tab === 'rateLimit'" class="form-panel form-panel--rate">
        <FormCell label="验证码(次/分/IP)">
          <NumberStepper v-model="rateDraft.captchaPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
        </FormCell>
        <FormCell label="登录(次/分/IP)">
          <NumberStepper v-model="rateDraft.loginPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
        </FormCell>
        <FormCell label="注册(次/分/IP)">
          <NumberStepper v-model="rateDraft.registerPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
        </FormCell>
        <text class="config-section">短信发送防刷</text>
        <FormCell label="短信(次/分/IP)">
          <NumberStepper v-model="rateDraft.smsPerIpMinute" :min="0" :max="200" :disabled="!canEdit" />
        </FormCell>
        <FormCell label="同号发送间隔(秒)">
          <NumberStepper v-model="rateDraft.smsSendIntervalSeconds" :min="30" :max="300" :disabled="!canEdit" />
        </FormCell>
        <FormCell label="同号每日上限(次)">
          <NumberStepper v-model="rateDraft.smsPerPhoneDaily" :min="0" :max="500" :disabled="!canEdit" />
        </FormCell>
        <FormCell label="同IP每日上限(次)" last>
          <NumberStepper v-model="rateDraft.smsPerIpDaily" :min="0" :max="500" :disabled="!canEdit" />
        </FormCell>
      </view>

      <!-- 登录 -->
      <view v-else-if="tab === 'login'" class="form-panel">
        <FormCell label="登录人机校检" switch-cell>
          <switch :checked="loginDraft.captchaEnabled" :disabled="!canEdit" @change="onLoginSwitch('captchaEnabled', $event)" />
        </FormCell>
        <FormCell v-if="loginDraft.captchaEnabled" label="验证码类型">
          <ConfigRadioGroup
            v-model="loginDraft.captchaType"
            :options="captchaTypeOptions"
            :disabled="!canEdit"
          />
        </FormCell>
        <FormCell label="短信验证码登录" switch-cell>
          <switch :checked="loginDraft.smsLoginEnabled" :disabled="!canEdit" @change="onLoginSwitch('smsLoginEnabled', $event)" />
        </FormCell>
        <FormCell v-if="loginDraft.smsLoginEnabled" label="发送前滑块验证" switch-cell>
          <switch :checked="loginDraft.smsLoginSliderCaptchaEnabled" :disabled="!canEdit" @change="onLoginSwitch('smsLoginSliderCaptchaEnabled', $event)" />
        </FormCell>
        <view v-if="loginDraft.smsLoginEnabled && !smsDraft.enabled" class="config-warn">
          <text>请先在「短信」中开启短信功能，否则无法保存</text>
        </view>
        <FormCell label="邮箱验证码登录" switch-cell>
          <switch :checked="loginDraft.emailLoginEnabled" :disabled="!canEdit" @change="onLoginSwitch('emailLoginEnabled', $event)" />
        </FormCell>
        <FormCell v-if="loginDraft.emailLoginEnabled" label="邮箱发送前滑块" switch-cell>
          <switch :checked="loginDraft.emailLoginSliderCaptchaEnabled" :disabled="!canEdit" @change="onLoginSwitch('emailLoginSliderCaptchaEnabled', $event)" />
        </FormCell>
        <view v-if="loginDraft.emailLoginEnabled && !emailDraft.enabled" class="config-warn">
          <text>请先在「邮件」中开启邮件功能，否则无法保存</text>
        </view>
        <FormCell label="记住我" switch-cell>
          <switch :checked="loginDraft.rememberMe" :disabled="!canEdit" @change="onLoginSwitch('rememberMe', $event)" />
        </FormCell>
        <FormCell label="账号最大重试">
          <NumberStepper v-model="loginDraft.maxRetryCount" :min="1" :max="20" :disabled="!canEdit" />
        </FormCell>
        <FormCell label="IP 最大重试">
          <NumberStepper v-model="loginDraft.maxRetryCountIp" :min="1" :max="50" :disabled="!canEdit" />
        </FormCell>
        <FormCell label="锁定时长" last>
          <view class="config-control-row">
            <NumberStepper v-model="loginDraft.lockTime" :min="1" :max="120" :disabled="!canEdit" />
            <text class="config-unit">分钟</text>
          </view>
        </FormCell>
      </view>

      <!-- 注册 -->
      <view v-else-if="tab === 'register'" class="form-panel">
        <FormCell label="开放注册" switch-cell>
          <switch :checked="registerDraft.enabled" :disabled="!canEdit" @change="onRegisterSwitch('enabled', $event)" />
        </FormCell>
        <FormCell label="注册验证码" switch-cell>
          <switch :checked="registerDraft.captchaEnabled" :disabled="!canEdit || !registerDraft.enabled" @change="onRegisterSwitch('captchaEnabled', $event)" />
        </FormCell>
        <FormCell v-if="registerDraft.enabled && registerDraft.captchaEnabled" label="验证码类型">
          <ConfigRadioGroup
            v-model="registerDraft.captchaType"
            :options="captchaTypeOptions"
            :disabled="!canEdit"
          />
        </FormCell>
        <FormCell label="密码最小长度">
          <NumberStepper v-model="registerDraft.minPasswordLength" :min="6" :max="32" :disabled="!canEdit || !registerDraft.enabled" />
        </FormCell>
        <FormCell label="默认角色" clickable boxed arrow @click="pickDefaultRole">
          <text class="picker-value">{{ defaultRoleLabel }}</text>
        </FormCell>
        <FormCell label="注册需审核" switch-cell :last="!registerDraft.needAudit">
          <switch :checked="registerDraft.needAudit" :disabled="!canEdit || !registerDraft.enabled" @change="onRegisterSwitch('needAudit', $event)" />
        </FormCell>
        <FormCell
          v-if="registerDraft.needAudit"
          label="审核人"
          clickable
          boxed
          arrow
          last
          @click="pickRegisterAuditors"
        >
          <text class="picker-value">{{ registerAuditorLabel }}</text>
        </FormCell>
      </view>

      <!-- 第三方 -->
      <view v-else-if="tab === 'thirdParty'" class="config-collapse-wrap">
        <CollapsePanel title="微信登录">
          <view class="form-panel form-panel--flat">
            <FormCell label="启用" switch-cell><switch :checked="thirdDraft.wechat.enabled" :disabled="!canEdit" @change="onThirdSwitch('wechat', 'enabled', $event)" /></FormCell>
            <FormCell v-model="thirdDraft.wechat.appId" label="AppID" editable boxed :disabled="!canEdit" />
            <FormCell v-model="thirdDraft.wechat.appSecret" label="AppSecret" editable boxed password :disabled="!canEdit" last />
          </view>
        </CollapsePanel>
        <CollapsePanel title="支付宝登录">
          <view class="form-panel form-panel--flat">
            <FormCell label="启用" switch-cell><switch :checked="thirdDraft.alipay.enabled" :disabled="!canEdit" @change="onThirdSwitch('alipay', 'enabled', $event)" /></FormCell>
            <FormCell v-model="thirdDraft.alipay.appId" label="AppID" editable boxed :disabled="!canEdit" />
            <view class="config-textarea">
              <text class="config-textarea__label">应用私钥</text>
              <textarea v-model="thirdDraft.alipay.privateKey" class="config-textarea__input" :disabled="!canEdit" />
            </view>
            <view class="config-textarea">
              <text class="config-textarea__label">支付宝公钥</text>
              <textarea v-model="thirdDraft.alipay.publicKey" class="config-textarea__input" :disabled="!canEdit" />
            </view>
          </view>
        </CollapsePanel>
        <CollapsePanel title="GitHub 登录">
          <view class="form-panel form-panel--flat">
            <FormCell label="启用" switch-cell><switch :checked="thirdDraft.github.enabled" :disabled="!canEdit" @change="onThirdSwitch('github', 'enabled', $event)" /></FormCell>
            <FormCell v-model="thirdDraft.github.clientId" label="Client ID" editable boxed :disabled="!canEdit" />
            <FormCell v-model="thirdDraft.github.clientSecret" label="Client Secret" editable boxed password :disabled="!canEdit" last />
          </view>
        </CollapsePanel>
        <CollapsePanel title="Google 登录">
          <view class="form-panel form-panel--flat">
            <FormCell label="启用" switch-cell><switch :checked="thirdDraft.google.enabled" :disabled="!canEdit" @change="onThirdSwitch('google', 'enabled', $event)" /></FormCell>
            <FormCell v-model="thirdDraft.google.clientId" label="Client ID" editable boxed :disabled="!canEdit" />
            <FormCell v-model="thirdDraft.google.clientSecret" label="Client Secret" editable boxed password :disabled="!canEdit" />
            <FormCell v-model="thirdDraft.google.redirectUri" label="重定向 URI" editable boxed :disabled="!canEdit" last />
            <text class="config-hint config-hint--inline">须在 Google Cloud 控制台配置相同重定向 URI</text>
          </view>
        </CollapsePanel>
      </view>

      <!-- 支付 -->
      <view v-else-if="tab === 'payment'" class="config-collapse-wrap">
        <CollapsePanel title="微信支付">
          <view class="form-panel form-panel--flat">
            <FormCell label="启用" switch-cell><switch :checked="paymentDraft.wechatPay.enabled" :disabled="!canEdit" @change="onPaySwitch('wechatPay', 'enabled', $event)" /></FormCell>
            <FormCell v-model="paymentDraft.wechatPay.mchId" label="商户号" editable boxed :disabled="!canEdit" />
            <FormCell v-model="paymentDraft.wechatPay.appId" label="AppID" editable boxed :disabled="!canEdit" />
            <FormCell v-model="paymentDraft.wechatPay.apiV3Key" label="APIv3 密钥" editable boxed password :disabled="!canEdit" />
            <view class="config-textarea">
              <text class="config-textarea__label">商户私钥</text>
              <textarea v-model="paymentDraft.wechatPay.privateKey" class="config-textarea__input" :disabled="!canEdit" />
            </view>
            <FormCell v-model="paymentDraft.wechatPay.certSerialNo" label="证书序列号" editable boxed :disabled="!canEdit" />
            <FormCell v-model="paymentDraft.wechatPay.notifyUrl" label="回调地址" editable boxed :disabled="!canEdit" last />
            <text class="config-hint config-hint--inline">须公网 HTTPS，对应 POST /api/pay/notify/wechat</text>
            <button v-if="canEdit && paymentDraft.wechatPay.enabled" class="config-test-btn" :loading="paymentTesting" @click="sendTestPayment('wechat')">测试微信支付</button>
          </view>
        </CollapsePanel>
        <CollapsePanel title="支付宝支付">
          <view class="form-panel form-panel--flat">
            <FormCell label="启用" switch-cell><switch :checked="paymentDraft.alipay.enabled" :disabled="!canEdit" @change="onPaySwitch('alipay', 'enabled', $event)" /></FormCell>
            <FormCell v-model="paymentDraft.alipay.appId" label="AppID" editable boxed :disabled="!canEdit" />
            <view class="config-textarea">
              <text class="config-textarea__label">应用私钥</text>
              <textarea v-model="paymentDraft.alipay.privateKey" class="config-textarea__input" :disabled="!canEdit" />
            </view>
            <view class="config-textarea">
              <text class="config-textarea__label">支付宝公钥</text>
              <textarea v-model="paymentDraft.alipay.publicKey" class="config-textarea__input" :disabled="!canEdit" />
            </view>
            <FormCell label="签名类型" clickable boxed arrow @click="pickAlipaySign">
              <text class="picker-value">{{ paymentDraft.alipay.signType }}</text>
            </FormCell>
            <FormCell label="网关环境" clickable boxed arrow @click="pickAlipayGateway">
              <text class="picker-value">{{ alipayGatewayLabel }}</text>
            </FormCell>
            <FormCell v-model="paymentDraft.alipay.notifyUrl" label="回调地址" editable boxed :disabled="!canEdit" />
            <FormCell v-model="paymentDraft.alipay.returnUrl" label="同步跳转" editable boxed :disabled="!canEdit" last />
            <text class="config-hint config-hint--inline">须公网 HTTPS，对应 POST /api/pay/notify/alipay</text>
            <button v-if="canEdit && paymentDraft.alipay.enabled" class="config-test-btn" :loading="paymentTesting" @click="sendTestPayment('alipay')">测试支付宝</button>
          </view>
        </CollapsePanel>
        <view class="config-info">
          <text>填写后务必点击页底「保存全部」。</text>
          <text>测试订单金额固定 0.01 元，保存配置后再生成订单。</text>
        </view>
      </view>

      <!-- 短信 -->
      <view v-else-if="tab === 'sms'" class="config-sms-wrap">
        <ConfigSectionCard title="基础配置">
          <view class="form-panel form-panel--flat">
            <FormCell label="启用短信" switch-cell>
              <switch :checked="smsDraft.enabled" :disabled="!canEdit" @change="onSmsSwitch('enabled', $event)" />
            </FormCell>
            <FormCell label="短信服务商" clickable boxed arrow @click="pickProvider">
              <text class="picker-value">{{ providerLabel }}</text>
            </FormCell>
            <FormCell v-model="smsDraft.accessKeyId" label="AccessKeyId" editable boxed :disabled="!canEdit" />
            <FormCell v-model="smsDraft.accessKeySecret" label="AccessKeySecret" editable boxed password :disabled="!canEdit" />
            <FormCell v-model="smsDraft.signName" label="签名" editable boxed :disabled="!canEdit" />
            <FormCell v-if="smsDraft.provider === 'tencent'" v-model="smsDraft.tencentAppId" label="腾讯云 AppId" editable boxed :disabled="!canEdit" />
            <FormCell v-if="smsDraft.provider === 'aliyunAuth'" label="验证码有效期(分钟)">
              <NumberStepper v-model="smsDraft.codeExpireMinutes" :min="1" :max="30" :disabled="!canEdit" />
            </FormCell>
          </view>
        </ConfigSectionCard>

        <ConfigSectionCard title="模板配置">
          <view class="form-panel form-panel--flat">
            <template v-if="smsDraft.provider === 'aliyunAuth'">
              <FormCell v-model="smsDraft.templateVerifyCode" label="登录/注册" editable boxed :disabled="!canEdit" />
              <FormCell v-model="smsDraft.templateModifyPhone" label="修改绑定手机" editable boxed :disabled="!canEdit" />
              <FormCell v-model="smsDraft.templateResetPassword" label="重置密码" editable boxed :disabled="!canEdit" />
              <FormCell v-model="smsDraft.templateBindPhone" label="绑定新手机" editable boxed :disabled="!canEdit" />
              <FormCell v-model="smsDraft.templateVerifyBindPhone" label="验证绑定手机" editable boxed :disabled="!canEdit" last />
            </template>
            <template v-else>
              <FormCell v-model="smsDraft.templateVerifyCode" label="验证码模板 ID" editable boxed :disabled="!canEdit" />
              <FormCell v-model="smsDraft.templateResetPassword" label="重置密码模板 ID" editable boxed :disabled="!canEdit" last />
            </template>
          </view>
        </ConfigSectionCard>

        <ConfigSectionCard v-if="canEdit" title="测试发送">
          <view class="form-panel form-panel--flat">
            <FormCell v-if="smsDraft.provider === 'aliyunAuth'" label="模板" clickable boxed arrow @click="pickTestTemplate">
              <text class="picker-value">{{ testTemplateLabel }}</text>
            </FormCell>
            <view class="sms-test__row">
              <input v-model="testSmsPhone" class="sms-test__input" type="number" :maxlength="11" placeholder="11 位手机号" />
              <button class="sms-test__btn" :loading="smsTesting" @click="sendTestSms">发送</button>
            </view>
            <text class="config-hint config-hint--inline">将发送随机 6 位验证码，用于测试配置是否正确</text>
          </view>
        </ConfigSectionCard>

        <ConfigSectionCard title="发送记录">
          <template #extra>
            <text class="sms-logs__more" @click="openSmsLogs">查看全部</text>
          </template>
          <view class="sms-logs-list">
            <ListCard v-for="log in recentSmsLogs" :key="log.id || `${log.phone}-${log.createTime}`">
              <view class="list-card__top">
                <text class="list-card__title">{{ log.phone }}</text>
                <DictTag :label="smsStatusText(log.status)" :effect="log.status === 1 ? 'success' : log.status === 2 ? 'danger' : 'warning'" />
              </view>
              <text class="list-card__sub">验证码 {{ log.content || '—' }} · {{ log.createTime || '—' }}</text>
            </ListCard>
            <EmptyState v-if="!recentSmsLogs.length" title="暂无发送记录" icon="contact-o" />
          </view>
        </ConfigSectionCard>

        <view class="config-info">
          <text>填写密钥与模板后请点击页底「保存全部」，保存后再测试发送。</text>
        </view>
      </view>

      <!-- 邮件 -->
      <view v-else-if="tab === 'email'" class="config-security-wrap">
        <ConfigSectionCard title="SMTP 基础配置">
          <view class="form-panel form-panel--flat">
            <FormCell label="启用邮件服务" switch-cell>
              <switch :checked="emailDraft.enabled" :disabled="!canEdit" @change="emailDraft.enabled = $event.detail.value" />
            </FormCell>
            <FormCell label="邮件服务商" clickable boxed arrow @click="pickEmailProvider">
              <text class="picker-value">{{ emailProviderLabel }}</text>
            </FormCell>
            <FormCell v-model="emailDraft.username" label="发件邮箱账号" editable boxed placeholder="如 wu@gmail.com" :disabled="!canEdit" />
            <FormCell v-model="emailDraft.password" label="SMTP 授权码" editable boxed placeholder="秘钥/授权码" password :disabled="!canEdit" />
            <FormCell v-model="emailDraft.host" label="SMTP 服务器" editable boxed placeholder="如 smtp.qq.com" :disabled="!canEdit" />
            <FormCell label="SMTP 端口">
              <NumberStepper v-model="emailDraft.port" :min="1" :max="65535" :disabled="!canEdit" />
            </FormCell>
            <FormCell label="加密传输方式" clickable boxed arrow @click="pickEmailSecurityType">
              <text class="picker-value">{{ emailSecurityLabel }}</text>
            </FormCell>
            <FormCell label="SMTP 认证" switch-cell>
              <switch :checked="emailDraft.authEnabled" :disabled="!canEdit" @change="emailDraft.authEnabled = $event.detail.value" />
            </FormCell>
            <FormCell v-model="emailDraft.fromName" label="发件人显示名称" editable boxed placeholder="如 wu-admin 系统团队" :disabled="!canEdit" last />
          </view>
        </ConfigSectionCard>

        <ConfigSectionCard title="高级网络与超时配置">
          <view class="form-panel form-panel--flat">
            <FormCell label="连接超时(ms)">
              <NumberStepper v-model="emailDraft.connectionTimeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" />
            </FormCell>
            <FormCell label="读取超时(ms)">
              <NumberStepper v-model="emailDraft.timeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" />
            </FormCell>
            <FormCell label="写入超时(ms)">
              <NumberStepper v-model="emailDraft.writeTimeoutMs" :min="1000" :max="30000" :step="1000" :disabled="!canEdit" />
            </FormCell>
            <FormCell label="默认字符编码" clickable boxed arrow @click="pickEmailEncoding">
              <text class="picker-value">{{ emailEncodingLabel }}</text>
            </FormCell>
            <FormCell label="Debug 日志" switch-cell last>
              <switch :checked="emailDraft.debug" :disabled="!canEdit" @change="emailDraft.debug = $event.detail.value" />
            </FormCell>
          </view>
        </ConfigSectionCard>

        <ConfigSectionCard title="验证码与防刷规则">
          <view class="form-panel form-panel--flat">
            <FormCell label="验证码有效期">
              <NumberStepper v-model="emailDraft.codeExpireMinutes" :min="1" :max="30" :disabled="!canEdit" />
              <text class="config-unit">分钟</text>
            </FormCell>
            <FormCell label="验证码长度">
              <NumberStepper v-model="emailDraft.codeLength" :min="4" :max="8" :disabled="!canEdit" />
              <text class="config-unit">位</text>
            </FormCell>
            <FormCell label="防刷间隔">
              <NumberStepper v-model="emailDraft.sendIntervalSeconds" :min="10" :max="300" :disabled="!canEdit" />
              <text class="config-unit">秒</text>
            </FormCell>
            <FormCell label="单箱每日上限" last>
              <NumberStepper v-model="emailDraft.dailyLimitPerEmail" :min="1" :max="100" :disabled="!canEdit" />
              <text class="config-unit">次</text>
            </FormCell>
          </view>
        </ConfigSectionCard>

        <ConfigSectionCard title="测试发送邮件">
          <view class="form-panel form-panel--flat">
            <view class="sms-test__row">
              <input v-model="testEmailTo" class="sms-test__input" placeholder="请输入接收测试邮箱" />
              <button class="sms-test__btn" :disabled="!canEdit || emailTesting" @click="sendTestEmailAction">
                {{ emailTesting ? '发送中...' : '测试发送' }}
              </button>
            </view>
            <text class="config-hint config-hint--inline">发送测试报文到该邮箱，验证 SMTP 配置与安全端口连通性</text>
          </view>
        </ConfigSectionCard>

        <ConfigSectionCard title="发送记录">
          <template #extra>
            <text class="sms-logs__more" @click="openEmailLogs">查看全部</text>
          </template>
          <view class="sms-logs-list">
            <ListCard v-for="log in recentEmailLogs" :key="log.id || `${log.email}-${log.createTime}`">
              <view class="list-card__top">
                <text class="list-card__title">{{ log.email }}</text>
                <DictTag :label="emailStatusText(log.status)" :effect="log.status === 1 ? 'success' : log.status === 2 ? 'danger' : 'warning'" />
              </view>
              <text class="list-card__sub">{{ log.subject || '系统邮件' }} · {{ log.createTime || '—' }}</text>
            </ListCard>
            <EmptyState v-if="!recentEmailLogs.length" title="暂无发送记录" icon="contact-o" />
          </view>
        </ConfigSectionCard>
      </view>

      <!-- 安全 -->
      <view v-else-if="tab === 'security'" class="config-security-wrap">
        <ConfigSectionCard title="前端安全">
          <view class="form-panel form-panel--flat">
            <FormCell
              label="禁止前端调试"
              hint="开启后将限制打开开发者工具，需刷新页面生效"
              switch-cell
              last
            >
              <switch :checked="securityDraft.disableDevtool" :disabled="!canEdit" @change="onSecuritySwitch('disableDevtool', $event)" />
            </FormCell>
          </view>
        </ConfigSectionCard>
        <ConfigSectionCard title="会话安全">
          <view class="form-panel form-panel--flat">
            <FormCell
              label="禁止多端同时在线"
              hint="开启后，同一账号再次登录会先踢掉之前的会话"
              switch-cell
              last
            >
              <switch :checked="forbidConcurrentLogin" :disabled="!canEdit" @change="onForbidConcurrentChange" />
            </FormCell>
          </view>
        </ConfigSectionCard>
        <ConfigSectionCard title="API 安全防线与防重放">
          <view class="form-panel form-panel--flat">
            <FormCell
              label="SM4 数据加密"
              hint="是否启用接口请求/响应数据加密（国密 SM4-CBC 模式 + 16 字节随机 IV 向量）"
              switch-cell
            >
              <switch :checked="securityDraft.sm4EncryptEnabled" :disabled="!canEdit" @change="onSecuritySwitch('sm4EncryptEnabled', $event)" />
            </FormCell>
            <FormCell
              label="数字签名验签"
              hint="是否启用接口签名验签（国密 HMAC-SM3 高性能签名防篡改）"
              switch-cell
            >
              <switch :checked="securityDraft.sm2SignEnabled" :disabled="!canEdit" @change="onSecuritySwitch('sm2SignEnabled', $event)" />
            </FormCell>
            <FormCell
              label="时间戳校验"
              hint="校验请求时间，防止过期请求（5分钟时间窗口）"
              switch-cell
            >
              <switch :checked="securityDraft.timestampEnabled ?? true" :disabled="!canEdit" @change="onSecuritySwitch('timestampEnabled', $event)" />
            </FormCell>
            <FormCell
              label="Nonce 校验"
              hint="校验随机数，防止高频重放攻击（Redis 查重）"
              switch-cell
              last
            >
              <switch :checked="securityDraft.nonceEnabled ?? true" :disabled="!canEdit" @change="onSecuritySwitch('nonceEnabled', $event)" />
            </FormCell>
          </view>
        </ConfigSectionCard>
        <view class="config-info">
          <text>保存全部后立即生效：接口安全开关即时作用于全部 REST 接口；时间戳与 Nonce 建议保持开启。</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="canEdit && !loading" class="config-page-footer">
      <view v-if="isDirty" class="config-dirty config-dirty--footer">
        <text>有未保存的修改</text>
      </view>
      <view class="config-page-footer__actions">
        <button class="page-footer__btn page-footer__btn--ghost" @click="resetAll">重置</button>
        <button class="page-footer__btn" :loading="saving" @click="saveAll">保存全部</button>
      </view>
    </view>

    <!-- 支付测试弹窗 -->
    <view v-if="showPaymentModal" class="pay-modal" @click="closePaymentModal">
      <view class="pay-modal__card" @click.stop>
        <text class="pay-modal__title">测试支付</text>
        <text class="pay-modal__row">方式：{{ paymentResult.type === 'wechat' ? '微信支付' : '支付宝' }}</text>
        <text class="pay-modal__row">订单：{{ paymentResult.orderNo }}</text>
        <text class="pay-modal__row">金额：¥ 0.01</text>
        <text class="pay-modal__row">状态：{{ payOrderStatus === 'PAID' ? '已支付' : '待支付' }}</text>
        <image v-if="paymentResult.qrcode" class="pay-modal__qr" :src="paymentResult.qrcode" mode="aspectFit" />
        <button class="config-test-btn" :loading="payStatusRefreshing" @click="pollPayOrderStatus(true)">刷新支付状态</button>
        <button class="pay-modal__close" @click="closePaymentModal">关闭</button>
      </view>
    </view>

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/common/ModuleDarkHero/index.vue'
import SegmentTabs from '@/components/common/SegmentTabs/index.vue'
import ListLoading from '@/components/common/ListLoading/index.vue'
import FormCell from '@/components/common/FormCell/index.vue'
import ListCard from '@/components/common/ListCard/index.vue'
import DictTag from '@/components/common/DictTag/index.vue'
import EmptyState from '@/components/common/EmptyState/index.vue'
import PermissionBlock from '@/components/common/PermissionBlock/index.vue'
import CollapsePanel from '@/components/common/CollapsePanel/index.vue'
import ConfigSectionCard from '@/components/common/ConfigSectionCard/index.vue'
import NumberStepper from '@/components/common/NumberStepper/index.vue'
import ConfigRadioGroup from '@/components/common/ConfigRadioGroup/index.vue'
import AppDialogHost from '@/components/common/AppDialogHost/index.vue'
import { useConfigEditor } from '@/composables/useConfigEditor'
import { useUnsavedLeaveGuard } from '@/composables/useUnsavedLeaveGuard'
import { useModulePermission } from '@/composables/useModulePermission'
import type { EmailAdminConfig } from '@/types/config-types'
import { showActionSheet } from '@/utils/app-dialog'
import { appendNavFromParam } from '@/utils/nav-from'
import { consumePagePickerResult, clearPagePickerResult } from '@/utils/page-picker-result'
import { setUserSelectMultiIds } from '@/utils/user-select-multi-init'

const { allowed, hasPerm } = useModulePermission('system:config:list')
const canEdit = computed(() => hasPerm('system:config:update'))

const tab = ref('site')
const tabs = [
  { key: 'site', label: '基础' },
  { key: 'session', label: '会话' },
  { key: 'file', label: '文件' },
  { key: 'rateLimit', label: '限流' },
  { key: 'login', label: '登录' },
  { key: 'register', label: '注册' },
  { key: 'thirdParty', label: '第三方' },
  { key: 'payment', label: '支付' },
  { key: 'sms', label: '短信' },
  { key: 'email', label: '邮件' },
  { key: 'security', label: '安全' },
]

const {
  loading, saving, smsTesting, emailTesting, testEmailTo, paymentTesting, platformMaxFileMb, isDirty, forbidConcurrentLogin,
  siteDraft, sessionDraft, securityDraft, loginDraft, registerDraft, smsDraft, emailDraft,
  fileDraft, rateDraft, thirdDraft, paymentDraft, savedSnapshot,
  roleOptions, userOptions, testSmsPhone, testSmsTemplate, recentSmsLogs, recentEmailLogs,
  showPaymentModal, payOrderStatus, payStatusRefreshing, paymentResult,
  captchaTypeOptions, providerOptions, smsTemplateOptions, alipaySignOptions, alipayGatewayOptions,
  smsStatusText, emailStatusText, load, saveAll, resetAll, loadRecentSmsLogs, openSmsLogs, loadRecentEmailLogs, openEmailLogs,
  sendTestSms, sendTestEmailAction, sendTestPayment, pollPayOrderStatus, closePaymentModal,
} = useConfigEditor()

useUnsavedLeaveGuard()

watch(tab, (t) => {
  if (t === 'sms') loadRecentSmsLogs()
  if (t === 'email') loadRecentEmailLogs()
})

const defaultRoleLabel = computed(() => {
  const role = roleOptions.value.find((r) => r.code === registerDraft.defaultRoleCode)
  return role ? `${role.name}（${role.code}）` : registerDraft.defaultRoleCode
})
const registerAuditorLabel = computed(() => {
  const ids = registerDraft.auditorUserIds || []
  if (!ids.length) return '默认超级管理员'
  const labels = ids.map((id) => {
    const user = userOptions.value.find((u) => Number(u.id) === Number(id))
    if (!user) return String(id)
    const name = user.nickname || user.username
    return user.deptName ? `${name}（${user.deptName}）` : name
  })
  if (labels.length <= 2) return labels.join('、')
  return `${labels.slice(0, 2).join('、')} 等${labels.length}人`
})
const providerLabel = computed(() =>
  providerOptions.find((o) => o.value === smsDraft.provider)?.label || smsDraft.provider,
)
const testTemplateLabel = computed(() =>
  smsTemplateOptions.find((o) => o.value === testSmsTemplate.value)?.label || testSmsTemplate.value,
)
const alipayGatewayLabel = computed(() =>
  alipayGatewayOptions.find((o) => o.value === paymentDraft.alipay.gatewayUrl)?.label || '自定义',
)

const emailProviderOptions = [
  { label: 'QQ 邮箱 (smtp.qq.com)', value: 'qq' },
  { label: '网易 163 邮箱 (smtp.163.com)', value: '163' },
  { label: 'Google Gmail (smtp.gmail.com)', value: 'gmail' },
  { label: '自定义 SMTP 服务器', value: 'custom' },
]
const emailSecurityOptions = [
  { label: 'SSL (推荐 465)', value: 'SSL' },
  { label: 'TLS (587)', value: 'TLS' },
  { label: 'STARTTLS', value: 'STARTTLS' },
  { label: '无加密 (25)', value: 'NONE' },
]
const emailEncodingOptions = [
  { label: 'UTF-8 (推荐通用编码)', value: 'UTF-8' },
  { label: 'GBK (简体中文扩展)', value: 'GBK' },
  { label: 'GB2312 (国标简体)', value: 'GB2312' },
  { label: 'ISO-8859-1 (西欧编码)', value: 'ISO-8859-1' },
]

const emailProviderLabel = computed(() =>
  emailProviderOptions.find((o) => o.value === emailDraft.provider)?.label || emailDraft.provider || '自定义',
)
const emailSecurityLabel = computed(() =>
  emailSecurityOptions.find((o) => o.value === emailDraft.securityType)?.label || emailDraft.securityType || 'SSL',
)
const emailEncodingLabel = computed(() =>
  emailEncodingOptions.find((o) => o.value === emailDraft.encoding)?.label || emailDraft.encoding || 'UTF-8',
)

function onLoginSwitch(key: keyof typeof loginDraft, e: { detail: { value: boolean } }) {
  ;(loginDraft as Record<string, unknown>)[key] = e.detail.value
}
function onRegisterSwitch(key: keyof typeof registerDraft, e: { detail: { value: boolean } }) {
  ;(registerDraft as Record<string, unknown>)[key] = e.detail.value
}
function onSmsSwitch(key: keyof typeof smsDraft, e: { detail: { value: boolean } }) {
  ;(smsDraft as Record<string, unknown>)[key] = e.detail.value
}
function onSecuritySwitch(key: keyof typeof securityDraft, e: { detail: { value: boolean } }) {
  ;(securityDraft as Record<string, unknown>)[key] = e.detail.value
}
function onForbidConcurrentChange(e: { detail: { value: boolean } }) {
  forbidConcurrentLogin.value = e.detail.value
}
function onThirdSwitch(provider: 'wechat' | 'alipay' | 'github' | 'google', key: string, e: { detail: { value: boolean } }) {
  ;(thirdDraft[provider] as Record<string, unknown>)[key] = e.detail.value
}
function onPaySwitch(provider: 'wechatPay' | 'alipay', key: string, e: { detail: { value: boolean } }) {
  ;(paymentDraft[provider] as Record<string, unknown>)[key] = e.detail.value
}

async function pickFromOptions(itemList: string[], onPick: (index: number) => void) {
  try {
    const index = await showActionSheet({ items: itemList.map((label) => ({ label })) })
    onPick(index)
  } catch {
    // 用户取消
  }
}

function pickEmailProvider() {
  if (!canEdit.value) return
  pickFromOptions(emailProviderOptions.map((o) => o.label), (i) => {
    const item = emailProviderOptions[i]
    if (item) {
      emailDraft.provider = item.value as 'qq' | '163' | 'gmail' | 'custom'
      const saved = savedSnapshot.email as EmailAdminConfig | undefined
      // 如果切回当前数据库已保存绑定的服务商，恢复原保存的账号与授权码
      if (saved && item.value && item.value === saved.provider) {
        emailDraft.host = saved.host || 'smtp.qq.com'
        emailDraft.port = saved.port || 465
        emailDraft.securityType = saved.securityType || 'SSL'
        emailDraft.username = saved.username || ''
        emailDraft.password = saved.password || ''
        return
      }

      // 否则切到未绑定的新服务商，填充预设网络参数并清空账号与密码
      if (item.value === 'qq') {
        emailDraft.host = 'smtp.qq.com'
        emailDraft.port = 465
        emailDraft.securityType = 'SSL'
      } else if (item.value === '163') {
        emailDraft.host = 'smtp.163.com'
        emailDraft.port = 465
        emailDraft.securityType = 'SSL'
      } else if (item.value === 'gmail') {
        emailDraft.host = 'smtp.gmail.com'
        emailDraft.port = 587
        emailDraft.securityType = 'TLS'
      }

      emailDraft.username = ''
      emailDraft.password = ''
    }
  })
}

function pickEmailSecurityType() {
  if (!canEdit.value) return
  pickFromOptions(emailSecurityOptions.map((o) => o.label), (i) => {
    const item = emailSecurityOptions[i]
    if (item) emailDraft.securityType = item.value as any
  })
}

function pickEmailEncoding() {
  if (!canEdit.value) return
  pickFromOptions(emailEncodingOptions.map((o) => o.label), (i) => {
    const item = emailEncodingOptions[i]
    if (item) emailDraft.encoding = item.value
  })
}

function pickDefaultRole() {
  if (!roleOptions.value.length) return
  pickFromOptions(roleOptions.value.map((r) => `${r.name}（${r.code}）`), (i) => {
    const role = roleOptions.value[i]
    if (role) registerDraft.defaultRoleCode = role.code
  })
}
function pickRegisterAuditors() {
  if (!canEdit.value || !registerDraft.enabled || !registerDraft.needAudit) return
  clearPagePickerResult()
  setUserSelectMultiIds(registerDraft.auditorUserIds || [])
  uni.navigateTo({
    url: appendNavFromParam(
      `/pages-sub/system/user-select?multi=1&allowEmpty=1&emptyLabel=${encodeURIComponent('未选择')}&title=${encodeURIComponent('选择审核人')}`,
    ),
  })
}
function applyRegisterAuditorPick() {
  const result = consumePagePickerResult('user-multi')
  if (!result?.ids) return
  registerDraft.auditorUserIds = result.ids
}
function pickProvider() {
  pickFromOptions(providerOptions.map((o) => o.label), (i) => {
    const item = providerOptions[i]
    if (item) smsDraft.provider = item.value as typeof smsDraft.provider
  })
}
function pickTestTemplate() {
  pickFromOptions(smsTemplateOptions.map((o) => o.label), (i) => {
    const item = smsTemplateOptions[i]
    if (item) testSmsTemplate.value = item.value
  })
}
function pickAlipaySign() {
  pickFromOptions(alipaySignOptions.map((o) => o.label), (i) => {
    const item = alipaySignOptions[i]
    if (item) paymentDraft.alipay.signType = item.value
  })
}
function pickAlipayGateway() {
  pickFromOptions(alipayGatewayOptions.map((o) => o.label), (i) => {
    const item = alipayGatewayOptions[i]
    if (item) paymentDraft.alipay.gatewayUrl = item.value
  })
}

onMounted(load)
onShow(() => {
  applyRegisterAuditorPick()
})
onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.config-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

.config-page :deep(.form-cell) {
  flex-direction: row;
  align-items: center;
  gap: 20rpx;
  min-height: 88rpx;
  padding: 16rpx 24rpx;
}

.config-page :deep(.form-cell__label-wrap) {
  width: 240rpx;
  flex-shrink: 0;
  padding-top: 0;
}

.config-page :deep(.form-panel--rate .form-cell__label-wrap) {
  width: 280rpx;
}

.config-page :deep(.form-cell__label) {
  display: block;
  font-size: $font-size-sm;
  line-height: 1.35;
  text-align: left;
}

.config-page :deep(.form-cell__hint) {
  display: none;
}

.config-page :deep(.form-cell__body) {
  flex: 1;
  min-width: 0;
  justify-content: flex-start;
}

.config-page :deep(.form-panel--file .form-cell__body),
.config-page :deep(.form-panel--session .form-cell__body),
.config-page :deep(.form-panel--login .form-cell__body) {
  justify-content: flex-start;
  align-items: center;
  gap: 12rpx;
}

.config-page :deep(.form-panel--file .form-cell--stacked) {
  align-items: flex-start;
  min-height: auto;
  padding-top: 20rpx;
  padding-bottom: 20rpx;
}

.config-page :deep(.form-panel--file .form-cell--stacked .form-cell__label-wrap) {
  padding-top: 14rpx;
}

.config-page :deep(.form-panel--file .form-cell--stacked .form-cell__body) {
  align-items: stretch;
}

.config-boxed-textarea {
  width: 100%;
  min-height: 160rpx;
  padding: 16rpx 20rpx;
  border: 1px solid $color-border-light;
  border-radius: $radius-md;
  background: $color-bg-card;
  font-size: $font-size-sm;
  line-height: 1.55;
  box-sizing: border-box;
  color: $color-text-primary;
}

.config-boxed-textarea::placeholder {
  color: $color-text-placeholder;
}

.config-page :deep(.form-panel--rate .form-cell) {
  padding-right: 12rpx;
}

.config-page :deep(.form-panel--rate .form-cell__body) {
  justify-content: flex-end;
}

.config-page :deep(.form-cell--switch .form-cell__body) {
  justify-content: flex-start;
}

.config-security-wrap :deep(.form-cell--switch .form-cell__body) {
  justify-content: flex-end;
}

.config-page :deep(.form-cell--boxed .form-cell__body) {
  flex: 1;
  min-width: 0;
  justify-content: flex-start;
}

.config-page :deep(.picker-value) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.config-control-row {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12rpx;
  width: 100%;
  flex-wrap: nowrap;
}

.config-control-row--end {
  justify-content: flex-end;
}

.config-unit {
  flex-shrink: 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  white-space: nowrap;
}

.config-page :deep(.form-cell__body .number-stepper) {
  flex-shrink: 0;
}

$config-label-width: 240rpx;
$config-label-width-rate: 280rpx;
$config-cell-gap: 20rpx;
$config-cell-padding-x: 24rpx;

.config-security-wrap {
  padding-bottom: 8rpx;
}

.config-security-wrap :deep(.form-cell__label-wrap) {
  width: $config-label-width-rate;
  flex: 1;
  min-width: 0;
}

.config-security-wrap :deep(.form-cell--switch) {
  align-items: flex-start;
}

.config-security-wrap :deep(.form-cell--switch .form-cell__body) {
  flex: none;
  padding-top: 6rpx;
}

.config-security-wrap :deep(.form-cell__hint) {
  display: block;
  margin-top: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.5;
  white-space: normal;
}

.config-page :deep(.form-panel) {
  margin-top: 0;
}

.config-page__scroll > .form-panel {
  margin-top: 16rpx;
}

.config-page :deep(.form-panel--flat) {
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.config-collapse-wrap,
.config-sms-wrap {
  padding-bottom: 8rpx;
}

.config-dirty {
  padding: 12rpx 24rpx;
  font-size: $font-size-sm;
  color: $color-warning;
  background: rgba(230, 162, 60, 0.12);
  border-top: 1px solid rgba(230, 162, 60, 0.22);
  text-align: center;
}

.config-dirty--footer {
  margin: 0;
  border-radius: 0;
}

.config-page-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  background: $color-bg-card;
  box-shadow: $shadow-footer;
  padding-bottom: env(safe-area-inset-bottom);
}

.config-page-footer__actions {
  display: flex;
  gap: 16rpx;
  padding: 16rpx 24rpx;
}

.config-page__scroll {
  flex: 1;
  height: 0;
  min-height: 0;
}

.config-section {
  display: block;
  padding: 24rpx 24rpx 12rpx;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $color-text-primary;
  background: $color-bg-muted;
  border-top: 1px solid $color-border-light;
}

.config-section:first-child {
  border-top: none;
}

.config-hint {
  display: block;
  padding: 0 32rpx 20rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.55;
  background: $color-bg-muted;
}

.config-warn {
  margin: 0 24rpx 16rpx;
  padding: 16rpx 20rpx;
  border-radius: $radius-md;
  background: rgba(230, 162, 60, 0.12);
  border: 1px solid rgba(230, 162, 60, 0.22);
  font-size: $font-size-xs;
  color: $color-warning;
  line-height: 1.55;
}

.config-hint--inline {
  display: block;
  padding: 0 32rpx 20rpx;
  background: $color-bg-card;
}

.config-info {
  margin-top: 8rpx;
  padding: 20rpx 24rpx;
  border-radius: $radius-md;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.18);
  font-size: $font-size-xs;
  color: $color-text-secondary;
  line-height: 1.65;

  text {
    display: block;
  }
}

.config-textarea {
  padding: 20rpx 32rpx 24rpx;
  border-bottom: 1px solid $color-border-light;
  background: $color-bg-card;
}

.config-textarea__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-md;
  color: $color-text-primary;
  font-weight: $font-weight-semibold;
}

.config-textarea__input {
  width: 100%;
  min-height: 180rpx;
  padding: 20rpx;
  border-radius: $radius-md;
  background: $color-bg-muted;
  border: 1px solid $color-border-light;
  font-size: $font-size-sm;
  line-height: 1.55;
  box-sizing: border-box;
}

.config-test-btn {
  margin: 16rpx 32rpx;
  background: $color-primary;
  color: #fff;
  border-radius: $radius-md;
  font-size: $font-size-base;
}

.sms-test__row {
  display: flex;
  gap: 16rpx;
  padding: 8rpx 32rpx 16rpx;
}

.sms-test__input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  border-radius: $radius-sm;
  background: $color-bg-muted;
  font-size: $font-size-base;
}

.sms-test__btn {
  flex-shrink: 0;
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 28rpx;
  border-radius: $radius-sm;
  background: $color-primary;
  color: #fff;
  font-size: $font-size-base;
}

.sms-logs-list {
  padding: 8rpx 0 16rpx;
}

.sms-logs__more {
  font-size: $font-size-sm;
  color: $color-primary;
  padding-right: 8rpx;
}

.pay-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  padding: 48rpx;
}

.pay-modal__card {
  width: 100%;
  max-width: 600rpx;
  padding: 40rpx 32rpx;
  border-radius: $radius-lg;
  background: $color-bg-card;
}

.pay-modal__title {
  display: block;
  margin-bottom: 24rpx;
  font-size: $font-size-lg;
  font-weight: $font-weight-bold;
  text-align: center;
}

.pay-modal__row {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-sm;
  color: $color-text-regular;
}

.pay-modal__qr {
  display: block;
  width: 360rpx;
  height: 360rpx;
  margin: 24rpx auto;
}

.pay-modal__close {
  margin-top: 16rpx;
  background: $color-bg-muted;
  color: $color-text-secondary;
  border-radius: $radius-md;
  font-size: $font-size-base;
}
</style>
