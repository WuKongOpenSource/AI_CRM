package com.kakarote.ai_crm.im.event;

/** Published after a member marks a conversation read; carries the new unread (0) for that user. */
public record ImConversationReadEvent(Long conversationId, Long userId) {
}
