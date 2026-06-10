package com.kakarote.ai_crm.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WecomApiClientTest {

    @Test
    void convertExternalUserIdsShouldParseNewExternalUseridItems() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        WecomApiClient client = new WecomApiClient(restTemplate);
        when(restTemplate.postForObject(any(String.class), any(), eq(String.class))).thenReturn("""
                {
                  "errcode": 0,
                  "errmsg": "ok",
                  "items": [
                    {
                      "external_userid": "wmlXttDgAASYaA3Igja3fcKOskcINyqA",
                      "new_external_userid": "wmlXttDgAArK-tlqW8m9PBFtLGqObPJg"
                    },
                    {
                      "external_userid": "wmlXttDgAArK-tlqW8m9PBFtLGqObPJg",
                      "new_external_userid": "wmlXttDgAArK-tlqW8m9PBFtLGqObPJg"
                    }
                  ]
                }
                """);

        Map<String, String> result = client.convertExternalUserIds("token-1", List.of(
                "wmlXttDgAASYaA3Igja3fcKOskcINyqA",
                "wmlXttDgAArK-tlqW8m9PBFtLGqObPJg"
        ));

        assertThat(result).containsEntry(
                "wmlXttDgAASYaA3Igja3fcKOskcINyqA",
                "wmlXttDgAArK-tlqW8m9PBFtLGqObPJg");
        assertThat(result).containsEntry(
                "wmlXttDgAArK-tlqW8m9PBFtLGqObPJg",
                "wmlXttDgAArK-tlqW8m9PBFtLGqObPJg");
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForObject(urlCaptor.capture(), any(), eq(String.class));
        assertThat(urlCaptor.getValue()).contains("/externalcontact/get_new_external_userid");
        assertThat(urlCaptor.getValue()).contains("access_token=token-1");
    }
}
