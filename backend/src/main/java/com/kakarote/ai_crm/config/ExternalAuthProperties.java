package com.kakarote.ai_crm.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external-auth")
public class ExternalAuthProperties {

    private ProviderConfig google = new ProviderConfig();

    private ProviderConfig outlook = new ProviderConfig();

    private ProviderConfig wechat = new ProviderConfig();

    private String frontendRedirectUri;

    private Integer stateTtlSeconds = 600;

    private Integer ticketTtlSeconds = 300;

    private ProxyConfig proxy = new ProxyConfig();

    public ProviderConfig getProvider(String provider) {
        return switch (StrUtil.emptyToDefault(provider, "").toLowerCase()) {
            case "google" -> google;
            case "outlook" -> outlook;
            case "wechat" -> wechat;
            default -> null;
        };
    }

    @Data
    public static class ProviderConfig {
        private Boolean enabled = Boolean.FALSE;
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String tenant = "common";

        public boolean isUsable(String provider) {
            if (!Boolean.TRUE.equals(enabled)) {
                return false;
            }
            return StrUtil.isNotBlank(clientId) && StrUtil.isNotBlank(clientSecret);
        }

        public boolean isUsable() {
            return isUsable(null);
        }

        public String resolveClientId(String provider) {
            return clientId;
        }
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
