package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.service.ImConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImPushService {

    public static final String USER_QUEUE = "/queue/im";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ImConversationService conversationService;

    /** Push a message (new or recalled) to each member, each with their own unread count. */
    public void pushMessage(Long conversationId, ImMessageVO message, List<Long> memberUserIds) {
        for (Long uid : memberUserIds) {
            long unread = conversationService.unreadCount(conversationId, uid);
            ImPushEnvelope env = new ImPushEnvelope("message", String.valueOf(conversationId), message, unread);
            // Spring resolves this to /user/{userId}/queue/im (user prefix + session suffix) for SimpleBroker.
            messagingTemplate.convertAndSendToUser(String.valueOf(uid), USER_QUEUE, env);
        }
    }

    /** Push an unread refresh (e.g. after the user themselves marked read). */
    public void pushUnread(Long conversationId, Long userId) {
        long unread = conversationService.unreadCount(conversationId, userId);
        ImPushEnvelope env = new ImPushEnvelope("unread", String.valueOf(conversationId), null, unread);
        // Spring resolves this to /user/{userId}/queue/im (user prefix + session suffix) for SimpleBroker.
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_QUEUE, env);
    }
}
