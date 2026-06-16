package com.kakarote.ai_crm.ai.tools.support;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.ToolChoiceBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CrmRequiredToolCallAdvisorTest {

    @Test
    void requiresToolChoiceBeforeAnyToolRuns() {
        TestAdvisor advisor = new TestAdvisor();
        OpenAiChatOptions options = OpenAiChatOptions.builder().build();

        advisor.apply(request(List.of(new UserMessage("创建联系人")), options));

        assertThat(options.getToolChoice()).isEqualTo("required");
        assertThat(options.getParallelToolCalls()).isFalse();
    }

    @Test
    void restoresAutoToolChoiceAfterToolResponse() {
        TestAdvisor advisor = new TestAdvisor();
        OpenAiChatOptions options = OpenAiChatOptions.builder().build();
        ToolResponseMessage toolResponse = ToolResponseMessage.builder()
            .responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "createContact", "ok")))
            .build();

        advisor.apply(request(List.of(new UserMessage("创建联系人"), toolResponse), options));

        assertThat(options.getToolChoice()).isEqualTo(ToolChoiceBuilder.AUTO);
        assertThat(options.getParallelToolCalls()).isFalse();
    }

    private ChatClientRequest request(List<Message> messages, OpenAiChatOptions options) {
        return ChatClientRequest.builder()
            .prompt(new Prompt(messages, options))
            .context(Map.of())
            .build();
    }

    private static class TestAdvisor extends CrmRequiredToolCallAdvisor {

        TestAdvisor() {
            super(mock(ToolCallingManager.class));
        }

        void apply(ChatClientRequest request) {
            super.doBeforeCall(request, mock(CallAdvisorChain.class));
        }
    }
}
