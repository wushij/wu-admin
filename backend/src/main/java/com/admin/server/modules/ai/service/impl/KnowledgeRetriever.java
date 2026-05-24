package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeMapper;
import com.admin.server.modules.ai.util.AiKnowledgeSanitizer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L2 知识库检索器（设计方案 §4.2）
 * <p>
 * 优先 ngram 全文检索（相关度 + sort 排序）；全文索引不可用（如 MySQL 5.6）或
 * 查询异常时降级为 keywords/title LIKE。命中结果拼装为 &lt;reference_data&gt; 块，
 * 单条截断、总量受限，注入前对未闭合围栏做兼容处理。
 * </p>
 */
@Component
public class KnowledgeRetriever {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetriever.class);

    /** 检索返回的最大条数（设计 §4.2 Top-3） */
    private static final int TOP_N = 3;
    /** 单条知识注入上限字符数（约 600 token） */
    private static final int PER_ITEM_MAX_CHARS = 900;
    /** 参考资料块总量上限字符数（约 1500 token，设计 §4.2 容量控制） */
    private static final int TOTAL_MAX_CHARS = 2200;

    @Resource
    private AiKnowledgeMapper aiKnowledgeMapper;

    /**
     * 依据用户问题检索知识并拼装 &lt;reference_data&gt; 块；无命中或查询为空时返回空串
     */
    public String retrieveReferenceBlock(String query) {
        String q = StrUtil.trim(query);
        if (StrUtil.isBlank(q)) {
            return "";
        }
        List<AiKnowledgeDO> hits = search(q);
        if (CollUtil.isEmpty(hits)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<reference_data>\n## 参考资料（回答相关问题时优先依据，若与用户问题无关请忽略）\n");
        int used = sb.length();
        int index = 1;
        for (AiKnowledgeDO hit : hits) {
            String body = AiKnowledgeSanitizer.sanitizeForPrompt(
                    StrUtil.brief(StrUtil.nullToEmpty(hit.getContent()), PER_ITEM_MAX_CHARS));
            String section = "### [" + index + "] " + StrUtil.nullToEmpty(hit.getTitle()) + "\n" + body + "\n";
            if (used + section.length() > TOTAL_MAX_CHARS) {
                break;
            }
            sb.append(section);
            used += section.length();
            index++;
        }
        if (index == 1) {
            return "";
        }
        sb.append("</reference_data>");
        return sb.toString();
    }

    /** 全文检索优先，异常降级 LIKE */
    private List<AiKnowledgeDO> search(String query) {
        try {
            List<AiKnowledgeDO> fullText = aiKnowledgeMapper.searchFullText(query, TOP_N);
            if (CollUtil.isNotEmpty(fullText)) {
                return fullText;
            }
        } catch (Exception e) {
            // MySQL 5.6 无 ngram 或全文语法异常时降级
            log.warn("AI 知识库全文检索不可用，降级 LIKE 检索: {}", e.getMessage());
        }
        return likeFallback(query);
    }

    /** LIKE 降级：title/keywords 命中，按 sort 优先取 Top-N */
    private List<AiKnowledgeDO> likeFallback(String query) {
        try {
            List<String> terms = splitTerms(query);
            LambdaQueryWrapper<AiKnowledgeDO> wrapper = new LambdaQueryWrapper<AiKnowledgeDO>()
                    .eq(AiKnowledgeDO::getStatus, 1)
                    .and(w -> {
                        for (int i = 0; i < terms.size(); i++) {
                            if (i > 0) {
                                w.or();
                            }
                            String term = terms.get(i);
                            w.like(AiKnowledgeDO::getKeywords, term).or().like(AiKnowledgeDO::getTitle, term);
                        }
                    })
                    .orderByDesc(AiKnowledgeDO::getSort)
                    .orderByAsc(AiKnowledgeDO::getId)
                    .last("LIMIT " + TOP_N);
            return aiKnowledgeMapper.selectList(wrapper);
        } catch (Exception e) {
            log.warn("AI 知识库 LIKE 降级检索失败，本轮不注入知识: {}", e.getMessage());
            return List.of();
        }
    }

    /** 提取查询词：优先按空白与常见分隔切分，去重去空；整句本身也作为一项 */
    private List<String> splitTerms(String query) {
        Map<String, Boolean> ordered = new LinkedHashMap<>();
        ordered.put(query, true);
        for (String part : query.split("[\\s,，、;；]+")) {
            String p = StrUtil.trim(part);
            if (p.length() >= 2) {
                ordered.put(p, true);
            }
        }
        return new ArrayList<>(ordered.keySet());
    }
}
