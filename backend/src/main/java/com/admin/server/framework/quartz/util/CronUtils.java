package com.admin.server.framework.quartz.util;

import org.quartz.CronExpression;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class CronUtils {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private CronUtils() {
    }

    public static void validate(String cron) {
        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("Cron 表达式不合法");
        }
    }

    public static String hint(String cron) {
        if (cron == null || cron.isBlank()) {
            return "";
        }
        return switch (cron.trim()) {
            case "0/30 * * * * ?" -> "每 30 秒";
            case "0 * * * * ?" -> "每分钟";
            case "0 0/5 * * * ?" -> "每 5 分钟";
            case "0 0 0 * * ?" -> "每天 0 点";
            case "0 30 2 * * ?" -> "每天凌晨 2:30";
            case "0 0 9 ? * MON" -> "每周一 9 点";
            case "0 0 3 ? * SUN" -> "每周日 3 点";
            default -> cron;
        };
    }

    public static List<String> nextFireTimes(String cron, int count) throws Exception {
        CronExpression expression = new CronExpression(cron);
        List<String> times = new ArrayList<>();
        Date cursor = new Date();
        for (int i = 0; i < count; i++) {
            Date next = expression.getNextValidTimeAfter(cursor);
            if (next == null) {
                break;
            }
            times.add(FORMAT.format(next));
            cursor = new Date(next.getTime() + 1000);
        }
        return times;
    }

    public static String formatDate(Date date) {
        return date == null ? null : FORMAT.format(date);
    }
}
