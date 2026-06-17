package com.kakarote.ai_crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgePreviewTokenServiceTest {

    @Test
    void createTokenReturnsTtlSeconds() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        KnowledgePreviewTokenService service = new KnowledgePreviewTokenService(redisTemplate, new ObjectMapper());

        KnowledgePreviewTokenService.PreviewToken token = service.createToken(10L, 20L);

        assertThat(token.expiresInSeconds()).isEqualTo(600L);
        verify(valueOperations).set(anyString(), anyString(), eq(Duration.ofMinutes(10)));
    }
}
