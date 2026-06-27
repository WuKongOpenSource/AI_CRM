package com.kakarote.ai_crm.im.event;

import java.util.List;

/** Published after a reaction is toggled. */
public record ImReactionChangedEvent(Long conversationId, Long messageId, List<Long> memberUserIds) {
}
