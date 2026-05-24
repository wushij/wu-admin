package com.admin.server.modules.infra.api.monitor.vo;

import lombok.Data;

@Data
public class JobTemplateVO {
    private String key;
    private String name;
    private String category;
    private String description;
    private String invokeTarget;
    private String cronExpression;
    private String jobGroup;
    private String relatedModule;
    private String icon;
}
