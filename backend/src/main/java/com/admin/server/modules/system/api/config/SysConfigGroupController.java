package com.admin.server.modules.system.api.config;

import com.admin.server.common.exception.BusinessException;
import com.admin.server.common.core.CommonResult;
import com.admin.server.common.core.PageResult;
import com.admin.server.framework.log.annotation.Log;
import com.admin.server.modules.system.dal.dataobject.config.SysConfigGroupDO;
import com.admin.server.modules.trade.dal.dataobject.sms.SmsLogDO;
import com.admin.server.modules.trade.framework.pay.PayServiceFactory;
import com.admin.server.modules.system.service.config.SysConfigGroupService;
import com.admin.server.modules.trade.service.sms.SmsLogService;
import com.admin.server.modules.trade.framework.sms.SmsServiceFactory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.admin.server.modules.system.framework.security.SystemPermissionService;
import com.admin.server.modules.system.service.config.SystemConfigHelper;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/system/config-group")
public class SysConfigGroupController {

    private static final String MASK_PLACEHOLDER = "••••••••••••••••••••••••••••";

    @Resource
    private SysConfigGroupService configGroupService;

    @Resource
    private SystemPermissionService ss;

    @Resource
    private PayServiceFactory payServiceFactory;

    @Resource
    private SmsServiceFactory smsServiceFactory;

    @Resource
    private SmsLogService smsLogService;

    @Resource
    private com.admin.server.modules.trade.service.email.EmailCodeService emailCodeService;

    @GetMapping("/list")
    @Operation(summary = "配置分组列表")
    @PreAuthorize("@ss.hasRead('system:config:list')")
    public CommonResult<List<SysConfigGroupDO>> list() {
        List<SysConfigGroupDO> list = configGroupService.listAll();
        if (ss.hasPermission("system:config:update")) {
            return CommonResult.success(list);
        }
        List<SysConfigGroupDO> maskedList = list.stream().map(this::maskConfigGroup).toList();
        return CommonResult.success(maskedList);
    }

    @GetMapping("/{groupCode}")
    @Operation(summary = "获取配置分组")
    @PreAuthorize("@ss.hasRead('system:config:list')")
    public CommonResult<SysConfigGroupDO> get(@PathVariable String groupCode) {
        SysConfigGroupDO row = configGroupService.getByGroupCode(groupCode);
        if (row == null) {
            throw new BusinessException(404, "配置分组不存在");
        }
        if (ss.hasPermission("system:config:update")) {
            return CommonResult.success(row);
        }
        return CommonResult.success(maskConfigGroup(row));
    }

    private SysConfigGroupDO maskConfigGroup(SysConfigGroupDO row) {
        if (row == null || StrUtil.isBlank(row.getConfigValue()) || !JSONUtil.isTypeJSON(row.getConfigValue())) {
            return row;
        }
        // 复制新实例，避免污染底层缓存
        SysConfigGroupDO masked = new SysConfigGroupDO();
        masked.setId(row.getId());
        masked.setGroupCode(row.getGroupCode());
        masked.setGroupName(row.getGroupName());
        masked.setRemark(row.getRemark());
        masked.setCreateTime(row.getCreateTime());
        masked.setUpdateTime(row.getUpdateTime());

        JSONObject json = JSONUtil.parseObj(row.getConfigValue());
        String code = row.getGroupCode();
        if (SystemConfigHelper.GROUP_SMS.equals(code)) {
            maskJsonField(json, "accessKeyId");
            maskJsonField(json, "accessKeySecret");
            maskJsonField(json, "tencentAppId");
        } else if (SystemConfigHelper.GROUP_PAYMENT.equals(code)) {
            JSONObject wx = json.getJSONObject("wechatPay");
            if (wx != null) {
                maskJsonField(wx, "mchId");
                maskJsonField(wx, "appId");
                maskJsonField(wx, "apiV3Key");
                maskJsonField(wx, "privateKey");
                maskJsonField(wx, "certSerialNo");
            }
            JSONObject alipay = json.getJSONObject("alipay");
            if (alipay != null) {
                maskJsonField(alipay, "appId");
                maskJsonField(alipay, "privateKey");
                maskJsonField(alipay, "publicKey");
            }
        } else if (SystemConfigHelper.GROUP_EMAIL.equals(code)) {
            maskJsonField(json, "username");
            maskJsonField(json, "password");
        } else if (SystemConfigHelper.GROUP_THIRD_PARTY.equals(code)) {
            JSONObject wechat = json.getJSONObject("wechat");
            if (wechat != null) {
                maskJsonField(wechat, "appId");
                maskJsonField(wechat, "appSecret");
            }
            JSONObject alipay = json.getJSONObject("alipay");
            if (alipay != null) {
                maskJsonField(alipay, "appId");
                maskJsonField(alipay, "privateKey");
                maskJsonField(alipay, "publicKey");
            }
            JSONObject github = json.getJSONObject("github");
            if (github != null) {
                maskJsonField(github, "clientId");
                maskJsonField(github, "clientSecret");
            }
            JSONObject google = json.getJSONObject("google");
            if (google != null) {
                maskJsonField(google, "clientId");
                maskJsonField(google, "clientSecret");
            }
        } else if (SystemConfigHelper.GROUP_SECURITY.equals(code)) {
            maskJsonField(json, "sm4SecretKey");
            maskJsonField(json, "sm3SignKey");
        } else if (SystemConfigHelper.GROUP_FILE.equals(code)) {
            maskJsonField(json, "accessKey");
            maskJsonField(json, "secretKey");
            maskJsonField(json, "accessKeyId");
            maskJsonField(json, "accessKeySecret");
        }
        masked.setConfigValue(json.toString());
        return masked;
    }

    private void maskJsonField(JSONObject json, String field) {
        if (json.containsKey(field)) {
            String val = json.getStr(field);
            if (StrUtil.isNotBlank(val)) {
                json.set(field, MASK_PLACEHOLDER);
            }
        }
    }

    @PutMapping("/{groupCode}")
    @Operation(summary = "更新配置分组")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    @Log(title = "系统配置", businessType = Log.BusinessType.UPDATE)
    public CommonResult<Boolean> update(@PathVariable String groupCode, @RequestBody Map<String, String> body) {
        String configValue = body.get("configValue");
        if (configValue == null || configValue.isBlank()) {
            throw new BusinessException(400, "configValue 不能为空");
        }
        configGroupService.updateConfig(groupCode, configValue);
        return CommonResult.success(true);
    }

    @Operation(summary = "创建测试支付订单")
    @PostMapping("/test-payment")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    public CommonResult<Map<String, String>> testPayment(@RequestBody TestPaymentRequest request) {
        if (request.getType() == null || request.getType().isBlank()) {
            throw new BusinessException(400, "支付类型不能为空");
        }
        String type = request.getType().trim();
        if (!payServiceFactory.isSupported(type)) {
            throw new BusinessException(400, "不支持的支付类型: " + type);
        }
        return CommonResult.success(payServiceFactory.createTestOrder(type));
    }

    @Data
    public static class TestPaymentRequest {
        /** wechat 或 alipay */
        private String type;
    }

    @Operation(summary = "测试发送短信")
    @PostMapping("/test-sms")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    public CommonResult<Boolean> testSms(@RequestBody TestSmsRequest request) {
        if (request.getPhone() == null || !request.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(400, "请输入正确的手机号");
        }
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        boolean success = smsServiceFactory.sendCode(request.getPhone(), code, request.getTemplateCode());
        if (success) {
            return CommonResult.success(true);
        }
        LambdaQueryWrapper<SmsLogDO> logQuery = new LambdaQueryWrapper<>();
        logQuery.eq(SmsLogDO::getPhone, request.getPhone())
                .orderByDesc(SmsLogDO::getCreateTime)
                .last("LIMIT 1");
        SmsLogDO latest = smsLogService.getOne(logQuery);
        String detail = latest != null && latest.getResultMsg() != null && !latest.getResultMsg().isBlank()
                ? latest.getResultMsg()
                : "请检查签名、模板 ID 与密钥是否正确";
        throw new BusinessException(500, "短信发送失败：" + detail);
    }

    @Operation(summary = "最近短信发送记录")
    @GetMapping("/sms-logs/recent")
    @PreAuthorize("@ss.hasRead('system:config:list')")
    public CommonResult<List<SmsLogDO>> getRecentSmsLogs(
            @RequestParam(defaultValue = "5") Integer limit) {
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 50);
        LambdaQueryWrapper<SmsLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SmsLogDO::getCreateTime).last("LIMIT " + size);
        return CommonResult.success(smsLogService.list(wrapper));
    }

    @Operation(summary = "分页查询短信发送记录")
    @GetMapping("/sms-logs")
    @PreAuthorize("@ss.hasRead('system:config:list')")
    public CommonResult<PageResult<SmsLogDO>> getSmsLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        LambdaQueryWrapper<SmsLogDO> wrapper = new LambdaQueryWrapper<>();
        if (phone != null && !phone.isBlank()) {
            wrapper.like(SmsLogDO::getPhone, phone.trim());
        }
        if (status != null) {
            wrapper.eq(SmsLogDO::getStatus, status);
        }
        wrapper.orderByDesc(SmsLogDO::getCreateTime);
        Page<SmsLogDO> result = smsLogService.page(new Page<>(pageNo, pageSize), wrapper);
        return CommonResult.success(PageResult.of(result.getRecords(), result.getTotal()));
    }

    @Data
    public static class TestSmsRequest {
        private String phone;
        /** 测试指定模板 CODE，为空则使用登录/注册模板 */
        private String templateCode;
    }

    @Operation(summary = "测试发送邮件")
    @PostMapping("/test-email")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    public CommonResult<Boolean> testEmail(@RequestBody TestEmailRequest request) {
        if (request.getToEmail() == null || !request.getToEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new BusinessException(400, "请输入正确的接收邮箱");
        }
        String err = emailCodeService.sendTestEmail(request.getToEmail().trim());
        if (err != null) {
            throw new BusinessException(500, "邮件发送失败：" + err);
        }
        return CommonResult.success(true);
    }

    @Data
    public static class TestEmailRequest {
        private String toEmail;
    }

    @Resource
    private com.admin.server.modules.trade.service.email.EmailLogService emailLogService;

    @Operation(summary = "最近邮件发送记录")
    @GetMapping("/email-logs/recent")
    @PreAuthorize("@ss.hasRead('system:config:list')")
    public CommonResult<List<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO>> getRecentEmailLogs(
            @RequestParam(defaultValue = "5") Integer limit) {
        int size = limit == null || limit < 1 ? 5 : Math.min(limit, 50);
        LambdaQueryWrapper<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO::getCreateTime).last("LIMIT " + size);
        return CommonResult.success(emailLogService.list(wrapper));
    }

    @Operation(summary = "分页查询邮件发送记录")
    @GetMapping("/email-logs")
    @PreAuthorize("@ss.hasRead('system:config:list')")
    public CommonResult<PageResult<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO>> getEmailLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer status) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 100);
        LambdaQueryWrapper<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO> wrapper = new LambdaQueryWrapper<>();
        if (email != null && !email.isBlank()) {
            wrapper.like(com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO::getEmail, email.trim());
        }
        if (status != null) {
            wrapper.eq(com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO::getStatus, status);
        }
        wrapper.orderByDesc(com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO::getCreateTime);
        Page<com.admin.server.modules.trade.dal.dataobject.email.EmailLogDO> result = emailLogService.page(new Page<>(pageNo, pageSize), wrapper);
        return CommonResult.success(PageResult.of(result.getRecords(), result.getTotal()));
    }

    @Operation(summary = "删除短信发送记录")
    @DeleteMapping("/sms-logs/{id}")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    public CommonResult<Boolean> deleteSmsLog(@PathVariable Long id) {
        smsLogService.removeById(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "批量删除短信发送记录")
    @DeleteMapping("/sms-logs/batch")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    public CommonResult<Boolean> deleteBatchSmsLogs(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            smsLogService.removeByIds(ids);
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "清空短信发送记录")
    @DeleteMapping("/sms-logs/clean")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    public CommonResult<Boolean> cleanSmsLogs() {
        smsLogService.remove(new LambdaQueryWrapper<>());
        return CommonResult.success(true);
    }

    @Operation(summary = "删除邮件发送记录")
    @DeleteMapping("/email-logs/{id}")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    public CommonResult<Boolean> deleteEmailLog(@PathVariable Long id) {
        emailLogService.removeById(id);
        return CommonResult.success(true);
    }

    @Operation(summary = "批量删除邮件发送记录")
    @DeleteMapping("/email-logs/batch")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    public CommonResult<Boolean> deleteBatchEmailLogs(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            emailLogService.removeByIds(ids);
        }
        return CommonResult.success(true);
    }

    @Operation(summary = "清空邮件发送记录")
    @DeleteMapping("/email-logs/clean")
    @PreAuthorize("@ss.hasPermission('system:config:update')")
    public CommonResult<Boolean> cleanEmailLogs() {
        emailLogService.remove(new LambdaQueryWrapper<>());
        return CommonResult.success(true);
    }
}
