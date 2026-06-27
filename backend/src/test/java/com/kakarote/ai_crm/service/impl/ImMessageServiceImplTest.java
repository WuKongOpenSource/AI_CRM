package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.ImSendMessageBO;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.mapper.ImConversationMapper;
import com.kakarote.ai_crm.mapper.ImConversationMemberMapper;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.service.ImConversationService;
import com.kakarote.ai_crm.service.ImReactionService;
import com.kakarote.ai_crm.service.ManageUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImMessageServiceImplTest {

    @Mock ImMessageMapper messageMapper;
    @Mock ImConversationService conversationService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock ImReactionService reactionService;
    @Mock ManageUserService manageUserService;
    @Mock ImConversationMapper conversationMapper;
    @Mock ImConversationMemberMapper memberMapper;

    ImMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImMessageServiceImpl();
        ReflectionTestUtils.setField(service, "conversationService", conversationService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        ReflectionTestUtils.setField(service, "reactionService", reactionService);
        ReflectionTestUtils.setField(service, "manageUserService", manageUserService);
        ReflectionTestUtils.setField(service, "conversationMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);
        // ServiceImpl exposes the mapper via baseMapper; inject it for getById/updateById.
        ReflectionTestUtils.setField(service, "baseMapper", messageMapper);
        when(conversationService.memberUserIds(any())).thenReturn(List.of(1L, 2L));
        // reactionService.aggregate returns empty list for all messages (called from toVO)
        when(reactionService.aggregate(anyLong(), any())).thenReturn(java.util.List.of());
    }

    private ImMessage msg(long id, long sender, Date created, String status) {
        ImMessage m = new ImMessage();
        m.setId(id); m.setSenderId(sender); m.setConversationId(10L);
        m.setContentType("text"); m.setContent("hi"); m.setStatus(status); m.setCreateTime(created);
        return m;
    }

    @Test
    void recallWithinWindowSucceeds() {
        ImMessage m = msg(100L, 1L, new Date(), "normal");
        when(messageMapper.selectById(100L)).thenReturn(m);
        when(messageMapper.updateById(org.mockito.ArgumentMatchers.<ImMessage>any())).thenReturn(1);

        var vo = service.recall(100L, 1L);

        assertThat(vo.getStatus()).isEqualTo("recalled");
    }

    @Test
    void recallByNonSenderRejected() {
        ImMessage m = msg(100L, 1L, new Date(), "normal");
        when(messageMapper.selectById(100L)).thenReturn(m);

        assertThatThrownBy(() -> service.recall(100L, 2L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void recallAfterWindowRejected() {
        Date old = new Date(System.currentTimeMillis() - 5 * 60 * 1000L);
        ImMessage m = msg(100L, 1L, old, "normal");
        when(messageMapper.selectById(100L)).thenReturn(m);

        assertThatThrownBy(() -> service.recall(100L, 1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void sendThreadReplyStoresParentAndBumpsRootReplyCount() {
        // arrange: conversationService.assertMember no-op; save sets id
        when(messageMapper.insert(org.mockito.ArgumentMatchers.<ImMessage>any())).thenAnswer(inv -> { ((ImMessage) inv.getArgument(0)).setId(11L); return 1; });
        when(conversationService.memberUserIds(99L)).thenReturn(java.util.List.of(1L));
        // atomic update returns 1 (row matched)
        when(messageMapper.update(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any())).thenReturn(1);

        ImSendMessageBO bo = new ImSendMessageBO();
        bo.setConversationId(99L); bo.setContent("re"); bo.setParentId(10L);
        var vo = service.send(1L, bo);

        // (a) reply message was stored with parentId=10L
        verify(messageMapper).insert(org.mockito.ArgumentMatchers.<ImMessage>argThat(m -> Long.valueOf(10L).equals(m.getParentId())));
        // (b) atomic SQL update was invoked once (wrapper-based: baseMapper.update(null, wrapper))
        verify(messageMapper).update(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendStoresMentions() {
        when(messageMapper.insert(org.mockito.ArgumentMatchers.<ImMessage>any())).thenAnswer(inv -> { ((ImMessage) inv.getArgument(0)).setId(12L); return 1; });
        when(conversationService.memberUserIds(anyLong())).thenReturn(java.util.List.of(1L));
        ImSendMessageBO bo = new ImSendMessageBO();
        bo.setConversationId(99L); bo.setContent("hi @a"); bo.setMentionAll(true);
        bo.setMentionedUserIds(java.util.List.of(2L, 3L));
        service.send(1L, bo);
        verify(messageMapper).insert(org.mockito.ArgumentMatchers.<ImMessage>argThat(m ->
            Boolean.TRUE.equals(m.getMentionAll()) && "2,3".equals(m.getMentionedUserIds())));
    }
}
