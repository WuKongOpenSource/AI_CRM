package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class TencentMeetingSyncServiceImpl {

    @Autowired
    private TencentMeetingApiGateway apiGateway;

    @Autowired
    private TencentMeetingUserMappingMapper userMappingMapper;

    @Autowired
    private TencentMeetingMapper meetingMapper;

    @Autowired
    private TencentMeetingParticipantMapper participantMapper;

    @Autowired
    private TencentMeetingRecordingMapper recordingMapper;

    @Autowired
    private TencentMeetingTranscriptSegmentMapper transcriptMapper;

    @Autowired
    private TencentMeetingSyncLogMapper syncLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public TencentMeetingSyncStatusVO runSync(TencentMeetingCorpConfig config, TencentMeetingSyncRunBO runBO) {
        TencentMeetingSyncRunBO actualRunBO = runBO == null ? new TencentMeetingSyncRunBO() : runBO;
        int syncDays = actualRunBO.getSyncDays() == null ? 30 : Math.max(1, actualRunBO.getSyncDays());
        boolean syncRecordings = actualRunBO.getSyncRecordings() == null || Boolean.TRUE.equals(actualRunBO.getSyncRecordings());
        boolean syncTranscripts = actualRunBO.getSyncTranscripts() == null || Boolean.TRUE.equals(actualRunBO.getSyncTranscripts());

        TencentMeetingSyncLog syncLog = new TencentMeetingSyncLog();
        syncLog.setAppId(config.getAppId());
        syncLog.setSyncType("manual");
        syncLog.setStatus("running");
        syncLog.setFetchedCount(0);
        syncLog.setSavedCount(0);
        syncLog.setFailedCount(0);
        syncLog.setStartedAt(new Date());
        syncLogMapper.insert(syncLog);

        TencentMeetingSyncStatusVO status = new TencentMeetingSyncStatusVO();
        status.setAppId(config.getAppId());
        int fetched = 0;
        int saved = 0;
        int failed = 0;
        try {
            List<TencentMeetingUserMapping> mappings = loadMappings(config, actualRunBO);
            for (TencentMeetingUserMapping mapping : mappings) {
                List<JSONObject> meetings = apiGateway.listEndedMeetings(config, mapping, syncDays);
                fetched += meetings.size();
                for (JSONObject rawMeeting : meetings) {
                    TencentMeeting meeting = upsertMeeting(config, mapping, rawMeeting);
                    saved++;
                    List<JSONObject> participants = apiGateway.getMeetingParticipants(config, meeting.getMeetingId(), mapping.getMeetingUserId());
                    for (JSONObject rawParticipant : participants) {
                        saveParticipant(config, meeting, rawParticipant);
                        saved++;
                    }
                    if (syncRecordings) {
                        List<JSONObject> recordings = apiGateway.listRecordings(config, mapping.getMeetingUserId());
                        for (JSONObject rawRecording : recordings) {
                            if (!matchesMeeting(meeting, rawRecording)) {
                                continue;
                            }
                            TencentMeetingRecording recording = saveRecording(config, meeting, rawRecording);
                            saved++;
                            List<JSONObject> segments = syncTranscripts && Boolean.TRUE.equals(config.getTranscriptEnabled())
                                    ? apiGateway.getTranscriptSegments(config, meeting.getMeetingId(), recording.getRecordFileId(), mapping.getMeetingUserId())
                                    : List.of();
                            List<String> texts = new ArrayList<>();
                            for (JSONObject rawSegment : segments) {
                                TencentMeetingTranscriptSegment segment = saveTranscriptSegment(recording, meeting, rawSegment);
                                if (StrUtil.isNotBlank(segment.getText())) {
                                    texts.add(formatTranscriptLine(segment));
                                }
                                saved++;
                            }
                            if (!texts.isEmpty() || StrUtil.isNotBlank(recording.getSummary()) || StrUtil.isNotBlank(recording.getTodoText())) {
                                meeting.setSummary(StrUtil.blankToDefault(meeting.getSummary(), recording.getSummary()));
                                meeting.setTodoText(StrUtil.blankToDefault(meeting.getTodoText(), recording.getTodoText()));
                                meeting.setTranscriptText(String.join("\n", texts));
                                meetingMapper.updateById(meeting);
                            }
                        }
                    }
                }
            }
            syncLog.setStatus("success");
            status.setLastSyncStatus("success");
        } catch (Exception e) {
            failed++;
            syncLog.setStatus("failed");
            syncLog.setErrorMessage(e.getMessage());
            status.setLastSyncStatus("failed");
            status.setLastSyncError(e.getMessage());
            log.error("Tencent Meeting sync failed: {}", e.getMessage(), e);
        }
        Date finishedAt = new Date();
        syncLog.setFetchedCount(fetched);
        syncLog.setSavedCount(saved);
        syncLog.setFailedCount(failed);
        syncLog.setFinishedAt(finishedAt);
        syncLogMapper.updateById(syncLog);
        status.setFetchedCount(fetched);
        status.setSavedCount(saved);
        status.setFailedCount(failed);
        status.setLastSyncTime(finishedAt);
        if (status.getLastSyncError() == null) {
            status.setLastSyncError(syncLog.getErrorMessage());
        }
        return status;
    }

    public void refreshMeetingByExternalId(String eventName, String meetingId) {
        log.info("Tencent Meeting webhook refresh requested: event={}, meetingId={}", eventName, meetingId);
    }

    private List<TencentMeetingUserMapping> loadMappings(TencentMeetingCorpConfig config, TencentMeetingSyncRunBO runBO) {
        List<TencentMeetingUserMapping> mappings = userMappingMapper.selectList(
                Wrappers.<TencentMeetingUserMapping>lambdaQuery()
                        .eq(TencentMeetingUserMapping::getAppId, config.getAppId())
                        .eq(TencentMeetingUserMapping::getStatus, 1));
        if (!mappings.isEmpty()) {
            return mappings;
        }
        String fallbackUserId = StrUtil.blankToDefault(runBO.getOperatorUserId(), config.getOperatorUserId());
        if (StrUtil.isBlank(fallbackUserId)) {
            return List.of();
        }
        TencentMeetingUserMapping fallback = new TencentMeetingUserMapping();
        fallback.setAppId(config.getAppId());
        fallback.setMeetingUserId(fallbackUserId);
        fallback.setUserName(fallbackUserId);
        fallback.setStatus(1);
        return List.of(fallback);
    }

    private TencentMeeting upsertMeeting(TencentMeetingCorpConfig config, TencentMeetingUserMapping mapping, JSONObject raw) {
        String externalMeetingId = firstText(raw, "meeting_id", "meetingId", "meeting_uuid");
        TencentMeeting meeting = meetingMapper.selectOne(Wrappers.<TencentMeeting>lambdaQuery()
                .eq(TencentMeeting::getAppId, config.getAppId())
                .eq(TencentMeeting::getMeetingId, externalMeetingId)
                .last("LIMIT 1"));
        boolean insert = meeting == null;
        if (insert) {
            meeting = new TencentMeeting();
            meeting.setAppId(config.getAppId());
            meeting.setMeetingId(externalMeetingId);
            meeting.setBindStatus("UNBOUND");
        }
        meeting.setMeetingCode(firstText(raw, "meeting_code", "meetingCode"));
        meeting.setSubject(StrUtil.blankToDefault(firstText(raw, "subject", "meeting_subject", "title"), "腾讯会议"));
        meeting.setStatus(normalizeStatus(firstText(raw, "status", "meeting_status")));
        meeting.setCreatorUserId(StrUtil.blankToDefault(firstText(raw, "creator_userid", "creator_user_id", "userid"), mapping.getMeetingUserId()));
        meeting.setCreatorName(StrUtil.blankToDefault(firstText(raw, "creator_name", "creatorName", "user_name"), mapping.getUserName()));
        meeting.setCrmCreatorUserId(mapping.getCrmUserId());
        meeting.setStartTime(toDate(raw.get("start_time")));
        meeting.setEndTime(toDate(raw.get("end_time")));
        if (meeting.getStartTime() != null && meeting.getEndTime() != null) {
            meeting.setDurationSeconds(Math.max(0L, (meeting.getEndTime().getTime() - meeting.getStartTime().getTime()) / 1000L));
        }
        meeting.setRawJson(raw.toJSONString());
        meeting.setSyncedAt(new Date());
        if (insert) {
            meetingMapper.insert(meeting);
        } else {
            meetingMapper.updateById(meeting);
        }
        return meeting;
    }

    private void saveParticipant(TencentMeetingCorpConfig config, TencentMeeting meeting, JSONObject raw) {
        TencentMeetingParticipant participant = new TencentMeetingParticipant();
        participant.setAppId(config.getAppId());
        participant.setMeetingDbId(meeting.getId());
        participant.setMeetingId(meeting.getMeetingId());
        participant.setUserId(firstText(raw, "userid", "user_id", "open_id"));
        participant.setUserName(firstText(raw, "user_name", "username", "name"));
        participant.setRole(firstText(raw, "role", "user_role"));
        participant.setJoinTime(toDate(raw.get("join_time")));
        participant.setLeaveTime(toDate(raw.get("left_time")));
        if (participant.getJoinTime() != null && participant.getLeaveTime() != null) {
            participant.setDurationSeconds(Math.max(0L, (participant.getLeaveTime().getTime() - participant.getJoinTime().getTime()) / 1000L));
        }
        participant.setRawJson(raw.toJSONString());
        participantMapper.insert(participant);
    }

    private TencentMeetingRecording saveRecording(TencentMeetingCorpConfig config, TencentMeeting meeting, JSONObject raw) {
        TencentMeetingRecording recording = new TencentMeetingRecording();
        recording.setAppId(config.getAppId());
        recording.setMeetingDbId(meeting.getId());
        recording.setMeetingId(meeting.getMeetingId());
        recording.setRecordFileId(firstText(raw, "record_file_id", "recordFileId", "file_id"));
        recording.setFileName(firstText(raw, "record_file_name", "file_name", "name"));
        recording.setDownloadUrl(firstText(raw, "download_url", "downloadUrl"));
        recording.setPlayUrl(firstText(raw, "play_url", "playUrl"));
        recording.setFileSize(toLong(raw.get("file_size")));
        recording.setDurationSeconds(toLong(raw.get("duration")));
        recording.setTranscriptStatus(firstText(raw, "transcript_status", "transcripts_status"));
        recording.setSummary(firstText(raw, "meeting_summary", "summary"));
        recording.setTodoText(firstText(raw, "todo_text", "todo"));
        recording.setRawJson(raw.toJSONString());
        recordingMapper.insert(recording);
        return recording;
    }

    private TencentMeetingTranscriptSegment saveTranscriptSegment(TencentMeetingRecording recording, TencentMeeting meeting, JSONObject raw) {
        TencentMeetingTranscriptSegment segment = new TencentMeetingTranscriptSegment();
        segment.setRecordingId(recording.getId());
        segment.setMeetingDbId(meeting.getId());
        segment.setMeetingId(meeting.getMeetingId());
        segment.setRecordFileId(recording.getRecordFileId());
        segment.setPid(firstText(raw, "pid", "paragraph_id"));
        segment.setSpeakerUserId(firstText(raw, "speaker_userid", "speaker_user_id", "userid"));
        segment.setSpeakerName(firstText(raw, "speaker_name", "user_name", "name"));
        segment.setStartTimeMs(toLong(raw.get("start_time")));
        segment.setEndTimeMs(toLong(raw.get("end_time")));
        segment.setText(firstText(raw, "text", "content"));
        segment.setRawJson(raw.toJSONString());
        transcriptMapper.insert(segment);
        return segment;
    }

    private boolean matchesMeeting(TencentMeeting meeting, JSONObject rawRecording) {
        String recordingMeetingId = firstText(rawRecording, "meeting_id", "meetingId");
        return StrUtil.isBlank(recordingMeetingId) || recordingMeetingId.equals(meeting.getMeetingId());
    }

    private String normalizeStatus(String rawStatus) {
        if (StrUtil.isBlank(rawStatus)) {
            return "ended";
        }
        String value = rawStatus.trim().toLowerCase();
        if (value.contains("取消") || value.contains("cancel")) {
            return "cancelled";
        }
        if (value.contains("未开始") || value.contains("not_started")) {
            return "not_started";
        }
        if (value.contains("结束") || value.contains("end")) {
            return "ended";
        }
        return value;
    }

    private String formatTranscriptLine(TencentMeetingTranscriptSegment segment) {
        return StrUtil.blankToDefault(segment.getSpeakerName(), "未知发言人") + "：" + StrUtil.blankToDefault(segment.getText(), "");
    }

    private String firstText(JSONObject raw, String... keys) {
        for (String key : keys) {
            String value = raw.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Date toDate(Object value) {
        Long raw = toLong(value);
        if (raw == null) {
            return null;
        }
        long millis = raw > 9_999_999_999L ? raw : raw * 1000L;
        return new Date(millis);
    }
}
