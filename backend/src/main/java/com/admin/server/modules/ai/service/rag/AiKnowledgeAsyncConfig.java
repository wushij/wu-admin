package com.admin.server.modules.ai.service.rag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 知识库文档处理专用线程池。
 *
 * <p>解析与向量化属于分钟级长任务，不能复用全局 {@code @Async} 池：
 * 全局池使用 CallerRunsPolicy，队列打满后会把任务压回 Web 请求线程，拖垮上传接口。
 * 这里使用小并发 + 有限队列 + AbortPolicy，队列满时由上层明确提示稍后重试。</p>
 */
@Configuration
public class AiKnowledgeAsyncConfig {

    public static final String EXECUTOR_NAME = "aiKnowledgeExecutor";

    @Bean(EXECUTOR_NAME)
    public ThreadPoolTaskExecutor aiKnowledgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("ai-knowledge-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
