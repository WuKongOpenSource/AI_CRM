package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingWebhookEvent;
import com.kakarote.ai_crm.mapper.TencentMeetingCorpConfigMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingWebhookEventMapper;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TencentMeetingWebhookServiceTest {

    @Test
    void handleWebhookShouldSkipDuplicateTraceId() {
        TencentMeetingWebhookService service = new TencentMeetingWebhookService();
        TencentMeetingWebhookEventMapper eventMapper = mock(TencentMeetingWebhookEventMapper.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "syncService", syncService);

        TencentMeetingWebhookEvent existing = new TencentMeetingWebhookEvent();
        existing.setTraceId("trace-1");
        when(eventMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        boolean processed = service.handleWebhook("""
                {"event":"meeting.end","trace_id":"trace-1","payload":[{"meeting_info":{"meeting_id":"m-1"}}]}
                """);

        assertThat(processed).isFalse();
        verify(eventMapper, never()).insert(any(TencentMeetingWebhookEvent.class));
        verify(syncService, never()).refreshMeetingByExternalId(any(), any());
    }

    @Test
    void handleWebhookShouldVerifySignatureWhenWebhookTokenIsConfigured() {
        TencentMeetingWebhookService service = new TencentMeetingWebhookService();
        TencentMeetingWebhookEventMapper eventMapper = mock(TencentMeetingWebhookEventMapper.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        TencentMeetingCorpConfigMapper configMapper = mock(TencentMeetingCorpConfigMapper.class);
        SecretTextCipher cipher = new SecretTextCipher("0123456789abcdef");
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "syncService", syncService);
        ReflectionTestUtils.setField(service, "configMapper", configMapper);
        ReflectionTestUtils.setField(service, "secretTextCipher", cipher);

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setTenantId(99L);
        config.setWebhookSecretEncrypted(cipher.encrypt("meeting-token"));
        when(configMapper.selectWebhookConfigsIgnoreTenant()).thenReturn(List.of(config));
        when(eventMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        String body = """
                {"event":"meeting.end","trace_id":"trace-2","payload":[{"meeting_info":{"meeting_id":"m-2"}}]}
                """;
        String signature = TencentMeetingWebhookService.calculateSignature("meeting-token", "1700000000", "nonce-1", body);

        boolean processed = service.handleWebhook(body, "1700000000", "nonce-1", signature);

        assertThat(processed).isTrue();
        verify(eventMapper).insert(any(TencentMeetingWebhookEvent.class));
        verify(syncService).refreshMeetingByExternalId("meeting.end", "m-2");
    }

    @Test
    void handleWebhookShouldRejectInvalidSignatureWhenWebhookTokenIsConfigured() {
        TencentMeetingWebhookService service = new TencentMeetingWebhookService();
        TencentMeetingWebhookEventMapper eventMapper = mock(TencentMeetingWebhookEventMapper.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        TencentMeetingCorpConfigMapper configMapper = mock(TencentMeetingCorpConfigMapper.class);
        SecretTextCipher cipher = new SecretTextCipher("0123456789abcdef");
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "syncService", syncService);
        ReflectionTestUtils.setField(service, "configMapper", configMapper);
        ReflectionTestUtils.setField(service, "secretTextCipher", cipher);

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setTenantId(99L);
        config.setWebhookSecretEncrypted(cipher.encrypt("meeting-token"));
        when(configMapper.selectWebhookConfigsIgnoreTenant()).thenReturn(List.of(config));

        assertThatThrownBy(() -> service.handleWebhook(
                "{\"event\":\"meeting.end\",\"trace_id\":\"trace-3\"}",
                "1700000000",
                "nonce-1",
                "bad-signature"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature verification failed");

        verify(eventMapper, never()).insert(any(TencentMeetingWebhookEvent.class));
        verify(syncService, never()).refreshMeetingByExternalId(any(), any());
    }
}
