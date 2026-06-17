package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.config.MinioConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MinioFileStorageServiceTest {

    @Test
    void publicEndpointCanBeRelativeAndKeepsSignedObjectPathStable() {
        MinioConfig config = new MinioConfig();
        config.setEndpoint("http://minio:9000");
        config.setPublicEndpoint("/s3/");

        MinioFileStorageService service = new MinioFileStorageService();
        ReflectionTestUtils.setField(service, "minioConfig", config);

        String publicUrl = ReflectionTestUtils.invokeMethod(
                service,
                "toPublicUrl",
                "http://minio:9000/ai-crm/chat/test.png?X-Amz-Signature=abc"
        );

        assertThat(publicUrl).isEqualTo("/s3/ai-crm/chat/test.png?X-Amz-Signature=abc");
    }
}
