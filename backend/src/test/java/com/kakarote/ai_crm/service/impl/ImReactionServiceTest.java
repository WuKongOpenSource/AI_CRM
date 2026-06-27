package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.entity.PO.ImMessageReaction;
import com.kakarote.ai_crm.entity.VO.ImReactionVO;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.mapper.ImMessageReactionMapper;
import com.kakarote.ai_crm.service.ImConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImReactionServiceTest {

    @Mock ImMessageReactionMapper reactionMapper;
    @Mock ImMessageMapper messageMapper;
    @Mock ImConversationService conversationService;
    @Mock ApplicationEventPublisher eventPublisher;
    ImReactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImReactionServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", reactionMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "conversationService", conversationService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        ImMessage m = new ImMessage();
        m.setId(10L); m.setConversationId(99L);
        when(messageMapper.selectById(10L)).thenReturn(m);
        when(conversationService.memberUserIds(99L)).thenReturn(List.of(1L, 2L));
    }

    @Test
    void toggleRejectsUnknownEmoji() {
        assertThatThrownBy(() -> service.toggle(1L, 10L, "🚀"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void toggleAddsWhenAbsentThenAggregates() {
        // not yet reacted
        when(reactionMapper.selectOne(any(), eq(false))).thenReturn(null); // getOne(...,false)
        // aggregate() list() returns the just-added row
        ImMessageReaction added = new ImMessageReaction();
        added.setMessageId(10L); added.setUserId(1L); added.setEmoji("👍");
        when(reactionMapper.selectList(any())).thenReturn(List.of(added));

        List<ImReactionVO> out = service.toggle(1L, 10L, "👍");

        verify(reactionMapper).insertIgnoreDuplicate(org.mockito.ArgumentMatchers.<ImMessageReaction>any());
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getEmoji()).isEqualTo("👍");
        assertThat(out.get(0).getCount()).isEqualTo(1);
        assertThat(out.get(0).isMine()).isTrue();
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void toggleRemovesWhenPresent() {
        ImMessageReaction existing = new ImMessageReaction();
        existing.setId(500L); existing.setMessageId(10L); existing.setUserId(1L); existing.setEmoji("👍");
        when(reactionMapper.selectOne(any(), eq(false))).thenReturn(existing);
        when(reactionMapper.selectList(any())).thenReturn(List.of()); // none left

        List<ImReactionVO> out = service.toggle(1L, 10L, "👍");

        verify(reactionMapper).deleteById(500L);
        verify(reactionMapper, never()).insert(any(ImMessageReaction.class));
        assertThat(out).isEmpty();
    }

    @Test
    void aggregateGroupsByEmojiWithMineFlag() {
        ImMessageReaction r1 = new ImMessageReaction(); r1.setEmoji("👍"); r1.setUserId(1L);
        ImMessageReaction r2 = new ImMessageReaction(); r2.setEmoji("👍"); r2.setUserId(2L);
        ImMessageReaction r3 = new ImMessageReaction(); r3.setEmoji("❤️"); r3.setUserId(2L);
        when(reactionMapper.selectList(any())).thenReturn(List.of(r1, r2, r3));

        List<ImReactionVO> out = service.aggregate(10L, 1L);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).getEmoji()).isEqualTo("👍");
        assertThat(out.get(0).getCount()).isEqualTo(2);
        assertThat(out.get(0).isMine()).isTrue();   // viewer 1 reacted 👍
        assertThat(out.get(1).getEmoji()).isEqualTo("❤️");
        assertThat(out.get(1).getCount()).isEqualTo(1);
        assertThat(out.get(1).isMine()).isFalse();
    }
}
