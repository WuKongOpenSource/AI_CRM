package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.entity.BO.WecomSyncRunBO;
import com.kakarote.ai_crm.entity.PO.WecomConversation;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.entity.PO.WecomMessage;
import com.kakarote.ai_crm.entity.PO.WecomSyncCursor;
import com.kakarote.ai_crm.entity.PO.WecomSyncLog;
import com.kakarote.ai_crm.entity.VO.WecomSyncStatusVO;
import com.kakarote.ai_crm.mapper.WecomConversationMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.mapper.WecomExternalCustomerMapper;
import com.kakarote.ai_crm.mapper.WecomMessageMapper;
import com.kakarote.ai_crm.mapper.WecomSyncCursorMapper;
import com.kakarote.ai_crm.mapper.WecomSyncLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WecomSyncServiceTest {

    @Test
    void runSyncShouldSaveArchiveMessagesAndAdvanceCursor() {
        WecomSyncServiceImpl service = newService();
        WecomFinanceArchiveGateway archiveGateway = mapper(service, "archiveGateway");
        WecomConversationMapper conversationMapper = mapper(service, "conversationMapper");
        WecomMessageMapper messageMapper = mapper(service, "messageMapper");
        WecomSyncCursorMapper cursorMapper = mapper(service, "cursorMapper");

        WecomCorpConfig config = config();
        config.setArchiveEnabled(true);
        WecomSyncRunBO runBO = new WecomSyncRunBO();
        runBO.setSyncConversations(true);
        runBO.setArchiveLimit(10);
        JSONObject rawMessage = textArchiveMessage();

        when(cursorMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(archiveGateway.fetchMessages(eq(config), eq(0L), eq(10))).thenReturn(List.of(rawMessage));
        when(conversationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            WecomConversation conversation = invocation.getArgument(0);
            conversation.setId(300L);
            return 1;
        }).when(conversationMapper).insert(any(WecomConversation.class));
        when(messageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        WecomSyncStatusVO status = service.runSync(config, runBO);

        assertThat(status.getLastSyncStatus()).isEqualTo("success");
        assertThat(status.getSavedCount()).isEqualTo(1);
        ArgumentCaptor<WecomMessage> messageCaptor = ArgumentCaptor.forClass(WecomMessage.class);
        verify(messageMapper).insert(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getCorpId()).isEqualTo("corp_1");
        assertThat(messageCaptor.getValue().getConversationId()).isEqualTo(300L);
        assertThat(messageCaptor.getValue().getMsgId()).isEqualTo("msg_1");
        assertThat(messageCaptor.getValue().getContentText()).isEqualTo("hello");

        ArgumentCaptor<WecomSyncCursor> cursorCaptor = ArgumentCaptor.forClass(WecomSyncCursor.class);
        verify(cursorMapper).insert(cursorCaptor.capture());
        assertThat(cursorCaptor.getValue().getSeq()).isEqualTo(7L);
    }

    @Test
    void runSyncShouldRecordFailedStatusWhenRealApiFails() {
        WecomSyncServiceImpl service = newService();
        WecomTokenService tokenService = mapper(service, "tokenService");
        WecomSyncLogMapper syncLogMapper = mapper(service, "syncLogMapper");

        WecomCorpConfig config = config();
        WecomSyncRunBO runBO = new WecomSyncRunBO();
        runBO.setSyncEmployees(true);
        when(tokenService.fetchAppAccessToken(config)).thenThrow(new RuntimeException("network down"));

        WecomSyncStatusVO status = service.runSync(config, runBO);

        assertThat(status.getLastSyncStatus()).isEqualTo("failed");
        assertThat(status.getFailedCount()).isEqualTo(1);
        assertThat(status.getLastSyncError()).contains("network down");
        ArgumentCaptor<WecomSyncLog> logCaptor = ArgumentCaptor.forClass(WecomSyncLog.class);
        verify(syncLogMapper).updateById(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("failed");
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(WecomSyncServiceImpl service, String fieldName) {
        return (T) ReflectionTestUtils.getField(service, fieldName);
    }

    private static WecomSyncServiceImpl newService() {
        WecomSyncServiceImpl service = new WecomSyncServiceImpl();
        ReflectionTestUtils.setField(service, "tokenService", mock(WecomTokenService.class));
        ReflectionTestUtils.setField(service, "apiClient", mock(WecomApiClient.class));
        ReflectionTestUtils.setField(service, "archiveGateway", mock(WecomFinanceArchiveGateway.class));
        ReflectionTestUtils.setField(service, "messageNormalizeService", new WecomMessageNormalizeService());
        ReflectionTestUtils.setField(service, "employeeMapper", mock(WecomEmployeeMapper.class));
        ReflectionTestUtils.setField(service, "externalCustomerMapper", mock(WecomExternalCustomerMapper.class));
        ReflectionTestUtils.setField(service, "conversationMapper", mock(WecomConversationMapper.class));
        ReflectionTestUtils.setField(service, "messageMapper", mock(WecomMessageMapper.class));
        ReflectionTestUtils.setField(service, "cursorMapper", mock(WecomSyncCursorMapper.class));
        ReflectionTestUtils.setField(service, "syncLogMapper", mock(WecomSyncLogMapper.class));
        return service;
    }

    private static WecomCorpConfig config() {
        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("corp_1");
        return config;
    }

    private static JSONObject textArchiveMessage() {
        JSONObject raw = new JSONObject();
        raw.put("seq", 7L);
        raw.put("msgid", "msg_1");
        raw.put("msgtype", "text");
        raw.put("from", "employee_1");
        raw.put("tolist", new JSONArray(List.of("wm_customer_1")));
        raw.put("msgtime", 1710000000000L);
        JSONObject text = new JSONObject();
        text.put("content", "hello");
        raw.put("text", text);
        return raw;
    }
}
