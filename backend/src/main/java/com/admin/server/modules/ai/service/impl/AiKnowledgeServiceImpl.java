package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiKnowledgePageReqVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeRespVO;
import com.admin.server.modules.ai.api.vo.AiKnowledgeSaveReqVO;
import com.admin.server.modules.ai.convert.AiKnowledgeConvert;
import com.admin.server.modules.ai.dal.dataobject.AiKnowledgeDO;
import com.admin.server.modules.ai.dal.mysql.AiKnowledgeMapper;
import com.admin.server.modules.ai.service.AiKnowledgeService;
import com.admin.server.modules.ai.util.AiKnowledgeSanitizer;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AiKnowledgeServiceImpl extends ServiceImpl<AiKnowledgeMapper, AiKnowledgeDO>
        implements AiKnowledgeService {

    private static final Set<String> CATEGORIES = Set.of("faq", "manual", "module", "other");
    private static final int MAX_TITLE_LEN = 128;
    private static final int MAX_KEYWORDS_LEN = 255;

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
}
