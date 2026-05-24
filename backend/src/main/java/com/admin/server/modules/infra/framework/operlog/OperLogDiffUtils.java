package com.admin.server.modules.infra.framework.operlog;

import java.lang.reflect.Field;
import java.util.*;

public class OperLogDiffUtils {
    private static final Map<String, String> FIELD_NAMES = new HashMap<>();
    static {
        FIELD_NAMES.put("nickname", "用户昵称");
        FIELD_NAMES.put("mobile", "手机号码");
        FIELD_NAMES.put("email", "邮箱地址");
        FIELD_NAMES.put("status", "状态");
        FIELD_NAMES.put("deptId", "部门ID");
        FIELD_NAMES.put("roleId", "角色ID");
        FIELD_NAMES.put("postIds", "岗位列表");
        FIELD_NAMES.put("name", "名称");
        FIELD_NAMES.put("code", "编码");
        FIELD_NAMES.put("remark", "备注");
        FIELD_NAMES.put("sort", "排序");
        FIELD_NAMES.put("parentId", "父级ID");
        FIELD_NAMES.put("path", "路由地址");
        FIELD_NAMES.put("component", "组件路径");
        FIELD_NAMES.put("permission", "权限标识");
        FIELD_NAMES.put("icon", "菜单图标");
        FIELD_NAMES.put("title", "标题");
        FIELD_NAMES.put("content", "内容");
        FIELD_NAMES.put("dictType", "字典类型");
        FIELD_NAMES.put("dictName", "字典名称");
        FIELD_NAMES.put("dictLabel", "字典标签");
        FIELD_NAMES.put("dictValue", "字典键值");
        FIELD_NAMES.put("cssClass", "CSS样式");
        FIELD_NAMES.put("cronExpression", "Cron表达式");
        FIELD_NAMES.put("handlerName", "处理器名称");
        FIELD_NAMES.put("retryCount", "重试次数");
        FIELD_NAMES.put("postCode", "岗位编码");
        FIELD_NAMES.put("postName", "岗位名称");
        FIELD_NAMES.put("leaderUserId", "负责人用户ID");
        FIELD_NAMES.put("phone", "联系电话");
        FIELD_NAMES.put("type", "类型");
        FIELD_NAMES.put("listClass", "列表样式");
        FIELD_NAMES.put("noticeType", "通知类型");
        FIELD_NAMES.put("targetType", "发布范围");
        FIELD_NAMES.put("targetIds", "目标ID列表");
        FIELD_NAMES.put("channels", "发布渠道");
    }

    public static List<String> diff(Object oldObj, Object newObj) {
        List<String> diffs = new ArrayList<>();
        if (oldObj == null || newObj == null) {
            return diffs;
        }
        Class<?> newClazz = newObj.getClass();
        Class<?> oldClazz = oldObj.getClass();
        
        for (Field newField : newClazz.getDeclaredFields()) {
            String fieldName = newField.getName();
            if ("id".equals(fieldName) || "serialVersionUID".equals(fieldName) || "children".equals(fieldName)) {
                continue;
            }
            String chineseName = FIELD_NAMES.getOrDefault(fieldName, fieldName);
            try {
                newField.setAccessible(true);
                Object newVal = newField.get(newObj);
                
                // Find matching field in oldObj
                Field oldField = null;
                try {
                    oldField = oldClazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    // Try parent classes
                    Class<?> currentClass = oldClazz.getSuperclass();
                    while (currentClass != null && oldField == null) {
                        try {
                            oldField = currentClass.getDeclaredField(fieldName);
                        } catch (NoSuchFieldException ex) {
                            currentClass = currentClass.getSuperclass();
                        }
                    }
                }
                
                if (oldField != null) {
                    oldField.setAccessible(true);
                    Object oldVal = oldField.get(oldObj);
                    
                    if (!Objects.equals(oldVal, newVal)) {
                        // Skip if both are empty/null equivalents
                        if (oldVal == null && (newVal instanceof String && ((String) newVal).isEmpty())) {
                            continue;
                        }
                        if (newVal == null && (oldVal instanceof String && ((String) oldVal).isEmpty())) {
                            continue;
                        }
                        
                        String oldStr = formatVal(fieldName, oldVal);
                        String newStr = formatVal(fieldName, newVal);
                        diffs.add(chineseName + ": " + oldStr + " -> " + newStr);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return diffs;
    }

    private static String formatVal(String fieldName, Object val) {
        if (val == null) {
            return "空";
        }
        if ("status".equals(fieldName)) {
            if (val instanceof Integer) {
                return (Integer) val == 0 ? "开启" : "禁用";
            }
            if (val instanceof Boolean) {
                return (Boolean) val ? "开启" : "禁用";
            }
        }
        if (val instanceof Boolean) {
            return (Boolean) val ? "是" : "否";
        }
        return val.toString();
    }
}
