package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 插件客户端生命周期操作入参（轮换 secret / 启停 / 卸载）。
 */
@Data
@Schema(name = "PluginClientActionBO", description = "Plugin client lifecycle action")
public class PluginClientActionBO {

    private String clientId;

    /** 用于 enable 接口：true=启用 false=停用 */
    private Boolean enabled;
}
