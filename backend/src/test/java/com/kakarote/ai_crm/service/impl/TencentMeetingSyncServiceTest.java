package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.entity.BO.TencentMeetingSyncRunBO;
import com.kakarote.ai_crm.entity.PO.TencentMeeting;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingParticipant;
import com.kakarote.ai_crm.entity.PO.TencentMeetingRecording;
import com.kakarote.ai_crm.entity.PO.TencentMeetingSyncLog;
import com.kakarote.ai_crm.entity.PO.TencentMeetingTranscriptSegment;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.entity.VO.TencentMeetingSyncStatusVO;
import com.kakarote.ai_crm.mapper.TencentMeetingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingParticipantMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingRecordingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingSyncLogMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingTranscriptSegmentMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingUserMappingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TencentMeetingSyncServiceTest {

    @Test
    void runSyncShouldSaveMeetingRecordingTranscriptAndSuccessLog() {
        TencentMeetingSyncServiceImpl service = newService();
        TencentMeetingApiGateway apiGateway = mapper(service, "apiGateway");
        TencentMeetingUserMappingMapper userMappingMapper = mapper(service, "userMappingMapper");
        TencentMeetingMapper meetingMapper = mapper(service, "meetingMapper");
        TencentMeetingParticipantMapper participantMapper = mapper(service, "participantMapper");
        TencentMeetingRecordingMapper recordingMapper = mapper(service, "recordingMapper");
        TencentMeetingTranscriptSegmentMapper transcriptMapper = mapper(service, "transcriptMapper");
        TencentMeetingSyncLogMapper syncLogMapper = mapper(service, "syncLogMapper");

        TencentMeetingCorpConfig config = new TencentMeetingCorpConfig();
        config.setId(1L);
        config.setAppId("app-1");
        config.setSyncEnabled(true);
        config.setTranscriptEnabled(true);

        TencentMeetingUserMapping mapping = new TencentMeetingUserMapping();
        mapping.setMeetingUserId("host-1");
        mapping.setCrmUserId(9L);
        when(userMappingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mapping));
        when(apiGateway.listEndedMeetings(config, mapping, 30)).thenReturn(List.of(meetingJson()));
        when(apiGateway.getMeetingParticipants(config, "meeting-1", "host-1")).thenReturn(List.of(participantJson()));
        when(apiGateway.listRecordings(config, "host-1")).thenReturn(List.of(recordingJson()));
        when(apiGateway.getTranscriptSegments(config, "meeting-1", "record-1", "host-1")).thenReturn(List.of(segmentJson()));
        when(meetingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            TencentMeeting meeting = invocation.getArgument(0);
            meeting.setId(300L);
            return 1;
        }).when(meetingMapper).insert(any(TencentMeeting.class));
        doAnswer(invocation -> {
            TencentMeetingRecording recording = invocation.getArgument(0);
            recording.setId(400L);
            return 1;
        }).when(recordingMapper).insert(any(TencentMeetingRecording.class));

        TencentMeetingSyncRunBO runBO = new TencentMeetingSyncRunBO();
        runBO.setSyncDays(30);
        runBO.setSyncRecordings(true);
        TencentMeetingSyncStatusVO status = service.runSync(config, runBO);

        assertThat(status.getLastSyncStatus()).isEqualTo("success");
        assertThat(status.getSavedCount()).isEqualTo(4);

        ArgumentCaptor<TencentMeeting> meetingCaptor = ArgumentCaptor.forClass(TencentMeeting.class);
        verify(meetingMapper).insert(meetingCaptor.capture());
        assertThat(meetingCaptor.getValue().getMeetingId()).isEqualTo("meeting-1");
        assertThat(meetingCaptor.getValue().getSubject()).isEqualTo("报价方案会");
        assertThat(meetingCaptor.getValue().getCreatorUserId()).isEqualTo("host-1");
        assertThat(meetingCaptor.getValue().getCrmCreatorUserId()).isEqualTo(9L);

        verify(participantMapper).insert(any(TencentMeetingParticipant.class));
        verify(recordingMapper).insert(any(TencentMeetingRecording.class));
        verify(transcriptMapper).insert(any(TencentMeetingTranscriptSegment.class));

        ArgumentCaptor<TencentMeetingSyncLog> logCaptor = ArgumentCaptor.forClass(TencentMeetingSyncLog.class);
        verify(syncLogMapper).updateById(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("success");
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(TencentMeetingSyncServiceImpl service, String fieldName) {
        return (T) ReflectionTestUtils.getField(service, fieldName);
    }

    private static TencentMeetingSyncServiceImpl newService() {
        TencentMeetingSyncServiceImpl service = new TencentMeetingSyncServiceImpl();
        ReflectionTestUtils.setField(service, "apiGateway", mock(TencentMeetingApiGateway.class));
        ReflectionTestUtils.setField(service, "userMappingMapper", mock(TencentMeetingUserMappingMapper.class));
        ReflectionTestUtils.setField(service, "meetingMapper", mock(TencentMeetingMapper.class));
        ReflectionTestUtils.setField(service, "participantMapper", mock(TencentMeetingParticipantMapper.class));
        ReflectionTestUtils.setField(service, "recordingMapper", mock(TencentMeetingRecordingMapper.class));
        ReflectionTestUtils.setField(service, "transcriptMapper", mock(TencentMeetingTranscriptSegmentMapper.class));
        ReflectionTestUtils.setField(service, "syncLogMapper", mock(TencentMeetingSyncLogMapper.class));
        return service;
    }

    private static JSONObject meetingJson() {
        JSONObject raw = new JSONObject();
        raw.put("meeting_id", "meeting-1");
        raw.put("meeting_code", "123456789");
        raw.put("subject", "报价方案会");
        raw.put("status", "已结束");
        raw.put("start_time", "1710000000");
        raw.put("end_time", "1710003600");
        raw.put("creator_userid", "host-1");
        raw.put("creator_name", "张三");
        return raw;
    }

    private static JSONObject participantJson() {
        JSONObject raw = new JSONObject();
        raw.put("userid", "guest-1");
        raw.put("user_name", "李四");
        raw.put("join_time", "1710000001");
        raw.put("left_time", "1710003500");
        return raw;
    }

    private static JSONObject recordingJson() {
        JSONObject raw = new JSONObject();
        raw.put("meeting_id", "meeting-1");
        raw.put("record_file_id", "record-1");
        raw.put("record_file_name", "报价方案会.mp4");
        raw.put("download_url", "https://example.com/record.mp4");
        raw.put("meeting_summary", "客户关注报价有效期");
        raw.put("todo_text", "下周三前发送报价方案");
        return raw;
    }

    private static JSONObject segmentJson() {
        JSONObject raw = new JSONObject();
        raw.put("pid", "1");
        raw.put("speaker_name", "张三");
        raw.put("text", "下周三之前把报价方案发给客户");
        raw.put("start_time", 1000L);
        raw.put("end_time", 5000L);
        return raw;
    }
}
