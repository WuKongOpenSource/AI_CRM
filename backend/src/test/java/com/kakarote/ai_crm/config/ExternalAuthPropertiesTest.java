package com.kakarote.ai_crm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalAuthPropertiesTest {

    @Test
    void resolvesProviderConfigByCode() {
        ExternalAuthProperties properties = new ExternalAuthProperties();

        assertThat(properties.getProvider("google")).isSameAs(properties.getGoogle());
        assertThat(properties.getProvider("wechat")).isSameAs(properties.getWechat());
        assertThat(properties.getProvider("wecom")).isSameAs(properties.getWecom());
        assertThat(properties.getProvider("unknown")).isNull();
    }

    @Test
    void providerUsabilityRequiresExpectedCredentials() {
        ExternalAuthProperties.ProviderConfig google = new ExternalAuthProperties.ProviderConfig();
        google.setEnabled(Boolean.TRUE);
        google.setClientId("google-client");
        google.setClientSecret("google-secret");

        ExternalAuthProperties.ProviderConfig wecom = new ExternalAuthProperties.ProviderConfig();
        wecom.setEnabled(Boolean.TRUE);
        wecom.setClientId("ignored-client");
        wecom.setClientSecret("wecom-secret");

        assertThat(google.isUsable("google")).isTrue();
        assertThat(wecom.isUsable("wecom")).isFalse();

        wecom.setCorpId("corp-id");
        assertThat(wecom.isUsable("wecom")).isTrue();
        assertThat(wecom.resolveClientId("wecom")).isEqualTo("corp-id");
    }
}
