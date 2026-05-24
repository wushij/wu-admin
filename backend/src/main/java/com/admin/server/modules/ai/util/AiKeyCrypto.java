package com.admin.server.modules.ai.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SM4;
import com.admin.server.modules.system.service.config.SystemConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AI 模块 API Key 加解密组件
 * <p>
 * 复用本项目既有 SM4-CBC 体系（与 ApiSecurityFilter 的密文格式一致：IV 32位Hex + 密文Hex），
 * 密钥取自系统配置 security 分组的 sm4SecretKey，不引入新的加密方式。
 * 密文带 "sm4:" 前缀标识；未配置密钥时降级 "plain:" 前缀明文存储并告警。
 * </p>
 */
@Component
public class AiKeyCrypto {

    private static final Logger log = LoggerFactory.getLogger(AiKeyCrypto.class);

    private static final String PREFIX_SM4 = "sm4:";
    private static final String PREFIX_PLAIN = "plain:";

    @Resource
    private SystemConfigHelper systemConfigHelper;

    /** 加密明文 API Key 用于入库，空串原样返回 */
    public String encrypt(String plainKey) {
        if (StrUtil.isBlank(plainKey)) {
            return "";
        }
        String sm4Key = systemConfigHelper.getSm4SecretKey();
        if (StrUtil.isBlank(sm4Key)) {
            log.warn("系统安全配置未设置 sm4SecretKey，AI API Key 将以明文形式存储，请尽快在系统配置中补齐密钥");
            return PREFIX_PLAIN + plainKey;
        }
        byte[] ivBytes = new byte[16];
        new SecureRandom().nextBytes(ivBytes);
        SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, toSm4KeyBytes(sm4Key), ivBytes);
        return PREFIX_SM4 + HexUtil.encodeHexStr(ivBytes) + sm4.encryptHex(plainKey);
    }

    /** 解密库中密文，返回明文 API Key；解密失败返回空串 */
    public String decrypt(String storedKey) {
        if (StrUtil.isBlank(storedKey)) {
            return "";
        }
        if (storedKey.startsWith(PREFIX_PLAIN)) {
            return storedKey.substring(PREFIX_PLAIN.length());
        }
        if (!storedKey.startsWith(PREFIX_SM4)) {
            // 历史数据无前缀时按明文处理
            return storedKey;
        }
        String cipherText = storedKey.substring(PREFIX_SM4.length());
        String sm4Key = systemConfigHelper.getSm4SecretKey();
        if (StrUtil.isBlank(sm4Key) || cipherText.length() <= 32) {
            log.error("AI API Key 解密失败：SM4 密钥未配置或密文格式非法");
            return "";
        }
        try {
            byte[] ivBytes = HexUtil.decodeHex(cipherText.substring(0, 32));
            SM4 sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, toSm4KeyBytes(sm4Key), ivBytes);
            return sm4.decryptStr(cipherText.substring(32));
        } catch (Exception e) {
            log.error("AI API Key SM4 解密失败: {}", e.getMessage());
            return "";
        }
    }

    /** 与 ApiSecurityFilter#toSm4KeyBytes 逻辑严格对齐 */
    private static byte[] toSm4KeyBytes(String key) {
        if (key.length() == 32 && key.matches("[0-9a-fA-F]{32}")) {
            return HexUtil.decodeHex(key);
        }
        return key.getBytes(StandardCharsets.UTF_8);
    }
}
