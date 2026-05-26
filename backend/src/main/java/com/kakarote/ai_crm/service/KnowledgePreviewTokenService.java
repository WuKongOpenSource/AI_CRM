package com.kakarote.ai_crm.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class KnowledgePreviewTokenService {

    private static final String KEY_PREFIX = "knowledge:preview_token:";
    private static final long DEFAULT_TTL_SECONDS = 10 * 60L;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public CreatedToken createToken(Long knowledgeId, Long tenantId, Long userId) {
        String token = IdUtil.fastSimpleUUID();
        long expiresAtEpochMillis = Instant.now().plusSeconds(DEFAULT_TTL_SECONDS).toEpochMilli();

        Payload payload = new Payload();
        payload.setKnowledgeId(knowledgeId);
        payload.setTenantId(tenantId);
        payload.setUserId(userId);
        payload.setExpiresAtEpochMillis(expiresAtEpochMillis);

        redisTemplate.opsForValue().set(
                buildKey(token),
                JSON.toJSONString(payload),
                DEFAULT_TTL_SECONDS,
                TimeUnit.SECONDS
        );

        return new CreatedToken(token, expiresAtEpochMillis, DEFAULT_TTL_SECONDS);
    }

    public Optional<Payload> validateToken(String token, Long knowledgeId) {
        if (StrUtil.isBlank(token) || knowledgeId == null) {
            return Optional.empty();
        }

        String tokenKey = buildKey(token);
        String rawPayload = redisTemplate.opsForValue().get(tokenKey);
        if (StrUtil.isBlank(rawPayload)) {
            return Optional.empty();
        }

        Payload payload;
        try {
            payload = JSON.parseObject(rawPayload, Payload.class);
        } catch (Exception ignored) {
            redisTemplate.delete(tokenKey);
            return Optional.empty();
        }

        if (payload == null
                || payload.getKnowledgeId() == null
                || !payload.getKnowledgeId().equals(knowledgeId)
                || payload.getTenantId() == null) {
            return Optional.empty();
        }

        Long expiresAt = payload.getExpiresAtEpochMillis();
        if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
            redisTemplate.delete(tokenKey);
            return Optional.empty();
        }

        return Optional.of(payload);
    }

    private String buildKey(String token) {
        return KEY_PREFIX + token;
    }

    public record CreatedToken(String token, long expiresAtEpochMillis, long expiresInSeconds) {
    }

    @Data
    public static class Payload {
        private Long knowledgeId;
        private Long tenantId;
        private Long userId;
        private Long expiresAtEpochMillis;
    }
}
