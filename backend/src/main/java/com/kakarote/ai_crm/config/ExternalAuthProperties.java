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

    private ProviderConfig wecom = new ProviderConfig();

    private String frontendRedirectUri;

    private Integer stateTtlSeconds = 600;

    private Integer ticketTtlSeconds = 300;

    private ProxyConfig proxy = new ProxyConfig();

    public ProviderConfig getProvider(String provider) {
        return switch (StrUtil.emptyToDefault(provider, "").toLowerCase()) {
            case "google" -> google;
            case "outlook" -> outlook;
            case "wechat" -> wechat;
            case "wecom" -> wecom;
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
        private String corpId;
        private String agentId;

        public boolean isUsable(String provider) {
            if (!Boolean.TRUE.equals(enabled)) {
                return false;
            }
            if ("wecom".equals(provider)) {
                return StrUtil.isNotBlank(corpId)
                        && StrUtil.isNotBlank(agentId)
                        && StrUtil.isNotBlank(clientSecret);
            }
            return StrUtil.isNotBlank(clientId) && StrUtil.isNotBlank(clientSecret);
        }

        public String resolveClientId(String provider) {
            if ("wecom".equals(provider) && StrUtil.isNotBlank(corpId)) {
                return corpId;
            }
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
