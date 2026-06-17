package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.config.ExternalAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalAuthServiceImplTest {

    @Test
    void configureRestTemplateProxyUsesExternalAuthProxyConfig() {
        ExternalAuthServiceImpl service = new ExternalAuthServiceImpl();
        ExternalAuthProperties properties = new ExternalAuthProperties();
        properties.getProxy().setEnabled(true);
        properties.getProxy().setUrl("socks5://127.0.0.1:7890");
        ReflectionTestUtils.setField(service, "externalAuthProperties", properties);

        ReflectionTestUtils.invokeMethod(service, "configureRestTemplateProxy");

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        assertThat(restTemplate).isNotNull();
        assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
        Proxy proxy = (Proxy) ReflectionTestUtils.getField(restTemplate.getRequestFactory(), "proxy");
        assertThat(proxy).isNotNull();
        assertThat(proxy.type()).isEqualTo(Proxy.Type.SOCKS);
    }
}
