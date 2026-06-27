package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.PO.ImConversation;

import java.util.List;

public interface ImConversationService extends IService<ImConversation> {

    /** Find or create the 1:1 direct conversation between the current user and peerUserId. */
    ImConversation getOrCreateDirect(Long currentUserId, Long peerUserId);

    /** All member user IDs of a conversation. */
    List<Long> memberUserIds(Long conversationId);

    /** Throws BusinessException if userId is not a member of conversationId. */
    void assertMember(Long conversationId, Long userId);

    /** Unread count for userId in conversationId (messages with id > lastRead, not own, status=normal). */
    long unreadCount(Long conversationId, Long userId);

    /** Build the canonical member_key for a user pair. */
    static String directMemberKey(Long a, Long b) {
        long lo = Math.min(a, b);
        long hi = Math.max(a, b);
        return lo + "_" + hi;
    }

    /** All conversations the user is a member of, newest-activity first, with peer info, last message, and unread. */
    java.util.List<com.kakarote.ai_crm.entity.VO.ImConversationVO> listMyConversations(Long userId);

    ImConversation createChannel(Long creatorId, String name, String description, String visibility, java.util.List<Long> memberIds);
    java.util.List<com.kakarote.ai_crm.entity.VO.ImConversationVO> browsePublicChannels(Long userId, String keyword);
    void joinChannel(Long userId, Long channelId);
    void leaveChannel(Long userId, Long channelId);
    void addMembers(Long actorId, Long channelId, java.util.List<Long> userIds);
}
