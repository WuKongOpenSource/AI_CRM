package com.kakarote.ai_crm.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业微信「代开发模板」回调配置。
 *
 * <p>与第三方应用回调（{@link WecomOpenPlatformProperties}）相互独立、各用一套密钥。
 * 代开发模板在企微服务商后台配置「回调URL + Token + EncodingAESKey」，此处必须填同一对，
 * 企微对回调URL做有效性验证（GET echostr）时才能验签/解密成功。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "wecom.dev-callback")
public class WecomDevCallbackProperties {

    /** 代开发模板回调 Token（与企微服务商后台保持一致，字母数字、≤32位） */
    private String token;

    /** 代开发模板回调 EncodingAESKey（与企微服务商后台保持一致，字母数字、固定43位） */
    private String encodingAesKey;

    public boolean isUsable() {
        return StrUtil.isNotBlank(token) && StrUtil.isNotBlank(encodingAesKey);
    }
}
