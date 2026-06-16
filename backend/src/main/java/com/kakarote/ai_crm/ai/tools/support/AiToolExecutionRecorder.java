package com.kakarote.ai_crm.ai.tools.support;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records AI tool executions for one chat turn so actual tool exceptions can be
 * surfaced without inferring success or failure from natural-language text.
 */
@Component
public class AiToolExecutionRecorder {

    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<ToolExecution>> EXECUTIONS =
            new ConcurrentHashMap<>();

    public void begin(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        EXECUTIONS.put(sessionId, new CopyOnWriteArrayList<>());
    }

    public void record(String toolName, String methodName, Object result, Throwable throwable) {
        Long sessionId = AiContextHolder.getCurrentSessionId();
        if (sessionId == null) {
            return;
        }

        String resultText = result instanceof String text ? StrUtil.trim(text) : null;
        String errorReason = resolveErrorReason(throwable);
        ToolExecution execution = new ToolExecution(
                toolName,
                methodName,
                resultText,
                errorReason
        );
        EXECUTIONS.computeIfAbsent(sessionId, ignored -> new CopyOnWriteArrayList<>()).add(execution);
    }

    public ToolExecution getLatestFailure(Long sessionId) {
        List<ToolExecution> executions = getExecutions(sessionId);
        for (int i = executions.size() - 1; i >= 0; i--) {
            ToolExecution execution = executions.get(i);
            if (execution.failed()) {
                return execution;
            }
        }
        return null;
    }

    public void finish(Long sessionId) {
        if (sessionId != null) {
            EXECUTIONS.remove(sessionId);
        }
    }

    private List<ToolExecution> getExecutions(Long sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        return EXECUTIONS.getOrDefault(sessionId, new CopyOnWriteArrayList<>());
    }

    private String resolveErrorReason(Throwable throwable) {
        if (throwable != null) {
            return StrUtil.blankToDefault(throwable.getMessage(), throwable.getClass().getSimpleName());
        }
        return null;
    }

    public record ToolExecution(
            String toolName,
            String methodName,
            String resultText,
            String errorReason
    ) {
        public boolean failed() {
            return StrUtil.isNotBlank(errorReason);
        }
    }
}
