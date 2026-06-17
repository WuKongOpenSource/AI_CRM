package com.kakarote.ai_crm.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
public class KnowledgePreviewTokenService {

    private static final String KEY_PREFIX = "knowledge:preview:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public KnowledgePreviewTokenService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public PreviewToken createToken(Long knowledgeId, Long userId) {
        String token = IdUtil.fastSimpleUUID();
        Instant expiresAt = Instant.now().plus(DEFAULT_TTL);
        PreviewPayload payload = new PreviewPayload(knowledgeId, userId, expiresAt.toEpochMilli());
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + token,
                    objectMapper.writeValueAsString(payload),
                    DEFAULT_TTL
            );
        } catch (Exception e) {
            log.error("Failed to create knowledge preview token: knowledgeId={}", knowledgeId, e);
            throw new IllegalStateException("Failed to create preview token", e);
        }
        return new PreviewToken(token, expiresAt, DEFAULT_TTL.toSeconds());
    }

    public boolean validateToken(String token, Long knowledgeId) {
        if (StrUtil.isBlank(token) || knowledgeId == null) {
            return false;
        }
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
            if (StrUtil.isBlank(value)) {
                return false;
            }
            PreviewPayload payload = objectMapper.readValue(value, PreviewPayload.class);
            return Objects.equals(payload.knowledgeId(), knowledgeId)
                    && payload.expiresAtEpochMillis() >= System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("Failed to validate knowledge preview token: knowledgeId={}", knowledgeId, e);
            return false;
        }
    }

    public record PreviewToken(String token, Instant expiresAt, long expiresInSeconds) {
    }

    private record PreviewPayload(Long knowledgeId, Long userId, long expiresAtEpochMillis) {
    }
}
