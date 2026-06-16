package com.kakarote.ai_crm.ai.tools.support;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiToolExecutionRecorderTest {

    private static final Long SESSION_ID = 91001L;

    private final AiToolExecutionRecorder recorder = new AiToolExecutionRecorder();

    @AfterEach
    void tearDown() {
        recorder.finish(SESSION_ID);
        AiContextHolder.clear();
        AiContextHolder.clearSession(SESSION_ID);
    }

    @Test
    void doesNotInferFailureFromReturnedText() {
        AiContextHolder.setContext(SESSION_ID, 10001L, 20001L);
        recorder.begin(SESSION_ID);

        recorder.record("ContactTools", "createContact", "创建联系人失败: 缺少关联客户。", null);

        assertThat(recorder.getLatestFailure(SESSION_ID)).isNull();
    }

    @Test
    void recordsThrownExceptionAsFailure() {
        AiContextHolder.setContext(SESSION_ID, 10001L, 20001L);
        recorder.begin(SESSION_ID);

        recorder.record("ContactTools", "createContact", null, new IllegalStateException("tool exploded"));

        AiToolExecutionRecorder.ToolExecution failure = recorder.getLatestFailure(SESSION_ID);
        assertThat(failure).isNotNull();
        assertThat(failure.errorReason()).isEqualTo("tool exploded");
    }
}
