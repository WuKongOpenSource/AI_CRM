package com.kakarote.syncdata.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.incremental.IncrementalSyncEvent;
import com.kakarote.syncdata.incremental.IncrementalSyncService;
import com.kakarote.syncdata.incremental.SyncDirection;
import com.kakarote.syncdata.incremental.SyncOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "sync.incremental.mq", name = "enabled", havingValue = "true")
public class MqIncrementalSyncConsumer {

    private static final Logger log = LoggerFactory.getLogger(MqIncrementalSyncConsumer.class);

    private final IncrementalSyncService incrementalSyncService;
    private final ObjectMapper objectMapper;
    private final SyncProperties properties;

    /**
     * 初始化 MQ 预留消费者，并打印当前配置的 topic 和消费组。
     */
    public MqIncrementalSyncConsumer(IncrementalSyncService incrementalSyncService,
                                     ObjectMapper objectMapper,
                                     SyncProperties properties) {
        this.incrementalSyncService = incrementalSyncService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        log.info("Incremental MQ placeholder enabled. topic={}, group={}",
                properties.getIncremental().getMq().getTopic(),
                properties.getIncremental().getMq().getConsumerGroup());
    }

    /**
     * 将一条 MQ 消息解析为增量事件，并交给统一增量处理器。
     */
    public void onMessage(String messageBody, String offset) {
        try {
            Map<String, Object> payload = objectMapper.readValue(messageBody, new TypeReference<>() {
            });
            IncrementalSyncEvent event = new IncrementalSyncEvent(
                    stringValue(payload.getOrDefault("direction", SyncDirection.CRM_TO_AICRM.name())),
                    stringValue(payload.get("type")),
                    stringValue(payload.get("eventId")),
                    stringValue(payload.get("traceId")),
                    stringValue(payload.getOrDefault("originSystem", "crm")),
                    stringValue(payload.getOrDefault("sourceSystem", "wk_crm")),
                    stringValue(payload.getOrDefault("targetSystem", "aicrm")),
                    longValue(payload.get("tenantId")),
                    longValue(payload.get("sourceCompanyId")),
                    stringValue(payload.get("entityType")),
                    stringValue(payload.get("sourceTable")),
                    stringValue(payload.get("sourceId")),
                    stringValue(payload.get("targetId")),
                    stringValue(payload.getOrDefault("operation", SyncOperation.UPDATE.name())),
                    offset,
                    Instant.now(),
                    mapValue(payload.get("payload"), payload),
                    messageBody,
                    stringValue(payload.getOrDefault("schemaVersion", "1"))
            );
            incrementalSyncService.handleEvent(event);
        } catch (Exception ex) {
            log.warn("Failed to consume incremental MQ message: {}", ex.getMessage());
        }
    }

    /**
     * 将可为空的消息字段转换为字符串。
     */
    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value, Map<String, Object> fallback) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : fallback;
    }

    /**
     * 将可为空的数字或文本消息字段转换为 Long。
     */
    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
