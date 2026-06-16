package com.kakarote.ai_crm.config;

import cn.hutool.core.util.StrUtil;
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

    private ProxyConfig proxy = new ProxyConfig();

    @Data
    public static class OAuthProvider {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String tenant = "common";
    }

    @Data
    public static class ProxyConfig {
        private Boolean enabled = Boolean.FALSE;
        private String url;

        public boolean isUsable() {
            return Boolean.TRUE.equals(enabled) && StrUtil.isNotBlank(url);
        }
    }
}
