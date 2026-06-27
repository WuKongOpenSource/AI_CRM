package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
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

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImMessageServiceImplTest {

    @Mock ImMessageMapper messageMapper;
    @Mock ImConversationService conversationService;
    @Mock ApplicationEventPublisher eventPublisher;

    ImMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImMessageServiceImpl();
        ReflectionTestUtils.setField(service, "conversationService", conversationService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        // ServiceImpl exposes the mapper via baseMapper; inject it for getById/updateById.
        ReflectionTestUtils.setField(service, "baseMapper", messageMapper);
        when(conversationService.memberUserIds(any())).thenReturn(List.of(1L, 2L));
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
        when(messageMapper.updateById(any(ImMessage.class))).thenReturn(1);

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
}
