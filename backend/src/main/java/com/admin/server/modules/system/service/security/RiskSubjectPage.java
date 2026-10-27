package com.admin.server.modules.system.service.security;

import com.admin.server.modules.system.api.security.vo.RiskSubjectVO;

import java.util.List;

/**
 * 风险主体分页结果。
 *
 * <p>主体来自 Redis 扫描，无法用数据库分页，因此在服务层做内存分页，
 * 再用这个结构承载「当前页记录 + 总数」，避免把 MyBatis 的 Page 硬套到非数据库数据上。</p>
 */
public record RiskSubjectPage(List<RiskSubjectVO> records, long total, int pageNo, int pageSize) {
}
