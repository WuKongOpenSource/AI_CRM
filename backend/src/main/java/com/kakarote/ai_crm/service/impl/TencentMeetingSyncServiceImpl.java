package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.auth.DataPermissionHolder;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.BO.TencentMeetingSyncRunBO;
import com.kakarote.ai_crm.entity.PO.TencentMeeting;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingParticipant;
import com.kakarote.ai_crm.entity.PO.TencentMeetingRecording;
import com.kakarote.ai_crm.entity.PO.TencentMeetingSyncLog;
import com.kakarote.ai_crm.entity.PO.TencentMeetingTranscriptSegment;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.entity.VO.TencentMeetingSyncStatusVO;
import com.kakarote.ai_crm.utils.UserUtil;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    @Autowired
    private TencentMeetingOAuthTokenProvider tokenProvider;

    @Autowired
    private ManageUserMapper manageUserMapper;

    /** 腾讯会议数据权限模块标识，与 GlobalDataPermissionHandler.MODULE_BY_MAPPER 中各腾讯会议 mapper 的映射保持一致。 */
    private static final String TENCENT_MEETING_PERMISSION_MODULE = "tencentMeeting";

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
        log.debug("Tencent Meeting sync started: syncLogId={}, appId={}, syncDays={}, syncRecordings={}, syncTranscripts={}",
                syncLog.getId(), config.getAppId(), syncDays, syncRecordings, syncTranscripts);

        TencentMeetingSyncStatusVO status = new TencentMeetingSyncStatusVO();
        status.setAppId(config.getAppId());
        int fetched = 0;
        int saved = 0;
        int failed = 0;
        String lastError = null;
        try {
            List<TencentMeetingUserMapping> mappings = loadMappings(config, actualRunBO);
            log.debug("Tencent Meeting sync mappings resolved: syncLogId={}, appId={}, userId={}, mappingCount={}",
                    syncLog.getId(), config.getAppId(), UserUtil.getUserIdOrNull(), mappings.size());
            if (mappings.isEmpty()) {
                failed++;
                log.debug("Tencent Meeting sync has no active authorized account: syncLogId={}, appId={}",
                        syncLog.getId(), config.getAppId());
                lastError = "请先授权腾讯会议账号";
            }
            for (TencentMeetingUserMapping mapping : mappings) {
                try {
                    log.debug("Tencent Meeting account sync start: syncLogId={}, appId={}, openId={}, crmUserId={}",
                            syncLog.getId(), config.getAppId(), mapping.getMeetingUserId(), mapping.getCrmUserId());
                    TencentMeetingOAuthCredential credential = tokenProvider.credential(config, mapping);
                    List<JSONObject> meetings = listMeetingsForSync(credential, syncDays);
                    log.debug("Tencent Meeting meetings fetched: syncLogId={}, appId={}, openId={}, syncDays={}, meetingCount={}",
                            syncLog.getId(), config.getAppId(), mapping.getMeetingUserId(), syncDays, meetings.size());
                    fetched += meetings.size();
                    for (JSONObject rawMeeting : meetings) {
                        TencentMeeting meeting = upsertMeeting(config, mapping, rawMeeting);
                        saved++;
                        try {
                            log.debug("Tencent Meeting participants fetch start: syncLogId={}, meetingId={}, openId={}",
                                    syncLog.getId(), meeting.getMeetingId(), mapping.getMeetingUserId());
                            List<JSONObject> participants = apiGateway.getMeetingParticipants(credential, meeting.getMeetingId());
                            List<TencentMeetingParticipant> savedParticipants = new ArrayList<>();
                            for (JSONObject rawParticipant : participants) {
                                savedParticipants.add(saveParticipant(config, meeting, rawParticipant));
                                saved++;
                            }
                            applyParticipantSummary(meeting, savedParticipants);
                            log.debug("Tencent Meeting participants saved: syncLogId={}, meetingId={}, openId={}, participantCount={}",
                                    syncLog.getId(), meeting.getMeetingId(), mapping.getMeetingUserId(), savedParticipants.size());
                        } catch (Exception detailException) {
                            failed++;
                            log.warn("Tencent Meeting participant sync skipped: meetingId={}, openId={}, error={}",
                                    meeting.getMeetingId(), mapping.getMeetingUserId(), detailException.getMessage());
                        }
                        if (syncRecordings) {
                            try {
                                log.debug("Tencent Meeting recordings scan start: syncLogId={}, meetingId={}, openId={}, syncTranscripts={}",
                                        syncLog.getId(), meeting.getMeetingId(), mapping.getMeetingUserId(),
                                        syncTranscripts && Boolean.TRUE.equals(config.getTranscriptEnabled()));
                                List<JSONObject> recordings = apiGateway.listRecordings(credential, syncDays);
                                int matchedRecordings = 0;
                                int savedSegments = 0;
                                for (JSONObject rawRecording : recordings) {
                                    if (!matchesMeeting(meeting, rawRecording)) {
                                        continue;
                                    }
                                    matchedRecordings++;
                                    TencentMeetingRecording recording = saveRecording(config, meeting, rawRecording);
                                    saved++;
                                    List<JSONObject> segments;
                                    try {
                                        segments = syncTranscripts && Boolean.TRUE.equals(config.getTranscriptEnabled())
                                                ? apiGateway.getTranscriptSegments(credential, meeting.getMeetingId(), recording.getRecordFileId())
                                                : List.of();
                                    } catch (Exception transcriptException) {
                                        failed++;
                                        log.warn("Tencent Meeting transcript sync skipped: meetingId={}, recordFileId={}, openId={}, error={}",
                                                meeting.getMeetingId(), recording.getRecordFileId(), mapping.getMeetingUserId(), transcriptException.getMessage());
                                        segments = List.of();
                                    }
                                    List<String> texts = new ArrayList<>();
                                    for (JSONObject rawSegment : segments) {
                                        TencentMeetingTranscriptSegment segment = saveTranscriptSegment(recording, meeting, rawSegment);
                                        if (StrUtil.isNotBlank(segment.getText())) {
                                            texts.add(formatTranscriptLine(segment));
                                        }
                                        savedSegments++;
                                        saved++;
                                    }
                                    if (!texts.isEmpty() || StrUtil.isNotBlank(recording.getSummary()) || StrUtil.isNotBlank(recording.getTodoText())) {
                                        meeting.setSummary(StrUtil.blankToDefault(meeting.getSummary(), recording.getSummary()));
                                        meeting.setTodoText(StrUtil.blankToDefault(meeting.getTodoText(), recording.getTodoText()));
                                        meeting.setTranscriptText(String.join("\n", texts));
                                        meetingMapper.updateById(meeting);
                                    }
                                }
                                log.debug("Tencent Meeting recordings saved: syncLogId={}, meetingId={}, openId={}, matchedRecordings={}, transcriptSegments={}",
                                        syncLog.getId(), meeting.getMeetingId(), mapping.getMeetingUserId(),
                                        matchedRecordings, savedSegments);
                            } catch (Exception recordingException) {
                                failed++;
                                log.warn("Tencent Meeting recording sync skipped: meetingId={}, openId={}, error={}",
                                        meeting.getMeetingId(), mapping.getMeetingUserId(), recordingException.getMessage());
                            }
                        }
                    }
                    mapping.setLastSyncTime(new Date());
                    mapping.setLastSyncError(null);
                    userMappingMapper.updateById(mapping);
                    log.debug("Tencent Meeting account sync done: syncLogId={}, appId={}, openId={}, fetchedSoFar={}, savedSoFar={}, failedSoFar={}",
                            syncLog.getId(), config.getAppId(), mapping.getMeetingUserId(), fetched, saved, failed);
                } catch (Exception accountException) {
                    failed++;
                    lastError = accountException.getMessage();
                    mapping.setLastSyncTime(new Date());
                    mapping.setLastSyncError(accountException.getMessage());
                    userMappingMapper.updateById(mapping);
                    log.warn("Tencent Meeting account sync skipped: openId={}, error={}", mapping.getMeetingUserId(), accountException.getMessage());
                }
            }
            if (failed > 0 && fetched == 0 && saved == 0) {
                syncLog.setStatus("failed");
                syncLog.setErrorMessage(lastError);
                status.setLastSyncStatus("failed");
                status.setLastSyncError(lastError);
            } else {
                syncLog.setStatus("success");
                status.setLastSyncStatus("success");
            }
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
        log.debug("Tencent Meeting sync finished: syncLogId={}, appId={}, status={}, fetched={}, saved={}, failed={}, error={}",
                syncLog.getId(), config.getAppId(), syncLog.getStatus(), fetched, saved, failed,
                StrUtil.maxLength(syncLog.getErrorMessage(), 500));
        return status;
    }

    @Transactional(rollbackFor = Exception.class)
    public void refreshMeetingByExternalId(String eventName, String meetingId) {
        log.info("Tencent Meeting webhook refresh requested: event={}, meetingId={}", eventName, meetingId);
        String targetStatus = statusForWebhookEvent(eventName);
        if (targetStatus == null || StrUtil.isBlank(meetingId)) {
            return;
        }
        TencentMeeting meeting = meetingMapper.selectOne(Wrappers.<TencentMeeting>lambdaQuery()
                .eq(TencentMeeting::getMeetingId, meetingId)
                .last("LIMIT 1"));
        if (meeting == null) {
            log.info("Tencent Meeting webhook refresh skipped, meeting not found: meetingId={}", meetingId);
            return;
        }
        if (targetStatus.equals(meeting.getStatus())) {
            return;
        }
        meeting.setStatus(targetStatus);
        meeting.setSyncedAt(new Date());
        meetingMapper.updateById(meeting);
        log.info("Tencent Meeting status updated via webhook: meetingId={}, status={}", meetingId, targetStatus);
    }

    private String statusForWebhookEvent(String eventName) {
        if (StrUtil.isBlank(eventName)) {
            return null;
        }
        String value = eventName.trim().toLowerCase();
        if ("meeting.created".equals(value)) {
            return "not_started";
        }
        if ("meeting.started".equals(value) || "meeting.start".equals(value)) {
            return "started";
        }
        if ("meeting.end".equals(value) || "meeting.ended".equals(value)) {
            return "ended";
        }
        if ("meeting.canceled".equals(value) || "meeting.cancelled".equals(value) || "meeting.cancel".equals(value)) {
            return "cancelled";
        }
        return null;
    }

    /**
     * 处理腾讯会议 webhook（meeting.created / started / end / canceled 等）：即时更新状态，并从回调载荷
     * 写入真实发起人与实际时长；会议结束时再调历史成员接口补全参会人。
     * 库中不存在的会议（如在非本系统直接创建）：仅当发起人能映射到本租户已授权账号时才新建，
     * 以保证会议可归属、可见（数据权限按 crm_creator_user_id 过滤），并把范围限定在本系统用户的会议。
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshMeetingFromWebhook(String eventName, JSONObject meetingInfo, TencentMeetingCorpConfig config) {
        // webhook 回调线程没有登录用户，而腾讯会议各 mapper 注册在数据权限模块；若不预置上下文，
        // DataPermissionInterceptor 会调用 UserUtil.getUserId() 抛 “网络错误，请稍候再试”。
        // 这里以租户级系统身份（全部数据）放行行级数据权限，租户隔离仍由 TenantContextHolder 保证。
        withTencentMeetingDataPermission(() -> doRefreshMeetingFromWebhook(eventName, meetingInfo, config));
    }

    private void doRefreshMeetingFromWebhook(String eventName, JSONObject meetingInfo, TencentMeetingCorpConfig config) {
        String meetingId = meetingInfo == null ? null
                : firstText(meetingInfo, "meeting_id", "meetingId", "meeting_uuid");
        log.info("Tencent Meeting webhook refresh requested: event={}, meetingId={}", eventName, meetingId);
        if (StrUtil.isBlank(meetingId)) {
            return;
        }
        String targetStatus = statusForWebhookEvent(eventName);
        TencentMeeting meeting = meetingMapper.selectOne(Wrappers.<TencentMeeting>lambdaQuery()
                .eq(TencentMeeting::getMeetingId, meetingId)
                .last("LIMIT 1"));
        TencentMeetingUserMapping creatorMapping = resolveCreatorMapping(config == null ? null : config.getAppId(), meetingInfo);
        boolean insert = false;
        if (meeting == null) {
            if (targetStatus == null) {
                // 录制/转写等非生命周期事件不为未知会议凭空建档。
                log.info("Tencent Meeting webhook skipped, unknown meeting for non-lifecycle event: event={}, meetingId={}", eventName, meetingId);
                return;
            }
            if (config == null || creatorMapping == null || creatorMapping.getCrmUserId() == null) {
                log.info("Tencent Meeting webhook create skipped, creator not mapped to an authorized account: event={}, meetingId={}", eventName, meetingId);
                return;
            }
            meeting = new TencentMeeting();
            meeting.setAppId(config.getAppId());
            meeting.setMeetingId(meetingId);
            meeting.setBindStatus("UNBOUND");
            insert = true;
        }
        if (targetStatus != null) {
            meeting.setStatus(targetStatus);
        } else if (StrUtil.isBlank(meeting.getStatus())) {
            meeting.setStatus("not_started");
        }
        applyWebhookSubject(meeting, meetingInfo);
        applyWebhookCreator(meeting, meetingInfo, creatorMapping);
        applyWebhookTimes(meeting, meetingInfo);
        meeting.setSyncedAt(new Date());
        if (insert) {
            meeting.setRawJson(meetingInfo == null ? null : meetingInfo.toJSONString());
            meetingMapper.insert(meeting);
            log.info("Tencent Meeting created via webhook: meetingId={}, status={}", meetingId, meeting.getStatus());
        } else {
            meetingMapper.updateById(meeting);
            log.info("Tencent Meeting refreshed via webhook: meetingId={}, status={}", meetingId, meeting.getStatus());
        }

        if ("ended".equals(targetStatus)) {
            refreshParticipantsFromHistory(config, meeting, meetingInfo);
        }
    }

    /**
     * 在无登录用户的系统线程（如 webhook 回调）中执行腾讯会议相关读写时，临时把数据权限上下文预置为
     * “全部数据”。这样 GlobalDataPermissionHandler 会直接复用缓存的上下文、跳过 UserUtil.getUserId()，
     * 避免因线程内没有 SecurityContext/AiContext 而抛 BusinessException；行级权限放行，租户隔离不受影响。
     * 执行结束后恢复/清理上下文，防止线程池复用导致 ThreadLocal 泄漏。
     */
    private void withTencentMeetingDataPermission(Runnable action) {
        DataPermissionContext previousContext = DataPermissionHolder.get(TENCENT_MEETING_PERMISSION_MODULE);
        DataPermissionHolder.put(TENCENT_MEETING_PERMISSION_MODULE, DataPermissionContext.all());
        try {
            action.run();
        } finally {
            if (previousContext == null) {
                DataPermissionHolder.remove(TENCENT_MEETING_PERMISSION_MODULE);
            } else {
                DataPermissionHolder.put(TENCENT_MEETING_PERMISSION_MODULE, previousContext);
            }
        }
    }

    private void applyWebhookSubject(TencentMeeting meeting, JSONObject meetingInfo) {
        if (meetingInfo == null) {
            return;
        }
        String subject = firstText(meetingInfo, "subject", "meeting_subject", "title");
        if (StrUtil.isNotBlank(subject)) {
            meeting.setSubject(subject);
        } else if (StrUtil.isBlank(meeting.getSubject())) {
            meeting.setSubject("腾讯会议");
        }
        String meetingCode = firstText(meetingInfo, "meeting_code", "meetingCode");
        if (StrUtil.isNotBlank(meetingCode)) {
            meeting.setMeetingCode(meetingCode);
        }
    }

    private void applyWebhookCreator(TencentMeeting meeting, JSONObject meetingInfo, TencentMeetingUserMapping creatorMapping) {
        if (meetingInfo == null) {
            return;
        }
        JSONObject creator = meetingInfo.getJSONObject("creator");
        String creatorUserId = creator != null
                ? firstText(creator, "userid", "user_id", "ms_open_id", "open_id")
                : firstText(meetingInfo, "creator_userid", "creator_user_id");
        String creatorName = creator != null
                ? firstText(creator, "user_name", "username", "nick_name", "nickname", "name")
                : firstText(meetingInfo, "creator_name", "creatorName");
        if (StrUtil.isBlank(creatorUserId) && StrUtil.isBlank(creatorName)) {
            return;
        }
        if (StrUtil.isNotBlank(creatorUserId)) {
            meeting.setCreatorUserId(creatorUserId);
        }
        if (creatorMapping != null && creatorMapping.getCrmUserId() != null) {
            meeting.setCrmCreatorUserId(creatorMapping.getCrmUserId());
        }
        String resolved = resolveDisplayName(creatorName, creatorUserId, creatorMapping);
        if (StrUtil.isNotBlank(resolved)) {
            meeting.setCreatorName(resolved);
        }
    }

    private TencentMeetingUserMapping resolveCreatorMapping(String appId, JSONObject meetingInfo) {
        if (StrUtil.isBlank(appId) || meetingInfo == null) {
            return null;
        }
        JSONObject creator = meetingInfo.getJSONObject("creator");
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (creator != null) {
            addIfNotBlank(candidates, firstText(creator, "userid"));
            addIfNotBlank(candidates, firstText(creator, "user_id"));
            addIfNotBlank(candidates, firstText(creator, "ms_open_id"));
            addIfNotBlank(candidates, firstText(creator, "open_id"));
        }
        addIfNotBlank(candidates, firstText(meetingInfo, "creator_userid"));
        addIfNotBlank(candidates, firstText(meetingInfo, "creator_user_id"));
        if (candidates.isEmpty()) {
            return null;
        }
        return userMappingMapper.selectOne(Wrappers.<TencentMeetingUserMapping>lambdaQuery()
                .eq(TencentMeetingUserMapping::getAppId, appId)
                .in(TencentMeetingUserMapping::getMeetingUserId, candidates)
                .last("LIMIT 1"));
    }

    private void addIfNotBlank(java.util.Collection<String> target, String value) {
        if (StrUtil.isNotBlank(value)) {
            target.add(value);
        }
    }

    private void applyWebhookTimes(TencentMeeting meeting, JSONObject meetingInfo) {
        if (meetingInfo == null) {
            return;
        }
        Date start = toDate(firstValue(meetingInfo, "media_start_time", "start_time"));
        Date end = toDate(firstValue(meetingInfo, "media_end_time", "end_time"));
        if (start != null) {
            meeting.setStartTime(start);
        }
        if (end != null) {
            meeting.setEndTime(end);
        }
        if (meeting.getStartTime() != null && meeting.getEndTime() != null
                && !meeting.getEndTime().before(meeting.getStartTime())) {
            meeting.setDurationSeconds(Math.max(0L,
                    (meeting.getEndTime().getTime() - meeting.getStartTime().getTime()) / 1000L));
        }
    }

    private void refreshParticipantsFromHistory(TencentMeetingCorpConfig config, TencentMeeting meeting, JSONObject meetingInfo) {
        if (config == null) {
            return;
        }
        try {
            TencentMeetingUserMapping mapping = resolveCredentialMapping(config, meetingInfo);
            if (mapping == null) {
                log.info("Tencent Meeting webhook participant refresh skipped, no active account: meetingId={}", meeting.getMeetingId());
                return;
            }
            TencentMeetingOAuthCredential credential = tokenProvider.credential(config, mapping);
            String subMeetingId = meetingInfo == null ? null : firstText(meetingInfo, "sub_meeting_id", "subMeetingId");
            List<JSONObject> participants = apiGateway.getMeetingParticipantsHistory(credential, meeting.getMeetingId(), subMeetingId);
            if (participants.isEmpty()) {
                // 历史成员接口为空时，退回实时成员接口（会议刚结束可能仍可取到）。
                participants = apiGateway.getMeetingParticipants(credential, meeting.getMeetingId());
            }
            if (participants.isEmpty()) {
                return;
            }
            participantMapper.delete(Wrappers.<TencentMeetingParticipant>lambdaQuery()
                    .eq(TencentMeetingParticipant::getMeetingDbId, meeting.getId()));
            List<TencentMeetingParticipant> saved = new ArrayList<>();
            for (JSONObject rawParticipant : participants) {
                saved.add(saveParticipant(config, meeting, rawParticipant));
            }
            applyParticipantSummary(meeting, saved);
            log.info("Tencent Meeting participants refreshed via webhook: meetingId={}, count={}", meeting.getMeetingId(), saved.size());
        } catch (Exception e) {
            log.warn("Tencent Meeting webhook participant refresh failed: meetingId={}, error={}", meeting.getMeetingId(), e.getMessage());
        }
    }

    private TencentMeetingUserMapping resolveCredentialMapping(TencentMeetingCorpConfig config, JSONObject meetingInfo) {
        JSONObject creator = meetingInfo == null ? null : meetingInfo.getJSONObject("creator");
        String creatorUserId = creator != null
                ? firstText(creator, "userid", "user_id", "ms_open_id", "open_id")
                : (meetingInfo == null ? null : firstText(meetingInfo, "creator_userid", "creator_user_id"));
        if (StrUtil.isNotBlank(creatorUserId)) {
            TencentMeetingUserMapping creatorMapping = userMappingMapper.selectOne(Wrappers.<TencentMeetingUserMapping>lambdaQuery()
                    .eq(TencentMeetingUserMapping::getAppId, config.getAppId())
                    .eq(TencentMeetingUserMapping::getMeetingUserId, creatorUserId)
                    .eq(TencentMeetingUserMapping::getStatus, 1)
                    .eq(TencentMeetingUserMapping::getAuthStatus, TencentMeetingOAuthTokenProvider.AUTH_STATUS_ACTIVE)
                    .last("LIMIT 1"));
            if (creatorMapping != null) {
                return creatorMapping;
            }
        }
        return userMappingMapper.selectOne(Wrappers.<TencentMeetingUserMapping>lambdaQuery()
                .eq(TencentMeetingUserMapping::getAppId, config.getAppId())
                .eq(TencentMeetingUserMapping::getStatus, 1)
                .eq(TencentMeetingUserMapping::getAuthStatus, TencentMeetingOAuthTokenProvider.AUTH_STATUS_ACTIVE)
                .orderByDesc(TencentMeetingUserMapping::getLastAuthTime)
                .last("LIMIT 1"));
    }

    public JSONObject createMeeting(TencentMeetingOAuthCredential credential, JSONObject requestBody) {
        return apiGateway.createMeeting(credential, requestBody);
    }

    public JSONObject getMeetingDetail(TencentMeetingOAuthCredential credential, String meetingId) {
        return apiGateway.getMeetingDetail(credential, meetingId);
    }

    private List<JSONObject> listMeetingsForSync(TencentMeetingOAuthCredential credential, int syncDays) {
        List<JSONObject> meetings = new ArrayList<>();
        appendAll(meetings, apiGateway.listUpcomingMeetings(credential));
        appendAll(meetings, apiGateway.listEndedMeetings(credential, syncDays));
        return deduplicateMeetings(meetings);
    }

    private void appendAll(List<JSONObject> target, List<JSONObject> source) {
        if (source != null && !source.isEmpty()) {
            target.addAll(source);
        }
    }

    private List<JSONObject> deduplicateMeetings(List<JSONObject> meetings) {
        LinkedHashMap<String, JSONObject> byId = new LinkedHashMap<>();
        for (JSONObject meeting : meetings) {
            if (meeting == null) {
                continue;
            }
            String meetingId = firstText(meeting, "meeting_id", "meetingId", "meeting_uuid");
            byId.putIfAbsent(StrUtil.blankToDefault(meetingId, meeting.toJSONString()), meeting);
        }
        return new ArrayList<>(byId.values());
    }

    private List<TencentMeetingUserMapping> loadMappings(TencentMeetingCorpConfig config, TencentMeetingSyncRunBO runBO) {
        Long userId = UserUtil.getUserIdOrNull();
        if (userId == null) {
            return List.of();
        }
        var wrapper = Wrappers.<TencentMeetingUserMapping>lambdaQuery()
                .eq(TencentMeetingUserMapping::getAppId, config.getAppId())
                .eq(TencentMeetingUserMapping::getStatus, 1)
                .eq(TencentMeetingUserMapping::getAuthStatus, TencentMeetingOAuthTokenProvider.AUTH_STATUS_ACTIVE)
                .isNotNull(TencentMeetingUserMapping::getCrmUserId)
                .eq(TencentMeetingUserMapping::getCrmUserId, userId)
                .orderByDesc(TencentMeetingUserMapping::getLastAuthTime);
        return userMappingMapper.selectList(wrapper);
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
        String creatorUserId = StrUtil.blankToDefault(firstText(raw, "creator_userid", "creator_user_id", "userid"), mapping.getMeetingUserId());
        meeting.setCreatorUserId(creatorUserId);
        meeting.setCreatorName(resolveDisplayName(
                firstText(raw, "creator_name", "creatorName", "nick_name", "nickname", "user_name", "username", "name"),
                creatorUserId,
                mapping));
        meeting.setCrmCreatorUserId(mapping.getCrmUserId());
        meeting.setStartTime(toDate(raw.get("start_time")));
        meeting.setEndTime(toDate(raw.get("end_time")));
        Long explicitDuration = resolveDurationSeconds(raw);
        if (explicitDuration != null) {
            meeting.setDurationSeconds(explicitDuration);
        } else if (meeting.getStartTime() != null && meeting.getEndTime() != null
                && !meeting.getEndTime().before(meeting.getStartTime())) {
            // 起止时间兜底（含已结束会议）。若随后取到参会人进出场时间，applyParticipantSummary 会用实际时长覆盖。
            meeting.setDurationSeconds(Math.max(0L, (meeting.getEndTime().getTime() - meeting.getStartTime().getTime()) / 1000L));
        } else {
            meeting.setDurationSeconds(null);
        }
        meeting.setRawJson(raw.toJSONString());
        meeting.setSyncedAt(new Date());
        if (insert) {
            meetingMapper.insert(meeting);
        } else {
            meetingMapper.updateById(meeting);
        }
        log.debug("Tencent Meeting upserted: appId={}, meetingId={}, dbId={}, inserted={}, status={}",
                config.getAppId(), externalMeetingId, meeting.getId(), insert, meeting.getStatus());
        return meeting;
    }

    private TencentMeetingParticipant saveParticipant(TencentMeetingCorpConfig config, TencentMeeting meeting, JSONObject raw) {
        TencentMeetingParticipant participant = new TencentMeetingParticipant();
        participant.setAppId(config.getAppId());
        participant.setMeetingDbId(meeting.getId());
        participant.setMeetingId(meeting.getMeetingId());
        participant.setUserId(firstText(raw, "userid", "user_id", "open_id"));
        participant.setUserName(resolveDisplayName(firstText(raw, "user_name", "username", "name"), participant.getUserId(), null));
        participant.setRole(firstText(raw, "role", "user_role"));
        participant.setJoinTime(toDate(raw.get("join_time")));
        participant.setLeaveTime(toDate(firstValue(raw, "left_time", "leave_time")));
        if (participant.getJoinTime() != null && participant.getLeaveTime() != null) {
            participant.setDurationSeconds(Math.max(0L, (participant.getLeaveTime().getTime() - participant.getJoinTime().getTime()) / 1000L));
        }
        participant.setRawJson(raw.toJSONString());
        participantMapper.insert(participant);
        return participant;
    }

    private void applyParticipantSummary(TencentMeeting meeting, List<TencentMeetingParticipant> participants) {
        if (participants == null || participants.isEmpty()) {
            return;
        }
        Date firstJoin = null;
        Date lastLeave = null;
        LinkedHashSet<String> participantNames = new LinkedHashSet<>();
        for (TencentMeetingParticipant participant : participants) {
            String displayName = StrUtil.blankToDefault(participant.getUserName(), participant.getUserId());
            if (StrUtil.isNotBlank(displayName)) {
                participantNames.add(displayName);
            }
            if (participant.getJoinTime() != null && (firstJoin == null || participant.getJoinTime().before(firstJoin))) {
                firstJoin = participant.getJoinTime();
            }
            if (participant.getLeaveTime() != null && (lastLeave == null || participant.getLeaveTime().after(lastLeave))) {
                lastLeave = participant.getLeaveTime();
            }
        }
        meeting.setParticipantCount(participants.size());
        meeting.setParticipantNames(participantNames.isEmpty() ? null : String.join(",", participantNames));
        if (firstJoin != null && lastLeave != null && !lastLeave.before(firstJoin)) {
            meeting.setDurationSeconds(Math.max(0L, (lastLeave.getTime() - firstJoin.getTime()) / 1000L));
        }
        meetingMapper.updateById(meeting);
    }

    private TencentMeetingRecording saveRecording(TencentMeetingCorpConfig config, TencentMeeting meeting, JSONObject raw) {
        TencentMeetingRecording recording = new TencentMeetingRecording();
        recording.setAppId(config.getAppId());
        recording.setMeetingDbId(meeting.getId());
        recording.setMeetingId(meeting.getMeetingId());
        recording.setRecordFileId(firstText(raw, "record_file_id", "recordFileId", "file_id"));
        recording.setFileName(firstText(raw, "record_file_name", "file_name", "name"));
        recording.setDownloadUrl(firstText(raw, "download_url", "downloadUrl", "download_address"));
        recording.setPlayUrl(firstText(raw, "play_url", "playUrl", "view_address", "sharing_url"));
        recording.setFileSize(toLong(firstValue(raw, "file_size", "record_size")));
        Long duration = toLong(raw.get("duration"));
        Long recordStart = toLong(raw.get("record_start_time"));
        Long recordEnd = toLong(raw.get("record_end_time"));
        if (duration == null && recordStart != null && recordEnd != null) {
            duration = Math.max(0L, (recordEnd - recordStart) / 1000L);
        }
        recording.setDurationSeconds(duration);
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
        if (value.contains("未开始") || value.contains("待开始") || value.contains("not_started") || value.contains("init")) {
            return "not_started";
        }
        if (value.contains("started") || value.contains("进行中") || value.contains("会议中")) {
            return "started";
        }
        if (value.contains("结束") || value.contains("end")) {
            return "ended";
        }
        return value;
    }

    private String formatTranscriptLine(TencentMeetingTranscriptSegment segment) {
        return StrUtil.blankToDefault(segment.getSpeakerName(), "未知发言人") + "：" + StrUtil.blankToDefault(segment.getText(), "");
    }

    private Long resolveDurationSeconds(JSONObject raw) {
        Long seconds = toLong(firstValue(raw, "duration_seconds", "duration"));
        if (seconds != null) {
            return seconds;
        }
        Long millis = toLong(firstValue(raw, "meeting_duration", "user_meeting_duration"));
        return millis == null ? null : Math.max(0L, millis / 1000L);
    }

    private String resolveDisplayName(String rawName, String userId, TencentMeetingUserMapping mapping) {
        String normalizedUserId = StrUtil.trim(userId);
        String mappingUserId = mapping == null ? null : StrUtil.trim(mapping.getMeetingUserId());
        String displayName = firstDisplayName(normalizedUserId, mappingUserId, rawName);
        if (displayName != null) {
            return displayName;
        }
        if (mapping != null) {
            displayName = firstDisplayName(normalizedUserId, mappingUserId, mapping.getUserName());
            if (displayName != null) {
                return displayName;
            }
            displayName = firstDisplayName(normalizedUserId, mappingUserId, crmUserDisplayName(mapping.getCrmUserId()));
            if (displayName != null) {
                return displayName;
            }
        }
        return StrUtil.blankToDefault(rawName, normalizedUserId);
    }

    private String firstDisplayName(String userId, String mappingUserId, String... values) {
        for (String value : values) {
            String candidate = decodeBase64IfNeeded(value);
            if (StrUtil.isBlank(candidate)) {
                continue;
            }
            if (candidate.equals(userId) || candidate.equals(mappingUserId)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private String decodeBase64IfNeeded(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            String decoded = new String(Base64.getDecoder().decode(trimmed), StandardCharsets.UTF_8).trim();
            if (isReadableText(decoded)) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            return trimmed;
        }
        return trimmed;
    }

    private boolean isReadableText(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        boolean hasNameCharacter = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isISOControl(character) || character == '\uFFFD') {
                return false;
            }
            if (Character.isLetterOrDigit(character)) {
                hasNameCharacter = true;
            }
        }
        return hasNameCharacter;
    }

    private String crmUserDisplayName(Long crmUserId) {
        if (crmUserId == null || manageUserMapper == null) {
            return null;
        }
        ManagerUser user = manageUserMapper.selectById(crmUserId);
        if (user == null) {
            return null;
        }
        return firstNotBlank(user.getRealname(), user.getUsername(), user.getMobile(), user.getEmail());
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String firstText(JSONObject raw, String... keys) {
        Object value = firstValue(raw, keys);
        return value == null ? null : String.valueOf(value);
    }

    private Object firstValue(JSONObject raw, String... keys) {
        for (String key : keys) {
            Object value = raw.get(key);
            if (value != null && StrUtil.isNotBlank(String.valueOf(value))) {
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
