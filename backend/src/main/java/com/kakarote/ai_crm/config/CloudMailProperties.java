package com.kakarote.ai_crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 云邮件发送配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cloud.mail")
public class CloudMailProperties {

    /**
     * 是否启用真实邮件发送。
     * 关闭时仅记录验证码日志，便于本地联调。
     */
    private boolean enabled = false;

    /**
     * 阿里云邮件推送地域。
     */
    private String regionId = "cn-hangzhou";

    /**
     * 阿里云 Access Key ID。
     */
    private String accessKeyId;

    /**
     * 阿里云 Access Key Secret。
     */
    private String accessKeySecret;

    /**
     * 发信地址。
     */
    private String accountName;

    /**
     * 发信别名。
     */
    private String fromAlias = "AI CRM";

    /**
     * 是否允许回复。
     */
    private boolean replyToAddress = false;

    /**
     * 判断是否存在Required配置。
     */
    public boolean hasRequiredConfig() {
        return enabled
                && hasText(accessKeyId)
                && hasText(accessKeySecret)
                && hasText(accountName);
    }

    /**
     * 判断是否存在文本。
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
