package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiKnowledgeDocumentPageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeDocumentRespVO;
import com.admin.server.modules.ai.convert.AiKnowledgeRagConvert;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeChunkDO;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDocumentDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeChunkMapper;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeDocumentMapper;
import com.admin.server.modules.ai.service.AiKnowledgeBaseService;
import com.admin.server.modules.ai.service.AiKnowledgeDocumentService;
import com.admin.server.modules.ai.service.rag.KnowledgeIndexService;
import com.admin.server.modules.ai.service.rag.VectorStore;
import com.admin.server.modules.infra.dal.dataobject.file.SysFileDO;
import com.admin.server.modules.infra.service.file.SysFileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

/**
 * AI 知识库文档管理实现
 */
@Service
public class AiKnowledgeDocumentServiceImpl
        extends ServiceImpl<AiKnowledgeDocumentMapper, AiKnowledgeDocumentDO>
        implements AiKnowledgeDocumentService {

    private static final Logger log = LoggerFactory.getLogger(AiKnowledgeDocumentServiceImpl.class);

    /** 知识库单文件业务上限（与系统上传策略取较小值） */
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx", "md", "txt");

    private static final String PARSE_PENDING = "PENDING";
    private static final String INDEX_PENDING = "PENDING";

    @Resource
    private AiKnowledgeBaseService knowledgeBaseService;

    @Resource
    private AiKnowledgeChunkMapper chunkMapper;

    @Resource
    private SysFileService sysFileService;

    @Resource
    private KnowledgeIndexService knowledgeIndexService;

    @Resource
    private VectorStore vectorStore;

    @Override
    public PageResult<AiKnowledgeDocumentRespVO> page(AiKnowledgeDocumentPageReqVO reqVO) {
        Page<AiKnowledgeDocumentDO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        LambdaQueryWrapper<AiKnowledgeDocumentDO> wrapper = new LambdaQueryWrapper<AiKnowledgeDocumentDO>()
                .eq(AiKnowledgeDocumentDO::getKnowledgeBaseId, reqVO.getKnowledgeBaseId())
                .like(StrUtil.isNotBlank(reqVO.getFileName()), AiKnowledgeDocumentDO::getFileName, reqVO.getFileName())
                .eq(StrUtil.isNotBlank(reqVO.getParseStatus()),
                        AiKnowledgeDocumentDO::getParseStatus, reqVO.getParseStatus())
                .eq(StrUtil.isNotBlank(reqVO.getIndexStatus()),
                        AiKnowledgeDocumentDO::getIndexStatus, reqVO.getIndexStatus())
                .orderByDesc(AiKnowledgeDocumentDO::getId);
        Page<AiKnowledgeDocumentDO> result = page(page, wrapper);
        List<AiKnowledgeDocumentRespVO> voList = new ArrayList<>(result.getRecords().size());
        for (AiKnowledgeDocumentDO document : result.getRecords()) {
            voList.add(AiKnowledgeRagConvert.documentToResp(document));
        }
        return PageResult.of(voList, result.getTotal());
    }

    @Override
    public AiKnowledgeDocumentRespVO detail(Long id) {
        return AiKnowledgeRagConvert.documentToResp(getDocumentOrThrow(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> upload(Long knowledgeBaseId, MultipartFile[] files) {
        knowledgeBaseService.getBaseOrThrow(knowledgeBaseId);
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择需要上传的文档");
        }
        List<Map<String, Object>> results = new ArrayList<>(files.length);
        List<Long> pendingDocuments = new ArrayList<>();
        for (MultipartFile file : files) {
            Map<String, Object> item = new LinkedHashMap<>();
            String fileName = file == null ? "" : StrUtil.nullToEmpty(file.getOriginalFilename());
            item.put("fileName", fileName);
            try {
                validateUpload(file);
                byte[] bytes = file.getBytes();
                String hash = DigestUtil.sha256Hex(bytes);
                Long duplicated = count(new LambdaQueryWrapper<AiKnowledgeDocumentDO>()
                        .eq(AiKnowledgeDocumentDO::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(AiKnowledgeDocumentDO::getContentHash, hash));
                if (duplicated != null && duplicated > 0) {
                    item.put("success", false);
                    item.put("message", "相同内容的文档已存在，已跳过");
                    results.add(item);
                    continue;
                }
                SysFileDO stored = sysFileService.uploadAiKnowledgeFile(file, knowledgeBaseId);
                AiKnowledgeDocumentDO document = new AiKnowledgeDocumentDO();
                document.setKnowledgeBaseId(knowledgeBaseId);
                document.setLegacyKnowledgeId(0L);
                document.setFileId(stored == null ? 0L : stored.getId());
                document.setFileName(StrUtil.brief(fileName, 250));
                document.setFileType(resolveExtension(fileName));
                document.setFileSize(file.getSize());
                document.setContentHash(hash);
                document.setCharCount(0);
                document.setChunkCount(0);
                document.setParseStatus(PARSE_PENDING);
                document.setIndexStatus(INDEX_PENDING);
                document.setErrorMsg("");
                document.setStatus(1);
                save(document);
                pendingDocuments.add(document.getId());
                item.put("success", true);
                item.put("message", "已提交解析，稍后自动切片并向量化");
            } catch (Exception e) {
                log.warn("知识文档上传失败 name={}: {}", fileName, e.getMessage());
                item.put("success", false);
                item.put("message", StrUtil.blankToDefault(e.getMessage(), "上传失败"));
            }
            results.add(item);
        }
        if (!pendingDocuments.isEmpty()) {
            // 等文档记录提交后再投递异步任务，避免异步线程查不到刚插入的文档
            runAfterCommit(() -> pendingDocuments.forEach(this::dispatchProcess));
        }
        knowledgeBaseService.refreshStats(knowledgeBaseId);
        return results;
    }

    @Override
    public void reparse(Long id) {
        getDocumentOrThrow(id);
        update(new LambdaUpdateWrapper<AiKnowledgeDocumentDO>()
                .eq(AiKnowledgeDocumentDO::getId, id)
                .set(AiKnowledgeDocumentDO::getParseStatus, PARSE_PENDING)
                .set(AiKnowledgeDocumentDO::getIndexStatus, INDEX_PENDING)
                .set(AiKnowledgeDocumentDO::getErrorMsg, ""));
        dispatchProcess(id);
    }

    @Override
    public void reindex(Long id) {
        AiKnowledgeDocumentDO document = getDocumentOrThrow(id);
        if (StrUtil.isBlank(document.getContent())) {
            throw new BusinessException("文档尚未解析出正文，请先执行「重新解析」");
        }
        dispatchReindex(id);
    }

    @Override
    public void reindexAll(Long knowledgeBaseId) {
        knowledgeBaseService.getBaseOrThrow(knowledgeBaseId);
        try {
            knowledgeIndexService.reindexKnowledgeBase(knowledgeBaseId);
        } catch (RejectedExecutionException e) {
            throw new BusinessException("索引任务排队已满，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AiKnowledgeDocumentDO document = getDocumentOrThrow(id);
        removeById(id);
        chunkMapper.delete(new LambdaQueryWrapper<AiKnowledgeChunkDO>()
                .eq(AiKnowledgeChunkDO::getDocumentId, id));
        vectorStore.removeByDocument(document.getKnowledgeBaseId(), id);
        knowledgeBaseService.refreshStats(document.getKnowledgeBaseId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            delete(id);
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("文档状态不合法");
        }
        AiKnowledgeDocumentDO document = getDocumentOrThrow(id);
        update(new LambdaUpdateWrapper<AiKnowledgeDocumentDO>()
                .eq(AiKnowledgeDocumentDO::getId, id)
                .set(AiKnowledgeDocumentDO::getStatus, status));
        if (status == 0) {
            // 停用后立即移出向量索引，避免继续被检索召回
            vectorStore.removeByDocument(document.getKnowledgeBaseId(), id);
            knowledgeBaseService.refreshStats(document.getKnowledgeBaseId());
        } else if (StrUtil.isNotBlank(document.getContent())) {
            dispatchReindex(id);
        }
    }

    private void validateUpload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException("单个文档不能超过 20MB");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("仅支持 PDF、Word(.doc/.docx)、Markdown(.md) 与 TXT 文档");
        }
    }

    private String resolveExtension(String fileName) {
        String name = StrUtil.nullToEmpty(fileName);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void dispatchProcess(Long documentId) {
        try {
            knowledgeIndexService.processDocument(documentId);
        } catch (RejectedExecutionException e) {
            log.warn("知识索引队列已满，文档 {} 待处理", documentId);
            update(new LambdaUpdateWrapper<AiKnowledgeDocumentDO>()
                    .eq(AiKnowledgeDocumentDO::getId, documentId)
                    .set(AiKnowledgeDocumentDO::getParseStatus, "FAILED")
                    .set(AiKnowledgeDocumentDO::getErrorMsg, "索引任务排队已满，请稍后点击「重新解析」"));
        }
    }

    private void dispatchReindex(Long documentId) {
        try {
            knowledgeIndexService.reindexDocument(documentId);
        } catch (RejectedExecutionException e) {
            throw new BusinessException("索引任务排队已满，请稍后重试");
        }
    }

    /** 事务提交后执行（无事务时立即执行），保证异步任务能读到已提交数据 */
    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    private AiKnowledgeDocumentDO getDocumentOrThrow(Long id) {
        AiKnowledgeDocumentDO document = id == null ? null : getById(id);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }
        return document;
    }
}
