package com.kakarote.ai_crm.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置
 * 支持 MinIO 和 RustFS（S3 兼容协议）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /**
     * 是否启用 MinIO
     */
    private boolean enabled = false;

    /**
     * MinIO/RustFS 服务地址
     */
    private String endpoint;

    /**
     * Access Key
     */
    private String accessKey;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucket;

    /**
     * 是否使用预签名 URL 下载
     */
    private boolean usePresignedUrl = true;

    /**
     * 预签名 URL 有效期（秒）
     */
    private int presignedExpiry = 3600;

    @Bean
    @ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
