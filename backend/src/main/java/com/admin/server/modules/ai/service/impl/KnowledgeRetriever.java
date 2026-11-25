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
 * 查询异常时降级为 keywords/title/content LIKE。命中结果拼装为
 * &lt;knowledge_base&gt; 块，单条截断、总量受限，注入前对未闭合围栏做兼容处理。
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

    @Resource
    private com.admin.server.modules.ai.service.rag.KnowledgeVectorSearchService knowledgeVectorSearchService;

    /**
     * 检索结果与命中方式，供管理端“测试检索”复用同一套逻辑。
     */
    public record SearchResult(List<AiKnowledgeDO> hits, String matchMode) {
    }

    /**
     * 依据用户问题检索知识并拼装 &lt;knowledge_base&gt; 块；无命中或查询为空时返回空串。
     * <p>优先向量语义检索（新知识库三层结构），向量不可用或无命中时回退旧全文检索。</p>
     */
    public String retrieveReferenceBlock(String query) {
        String q = StrUtil.trim(query);
        if (StrUtil.isBlank(q)) {
            return "";
        }
        try {
            List<com.admin.server.modules.ai.service.rag.KnowledgeVectorSearchService.ChunkHit> vectorHits =
                    knowledgeVectorSearchService.searchAcrossEnabledBases(
                            q, TOP_N, com.admin.server.modules.ai.service.rag.KnowledgeVectorSearchService.DEFAULT_MIN_SCORE);
            if (CollUtil.isNotEmpty(vectorHits)) {
                return buildVectorReferenceBlock(vectorHits);
            }
        } catch (Exception e) {
            log.warn("知识库向量检索不可用，回退全文检索: {}", e.getMessage());
        }
        return buildReferenceBlock(search(q, TOP_N));
    }

    /**
     * 向量检索命中拼装：保留文档名与切片标题，便于模型引用来源。
     */
    public String buildVectorReferenceBlock(
            List<com.admin.server.modules.ai.service.rag.KnowledgeVectorSearchService.ChunkHit> hits) {
        if (CollUtil.isEmpty(hits)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<knowledge_base>\n");
        sb.append("【知识库语义检索结果（必须首先依此回答）】：\n");
        sb.append("系统已按语义相似度检索知识库。若以下内容与用户提问相关，你必须【严格以知识库为准】回答，严禁编造；")
                .append("知识库未覆盖时再结合系统通用功能作答。\n\n");
        int used = sb.length();
        int index = 1;
        for (com.admin.server.modules.ai.service.rag.KnowledgeVectorSearchService.ChunkHit hit : hits) {
            String body = AiKnowledgeSanitizer.sanitizeForPrompt(
                    StrUtil.brief(StrUtil.nullToEmpty(hit.content()), PER_ITEM_MAX_CHARS));
            String source = StrUtil.isBlank(hit.heading())
                    ? "《" + StrUtil.nullToEmpty(hit.documentName()) + "》"
                    : "《" + StrUtil.nullToEmpty(hit.documentName()) + "》 · " + hit.heading();
            String section = "### [" + index + "] " + source + "\n" + body + "\n\n";
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
        sb.append("</knowledge_base>");
        return sb.toString();
    }

    /**
     * 检索知识条目；全文检索优先，异常或空结果时降级 LIKE。
     */
    public SearchResult searchWithMode(String query, int topN) {
        String q = StrUtil.trim(query);
        if (StrUtil.isBlank(q)) {
            return new SearchResult(List.of(), "NONE");
        }
        try {
            List<AiKnowledgeDO> fullText = aiKnowledgeMapper.searchFullText(q, topN);
            if (CollUtil.isNotEmpty(fullText)) {
                return new SearchResult(fullText, "FULLTEXT");
            }
        } catch (Exception e) {
            // MySQL 5.6 无 ngram 或全文语法异常时降级
            log.warn("AI 知识库全文检索不可用，降级 LIKE 检索: {}", e.getMessage());
        }
        List<AiKnowledgeDO> fallback = likeFallback(q, topN);
        return new SearchResult(fallback, CollUtil.isNotEmpty(fallback) ? "LIKE" : "NONE");
    }

    public List<AiKnowledgeDO> search(String query, int topN) {
        return searchWithMode(query, topN).hits();
    }

    /**
     * 把命中条目拼装为注入 Prompt 的参考资料块，统一使用 &lt;knowledge_base&gt; 标签。
     */
    public String buildReferenceBlock(List<AiKnowledgeDO> hits) {
        if (CollUtil.isEmpty(hits)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<knowledge_base>\n");
        sb.append("【本地 Markdown 知识库优先检索结果（必须首先依此回答）】：\n");
        sb.append("系统在处理用户提问前已优先检索了本地 Markdown 知识库。若以下检索出的文档内容与用户提问相关，你必须【严格以知识库文档为准】进行回答，严禁主观编造；只有在知识库中未提及该问题时，方可结合系统通用功能进行作答。\n\n");
        int used = sb.length();
        int index = 1;
        for (AiKnowledgeDO hit : hits) {
            String body = AiKnowledgeSanitizer.sanitizeForPrompt(
                    StrUtil.brief(StrUtil.nullToEmpty(hit.getContent()), PER_ITEM_MAX_CHARS));
            String section = "### [" + index + "] 《" + StrUtil.nullToEmpty(hit.getTitle()) + "》\n" + body + "\n\n";
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
        sb.append("</knowledge_base>");
        return sb.toString();
    }

    /** LIKE 降级：title/keywords/content 命中，优先取 Top-N */
    private List<AiKnowledgeDO> likeFallback(String query, int topN) {
        try {
            List<String> terms = splitTerms(query);
            if (terms.isEmpty()) {
                return List.of();
            }
            LambdaQueryWrapper<AiKnowledgeDO> wrapper = new LambdaQueryWrapper<AiKnowledgeDO>()
                    .eq(AiKnowledgeDO::getStatus, 1)
                    .and(w -> {
                        for (int i = 0; i < terms.size(); i++) {
                            if (i > 0) {
                                w.or();
                            }
                            String term = terms.get(i);
                            w.like(AiKnowledgeDO::getTitle, term)
                                    .or().like(AiKnowledgeDO::getKeywords, term)
                                    .or().like(AiKnowledgeDO::getContent, term);
                        }
                    })
                    .orderByDesc(AiKnowledgeDO::getSort)
                    .orderByDesc(AiKnowledgeDO::getUpdateTime)
                    .last("LIMIT " + Math.max(1, topN));
            return aiKnowledgeMapper.selectList(wrapper);
        } catch (Exception e) {
            log.warn("AI 知识库 LIKE 降级检索失败，本轮不注入知识: {}", e.getMessage());
            return List.of();
        }
    }

    /** 单次检索最多使用的分词数量，避免长 query 生成大量 OR LIKE */
    private static final int MAX_SEARCH_TERMS = 12;

    /** 提取查询词：优先按空白与常见分隔切分，去重去空；整句本身也作为一项 */
    private List<String> splitTerms(String query) {
        Map<String, Boolean> ordered = new LinkedHashMap<>();
        ordered.put(query, true);
        for (String part : query.split("[\\s,，、;；]+")) {
            if (ordered.size() >= MAX_SEARCH_TERMS) {
                break;
            }
            String p = StrUtil.trim(part);
            if (p.length() >= 2) {
                ordered.put(p, true);
            }
        }
        return new ArrayList<>(ordered.keySet());
    }
}
