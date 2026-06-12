package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.entity.BO.TencentMeetingSyncRunBO;
import com.kakarote.ai_crm.entity.PO.TencentMeeting;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingSyncLog;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.entity.VO.TencentMeetingOAuthStatusVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingSyncStatusVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingCorpConfigMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingSyncLogMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.service.support.SyncTaskExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TencentMeetingServiceImplTest {

    @Test
    void getSyncStatusShouldIgnoreStaleAuthRequiredFailureWhenCurrentUserAuthorized() {
        TencentMeetingServiceImpl service = new TencentMeetingServiceImpl();
        TencentMeetingCorpConfigMapper configMapper = mock(TencentMeetingCorpConfigMapper.class);
        TencentMeetingSyncLogMapper syncLogMapper = mock(TencentMeetingSyncLogMapper.class);
        TencentMeetingOAuthService oauthService = mock(TencentMeetingOAuthService.class);

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setAppId("app-1");
        when(configMapper.selectLatestOAuthConfigIgnoreTenant()).thenReturn(config);

        TencentMeetingSyncLog latest = new TencentMeetingSyncLog();
        latest.setStatus("failed");
        latest.setErrorMessage("请先授权腾讯会议账号");
        latest.setFinishedAt(new Date());
        latest.setFetchedCount(0);
        latest.setSavedCount(0);
        latest.setFailedCount(1);
        when(syncLogMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(latest);

        TencentMeetingOAuthStatusVO oauthStatus = new TencentMeetingOAuthStatusVO();
        oauthStatus.setConfigured(Boolean.TRUE);
        oauthStatus.setAuthorized(Boolean.TRUE);
        when(oauthService.getStatus()).thenReturn(oauthStatus);

        ReflectionTestUtils.setField(service, "configMapper", configMapper);
        ReflectionTestUtils.setField(service, "syncLogMapper", syncLogMapper);
        ReflectionTestUtils.setField(service, "oauthService", oauthService);

        TencentMeetingSyncStatusVO status = service.getSyncStatus();

        assertThat(status.getLastSyncStatus()).isNull();
        assertThat(status.getLastSyncError()).isNull();
        assertThat(status.getFailedCount()).isZero();
    }

    @Test
    void refreshJoinUrlShouldFetchMeetingDetailAndPersistJoinUrlIntoRawJson() {
        TencentMeetingServiceImpl service = new TencentMeetingServiceImpl();
        TencentMeetingCorpConfigMapper configMapper = mock(TencentMeetingCorpConfigMapper.class);
        TencentMeetingMapper meetingMapper = mock(TencentMeetingMapper.class);
        TencentMeetingOAuthService oauthService = mock(TencentMeetingOAuthService.class);
        TencentMeetingOAuthTokenProvider tokenProvider = mock(TencentMeetingOAuthTokenProvider.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        DataPermissionService dataPermissionService = mock(DataPermissionService.class);

        TencentMeeting meeting = new TencentMeeting();
        meeting.setId(100L);
        meeting.setAppId("app-1");
        meeting.setMeetingId("meeting-1");
        meeting.setMeetingCode("123456789");
        meeting.setSubject("Sales review");
        meeting.setStatus("not_started");
        meeting.setRawJson("{\"meeting_id\":\"meeting-1\",\"subject\":\"Sales review\"}");
        when(meetingMapper.selectById(100L)).thenReturn(meeting);
        when(dataPermissionService.createContext("tencentMeeting")).thenReturn(DataPermissionContext.all());

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setAppId("app-1");
        config.setSdkId("sdk-1");
        when(configMapper.selectLatestOAuthConfigIgnoreTenant()).thenReturn(config);

        TencentMeetingUserMapping account = new TencentMeetingUserMapping();
        account.setMeetingUserId("open-1");
        account.setAuthStatus(TencentMeetingOAuthService.AUTH_STATUS_ACTIVE);
        account.setStatus(1);
        when(oauthService.requireCurrentAuthorizedAccount(config)).thenReturn(account);
        TencentMeetingOAuthCredential credential = new TencentMeetingOAuthCredential(config, account, "access-token");
        when(tokenProvider.credential(config, account)).thenReturn(credential);

        JSONObject remoteDetail = new JSONObject();
        remoteDetail.put("meeting_id", "meeting-1");
        remoteDetail.put("join_url", "https://meeting.tencent.com/s/abc");
        remoteDetail.put("meeting_code", "123456789");
        when(syncService.getMeetingDetail(credential, "meeting-1")).thenReturn(remoteDetail);

        ReflectionTestUtils.setField(service, "configMapper", configMapper);
        ReflectionTestUtils.setField(service, "meetingMapper", meetingMapper);
        ReflectionTestUtils.setField(service, "customerMapper", mock(CustomerMapper.class));
        ReflectionTestUtils.setField(service, "oauthService", oauthService);
        ReflectionTestUtils.setField(service, "tokenProvider", tokenProvider);
        ReflectionTestUtils.setField(service, "syncService", syncService);
        ReflectionTestUtils.setField(service, "dataPermissionService", dataPermissionService);

        TencentMeetingVO vo = service.refreshJoinUrl(100L);

        assertThat(vo.getJoinUrl()).isEqualTo("https://meeting.tencent.com/s/abc");
        ArgumentCaptor<TencentMeeting> meetingCaptor = ArgumentCaptor.forClass(TencentMeeting.class);
        verify(meetingMapper).updateById(meetingCaptor.capture());
        JSONObject savedRaw = JSONObject.parseObject(meetingCaptor.getValue().getRawJson());
        assertThat(savedRaw.getString("subject")).isEqualTo("Sales review");
        assertThat(savedRaw.getString("join_url")).isEqualTo("https://meeting.tencent.com/s/abc");
    }

    @Test
    void runSyncShouldReturnRunningAndSubmitBackgroundTask() {
        TencentMeetingServiceImpl service = new TencentMeetingServiceImpl();
        TencentMeetingCorpConfigMapper configMapper = mock(TencentMeetingCorpConfigMapper.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        SyncTaskExecutor syncTaskExecutor = mock(SyncTaskExecutor.class);

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setAppId("app-1");
        config.setSdkId("sdk-1");
        when(configMapper.selectLatestOAuthConfigIgnoreTenant()).thenReturn(config);

        ReflectionTestUtils.setField(service, "configMapper", configMapper);
        ReflectionTestUtils.setField(service, "syncService", syncService);
        ReflectionTestUtils.setField(service, "syncTaskExecutor", syncTaskExecutor);

        TencentMeetingSyncStatusVO status = service.runSync(new TencentMeetingSyncRunBO());

        assertThat(status.getLastSyncStatus()).isEqualTo("running");
        assertThat(status.getAppId()).isEqualTo("app-1");
        verify(syncTaskExecutor).submit(anyString(), any(Runnable.class));
        verify(syncService, never()).runSync(any(TencentMeetingCorpConfig.class), any(TencentMeetingSyncRunBO.class));
    }

    @Test
    void refreshMeetingShouldSubmitBackgroundTaskAfterAccessCheck() {
        TencentMeetingServiceImpl service = new TencentMeetingServiceImpl();
        TencentMeetingMapper meetingMapper = mock(TencentMeetingMapper.class);
        TencentMeetingSyncServiceImpl syncService = mock(TencentMeetingSyncServiceImpl.class);
        SyncTaskExecutor syncTaskExecutor = mock(SyncTaskExecutor.class);
        DataPermissionService dataPermissionService = mock(DataPermissionService.class);

        TencentMeeting meeting = new TencentMeeting();
        meeting.setId(100L);
        meeting.setMeetingId("meeting-1");
        when(meetingMapper.selectById(100L)).thenReturn(meeting);
        when(dataPermissionService.createContext("tencentMeeting")).thenReturn(DataPermissionContext.all());

        ReflectionTestUtils.setField(service, "meetingMapper", meetingMapper);
        ReflectionTestUtils.setField(service, "syncService", syncService);
        ReflectionTestUtils.setField(service, "syncTaskExecutor", syncTaskExecutor);
        ReflectionTestUtils.setField(service, "dataPermissionService", dataPermissionService);

        service.refreshMeeting(100L);

        verify(syncTaskExecutor).submit(anyString(), any(Runnable.class));
        verify(syncService, never()).refreshMeetingByExternalId(anyString(), anyString());
    }
}
