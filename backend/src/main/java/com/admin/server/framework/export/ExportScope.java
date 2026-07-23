package com.admin.server.framework.export;

public enum ExportScope {
    /** 当前筛选条件下全部（有上限） */
    FILTERED,
    /** 仅当前页 */
    PAGE;

    public static ExportScope fromParam(String scope) {
        if (scope != null && "page".equalsIgnoreCase(scope.trim())) {
            return PAGE;
        }
        return FILTERED;
    }
}
