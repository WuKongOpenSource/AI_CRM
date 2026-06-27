package com.kakarote.ai_crm.config.security.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTokenLookupTest {

    @Test
    void blankTokenReturnsNull() {
        TokenService service = new TokenService();
        assertThat(service.getLoginUser((String) null)).isNull();
        assertThat(service.getLoginUser("")).isNull();
    }

    @Test
    void garbageTokenReturnsNullNotThrow() {
        TokenService service = new TokenService();
        // No secret/redis configured → parseToken throws internally → caught → null.
        assertThat(service.getLoginUser("not-a-jwt")).isNull();
    }
}
