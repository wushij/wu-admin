package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiKnowledgePageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeSaveReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeTestRetrieveReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeTestRetrieveRespVO;
import com.admin.server.modules.ai.convert.AiKnowledgeConvert;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeMapper;
import com.admin.server.modules.ai.service.AiKnowledgeService;
import com.admin.server.modules.ai.util.AiKnowledgeSanitizer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AiKnowledgeServiceImpl extends ServiceImpl<AiKnowledgeMapper, AiKnowledgeDO>
        implements AiKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeServiceImpl.class);

    private static final Set<String> CATEGORIES = Set.of("faq", "manual", "module", "other");
    private static final int MAX_TITLE_LEN = 128;
    private static final int MAX_KEYWORDS_LEN = 255;
    private static final int MAX_CONTENT_LEN = 50_000;
    /** 单个 Markdown 文件大小上限，避免一次性读入超大文件 */
    private static final long MAX_MD_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_BATCH_SIZE = 500;
    private static final int MAX_BATCH_IDS = 500;
    private static final int TEST_RETRIEVE_TOP_N = 5;
    /** 单次检索最多使用的分词数量，避免长 query 生成大量 OR LIKE */
    private static final int MAX_SEARCH_TERMS = 12;

    @Resource
    private KnowledgeRetriever knowledgeRetriever;

    @Override
    public PageResult<AiKnowledgeRespVO> page(AiKnowledgePageReqVO reqVO) {
        Page<AiKnowledgeDO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        LambdaQueryWrapper<AiKnowledgeDO> wrapper = new LambdaQueryWrapper<AiKnowledgeDO>()
                .eq(StrUtil.isNotBlank(reqVO.getCategory()), AiKnowledgeDO::getCategory, reqVO.getCategory())
                .eq(reqVO.getStatus() != null, AiKnowledgeDO::getStatus, reqVO.getStatus())
                .and(StrUtil.isNotBlank(reqVO.getKeyword()), w -> w
                        .like(AiKnowledgeDO::getTitle, reqVO.getKeyword())
                        .or().like(AiKnowledgeDO::getKeywords, reqVO.getKeyword()))
                .orderByDesc(AiKnowledgeDO::getSort)
                .orderByDesc(AiKnowledgeDO::getUpdateTime);
        Page<AiKnowledgeDO> result = page(page, wrapper);
        return PageResult.of(AiKnowledgeConvert.convertList(result.getRecords()), result.getTotal());
    }

    @Override
    public AiKnowledgeRespVO detail(Long id) {
        return AiKnowledgeConvert.convert(getOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(AiKnowledgeSaveReqVO reqVO) {
        validate(reqVO);
        AiKnowledgeDO knowledge = AiKnowledgeConvert.convertSave(reqVO);
        knowledge.setId(null);
        applyDefaults(knowledge);
        save(knowledge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AiKnowledgeSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw new BusinessException("知识ID不能为空");
        }
        getOrThrow(reqVO.getId());
        validate(reqVO);
        AiKnowledgeDO knowledge = AiKnowledgeConvert.convertSave(reqVO);
        applyDefaults(knowledge);
        updateById(knowledge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getOrThrow(id);
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        getOrThrow(id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0 或 1");
        }
        lambdaUpdate().eq(AiKnowledgeDO::getId, id).set(AiKnowledgeDO::getStatus, status).update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (CollUtil.isEmpty(ids)) {
            throw new BusinessException("请选择要操作的知识条目");
        }
        if (ids.size() > MAX_BATCH_IDS) {
            throw new BusinessException("单次最多操作 " + MAX_BATCH_IDS + " 条知识");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态值只能为 0 或 1");
        }
        lambdaUpdate().in(AiKnowledgeDO::getId, ids).set(AiKnowledgeDO::getStatus, status).update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new BusinessException("请选择要删除的知识条目");
        }
        if (ids.size() > MAX_BATCH_IDS) {
            throw new BusinessException("单次最多删除 " + MAX_BATCH_IDS + " 条知识");
        }
        removeByIds(ids);
    }

    @Override
    public AiKnowledgeTestRetrieveRespVO testRetrieve(AiKnowledgeTestRetrieveReqVO reqVO) {
        String query = StrUtil.trim(reqVO.getQuery());
        AiKnowledgeTestRetrieveRespVO resp = new AiKnowledgeTestRetrieveRespVO();
        resp.setQuery(query);
        if (StrUtil.isBlank(query)) {
            resp.setSearchTerms(Collections.emptyList());
            resp.setMatchMode("NONE");
            resp.setHitList(Collections.emptyList());
            resp.setReferenceBlock("");
            resp.setCharCount(0);
            resp.setEstimatedTokens(0);
            return resp;
        }

        List<String> terms = splitTerms(query);
        resp.setSearchTerms(terms);

        // 与真实对话共用同一套检索与拼装逻辑，避免“测试无命中、实际命中”
        KnowledgeRetriever.SearchResult searchResult =
                knowledgeRetriever.searchWithMode(query, TEST_RETRIEVE_TOP_N);
        List<AiKnowledgeDO> hits = searchResult.hits();

        if (CollUtil.isEmpty(hits)) {
            resp.setMatchMode("NONE");
            resp.setHitList(Collections.emptyList());
            resp.setReferenceBlock("");
            resp.setCharCount(0);
            resp.setEstimatedTokens(0);
            return resp;
        }

        resp.setMatchMode(searchResult.matchMode());
        resp.setHitList(AiKnowledgeConvert.convertList(hits));

        String refBlock = knowledgeRetriever.buildReferenceBlock(hits);
        resp.setReferenceBlock(refBlock);
        resp.setCharCount(refBlock.length());
        resp.setEstimatedTokens((int) Math.ceil(refBlock.length() / 1.5));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchImport(List<AiKnowledgeSaveReqVO> list) {
        if (CollUtil.isEmpty(list)) {
            throw new BusinessException("导入数据不能为空");
        }
        if (list.size() > MAX_BATCH_SIZE) {
            throw new BusinessException("单次最多导入 " + MAX_BATCH_SIZE + " 条知识");
        }
        for (AiKnowledgeSaveReqVO reqVO : list) {
            validate(reqVO);
            AiKnowledgeDO knowledge = AiKnowledgeConvert.convertSave(reqVO);
            knowledge.setId(null);
            applyDefaults(knowledge);

            // 如果相同标题已存在，则更新，否则新增
            AiKnowledgeDO existing = getOne(new LambdaQueryWrapper<AiKnowledgeDO>()
                    .eq(AiKnowledgeDO::getTitle, knowledge.getTitle())
                    .last("LIMIT 1"));
            if (existing != null) {
                knowledge.setId(existing.getId());
                updateById(knowledge);
            } else {
                save(knowledge);
            }
        }
    }

    @Override
    public List<AiKnowledgeRespVO> exportAll() {
        List<AiKnowledgeDO> list = list(new LambdaQueryWrapper<AiKnowledgeDO>()
                .orderByDesc(AiKnowledgeDO::getSort)
                .orderByDesc(AiKnowledgeDO::getUpdateTime));
        return AiKnowledgeConvert.convertList(list);
    }

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

    private AiKnowledgeDO getOrThrow(Long id) {
        AiKnowledgeDO knowledge = getById(id);
        if (knowledge == null) {
            throw new BusinessException("知识条目不存在");
        }
        return knowledge;
    }

    /** 入库前校验并归一化：标题/正文必填、分类合法、正文围栏成对闭合 */
    private void validate(AiKnowledgeSaveReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getTitle())) {
            throw new BusinessException("知识标题不能为空");
        }
        if (reqVO.getTitle().length() > MAX_TITLE_LEN) {
            throw new BusinessException("知识标题长度不能超过 " + MAX_TITLE_LEN + " 字");
        }
        if (StrUtil.isBlank(reqVO.getContent())) {
            throw new BusinessException("知识正文不能为空");
        }
        if (reqVO.getContent().length() > MAX_CONTENT_LEN) {
            throw new BusinessException("知识正文长度不能超过 " + MAX_CONTENT_LEN + " 字");
        }
        if (StrUtil.isNotBlank(reqVO.getKeywords()) && reqVO.getKeywords().length() > MAX_KEYWORDS_LEN) {
            throw new BusinessException("关键词长度不能超过 " + MAX_KEYWORDS_LEN + " 字");
        }
        if (StrUtil.isNotBlank(reqVO.getCategory()) && !CATEGORIES.contains(reqVO.getCategory())) {
            throw new BusinessException("分类只能为 faq/manual/module/other");
        }
        if (!AiKnowledgeSanitizer.isFenceBalanced(reqVO.getContent())) {
            throw new BusinessException("知识正文中的代码块围栏 ``` 未成对闭合，请检查后重试");
        }
    }

    private void applyDefaults(AiKnowledgeDO knowledge) {
        if (StrUtil.isBlank(knowledge.getCategory())) {
            knowledge.setCategory("faq");
        }
        if (knowledge.getSort() == null) {
            knowledge.setSort(0);
        }
        if (knowledge.getStatus() == null) {
            knowledge.setStatus(1);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadMd(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择需要上传的 Markdown (.md) 文件");
        }
        int total = 0;
        int created = 0;
        int updated = 0;
        List<String> successFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalFilename = file.getOriginalFilename();
            if (StrUtil.isBlank(originalFilename)) {
                continue;
            }
            String lowerName = originalFilename.toLowerCase();
            if (!lowerName.endsWith(".md") && !lowerName.endsWith(".markdown")) {
                throw new BusinessException("仅支持上传 .md 或 .markdown 格式的文件: " + originalFilename);
            }
            if (file.getSize() > MAX_MD_FILE_BYTES) {
                throw new BusinessException("Markdown 文件不能超过 10MB: " + originalFilename);
            }

            // 提取文档标题（去除 .md 后缀）
            String title = originalFilename;
            if (lowerName.endsWith(".md")) {
                title = originalFilename.substring(0, originalFilename.length() - 3);
            } else if (lowerName.endsWith(".markdown")) {
                title = originalFilename.substring(0, originalFilename.length() - 9);
            }
            title = StrUtil.brief(title.trim(), MAX_TITLE_LEN);

            // 读取文件内容为 UTF-8 文本
            String content;
            try {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("读取 Markdown 文件失败: {}", originalFilename, e);
                throw new BusinessException("读取文件 [" + originalFilename + "] 失败: " + e.getMessage());
            }

            if (StrUtil.isBlank(content)) {
                continue;
            }
            if (content.length() > MAX_CONTENT_LEN) {
                content = content.substring(0, MAX_CONTENT_LEN);
            }

            String keywords = extractKeywordsFromMd(title, content);

            // 检查同名文档
            AiKnowledgeDO existing = getOne(new LambdaQueryWrapper<AiKnowledgeDO>()
                    .eq(AiKnowledgeDO::getTitle, title)
                    .last("LIMIT 1"));

            if (existing != null) {
                existing.setContent(content);
                existing.setKeywords(keywords);
                existing.setStatus(1); // 上传后默认启用
                applyDefaults(existing);
                updateById(existing);
                updated++;
            } else {
                AiKnowledgeDO doc = new AiKnowledgeDO();
                doc.setTitle(title);
                doc.setContent(content);
                doc.setKeywords(keywords);
                doc.setCategory("manual");
                doc.setSort(10);
                doc.setStatus(1);
                applyDefaults(doc);
                save(doc);
                created++;
            }
            total++;
            successFiles.add(title);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("created", created);
        result.put("updated", updated);
        result.put("files", successFiles);
        return result;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> row = baseMapper.selectStats();
        long total = numberValue(row, "total");
        long activeCount = numberValue(row, "activeCount");
        long disabledCount = total - activeCount;
        long totalWords = numberValue(row, "totalWords");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("activeCount", activeCount);
        stats.put("disabledCount", disabledCount);
        stats.put("totalWords", totalWords);
        return stats;
    }

    private long numberValue(Map<String, Object> row, String key) {
        if (row == null) {
            return 0L;
        }
        Object value = row.get(key);
        if (value == null) {
            value = row.get(key.toLowerCase(Locale.ROOT));
        }
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private String extractKeywordsFromMd(String title, String content) {
        Set<String> set = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(title)) {
            set.add(title);
        }
        // 提取 Markdown 中的 # 一级、二级标题
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") && !trimmed.startsWith("####")) {
                String heading = trimmed.replaceAll("^#+\\s*", "").trim();
                if (heading.length() >= 2 && heading.length() <= 30) {
                    set.add(heading);
                }
            }
            if (set.size() >= 8) {
                break;
            }
        }
        return StrUtil.brief(String.join(",", set), MAX_KEYWORDS_LEN);
    }
}
