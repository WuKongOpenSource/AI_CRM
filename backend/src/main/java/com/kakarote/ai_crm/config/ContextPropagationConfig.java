package com.kakarote.ai_crm.config;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Micrometer Context Propagation 配置
 *
 * 使用 Spring Boot 3.3+ 的 Context Propagation 功能，
 * 自动将 AiContextHolder 中的 ThreadLocal 值传播到 Reactor 的操作中。
 *
 * 这是解决 Spring AI 工具调用在不同线程执行时无法获取用户上下文的关键配置。
 *
 * 工作原理：
 * 1. 在主线程设置 AiContextHolder.setContext(sessionId, userId)
 * 2. sessionId 同时存储在 ThreadLocal 和全局 ConcurrentHashMap 中
 * 3. 当 Reactor 切换到其他线程时，context-propagation 会自动传播 sessionId
 * 4. 在工具执行线程中，通过 sessionId 从全局 Map 中获取 userId
 */
@Slf4j
@Configuration
public class ContextPropagationConfig {

    @PostConstruct
    public void init() {
        log.info("初始化 Context Propagation 配置...");

        // 启用 Reactor 的自动上下文传播
        Hooks.enableAutomaticContextPropagation();
        log.info("已启用 Reactor 自动上下文传播");

        // 注册 SessionId 的 ThreadLocalAccessor
        ContextRegistry.getInstance().registerThreadLocalAccessor(new AiSessionIdAccessor());
        log.info("已注册 AiSessionIdAccessor");

        log.info("Context Propagation 配置完成");
    }

    /**
     * AI 会话 ID 的 ThreadLocalAccessor
     * 用于在 Reactor 操作符之间传播 sessionId
     */
    private static class AiSessionIdAccessor implements ThreadLocalAccessor<Long> {
        private static final String KEY = "ai.context.sessionId";

        @Override
        public Object key() {
            return KEY;
        }

        @Override
        public Long getValue() {
            Long sessionId = AiContextHolder.getCurrentSessionId();
            if (sessionId != null) {
                log.trace("Context Propagation 获取 sessionId: {}, 线程: {}",
                    sessionId, Thread.currentThread().getName());
            }
            return sessionId;
        }

        @Override
        public void setValue(Long sessionId) {
            if (sessionId != null) {
                // 恢复上下文：设置 sessionId，同时从全局 Map 恢复 userId
                AiContextHolder.setSessionId(sessionId);
                log.trace("Context Propagation 恢复 sessionId: {}, 线程: {}",
                    sessionId, Thread.currentThread().getName());
            }
        }

        @Override
        public void setValue() {
            // 当上下文需要被清除时调用
            // 不执行任何操作，让 Reactor 流结束时自然清理
            log.trace("Context Propagation setValue() 调用, 线程: {}",
                Thread.currentThread().getName());
        }
    }
}
