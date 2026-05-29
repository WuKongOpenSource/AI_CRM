package com.kakarote.ai_crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mail.integration")
public class MailIntegrationProperties {

    private boolean enabled = true;

    private long schedulerFixedDelayMillis = 600000;

    private int maxConcurrentSync = 10;

    private int defaultSyncDays = 90;

    private int defaultSyncLimit = 500;

    private String defaultBodySyncMode = "summary";

    private String defaultAttachmentSyncMode = "metadata";

    private long defaultMaxAutoAttachmentSize = 10 * 1024 * 1024L;

    private int defaultRetentionDays = 180;

    private OAuthProvider gmail = new OAuthProvider();

    private OAuthProvider outlook = new OAuthProvider();

    @Data
    public static class OAuthProvider {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
    }
}
