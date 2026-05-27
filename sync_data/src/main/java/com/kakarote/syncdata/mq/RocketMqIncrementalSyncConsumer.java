package com.kakarote.syncdata.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.incremental.IncrementalSyncEvent;
import com.kakarote.syncdata.incremental.IncrementalSyncService;
import com.kakarote.syncdata.incremental.SyncDirection;
import com.kakarote.syncdata.incremental.SyncOperation;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "sync.rocketmq", name = "enabled", havingValue = "true")
public class RocketMqIncrementalSyncConsumer {

    private static final Logger log = LoggerFactory.getLogger(RocketMqIncrementalSyncConsumer.class);

    private final DefaultMQPushConsumer consumer;
    private final ObjectMapper objectMapper;
    private final IncrementalSyncService incrementalSyncService;
    private final String crmToAicrmTag;
    private final String aicrmToCrmTag;

    public RocketMqIncrementalSyncConsumer(SyncProperties properties,
                                           ObjectMapper objectMapper,
                                           IncrementalSyncService incrementalSyncService) throws Exception {
        this.objectMapper = objectMapper;
        this.incrementalSyncService = incrementalSyncService;
        this.crmToAicrmTag = RocketMqSyncSettings.crmToAicrmTag(properties);
        this.aicrmToCrmTag = RocketMqSyncSettings.aicrmToCrmTag(properties);
        String topic = RocketMqSyncSettings.topic(properties);
        this.consumer = new DefaultMQPushConsumer(RocketMqSyncSettings.crmToAicrmGroup(properties));
        this.consumer.setNamesrvAddr(properties.getRocketmq().getNameServer());
        this.consumer.subscribe(topic, crmToAicrmTag);
        this.consumer.registerMessageListener((List<MessageExt> messages, ConsumeConcurrentlyContext context) -> {
            for (MessageExt message : messages) {
                try {
                    String body = new String(message.getBody(), StandardCharsets.UTF_8);
                    IncrementalSyncEvent event = parseEvent(body, message);
                    String status = incrementalSyncService.handleEvent(event);
                    if ("failed".equals(status)) {
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                } catch (DirectionConflictException ex) {
                    incrementalSyncService.rejectEvent(ex.event(), ex.getMessage());
                } catch (Exception ex) {
                    log.warn("RocketMQ incremental event consume failed. msgId={}, error={}",
                            message.getMsgId(), ex.getMessage());
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });
        this.consumer.start();
        log.info("RocketMQ CRM_TO_AICRM consumer started. topic={}, tag={}, group={}",
                topic, crmToAicrmTag, RocketMqSyncSettings.crmToAicrmGroup(properties));
    }

    private IncrementalSyncEvent parseEvent(String body, MessageExt message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(body, new TypeReference<>() {
        });
        SyncDirection tagDirection = directionFromTag(firstNonBlank(message.getTags(), crmToAicrmTag));
        String bodyDirection = stringValue(payload.get("direction"));
        if (tagDirection != null && bodyDirection != null && !bodyDirection.isBlank()
                && !tagDirection.name().equalsIgnoreCase(bodyDirection)) {
            IncrementalSyncEvent event = buildEvent(payload, body, message, tagDirection.name());
            throw new DirectionConflictException(event, "RocketMQ tag conflicts with body direction. tag="
                    + message.getTags() + ", direction=" + bodyDirection);
        }
        String direction = tagDirection == null
                ? stringValue(payload.getOrDefault("direction", SyncDirection.CRM_TO_AICRM.name()))
                : tagDirection.name();
        return buildEvent(payload, body, message, direction);
    }

    private IncrementalSyncEvent buildEvent(Map<String, Object> payload, String body, MessageExt message, String direction) {
        return new IncrementalSyncEvent(
                direction,
                stringValue(payload.get("type")),
                stringValue(payload.getOrDefault("eventId", message.getMsgId())),
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
                String.valueOf(message.getQueueOffset()),
                instantValue(payload.get("eventTime")),
                mapValue(payload.get("payload"), payload),
                body,
                stringValue(payload.getOrDefault("schemaVersion", "1"))
        );
    }

    private SyncDirection directionFromTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        if (crmToAicrmTag.equals(tag)) {
            return SyncDirection.CRM_TO_AICRM;
        }
        if (aicrmToCrmTag.equals(tag)) {
            return SyncDirection.AICRM_TO_CRM;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value, Map<String, Object> fallback) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : fallback;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String firstNonBlank(String... values) {
        return RocketMqSyncSettings.firstNonBlank(values);
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private Instant instantValue(Object value) {
        if (value == null) {
            return Instant.now();
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return Instant.parse(value.toString());
    }

    @PreDestroy
    public void shutdown() {
        consumer.shutdown();
    }

    private static class DirectionConflictException extends Exception {
        private final IncrementalSyncEvent event;

        DirectionConflictException(IncrementalSyncEvent event, String message) {
            super(message);
            this.event = event;
        }

        IncrementalSyncEvent event() {
            return event;
        }
    }
}
