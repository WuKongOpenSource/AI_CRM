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
import org.springframework.web.client.RestClientResponseException;
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

    private final RestTemplate restTemplate;

    public TencentMeetingApiClient() {
        this(new RestTemplate());
    }

    TencentMeetingApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<JSONObject> listEndedMeetings(TencentMeetingCorpConfig config, TencentMeetingUserMapping mapping, int syncDays) {
        long end = Instant.now().getEpochSecond();
        long start = Math.max(0L, end - Math.max(syncDays, 1) * 86400L);
        String path = UriComponentsBuilder.fromPath("/v1/history/meetings/" + mapping.getMeetingUserId())
                .queryParam("page_size", 20)
                .queryParam("page", 1)
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
                .queryParam("userid", operatorUserId)
                .queryParam("size", 50)
                .build()
                .toUriString();
        JSONObject response = get(config, path, false);
        return firstArray(response, "participants", "participant_list", "users");
    }

    @Override
    public List<JSONObject> listRecordings(TencentMeetingCorpConfig config, String operatorUserId, int syncDays) {
        if (StrUtil.isBlank(operatorUserId)) {
            return List.of();
        }
        long end = Instant.now().getEpochSecond();
        long start = Math.max(0L, end - Math.max(syncDays, 1) * 86400L);
        String path = UriComponentsBuilder.fromPath("/v1/corp/records")
                .queryParam("start_time", start)
                .queryParam("end_time", end)
                .queryParam("page_size", 20)
                .queryParam("page", 1)
                .queryParam("operator_id", operatorUserId)
                .queryParam("operator_id_type", 1)
                .build()
                .toUriString();
        JSONObject response = get(config, path, false);
        return flattenRecordFiles(response);
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
        return flattenTranscriptSegments(response);
    }

    @Override
    public JSONObject createMeeting(TencentMeetingCorpConfig config, JSONObject requestBody) {
        JSONObject response = exchange(config, "/v1/meetings", HttpMethod.POST, requestBody == null ? new JSONObject() : requestBody, false);
        JSONArray meetings = response.getJSONArray("meeting_info_list");
        if (meetings != null && !meetings.isEmpty() && meetings.get(0) instanceof JSONObject meeting) {
            return meeting;
        }
        return response;
    }

    private JSONObject get(TencentMeetingCorpConfig config, String pathWithQuery, boolean sensitive) {
        return exchange(config, pathWithQuery, HttpMethod.GET, null, sensitive);
    }

    private JSONObject exchange(TencentMeetingCorpConfig config, String pathWithQuery, HttpMethod method, JSONObject body, boolean sensitive) {
        String secretId = decryptRequired(config.getSecretIdEncrypted(), "Tencent Meeting SecretId is not configured");
        String secretKey = decryptRequired(config.getSecretKeyEncrypted(), "Tencent Meeting SecretKey is not configured");
        String nonce = String.valueOf(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestBody = body == null ? "" : body.toJSONString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-TC-Key", secretId);
        headers.set("X-TC-Timestamp", timestamp);
        headers.set("X-TC-Nonce", nonce);
        headers.set("X-TC-Signature", TencentMeetingSigner.sign(secretId, secretKey, method.name(), nonce, timestamp, pathWithQuery, requestBody));
        headers.set("AppId", StrUtil.blankToDefault(config.getAppId(), ""));
        if (StrUtil.isNotBlank(config.getSdkId())) {
            headers.set("SdkId", config.getSdkId());
        }
        headers.set("X-TC-Registered", "1");
        if (sensitive && hasValidStsToken(config)) {
            headers.set("STS-Token", secretTextCipher.decrypt(config.getStsTokenEncrypted()));
        }

        URI uri = URI.create(BASE_URL + pathWithQuery);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, method, new HttpEntity<>(requestBody, headers), String.class);
            return StrUtil.isBlank(response.getBody()) ? new JSONObject() : JSONObject.parseObject(response.getBody());
        } catch (RestClientResponseException e) {
            String responseBody = StrUtil.blankToDefault(e.getResponseBodyAsString(), "no body");
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "Tencent Meeting API " + method.name() + " " + pathWithQuery + " failed: "
                            + e.getStatusCode() + " [" + responseBody + "]");
        }
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

    private List<JSONObject> flattenRecordFiles(JSONObject root) {
        List<JSONObject> result = new ArrayList<>();
        for (JSONObject recordMeeting : firstArray(root, "record_meetings", "meeting_recordings")) {
            JSONArray files = recordMeeting.getJSONArray("record_files");
            if (files == null || files.isEmpty()) {
                result.add(recordMeeting);
                continue;
            }
            for (int i = 0; i < files.size(); i++) {
                Object item = files.get(i);
                if (item instanceof JSONObject file) {
                    JSONObject enriched = new JSONObject();
                    enriched.putAll(file);
                    copyIfPresent(recordMeeting, enriched, "meeting_id");
                    copyIfPresent(recordMeeting, enriched, "meeting_code");
                    copyIfPresent(recordMeeting, enriched, "subject");
                    copyIfPresent(recordMeeting, enriched, "userid");
                    result.add(enriched);
                }
            }
        }
        return result;
    }

    private List<JSONObject> flattenTranscriptSegments(JSONObject root) {
        List<JSONObject> direct = firstArray(root, "transcripts", "transcript_segments", "paragraphs");
        if (!direct.isEmpty()) {
            return direct;
        }
        JSONObject minutes = root.getJSONObject("minutes");
        if (minutes == null) {
            return List.of();
        }
        List<JSONObject> result = new ArrayList<>();
        JSONArray paragraphs = minutes.getJSONArray("paragraphs");
        if (paragraphs == null) {
            return result;
        }
        for (int i = 0; i < paragraphs.size(); i++) {
            Object item = paragraphs.get(i);
            if (item instanceof JSONObject paragraph) {
                JSONObject segment = new JSONObject();
                segment.putAll(paragraph);
                segment.put("text", extractWords(paragraph));
                result.add(segment);
            }
        }
        return result;
    }

    private String extractWords(JSONObject paragraph) {
        JSONArray sentences = paragraph.getJSONArray("sentences");
        if (sentences == null) {
            return paragraph.getString("text");
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < sentences.size(); i++) {
            Object item = sentences.get(i);
            if (!(item instanceof JSONObject sentence)) {
                continue;
            }
            JSONArray words = sentence.getJSONArray("words");
            if (words == null) {
                text.append(StrUtil.nullToEmpty(sentence.getString("text")));
                continue;
            }
            for (int j = 0; j < words.size(); j++) {
                Object wordItem = words.get(j);
                if (wordItem instanceof JSONObject word) {
                    text.append(StrUtil.blankToDefault(word.getString("text"), word.getString("word")));
                }
            }
        }
        return text.toString();
    }

    private void copyIfPresent(JSONObject source, JSONObject target, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }
}
