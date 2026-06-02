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
import com.kakarote.ai_crm.entity.BO.TencentMeetingBindBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingCandidateQueryBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingConfigSaveBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingCreateBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingQueryBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingSyncRunBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingUnbindBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.TencentMeeting;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCustomerBinding;
import com.kakarote.ai_crm.entity.PO.TencentMeetingParticipant;
import com.kakarote.ai_crm.entity.PO.TencentMeetingRecording;
import com.kakarote.ai_crm.entity.PO.TencentMeetingSyncLog;
import com.kakarote.ai_crm.entity.PO.TencentMeetingTranscriptSegment;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.entity.VO.TencentMeetingCandidateVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingConfigVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingCustomerBindingVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingDetailVO;
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
import com.kakarote.ai_crm.mapper.TencentMeetingUserMappingMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class TencentMeetingServiceImpl {

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
    private TencentMeetingUserMappingMapper userMappingMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Autowired
    private TencentMeetingSyncServiceImpl syncService;

    @Autowired
    private TencentMeetingCustomerBindingServiceImpl bindingService;

    @Autowired
    private DataPermissionService dataPermissionService;

    public TencentMeetingConfigVO getConfig() {
        return toConfigVO(findConfig());
    }

    @Transactional(rollbackFor = Exception.class)
    public TencentMeetingConfigVO saveConfig(TencentMeetingConfigSaveBO saveBO) {
        TencentMeetingCorpConfig config = findConfig();
        if (config == null) {
            config = new TencentMeetingCorpConfig();
        }
        config.setAppId(StrUtil.trim(saveBO.getAppId()));
        config.setSdkId(StrUtil.trimToNull(saveBO.getSdkId()));
        config.setCorpName(StrUtil.trimToNull(saveBO.getCorpName()));
        config.setOperatorUserId(StrUtil.trimToNull(saveBO.getOperatorUserId()));
        config.setSyncEnabled(Boolean.TRUE.equals(saveBO.getSyncEnabled()));
        config.setTranscriptEnabled(Boolean.TRUE.equals(saveBO.getTranscriptEnabled()));
        config.setArchiveToKnowledge(Boolean.TRUE.equals(saveBO.getArchiveToKnowledge()));
        setEncryptedIfPresent(saveBO.getSecretId(), config::setSecretIdEncrypted);
        setEncryptedIfPresent(saveBO.getSecretKey(), config::setSecretKeyEncrypted);
        setEncryptedIfPresent(saveBO.getWebhookSecret(), config::setWebhookSecretEncrypted);
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        return toConfigVO(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public TencentMeetingSyncStatusVO runSync(TencentMeetingSyncRunBO runBO) {
        TencentMeetingCorpConfig config = requireConfig();
        TencentMeetingSyncStatusVO status = syncService.runSync(config, runBO);
        config.setLastSyncTime(status.getLastSyncTime());
        config.setLastSyncStatus(status.getLastSyncStatus());
        config.setLastSyncError(status.getLastSyncError());
        configMapper.updateById(config);
        return status;
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
        syncService.refreshMeetingByExternalId("manual.refresh", meeting.getMeetingId());
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
        TencentMeetingUserMapping mapping = resolveCreatorMapping(config, actual.getCreatorUserId());
        JSONObject requestBody = buildCreateMeetingBody(config, mapping, actual);
        JSONObject rawMeeting = syncService.createMeeting(config, requestBody);

        TencentMeeting meeting = new TencentMeeting();
        meeting.setAppId(config.getAppId());
        meeting.setMeetingId(firstText(rawMeeting, "meeting_id", "meetingId", "meeting_uuid"));
        meeting.setMeetingCode(firstText(rawMeeting, "meeting_code", "meetingCode"));
        meeting.setSubject(StrUtil.blankToDefault(firstText(rawMeeting, "subject", "meeting_subject", "title"), actual.getSubject()));
        meeting.setStatus("not_started");
        meeting.setCreatorUserId(mapping.getMeetingUserId());
        meeting.setCreatorName(StrUtil.blankToDefault(mapping.getUserName(), mapping.getMeetingUserId()));
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
        return configMapper.selectOne(Wrappers.<TencentMeetingCorpConfig>lambdaQuery()
                .orderByDesc(TencentMeetingCorpConfig::getUpdateTime)
                .last("LIMIT 1"));
    }

    private TencentMeetingCorpConfig requireConfig() {
        TencentMeetingCorpConfig config = findConfig();
        if (config == null || StrUtil.isBlank(config.getAppId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent Meeting config is not configured");
        }
        return config;
    }

    private TencentMeetingUserMapping resolveCreatorMapping(TencentMeetingCorpConfig config, String requestedCreatorUserId) {
        String meetingUserId = StrUtil.blankToDefault(requestedCreatorUserId, config.getOperatorUserId());
        TencentMeetingUserMapping mapping = null;
        if (StrUtil.isNotBlank(meetingUserId)) {
            mapping = userMappingMapper.selectOne(Wrappers.<TencentMeetingUserMapping>lambdaQuery()
                    .eq(TencentMeetingUserMapping::getAppId, config.getAppId())
                    .eq(TencentMeetingUserMapping::getMeetingUserId, meetingUserId)
                    .eq(TencentMeetingUserMapping::getStatus, 1)
                    .last("LIMIT 1"));
        }
        if (mapping == null) {
            mapping = new TencentMeetingUserMapping();
            mapping.setAppId(config.getAppId());
            mapping.setMeetingUserId(meetingUserId);
            mapping.setUserName(meetingUserId);
            mapping.setCrmUserId(UserUtil.getUserIdOrNull());
            mapping.setStatus(1);
        }
        if (StrUtil.isBlank(mapping.getMeetingUserId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent Meeting operator user is not configured");
        }
        if (mapping.getCrmUserId() == null) {
            mapping.setCrmUserId(UserUtil.getUserIdOrNull());
        }
        return mapping;
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

    private TencentMeetingConfigVO toConfigVO(TencentMeetingCorpConfig config) {
        TencentMeetingConfigVO vo = new TencentMeetingConfigVO();
        if (config == null) {
            return vo;
        }
        BeanUtil.copyProperties(config, vo);
        vo.setSecretIdMasked(maskEncrypted(config.getSecretIdEncrypted()));
        return vo;
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

    private String maskEncrypted(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            return null;
        }
        try {
            String plain = secretTextCipher.decrypt(encrypted);
            if (plain.length() <= 6) {
                return "******";
            }
            return plain.substring(0, 3) + "****" + plain.substring(plain.length() - 3);
        } catch (Exception e) {
            return "******";
        }
    }

    private void setEncryptedIfPresent(String raw, java.util.function.Consumer<String> setter) {
        if (StrUtil.isNotBlank(raw)) {
            setter.accept(secretTextCipher.encrypt(raw));
        }
    }
}
