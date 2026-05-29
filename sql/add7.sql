-- 第三方配置 + 支付配置（可重复执行，无 DROP）
INSERT INTO sys_config_group (group_code, group_name, config_value, remark) VALUES
('thirdParty', '第三方配置',
 '{"wechat":{"enabled":false,"appId":"","appSecret":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":""},"github":{"enabled":false,"clientId":"","clientSecret":""},"google":{"enabled":false,"clientId":"","clientSecret":"","redirectUri":""}}',
 '微信/支付宝/GitHub/Google 第三方登录密钥'),
('payment', '支付配置',
 '{"wechatPay":{"enabled":false,"mchId":"","appId":"","apiV3Key":"","privateKey":"","certSerialNo":"","notifyUrl":""},"alipay":{"enabled":false,"appId":"","privateKey":"","publicKey":"","signType":"RSA2","gatewayUrl":"https://openapi.alipay.com/gateway.do","notifyUrl":"","returnUrl":""}}',
 '微信/支付宝支付与测试下单')
ON DUPLICATE KEY UPDATE
  group_name = VALUES(group_name),
  remark = VALUES(remark);
