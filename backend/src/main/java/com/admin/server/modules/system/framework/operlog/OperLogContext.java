package com.admin.server.modules.system.framework.operlog;

import java.util.List;

/**
 * 操作日志动态上下文，用于在业务逻辑中记录精准的模块名称、具体操作动作与变更明细。
 */
public class OperLogContext {

    private static final ThreadLocal<String> TITLE = new ThreadLocal<>();
    private static final ThreadLocal<String> ACTION = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> DIFF_ITEMS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SKIP_LOG = new ThreadLocal<>();

    public static void setTitle(String title) {
        TITLE.set(title);
    }

    public static String getTitle() {
        return TITLE.get();
    }

    public static void setAction(String action) {
        ACTION.set(action);
    }

    public static String getAction() {
        return ACTION.get();
    }

    public static void setDiffItems(List<String> diffItems) {
        DIFF_ITEMS.set(diffItems);
    }

    public static List<String> getDiffItems() {
        return DIFF_ITEMS.get();
    }

    public static void setSkipLog(boolean skip) {
        SKIP_LOG.set(skip);
    }

    public static boolean isSkipLog() {
        return Boolean.TRUE.equals(SKIP_LOG.get());
    }

    public static void clear() {
        TITLE.remove();
        ACTION.remove();
        DIFF_ITEMS.remove();
        SKIP_LOG.remove();
    }
}
