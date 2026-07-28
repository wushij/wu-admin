package com.admin.server.modules.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.admin.server.common.core.PageResult;
import com.admin.server.common.exception.BusinessException;
import com.admin.server.modules.ai.api.vo.AiChatLogPageReqVO;
import com.admin.server.modules.ai.dal.dataobject.AiChatLogDO;
import com.admin.server.modules.ai.dal.mysql.AiChatLogMapper;
import com.admin.server.modules.ai.service.AiChatLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AiChatLogServiceImpl extends ServiceImpl<AiChatLogMapper, AiChatLogDO> implements AiChatLogService {

    private static final Logger log = LoggerFactory.getLogger(AiChatLogServiceImpl.class);

    @Override
    public PageResult<AiChatLogDO> page(AiChatLogPageReqVO reqVO) {
        Page<AiChatLogDO> page = new Page<>(reqVO.getPageNo(), reqVO.getPageSize());
        LambdaQueryWrapper<AiChatLogDO> wrapper = new LambdaQueryWrapper<AiChatLogDO>()
                .like(StrUtil.isNotBlank(reqVO.getUsername()), AiChatLogDO::getUsername, reqVO.getUsername())
                .eq(StrUtil.isNotBlank(reqVO.getProvider()), AiChatLogDO::getProvider, reqVO.getProvider())
                .eq(reqVO.getChatStatus() != null, AiChatLogDO::getChatStatus, reqVO.getChatStatus())
                .eq(StrUtil.isNotBlank(reqVO.getConversationId()), AiChatLogDO::getConversationId, reqVO.getConversationId())
                .orderByDesc(AiChatLogDO::getId);
        Page<AiChatLogDO> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    @Override
    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(404, "对话日志不存在");
        }
    }

    @Override
    public void cleanLogs() {
        remove(new LambdaQueryWrapper<>());
    }

    @Async
    @Override
    public void saveLogAsync(AiChatLogDO chatLog) {
        try {
            save(chatLog);
        } catch (Exception e) {
            log.error("AI 对话日志异步落库失败: {}", e.getMessage());
        }
    }
}
