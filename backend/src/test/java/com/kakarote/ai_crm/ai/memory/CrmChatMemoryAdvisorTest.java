package com.kakarote.ai_crm.ai.memory;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrmChatMemoryAdvisorTest {

    @Test
    void beforeInjectsReadOnlyHistoryUsingConversationIdContext() {
        CrmChatMemoryService chatMemoryService = mock(CrmChatMemoryService.class);
        when(chatMemoryService.loadRecentMessages(10L, 20L))
            .thenReturn(List.of(new UserMessage("previous question")));

        CrmChatMemoryAdvisor advisor = new CrmChatMemoryAdvisor(chatMemoryService);
        ChatClientRequest request = ChatClientRequest.builder()
            .prompt(new Prompt(List.of(new SystemMessage("system"), new UserMessage("current question"))))
            .context(Map.of(
                ChatMemory.CONVERSATION_ID, 10L,
                CrmChatMemoryAdvisor.CURRENT_MESSAGE_ID, 20L
            ))
            .build();

        ChatClientRequest advisedRequest = advisor.before(request, null);
        List<Message> messages = advisedRequest.prompt().getInstructions();

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(messages).extracting(this::contentOf)
            .containsExactly("system", "previous question", "current question");
        verify(chatMemoryService).loadRecentMessages(10L, 20L);
    }

    private String contentOf(Message message) {
        return switch (message) {
            case SystemMessage systemMessage -> systemMessage.getText();
            case UserMessage userMessage -> userMessage.getText();
            default -> "";
        };
    }
}
