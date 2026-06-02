package com.kakarote.ai_crm.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalAuthPropertiesTest {

    @Test
    void resolvesProviderConfigByCode() {
        ExternalAuthProperties properties = new ExternalAuthProperties();

        assertThat(properties.getProvider("google")).isSameAs(properties.getGoogle());
        assertThat(properties.getProvider("outlook")).isSameAs(properties.getOutlook());
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

        ExternalAuthProperties.ProviderConfig outlook = new ExternalAuthProperties.ProviderConfig();
        outlook.setEnabled(Boolean.TRUE);
        outlook.setClientId("outlook-client");
        outlook.setClientSecret("outlook-secret");

        ExternalAuthProperties.ProviderConfig wecom = new ExternalAuthProperties.ProviderConfig();
        wecom.setEnabled(Boolean.TRUE);
        wecom.setClientId("ignored-client");
        wecom.setClientSecret("wecom-secret");

        assertThat(google.isUsable("google")).isTrue();
        assertThat(outlook.isUsable("outlook")).isTrue();
        assertThat(outlook.getTenant()).isEqualTo("common");
        assertThat(wecom.isUsable("wecom")).isFalse();

        wecom.setCorpId("corp-id");
        assertThat(wecom.isUsable("wecom")).isFalse();

        wecom.setAgentId("1000002");
        assertThat(wecom.isUsable("wecom")).isTrue();
        assertThat(wecom.resolveClientId("wecom")).isEqualTo("corp-id");
    }

    @Test
    void proxyUsabilityRequiresEnabledUrl() {
        ExternalAuthProperties.ProxyConfig proxy = new ExternalAuthProperties.ProxyConfig();

        assertThat(proxy.isUsable()).isFalse();

        proxy.setEnabled(Boolean.TRUE);
        assertThat(proxy.isUsable()).isFalse();

        proxy.setUrl("http://192.168.1.116:7890");
        assertThat(proxy.isUsable()).isTrue();
    }
}
