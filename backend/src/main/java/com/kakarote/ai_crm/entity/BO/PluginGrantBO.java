package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 为某插件客户端授予 scope 的入参（实际授予 = 申请 ∩ 当前管理员所持权限）。
 */
@Data
@Schema(name = "PluginGrantBO", description = "Grant module:action scopes to a plugin client")
public class PluginGrantBO {

    private String clientId;

    @Schema(description = "申请的 scope（module:action）")
    private List<String> requestedScopes;
}
