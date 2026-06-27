package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 注册成功返回；client_secret 仅在此返回一次。
 */
@Data
@Schema(name = "PluginClientRegisteredVO", description = "Registered plugin OAuth client (secret shown once)")
public class PluginClientRegisteredVO {

    private String clientId;

    /** 明文 secret，仅返回这一次，之后只存哈希 */
    private String clientSecret;

    private String pluginId;

    private String displayName;
}
