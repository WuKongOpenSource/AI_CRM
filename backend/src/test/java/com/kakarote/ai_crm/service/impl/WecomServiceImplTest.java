package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.entity.BO.WecomConversationQueryBO;
import com.kakarote.ai_crm.entity.BO.WecomSyncRunBO;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.entity.VO.WecomConfigVO;
import com.kakarote.ai_crm.entity.VO.WecomJsSdkAgentConfigVO;
import com.kakarote.ai_crm.entity.VO.WecomSyncStatusVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.ExternalAuthIdentityMapper;
import com.kakarote.ai_crm.mapper.WecomConversationMapper;
import com.kakarote.ai_crm.mapper.WecomCorpConfigMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.mapper.WecomExternalCustomerMapper;
import com.kakarote.ai_crm.mapper.WecomMessageMapper;
import com.kakarote.ai_crm.mapper.WecomSyncLogMapper;
import com.kakarote.ai_crm.service.support.SyncTaskExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WecomServiceImplTest {

    @Test
    void getConfigShouldExposeThirdPartyStateWhenTenantHasNotAuthorizedYet() {
        WecomServiceImpl service = newService();
        WecomOpenPlatformService openPlatformService = mapper(service, "openPlatformService");
        when(openPlatformService.isUsable()).thenReturn(true);

        WecomConfigVO config = service.getConfig();

        assertThat(config.getThirdPartyEnabled()).isTrue();
        assertThat(config.getThirdPartyAuthorized()).isFalse();
    }

    @Test
    void syncOrganizationShouldAskForWecomAuthorizationWhenConfigIsMissing() {
        WecomServiceImpl service = newService();
        WecomOpenPlatformService openPlatformService = mapper(service, "openPlatformService");
        when(openPlatformService.isUsable()).thenReturn(true);

        assertThatThrownBy(service::syncOrganization)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Please authorize WeCom third-party app first");
    }

    @Test
    void getJsSdkAgentConfigShouldSignRequestedUrlWithAgentTicket() throws Exception {
        WecomServiceImpl service = newService();
        WecomCorpConfigMapper configMapper = mapper(service, "configMapper");
        WecomOpenPlatformService openPlatformService = mapper(service, "openPlatformService");
        WecomTokenService tokenService = mapper(service, "tokenService");
        WecomApiClient apiClient = mapper(service, "apiClient");
        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("corp_1");
        config.setAgentId("100001");
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);
        when(openPlatformService.isAuthorized(config)).thenReturn(true);
        when(tokenService.fetchAppAccessToken(config)).thenReturn("app-token");
        when(apiClient.getAgentJsApiTicket("app-token")).thenReturn("agent-ticket");

        WecomJsSdkAgentConfigVO vo = service.getJsSdkAgentConfig("https://example.com/crm/#/wecom/scrm");

        assertThat(vo.getCorpId()).isEqualTo("corp_1");
        assertThat(vo.getAgentId()).isEqualTo("100001");
        assertThat(vo.getNonceStr()).isNotBlank();
        assertThat(vo.getTimestamp()).isPositive();
        assertThat(vo.getSignature()).isEqualTo(sha1("jsapi_ticket=agent-ticket&noncestr="
                + vo.getNonceStr() + "&timestamp=" + vo.getTimestamp() + "&url=https://example.com/crm/"));
    }

    @Test
    void queryConversationsShouldNotDrainArchiveOnOpen() {
        WecomServiceImpl service = newService();
        WecomCorpConfigMapper configMapper = mapper(service, "configMapper");
        WecomConversationMapper conversationMapper = mapper(service, "conversationMapper");
        WecomSyncServiceImpl syncService = mapper(service, "syncService");
        com.kakarote.ai_crm.service.DataPermissionService dataPermissionService = mapper(service, "dataPermissionService");
        WecomCorpConfig config = new WecomCorpConfig();
        config.setCorpId("corp_1");
        config.setArchiveEnabled(true);
        config.setArchiveSecretEncrypted("encrypted-secret");

        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);
        when(dataPermissionService.createContext("wecomCustomerSession")).thenReturn(DataPermissionContext.all());
        when(conversationMapper.selectPage(any(), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WecomConversationQueryBO queryBO = new WecomConversationQueryBO();
        queryBO.setConversationType("customer");
        service.queryConversations(queryBO);

        verify(syncService, never()).drainArchive(any(WecomCorpConfig.class), anyInt());
    }

    @Test
    void runSyncShouldReturnRunningAndSubmitBackgroundTask() {
        WecomServiceImpl service = newService();
        WecomCorpConfigMapper configMapper = mapper(service, "configMapper");
        WecomOpenPlatformService openPlatformService = mapper(service, "openPlatformService");
        WecomSyncServiceImpl syncService = mapper(service, "syncService");
        SyncTaskExecutor syncTaskExecutor = mapper(service, "syncTaskExecutor");
        WecomCorpConfig config = authorizedConfig();
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);
        when(openPlatformService.isAuthorized(config)).thenReturn(true);

        WecomSyncStatusVO status = service.runSync(new WecomSyncRunBO());

        assertThat(status.getLastSyncStatus()).isEqualTo("running");
        assertThat(status.getCorpId()).isEqualTo("corp_1");
        verify(syncTaskExecutor).submit(anyString(), any(Runnable.class));
        verify(syncService, never()).runSync(any(WecomCorpConfig.class), any(WecomSyncRunBO.class));
    }

    @Test
    void syncOrganizationShouldReturnRunningAndSubmitBackgroundTask() {
        WecomServiceImpl service = newService();
        WecomCorpConfigMapper configMapper = mapper(service, "configMapper");
        WecomOpenPlatformService openPlatformService = mapper(service, "openPlatformService");
        WecomSyncServiceImpl syncService = mapper(service, "syncService");
        SyncTaskExecutor syncTaskExecutor = mapper(service, "syncTaskExecutor");
        WecomCorpConfig config = authorizedConfig();
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);
        when(openPlatformService.isAuthorized(config)).thenReturn(true);

        WecomSyncStatusVO status = service.syncOrganization();

        assertThat(status.getLastSyncStatus()).isEqualTo("running");
        verify(syncTaskExecutor).submit(anyString(), any(Runnable.class));
        verify(syncService, never()).syncOrganization(any(WecomCorpConfig.class));
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(WecomServiceImpl service, String fieldName) {
        return (T) ReflectionTestUtils.getField(service, fieldName);
    }

    private static WecomServiceImpl newService() {
        WecomServiceImpl service = new WecomServiceImpl();
        ReflectionTestUtils.setField(service, "configMapper", mock(WecomCorpConfigMapper.class));
        ReflectionTestUtils.setField(service, "employeeMapper", mock(WecomEmployeeMapper.class));
        ReflectionTestUtils.setField(service, "externalCustomerMapper", mock(WecomExternalCustomerMapper.class));
        ReflectionTestUtils.setField(service, "conversationMapper", mock(WecomConversationMapper.class));
        ReflectionTestUtils.setField(service, "messageMapper", mock(WecomMessageMapper.class));
        ReflectionTestUtils.setField(service, "syncLogMapper", mock(WecomSyncLogMapper.class));
        ReflectionTestUtils.setField(service, "customerMapper", mock(CustomerMapper.class));
        ReflectionTestUtils.setField(service, "identityMapper", mock(ExternalAuthIdentityMapper.class));
        ReflectionTestUtils.setField(service, "syncService", mock(WecomSyncServiceImpl.class));
        ReflectionTestUtils.setField(service, "syncTaskExecutor", mock(SyncTaskExecutor.class));
        ReflectionTestUtils.setField(service, "openPlatformService", mock(WecomOpenPlatformService.class));
        ReflectionTestUtils.setField(service, "tokenService", mock(WecomTokenService.class));
        ReflectionTestUtils.setField(service, "apiClient", mock(WecomApiClient.class));
        ReflectionTestUtils.setField(service, "bindingService", mock(WecomCustomerBindingServiceImpl.class));
        ReflectionTestUtils.setField(service, "dataPermissionService", mock(com.kakarote.ai_crm.service.DataPermissionService.class));
        return service;
    }

    private static WecomCorpConfig authorizedConfig() {
        WecomCorpConfig config = new WecomCorpConfig();
        config.setId(10L);
        config.setTenantId(20L);
        config.setCorpId("corp_1");
        return config;
    }

    private static String sha1(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
