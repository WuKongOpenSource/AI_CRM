package com.kakarote.ai_crm.ai.memory;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CrmChatMemoryAdvisor implements BaseAdvisor {

    public static final String CURRENT_MESSAGE_ID = "crm_current_message_id";

    private final CrmChatMemoryService chatMemoryService;

    public CrmChatMemoryAdvisor(CrmChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        Long sessionId = getLongContextValue(request.context(), ChatMemory.CONVERSATION_ID);
        if (sessionId == null) {
            return request;
        }

        Long currentMessageId = getLongContextValue(request.context(), CURRENT_MESSAGE_ID);
        List<Message> history = chatMemoryService.loadRecentMessages(sessionId, currentMessageId);
        if (history.isEmpty()) {
            return request;
        }

        List<Message> messages = new ArrayList<>(history.size() + request.prompt().getInstructions().size());
        messages.addAll(history);
        messages.addAll(request.prompt().getInstructions());
        moveFirstSystemMessageToFront(messages);

        Prompt nextPrompt = request.prompt().mutate().messages(messages).build();
        return request.mutate().prompt(nextPrompt).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public int getOrder() {
        return Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;
    }

    private Long getLongContextValue(Map<String, Object> context, String key) {
        if (context == null || key == null || !context.containsKey(key)) {
            return null;
        }
        Object value = context.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void moveFirstSystemMessageToFront(List<Message> messages) {
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof SystemMessage) {
                Message systemMessage = messages.remove(i);
                messages.add(0, systemMessage);
                return;
            }
        }
    }
}
