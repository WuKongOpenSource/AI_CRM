package com.kakarote.ai_crm.plugin;

import java.util.Map;

/**
 * 领域事件（P1-c）：业务 service 经 ApplicationEventPublisher 发布,
 * 由 PluginWebhookDispatcher 在事务提交后投递给订阅插件。
 */
public class PluginDomainEvent {

    private final String eventType;
    private final Map<String, Object> payload;

    public PluginDomainEvent(String eventType, Map<String, Object> payload) {
        this.eventType = eventType;
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
