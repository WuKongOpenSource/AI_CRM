package com.kakarote.ai_crm.ai.tools.support;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionRequest.ToolChoiceBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Forces the first CRM write-model hop to call a tool, then lets the model write the
 * final answer normally after the tool result is in the conversation.
 */
@Component
public class CrmRequiredToolCallAdvisor extends ToolCallAdvisor {

    private static final String REQUIRED_TOOL_CHOICE = "required";

    public CrmRequiredToolCallAdvisor(ToolCallingManager toolCallingManager) {
        super(toolCallingManager, Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 100, true, false);
    }

    @Override
    protected ChatClientRequest doBeforeCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return applyToolChoice(chatClientRequest);
    }

    @Override
    protected ChatClientRequest doBeforeStream(ChatClientRequest chatClientRequest,
                                               StreamAdvisorChain streamAdvisorChain) {
        return applyToolChoice(chatClientRequest);
    }

    private ChatClientRequest applyToolChoice(ChatClientRequest chatClientRequest) {
        if (chatClientRequest.prompt().getOptions() instanceof OpenAiChatOptions options) {
            boolean toolAlreadyRan = hasToolResponse(chatClientRequest.prompt().getInstructions());
            options.setToolChoice(toolAlreadyRan ? ToolChoiceBuilder.AUTO : REQUIRED_TOOL_CHOICE);
            options.setParallelToolCalls(Boolean.FALSE);
        }
        return chatClientRequest;
    }

    private boolean hasToolResponse(List<Message> messages) {
        return messages.stream().anyMatch(ToolResponseMessage.class::isInstance);
    }
}
