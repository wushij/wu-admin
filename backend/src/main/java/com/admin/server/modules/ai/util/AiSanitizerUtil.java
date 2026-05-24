package com.admin.server.modules.ai.util;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * AI Prompt 敏感数据脱敏工具
 * <p>发送至大模型前对手机号、身份证号、邮箱、银行卡号做打码，避免敏感信息外泄。</p>
 */
public final class AiSanitizerUtil {

    private AiSanitizerUtil() {
    }

    /** 中国大陆手机号 */
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)(1[3-9]\\d)(\\d{4})(\\d{4})(?!\\d)");
    /** 18 位身份证号 */
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(\\d{6})(\\d{8})(\\d{3}[0-9Xx])(?!\\d)");
    /** 邮箱地址 */
    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-]{1,3})[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    /** 16~19 位银行卡号 */
    private static final Pattern BANK_CARD = Pattern.compile("(?<!\\d)(\\d{4})\\d{8,11}(\\d{4})(?!\\d)");

    /** 对文本做敏感信息脱敏 */
    public static String sanitize(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        String result = ID_CARD.matcher(text).replaceAll("$1********$3");
        result = MOBILE.matcher(result).replaceAll("$1****$3");
        result = BANK_CARD.matcher(result).replaceAll("$1********$2");
        result = EMAIL.matcher(result).replaceAll("$1***$2");
        return result;
    }
}
