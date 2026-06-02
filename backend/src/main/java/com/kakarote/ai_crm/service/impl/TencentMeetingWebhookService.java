package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingWebhookEvent;
import com.kakarote.ai_crm.mapper.TencentMeetingCorpConfigMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingWebhookEventMapper;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class TencentMeetingWebhookService {

    @Autowired
    private TencentMeetingWebhookEventMapper eventMapper;

    @Autowired
    private TencentMeetingSyncServiceImpl syncService;

    @Autowired
    private TencentMeetingCorpConfigMapper configMapper;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Transactional(rollbackFor = Exception.class)
    public boolean handleWebhook(String body) {
        return handleWebhook(body, null, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handleWebhook(String body, String timestamp, String nonce, String signature) {
        if (StrUtil.isBlank(body)) {
            return false;
        }
        TencentMeetingCorpConfig matchedConfig = resolveWebhookConfig(body, timestamp, nonce, signature);
        Long previousTenantId = TenantContextHolder.getTenantId();
        if (matchedConfig != null && matchedConfig.getTenantId() != null) {
            TenantContextHolder.setTenantId(matchedConfig.getTenantId());
        }
        try {
            return doHandleWebhook(body);
        } finally {
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            } else {
                TenantContextHolder.clear();
            }
        }
    }

    public String verifyWebhookUrl(String checkStr, String timestamp, String nonce, String signature) {
        String decodedCheckStr = URLDecoder.decode(StrUtil.nullToEmpty(checkStr), StandardCharsets.UTF_8);
        resolveWebhookConfig(decodedCheckStr, timestamp, nonce, signature);
        try {
            return new String(Base64.getDecoder().decode(decodedCheckStr), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return decodedCheckStr;
        }
    }

    private boolean doHandleWebhook(String body) {
        JSONObject root = JSONObject.parseObject(body);
        String traceId = root.getString("trace_id");
        String eventName = root.getString("event");
        if (StrUtil.isNotBlank(traceId)) {
            TencentMeetingWebhookEvent existing = eventMapper.selectOne(
                    Wrappers.<TencentMeetingWebhookEvent>lambdaQuery()
                            .eq(TencentMeetingWebhookEvent::getTraceId, traceId)
                            .last("LIMIT 1"));
            if (existing != null) {
                return false;
            }
        }

        TencentMeetingWebhookEvent event = new TencentMeetingWebhookEvent();
        event.setEventName(eventName);
        event.setTraceId(traceId);
        event.setRawJson(body);
        event.setProcessStatus("processing");
        eventMapper.insert(event);

        try {
            String meetingId = extractMeetingId(root);
            if (StrUtil.isNotBlank(meetingId) && isRefreshEvent(eventName)) {
                syncService.refreshMeetingByExternalId(eventName, meetingId);
            }
            event.setProcessStatus("success");
            eventMapper.updateById(event);
            return true;
        } catch (Exception e) {
            event.setProcessStatus("failed");
            event.setProcessError(e.getMessage());
            eventMapper.updateById(event);
            log.error("Tencent Meeting webhook failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private TencentMeetingCorpConfig resolveWebhookConfig(String data, String timestamp, String nonce, String signature) {
        List<TencentMeetingCorpConfig> configs = loadWebhookConfigs();
        if (configs.isEmpty()) {
            return null;
        }
        if (StrUtil.hasBlank(timestamp, nonce, signature)) {
            throw new IllegalArgumentException("Tencent Meeting webhook signature headers are required");
        }

        String signaturePayload = extractSignaturePayload(data);
        for (TencentMeetingCorpConfig config : configs) {
            String token = decryptWebhookToken(config);
            if (StrUtil.isBlank(token)) {
                continue;
            }
            String expected = calculateSignature(token, timestamp, nonce, signaturePayload);
            if (expected.equalsIgnoreCase(signature)) {
                return config;
            }
        }
        throw new IllegalArgumentException("Tencent Meeting webhook signature verification failed");
    }

    private List<TencentMeetingCorpConfig> loadWebhookConfigs() {
        if (configMapper == null) {
            return Collections.emptyList();
        }
        List<TencentMeetingCorpConfig> configs = configMapper.selectWebhookConfigsIgnoreTenant();
        if (configs == null) {
            return Collections.emptyList();
        }
        return configs;
    }

    private String decryptWebhookToken(TencentMeetingCorpConfig config) {
        if (config == null || StrUtil.isBlank(config.getWebhookSecretEncrypted()) || secretTextCipher == null) {
            return null;
        }
        return secretTextCipher.decrypt(config.getWebhookSecretEncrypted());
    }

    private String extractSignaturePayload(String data) {
        if (StrUtil.isBlank(data)) {
            return "";
        }
        try {
            JSONObject root = JSONObject.parseObject(data);
            String encryptedData = root.getString("data");
            if (StrUtil.isNotBlank(encryptedData)) {
                return encryptedData;
            }
        } catch (Exception ignored) {
            // Plain callback validation payloads are signed directly.
        }
        return data;
    }

    static String calculateSignature(String token, String timestamp, String nonce, String data) {
        List<String> parts = new ArrayList<>();
        parts.add(StrUtil.nullToEmpty(token));
        parts.add(StrUtil.nullToEmpty(timestamp));
        parts.add(StrUtil.nullToEmpty(nonce));
        parts.add(StrUtil.nullToEmpty(data));
        parts.sort(Comparator.naturalOrder());
        StringBuilder raw = new StringBuilder();
        for (String part : parts) {
            raw.append(part);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(raw.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate Tencent Meeting webhook signature", e);
        }
    }

    private boolean isRefreshEvent(String eventName) {
        return "meeting.end".equals(eventName)
                || "recording.completed".equals(eventName)
                || "smart.transcripts".equals(eventName);
    }

    private String extractMeetingId(JSONObject root) {
        JSONArray payload = root.getJSONArray("payload");
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        JSONObject first = payload.getJSONObject(0);
        if (first == null) {
            return null;
        }
        JSONObject meetingInfo = first.getJSONObject("meeting_info");
        if (meetingInfo != null) {
            return meetingInfo.getString("meeting_id");
        }
        return first.getString("meeting_id");
    }
}
