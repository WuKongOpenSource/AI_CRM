package com.kakarote.ai_crm.ai.memory;

import com.kakarote.ai_crm.entity.PO.ChatMessage;
import com.kakarote.ai_crm.mapper.ChatMessageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmChatMemoryServiceTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Test
    void loadRecentMessagesReturnsDatabaseDescendingRowsInChronologicalOrder() {
        when(chatMessageMapper.selectList(any())).thenReturn(List.of(
            chatMessage(103L, "assistant", "new answer"),
            chatMessage(102L, "user", "middle question"),
            chatMessage(101L, "assistant", "old answer")
        ));

        CrmChatMemoryService service = new CrmChatMemoryService(chatMessageMapper);
        List<Message> messages = service.loadRecentMessages(1L, 104L);

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(2)).isInstanceOf(AssistantMessage.class);
        assertThat(messages).extracting(this::contentOf)
            .containsExactly("old answer", "middle question", "new answer");
    }

    private ChatMessage chatMessage(Long id, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setMessageId(id);
        message.setSessionId(1L);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(new Date(id));
        return message;
    }

    private String contentOf(Message message) {
        return switch (message) {
            case UserMessage userMessage -> userMessage.getText();
            case AssistantMessage assistantMessage -> assistantMessage.getText();
            default -> "";
        };
    }
}
