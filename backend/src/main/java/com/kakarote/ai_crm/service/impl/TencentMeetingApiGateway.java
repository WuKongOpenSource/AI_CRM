package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;

import java.util.List;

public interface TencentMeetingApiGateway {

    List<JSONObject> listEndedMeetings(TencentMeetingCorpConfig config, TencentMeetingUserMapping mapping, int syncDays);

    List<JSONObject> getMeetingParticipants(TencentMeetingCorpConfig config, String meetingId, String operatorUserId);

    List<JSONObject> listRecordings(TencentMeetingCorpConfig config, String operatorUserId);

    List<JSONObject> getTranscriptSegments(TencentMeetingCorpConfig config, String meetingId, String recordFileId, String operatorUserId);
}
