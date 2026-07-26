package com.admin.server.modules.infra.api.recycle.vo;

import lombok.Data;

@Data
public class RecycleSummaryVO {
    private long user;
    private long role;
    private long menu;
    private long dept;
    private long post;
    private long ticket;
    private long approval;
    private long dict;
    private long dictData;
    private long announce;
    private long job;
    private long jobLog;
    private long file;
    private long gen;
    private long total;
}
