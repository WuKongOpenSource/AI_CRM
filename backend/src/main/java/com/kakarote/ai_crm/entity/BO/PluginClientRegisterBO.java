package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 注册插件 OAuth 客户端的入参。
 */
@Data
@Schema(name = "PluginClientRegisterBO", description = "Register a plugin OAuth client")
public class PluginClientRegisterBO {

    @Schema(description = "插件标识，如 acme.invoice-sync")
    private String pluginId;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "授权类型，缺省 [client_credentials]")
    private List<String> grantTypes;

    @Schema(description = "默认申请的 scope（module:action），如 [customer:view, task:create]")
    private List<String> defaultScopes;

    @Schema(description = "authorization_code 回调地址（可选）")
    private List<String> redirectUris;
}
