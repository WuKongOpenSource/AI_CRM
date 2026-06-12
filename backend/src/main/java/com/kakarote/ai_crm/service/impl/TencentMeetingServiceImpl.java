package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.BO.TencentMeetingBindBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingCandidateQueryBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingCreateBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingQueryBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingSyncRunBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingUnbindBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.TencentMeeting;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCustomerBinding;
import com.kakarote.ai_crm.entity.PO.TencentMeetingParticipant;
import com.kakarote.ai_crm.entity.PO.TencentMeetingRecording;
import com.kakarote.ai_crm.entity.PO.TencentMeetingSyncLog;
import com.kakarote.ai_crm.entity.PO.TencentMeetingTranscriptSegment;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.entity.VO.TencentMeetingCandidateVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingCustomerBindingVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingDetailVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingOAuthStatusVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingParticipantVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingRecordingVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingSyncStatusVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingTranscriptSegmentVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingCorpConfigMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingCustomerBindingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingParticipantMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingRecordingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingSyncLogMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingTranscriptSegmentMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.service.support.SyncTaskExecutor;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

@Service
public class TencentMeetingServiceImpl {

    private static final String AUTH_REQUIRED_ERROR = "请先授权腾讯会议账号";

    @Autowired
    private TencentMeetingCorpConfigMapper configMapper;

    @Autowired
    private TencentMeetingMapper meetingMapper;

    @Autowired
    private TencentMeetingParticipantMapper participantMapper;

    @Autowired
    private TencentMeetingRecordingMapper recordingMapper;

    @Autowired
    private TencentMeetingTranscriptSegmentMapper transcriptMapper;

    @Autowired
    private TencentMeetingCustomerBindingMapper bindingMapper;

    @Autowired
    private TencentMeetingSyncLogMapper syncLogMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private TencentMeetingSyncServiceImpl syncService;

    @Autowired
    private SyncTaskExecutor syncTaskExecutor;

    @Autowired
    private TencentMeetingCustomerBindingServiceImpl bindingService;

    @Autowired
    private TencentMeetingOAuthService oauthService;

    @Autowired
    private TencentMeetingOAuthTokenProvider tokenProvider;

    @Autowired
    private DataPermissionService dataPermissionService;

    @Transactional(rollbackFor = Exception.class)
    public TencentMeetingSyncStatusVO runSync(TencentMeetingSyncRunBO runBO) {
        TencentMeetingCorpConfig config = requireConfig();
        TencentMeetingSyncRunBO actualRunBO = runBO == null ? new TencentMeetingSyncRunBO() : runBO;
        TencentMeetingSyncStatusVO runningStatus = markConfigSyncRunning(config);
        syncTaskExecutor.submit("tencent-meeting-sync-" + config.getAppId(),
                () -> runBackgroundSync(config, () -> syncService.runSync(config, actualRunBO)));
        return runningStatus;
    }

    public TencentMeetingSyncStatusVO getSyncStatus() {
        TencentMeetingCorpConfig config = findConfig();
        TencentMeetingSyncLog latest = syncLogMapper.selectOne(Wrappers.<TencentMeetingSyncLog>lambdaQuery()
                .orderByDesc(TencentMeetingSyncLog::getStartedAt)
                .last("LIMIT 1"));
        TencentMeetingSyncStatusVO status = new TencentMeetingSyncStatusVO();
        if (config != null) {
            status.setAppId(config.getAppId());
            status.setLastSyncTime(config.getLastSyncTime());
            status.setLastSyncStatus(config.getLastSyncStatus());
            status.setLastSyncError(config.getLastSyncError());
        }
        if (latest != null) {
            status.setLastSyncTime(latest.getFinishedAt());
            status.setLastSyncStatus(latest.getStatus());
            status.setLastSyncError(latest.getErrorMessage());
            status.setFetchedCount(latest.getFetchedCount());
            status.setSavedCount(latest.getSavedCount());
            status.setFailedCount(latest.getFailedCount());
        }
        clearStaleAuthRequiredFailure(status);
        return status;
    }

    public BasePage<TencentMeetingVO> queryPageList(TencentMeetingQueryBO queryBO) {
        TencentMeetingQueryBO actual = queryBO == null ? new TencentMeetingQueryBO() : queryBO;
        var wrapper = Wrappers.<TencentMeeting>lambdaQuery()
                .eq(StrUtil.isNotBlank(actual.getStatus()), TencentMeeting::getStatus, actual.getStatus())
                .eq(StrUtil.isNotBlank(actual.getBindStatus()), TencentMeeting::getBindStatus, actual.getBindStatus())
                .eq(actual.getCustomerId() != null, TencentMeeting::getCustomerId, actual.getCustomerId())
                .ge(actual.getStartTimeFrom() != null, TencentMeeting::getStartTime, actual.getStartTimeFrom())
                .le(actual.getStartTimeTo() != null, TencentMeeting::getStartTime, actual.getStartTimeTo())
                .and(StrUtil.isNotBlank(actual.getKeyword()), nested -> nested
                        .like(TencentMeeting::getSubject, actual.getKeyword())
                        .or()
                        .like(TencentMeeting::getMeetingCode, actual.getKeyword())
                        .or()
                        .like(TencentMeeting::getCreatorName, actual.getKeyword())
                        .or()
                        .like(TencentMeeting::getParticipantNames, actual.getKeyword()))
                .orderByDesc(TencentMeeting::getStartTime)
                .orderByDesc(TencentMeeting::getUpdateTime);
        applyMeetingDataPermission(wrapper);
        IPage<TencentMeeting> page = meetingMapper.selectPage(actual.parse(), wrapper);
        BasePage<TencentMeetingVO> result = new BasePage<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toMeetingVO).toList());
        return result;
    }

    public TencentMeetingDetailVO getDetail(Long id) {
        TencentMeeting meeting = requireMeetingAccess(id);
        TencentMeetingDetailVO detail = new TencentMeetingDetailVO();
        BeanUtil.copyProperties(meeting, detail);
        List<TencentMeetingParticipant> participants = participantMapper.selectList(Wrappers.<TencentMeetingParticipant>lambdaQuery()
                .eq(TencentMeetingParticipant::getMeetingDbId, meeting.getId())
                .orderByAsc(TencentMeetingParticipant::getJoinTime));
        detail.setParticipants(participants.stream().map(item -> {
            TencentMeetingParticipantVO vo = new TencentMeetingParticipantVO();
            BeanUtil.copyProperties(item, vo);
            return vo;
        }).toList());
        List<TencentMeetingRecording> recordings = recordingMapper.selectList(Wrappers.<TencentMeetingRecording>lambdaQuery()
                .eq(TencentMeetingRecording::getMeetingDbId, meeting.getId())
                .orderByAsc(TencentMeetingRecording::getCreateTime));
        detail.setRecordings(recordings.stream().map(item -> {
            TencentMeetingRecordingVO vo = new TencentMeetingRecordingVO();
            BeanUtil.copyProperties(item, vo);
            return vo;
        }).toList());
        List<TencentMeetingTranscriptSegment> segments = transcriptMapper.selectList(Wrappers.<TencentMeetingTranscriptSegment>lambdaQuery()
                .eq(TencentMeetingTranscriptSegment::getMeetingDbId, meeting.getId())
                .orderByAsc(TencentMeetingTranscriptSegment::getStartTimeMs)
                .orderByAsc(TencentMeetingTranscriptSegment::getId));
        detail.setTranscriptSegments(segments.stream().map(this::toTranscriptSegmentVO).toList());
        return detail;
    }

    public List<TencentMeetingVO> listCustomerMeetings(Long customerId) {
        if (customerId == null) {
            return List.of();
        }
        return meetingMapper.selectList(Wrappers.<TencentMeeting>lambdaQuery()
                        .eq(TencentMeeting::getCustomerId, customerId)
                        .orderByDesc(TencentMeeting::getStartTime))
                .stream()
                .map(this::toMeetingVO)
                .toList();
    }

    public List<TencentMeetingCandidateVO> queryCandidates(TencentMeetingCandidateQueryBO queryBO) {
        TencentMeetingCandidateQueryBO actual = queryBO == null ? new TencentMeetingCandidateQueryBO() : queryBO;
        int limit = actual.getLimit() == null ? 20 : Math.min(Math.max(actual.getLimit(), 1), 50);
        String keyword = StrUtil.blankToDefault(actual.getKeyword(), actual.getInputText());
        var wrapper = Wrappers.<TencentMeeting>lambdaQuery()
                .eq(TencentMeeting::getBindStatus, "UNBOUND")
                .ge(actual.getStartTimeFrom() != null, TencentMeeting::getStartTime, actual.getStartTimeFrom())
                .le(actual.getStartTimeTo() != null, TencentMeeting::getStartTime, actual.getStartTimeTo())
                .and(StrUtil.isNotBlank(keyword), nested -> nested
                        .like(TencentMeeting::getSubject, keyword)
                        .or()
                        .like(TencentMeeting::getParticipantNames, keyword)
                        .or()
                        .like(TencentMeeting::getSummary, keyword)
                        .or()
                        .like(TencentMeeting::getTranscriptText, keyword))
                .orderByDesc(TencentMeeting::getStartTime)
                .last("LIMIT " + limit);
        applyMeetingDataPermission(wrapper);
        return meetingMapper.selectList(wrapper).stream()
                .map(meeting -> toCandidateVO(meeting, keyword))
                .sorted(Comparator.comparing(TencentMeetingCandidateVO::getScore).reversed())
                .toList();
    }

    public void bind(TencentMeetingBindBO bindBO) {
        bindingService.bindCustomer(bindBO);
    }

    public void unbind(TencentMeetingUnbindBO unbindBO) {
        bindingService.unbindCustomer(unbindBO);
    }

    public void refreshMeeting(Long id) {
        TencentMeeting meeting = requireMeetingAccess(id);
        syncTaskExecutor.submit("tencent-meeting-refresh-" + meeting.getMeetingId(),
                () -> syncService.refreshMeetingByExternalId("manual.refresh", meeting.getMeetingId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public TencentMeetingVO refreshJoinUrl(Long id) {
        TencentMeeting meeting = requireMeetingAccess(id);
        if (StrUtil.isBlank(meeting.getMeetingId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent meeting id is missing");
        }
        JSONObject raw = parseRawJson(meeting.getRawJson());
        if (StrUtil.isNotBlank(firstText(raw,
                "join_url", "joinUrl", "meeting_url", "meetingUrl", "participant_join_url",
                "host_join_url", "hostJoinUrl", "host_join_url_wx", "hostMeetingUrl"))) {
            return toMeetingVO(meeting);
        }

        TencentMeetingCorpConfig config = requireConfig();
        TencentMeetingUserMapping mapping = oauthService.requireCurrentAuthorizedAccount(config);
        TencentMeetingOAuthCredential credential = tokenProvider.credential(config, mapping);
        JSONObject remoteDetail = syncService.getMeetingDetail(credential, meeting.getMeetingId());
        JSONObject merged = raw == null ? new JSONObject() : raw;
        if (remoteDetail != null) {
            merged.putAll(remoteDetail);
        }
        meeting.setRawJson(merged.toJSONString());
        meeting.setSyncedAt(new Date());
        meetingMapper.updateById(meeting);
        return toMeetingVO(meeting);
    }

    @Transactional(rollbackFor = Exception.class)
    public TencentMeetingVO createMeeting(TencentMeetingCreateBO createBO) {
        TencentMeetingCreateBO actual = createBO == null ? new TencentMeetingCreateBO() : createBO;
        if (StrUtil.isBlank(actual.getSubject())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Meeting subject is required");
        }
        if (actual.getStartTime() == null || actual.getEndTime() == null || !actual.getEndTime().after(actual.getStartTime())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Meeting start and end time are required");
        }

        TencentMeetingCorpConfig config = requireConfig();
        TencentMeetingUserMapping mapping = oauthService.requireCurrentAuthorizedAccount(config);
        TencentMeetingOAuthCredential credential = tokenProvider.credential(config, mapping);
        JSONObject requestBody = buildCreateMeetingBody(config, mapping, actual);
        JSONObject rawMeeting = syncService.createMeeting(credential, requestBody);

        TencentMeeting meeting = new TencentMeeting();
        meeting.setAppId(config.getAppId());
        meeting.setMeetingId(firstText(rawMeeting, "meeting_id", "meetingId", "meeting_uuid"));
        meeting.setMeetingCode(firstText(rawMeeting, "meeting_code", "meetingCode"));
        meeting.setSubject(StrUtil.blankToDefault(firstText(rawMeeting, "subject", "meeting_subject", "title"), actual.getSubject()));
        meeting.setStatus("not_started");
        meeting.setCreatorUserId(mapping.getMeetingUserId());
        meeting.setCreatorName(resolveCurrentCreatorName(mapping));
        meeting.setCrmCreatorUserId(mapping.getCrmUserId());
        meeting.setStartTime(actual.getStartTime());
        meeting.setEndTime(actual.getEndTime());
        meeting.setDurationSeconds(Math.max(0L, (actual.getEndTime().getTime() - actual.getStartTime().getTime()) / 1000L));
        meeting.setParticipantCount(actual.getInviteeUserIds() == null ? 0 : actual.getInviteeUserIds().size());
        meeting.setParticipantNames(actual.getInviteeUserIds() == null ? null : String.join(",", actual.getInviteeUserIds()));
        meeting.setBindStatus("UNBOUND");
        meeting.setRawJson(rawMeeting.toJSONString());
        meeting.setSyncedAt(new Date());
        if (StrUtil.isBlank(meeting.getMeetingId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent Meeting create response has no meeting_id");
        }
        meetingMapper.insert(meeting);
        return toMeetingVO(meeting);
    }

    public List<TencentMeetingCustomerBindingVO> getCustomerBindings(Long customerId) {
        if (customerId == null) {
            return List.of();
        }
        return bindingMapper.selectList(Wrappers.<TencentMeetingCustomerBinding>lambdaQuery()
                        .eq(TencentMeetingCustomerBinding::getCustomerId, customerId)
                        .eq(TencentMeetingCustomerBinding::getStatus, 1)
                        .orderByDesc(TencentMeetingCustomerBinding::getBindTime))
                .stream()
                .map(this::toBindingVO)
                .toList();
    }

    private TencentMeeting requireMeetingAccess(Long id) {
        TencentMeeting meeting = meetingMapper.selectById(id);
        if (meeting == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent meeting not found");
        }
        DataPermissionContext context = dataPermissionService.createContext("tencentMeeting");
        if (context.isAllData()) {
            return meeting;
        }
        if (context.isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "No permission to access this meeting");
        }
        if (meeting.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(meeting.getCustomerId());
            if (customer != null && context.getUserIds().contains(customer.getOwnerId())) {
                return meeting;
            }
        }
        if (meeting.getCustomerId() == null && context.getUserIds().contains(meeting.getCrmCreatorUserId())) {
            return meeting;
        }
        throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "No permission to access this meeting");
    }

    private void applyMeetingDataPermission(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TencentMeeting> wrapper) {
        DataPermissionContext context = dataPermissionService.createContext("tencentMeeting");
        if (context.isAllData()) {
            return;
        }
        if (context.isEmpty()) {
            wrapper.eq(TencentMeeting::getId, -1L);
            return;
        }
        wrapper.and(nested -> nested
                .in(TencentMeeting::getCrmCreatorUserId, context.getUserIds())
                .or()
                .inSql(TencentMeeting::getCustomerId,
                        "SELECT customer_id FROM crm_customer WHERE owner_id IN ("
                                + context.getUserIds().stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("-1")
                                + ")"));
    }

    private TencentMeetingCorpConfig findConfig() {
        return configMapper.selectLatestOAuthConfigIgnoreTenant();
    }

    private void clearStaleAuthRequiredFailure(TencentMeetingSyncStatusVO status) {
        if (status == null || !isAuthRequiredError(status.getLastSyncError())) {
            return;
        }
        try {
            TencentMeetingOAuthStatusVO oauthStatus = oauthService.getStatus();
            if (!Boolean.TRUE.equals(oauthStatus.getAuthorized())) {
                return;
            }
        } catch (Exception ignored) {
            return;
        }
        status.setLastSyncStatus(null);
        status.setLastSyncError(null);
        status.setFailedCount(0);
    }

    private boolean isAuthRequiredError(String errorMessage) {
        return StrUtil.isNotBlank(errorMessage) && errorMessage.contains(AUTH_REQUIRED_ERROR);
    }

    private TencentMeetingSyncStatusVO markConfigSyncRunning(TencentMeetingCorpConfig config) {
        TencentMeetingSyncStatusVO status = new TencentMeetingSyncStatusVO();
        status.setAppId(config.getAppId());
        status.setLastSyncTime(new Date());
        status.setLastSyncStatus("running");
        status.setFetchedCount(0);
        status.setSavedCount(0);
        status.setFailedCount(0);
        config.setLastSyncTime(status.getLastSyncTime());
        config.setLastSyncStatus(status.getLastSyncStatus());
        config.setLastSyncError(null);
        configMapper.updateSyncStatusIgnoreTenant(config);
        return status;
    }

    private void runBackgroundSync(TencentMeetingCorpConfig config, Supplier<TencentMeetingSyncStatusVO> action) {
        try {
            TencentMeetingSyncStatusVO status = action.get();
            config.setLastSyncTime(status.getLastSyncTime());
            config.setLastSyncStatus(status.getLastSyncStatus());
            config.setLastSyncError(status.getLastSyncError());
            configMapper.updateSyncStatusIgnoreTenant(config);
        } catch (Exception e) {
            config.setLastSyncTime(new Date());
            config.setLastSyncStatus("failed");
            config.setLastSyncError(StrUtil.maxLength(e.getMessage(), 1000));
            configMapper.updateSyncStatusIgnoreTenant(config);
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(e);
        }
    }

    private TencentMeetingCorpConfig requireConfig() {
        TencentMeetingCorpConfig config = findConfig();
        if (config == null || StrUtil.isBlank(config.getAppId()) || StrUtil.isBlank(config.getSdkId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent Meeting config is not configured");
        }
        return config;
    }

    private JSONObject buildCreateMeetingBody(TencentMeetingCorpConfig config, TencentMeetingUserMapping mapping, TencentMeetingCreateBO createBO) {
        JSONObject body = new JSONObject();
        body.put("userid", mapping.getMeetingUserId());
        body.put("instanceid", 1);
        body.put("subject", createBO.getSubject().trim());
        body.put("type", 0);
        body.put("start_time", String.valueOf(createBO.getStartTime().getTime() / 1000L));
        body.put("end_time", String.valueOf(createBO.getEndTime().getTime() / 1000L));
        if (StrUtil.isNotBlank(createBO.getPassword())) {
            body.put("password", createBO.getPassword().trim());
        }
        JSONArray hosts = new JSONArray();
        JSONObject host = new JSONObject();
        host.put("userid", mapping.getMeetingUserId());
        hosts.add(host);
        body.put("hosts", hosts);
        JSONArray invitees = new JSONArray();
        if (createBO.getInviteeUserIds() != null) {
            for (String inviteeUserId : createBO.getInviteeUserIds()) {
                if (StrUtil.isBlank(inviteeUserId)) {
                    continue;
                }
                JSONObject invitee = new JSONObject();
                invitee.put("userid", inviteeUserId.trim());
                invitees.add(invitee);
            }
        }
        if (!invitees.isEmpty()) {
            body.put("invitees", invitees);
        }
        if (StrUtil.isNotBlank(config.getSdkId())) {
            body.put("sdk_id", config.getSdkId());
        }
        return body;
    }

    private String resolveCurrentCreatorName(TencentMeetingUserMapping mapping) {
        if (mapping != null && StrUtil.isNotBlank(mapping.getUserName()) && !mapping.getUserName().equals(mapping.getMeetingUserId())) {
            return mapping.getUserName().trim();
        }
        LoginUser loginUser = UserUtil.getLoginUser();
        ManagerUser user = loginUser == null ? null : loginUser.getUser();
        String displayName = user == null ? null : firstNotBlank(user.getRealname(), user.getUsername(), user.getMobile(), user.getEmail());
        return StrUtil.blankToDefault(displayName, mapping == null ? null : mapping.getMeetingUserId());
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
        if (raw == null) {
            return null;
        }
        for (String key : keys) {
            String value = raw.getString(key);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private TencentMeetingVO toMeetingVO(TencentMeeting meeting) {
        TencentMeetingVO vo = new TencentMeetingVO();
        BeanUtil.copyProperties(meeting, vo);
        JSONObject raw = parseRawJson(meeting.getRawJson());
        vo.setJoinUrl(firstText(raw, "join_url", "joinUrl", "meeting_url", "meetingUrl", "participant_join_url"));
        vo.setHostJoinUrl(firstText(raw, "host_join_url", "hostJoinUrl", "host_join_url_wx", "hostMeetingUrl"));
        return vo;
    }

    private JSONObject parseRawJson(String rawJson) {
        if (StrUtil.isBlank(rawJson)) {
            return null;
        }
        try {
            return JSONObject.parseObject(rawJson);
        } catch (Exception ignored) {
            return null;
        }
    }

    private TencentMeetingTranscriptSegmentVO toTranscriptSegmentVO(TencentMeetingTranscriptSegment segment) {
        TencentMeetingTranscriptSegmentVO vo = new TencentMeetingTranscriptSegmentVO();
        BeanUtil.copyProperties(segment, vo);
        return vo;
    }

    private TencentMeetingCustomerBindingVO toBindingVO(TencentMeetingCustomerBinding binding) {
        TencentMeetingCustomerBindingVO vo = new TencentMeetingCustomerBindingVO();
        BeanUtil.copyProperties(binding, vo);
        TencentMeeting meeting = meetingMapper.selectById(binding.getMeetingId());
        if (meeting != null) {
            vo.setMeetingExternalId(meeting.getMeetingId());
        }
        Customer customer = customerMapper.selectById(binding.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getCompanyName());
        }
        return vo;
    }

    private TencentMeetingCandidateVO toCandidateVO(TencentMeeting meeting, String keyword) {
        TencentMeetingCandidateVO vo = new TencentMeetingCandidateVO();
        BeanUtil.copyProperties(meeting, vo);
        int score = 50;
        String reason = "最近未关联会议";
        if (StrUtil.isNotBlank(keyword)) {
            if (StrUtil.containsIgnoreCase(StrUtil.blankToDefault(meeting.getSubject(), ""), keyword)) {
                score += 30;
                reason = "会议标题匹配";
            }
            if (StrUtil.containsIgnoreCase(StrUtil.blankToDefault(meeting.getParticipantNames(), ""), keyword)) {
                score += 20;
                reason = "参会人匹配";
            }
            if (StrUtil.containsIgnoreCase(StrUtil.blankToDefault(meeting.getTranscriptText(), ""), keyword)) {
                score += 20;
                reason = "转写内容匹配";
            }
        }
        vo.setScore(score);
        vo.setMatchReason(reason);
        return vo;
    }

}
