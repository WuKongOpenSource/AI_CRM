package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class TencentMeetingApiClient implements TencentMeetingApiGateway {

    private static final String BASE_URL = "https://api.meeting.qq.com";

    @Autowired
    private SecretTextCipher secretTextCipher;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<JSONObject> listEndedMeetings(TencentMeetingCorpConfig config, TencentMeetingUserMapping mapping, int syncDays) {
        long end = Instant.now().getEpochSecond();
        long start = Math.max(0L, end - Math.max(syncDays, 1) * 86400L);
        String path = UriComponentsBuilder.fromPath("/v1/history/meetings")
                .queryParam("userid", mapping.getMeetingUserId())
                .queryParam("instanceid", 1)
                .queryParam("start_time", start)
                .queryParam("end_time", end)
                .build()
                .toUriString();
        JSONObject response = get(config, path, false);
        return firstArray(response, "meetings", "meeting_list", "meeting_info_list");
    }

    @Override
    public List<JSONObject> getMeetingParticipants(TencentMeetingCorpConfig config, String meetingId, String operatorUserId) {
        if (StrUtil.isBlank(meetingId) || StrUtil.isBlank(operatorUserId)) {
            return List.of();
        }
        String path = UriComponentsBuilder.fromPath("/v1/meetings/" + meetingId + "/participants")
                .queryParam("operator_id", operatorUserId)
                .queryParam("operator_id_type", 1)
                .queryParam("instanceid", 1)
                .queryParam("page", 1)
                .queryParam("page_size", 50)
                .build()
                .toUriString();
        JSONObject response = get(config, path, false);
        return firstArray(response, "participants", "participant_list", "users");
    }

    @Override
    public List<JSONObject> listRecordings(TencentMeetingCorpConfig config, String operatorUserId) {
        if (StrUtil.isBlank(operatorUserId)) {
            return List.of();
        }
        String path = UriComponentsBuilder.fromPath("/v1/records")
                .queryParam("operator_id", operatorUserId)
                .queryParam("operator_id_type", 1)
                .queryParam("page", 1)
                .queryParam("page_size", 50)
                .build()
                .toUriString();
        JSONObject response = get(config, path, false);
        return firstArray(response, "record_meetings", "record_files", "records", "meeting_recordings");
    }

    @Override
    public List<JSONObject> getTranscriptSegments(TencentMeetingCorpConfig config, String meetingId, String recordFileId, String operatorUserId) {
        if (StrUtil.isBlank(recordFileId) || StrUtil.isBlank(operatorUserId)) {
            return List.of();
        }
        String path = UriComponentsBuilder.fromPath("/v1/records/transcripts/details")
                .queryParam("meeting_id", meetingId)
                .queryParam("record_file_id", recordFileId)
                .queryParam("operator_id", operatorUserId)
                .queryParam("operator_id_type", 1)
                .queryParam("limit", 200)
                .build()
                .toUriString();
        JSONObject response = get(config, path, true);
        return firstArray(response, "transcripts", "transcript_segments", "paragraphs");
    }

    private JSONObject get(TencentMeetingCorpConfig config, String pathWithQuery, boolean sensitive) {
        String secretId = decryptRequired(config.getSecretIdEncrypted(), "Tencent Meeting SecretId is not configured");
        String secretKey = decryptRequired(config.getSecretKeyEncrypted(), "Tencent Meeting SecretKey is not configured");
        String nonce = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-TC-Key", secretId);
        headers.set("X-TC-Timestamp", timestamp);
        headers.set("X-TC-Nonce", nonce);
        headers.set("X-TC-Signature", TencentMeetingSigner.sign(secretId, secretKey, "GET", nonce, timestamp, pathWithQuery, ""));
        headers.set("AppId", StrUtil.blankToDefault(config.getAppId(), ""));
        if (StrUtil.isNotBlank(config.getSdkId())) {
            headers.set("SdkId", config.getSdkId());
        }
        headers.set("X-TC-Registered", "1");
        if (sensitive && hasValidStsToken(config)) {
            headers.set("STS-Token", secretTextCipher.decrypt(config.getStsTokenEncrypted()));
        }

        URI uri = URI.create(BASE_URL + pathWithQuery);
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return StrUtil.isBlank(response.getBody()) ? new JSONObject() : JSONObject.parseObject(response.getBody());
    }

    private boolean hasValidStsToken(TencentMeetingCorpConfig config) {
        return StrUtil.isNotBlank(config.getStsTokenEncrypted())
                && (config.getStsTokenExpireTime() == null || config.getStsTokenExpireTime().after(new Date()));
    }

    private String decryptRequired(String encrypted, String message) {
        if (StrUtil.isBlank(encrypted)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, message);
        }
        return secretTextCipher.decrypt(encrypted);
    }

    private List<JSONObject> firstArray(JSONObject root, String... keys) {
        for (String key : keys) {
            JSONArray array = root.getJSONArray(key);
            if (array != null) {
                List<JSONObject> result = new ArrayList<>();
                for (int i = 0; i < array.size(); i++) {
                    Object item = array.get(i);
                    if (item instanceof JSONObject object) {
                        result.add(object);
                    }
                }
                return result;
            }
        }
        return List.of();
    }
}
