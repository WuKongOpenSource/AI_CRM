package com.kakarote.ai_crm.im.event;

import java.util.List;

/** Published after a message is persisted. memberUserIds = all members of the conversation. */
public record ImMessageSentEvent(Long conversationId, Long messageId, List<Long> memberUserIds) {
}
