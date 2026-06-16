package com.kakarote.ai_crm.ai.memory;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.entity.PO.ChatMessage;
import com.kakarote.ai_crm.mapper.ChatMessageMapper;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class CrmChatMemoryService {

    private static final int DEFAULT_MAX_MESSAGES = 20;

    private final ChatMessageMapper chatMessageMapper;

    public CrmChatMemoryService(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    public List<Message> loadRecentMessages(Long sessionId, Long beforeMessageId) {
        if (sessionId == null) {
            return List.of();
        }

        List<ChatMessage> dbMessages = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .lt(beforeMessageId != null, ChatMessage::getMessageId, beforeMessageId)
                .orderByDesc(ChatMessage::getCreateTime)
                .orderByDesc(ChatMessage::getMessageId)
                .last("LIMIT " + DEFAULT_MAX_MESSAGES)
        );

        if (dbMessages == null || dbMessages.isEmpty()) {
            return List.of();
        }

        List<ChatMessage> chronological = new ArrayList<>(dbMessages);
        Collections.reverse(chronological);

        List<Message> messages = new ArrayList<>(chronological.size());
        for (ChatMessage dbMsg : chronological) {
            Message message = toMessage(dbMsg);
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    private Message toMessage(ChatMessage dbMsg) {
        if (dbMsg == null || StrUtil.isBlank(dbMsg.getContent())) {
            return null;
        }
        return switch (StrUtil.nullToEmpty(dbMsg.getRole()).trim()) {
            case "user" -> new UserMessage(dbMsg.getContent());
            case "assistant" -> new AssistantMessage(dbMsg.getContent());
            case "system" -> new SystemMessage(dbMsg.getContent());
            default -> null;
        };
    }
}
