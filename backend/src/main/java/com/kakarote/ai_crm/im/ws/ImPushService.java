package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.service.ImConversationService;
import com.kakarote.ai_crm.service.ImReactionService;
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

    @Autowired
    private ImReactionService reactionService;

    /** Push a message (new or recalled) to each member, each with their own unread count. */
    public void pushMessage(Long conversationId, ImMessageVO message, List<Long> memberUserIds) {
        for (Long uid : memberUserIds) {
            long unread = conversationService.unreadCount(conversationId, uid);
            messagingTemplate.convertAndSendToUser(String.valueOf(uid), USER_QUEUE,
                    ImPushEnvelope.message(String.valueOf(conversationId), message, unread));
        }
    }

    /** Push an unread refresh (e.g. after the user themselves marked read). */
    public void pushUnread(Long conversationId, Long userId) {
        long unread = conversationService.unreadCount(conversationId, userId);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_QUEUE,
                ImPushEnvelope.unread(String.valueOf(conversationId), unread));
    }

    /** Push fresh aggregated reactions for a message to each member (per-member `mine`). */
    public void pushReaction(Long conversationId, Long messageId, List<Long> memberUserIds) {
        for (Long uid : memberUserIds) {
            ImPushEnvelope env = ImPushEnvelope.reaction(
                    String.valueOf(conversationId), String.valueOf(messageId),
                    reactionService.aggregate(messageId, uid));
            messagingTemplate.convertAndSendToUser(String.valueOf(uid), USER_QUEUE, env);
        }
    }
}
