package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建插件事件订阅的入参。
 */
@Data
@Schema(name = "PluginSubscribeBO", description = "Create a plugin event subscription")
public class PluginSubscribeBO {

    private String clientId;

    /** 订阅的事件类型，如 customer.created */
    private String eventType;

    /** 接收 webhook 的端点 URL */
    private String endpointUrl;
}
