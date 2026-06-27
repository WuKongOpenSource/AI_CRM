package com.kakarote.ai_crm.im.event;

import java.util.List;

public record ImMessageRecalledEvent(Long conversationId, Long messageId, List<Long> memberUserIds) {
}
