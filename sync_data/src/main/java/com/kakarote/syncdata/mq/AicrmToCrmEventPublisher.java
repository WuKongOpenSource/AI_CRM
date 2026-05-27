package com.kakarote.syncdata.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.db.CompanyBindingRepository;
import com.kakarote.syncdata.incremental.EntitySyncStateRepository;
import com.kakarote.syncdata.incremental.IncrementalEventLogRepository;
import com.kakarote.syncdata.incremental.IncrementalSyncEvent;
import com.kakarote.syncdata.incremental.SyncDirection;
import com.kakarote.syncdata.model.CompanyBinding;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class AicrmToCrmEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AicrmToCrmEventPublisher.class);

    private final SyncProperties properties;
    private final CompanyBindingRepository bindingRepository;
    private final IncrementalEventLogRepository eventLogRepository;
    private final EntitySyncStateRepository entityStateRepository;
    private final ObjectMapper objectMapper;
    private DefaultMQProducer producer;

    public AicrmToCrmEventPublisher(SyncProperties properties,
                                    CompanyBindingRepository bindingRepository,
                                    IncrementalEventLogRepository eventLogRepository,
                                    EntitySyncStateRepository entityStateRepository,
                                    ObjectMapper objectMapper) {
        this.properties = properties;
        this.bindingRepository = bindingRepository;
        this.eventLogRepository = eventLogRepository;
        this.entityStateRepository = entityStateRepository;
        this.objectMapper = objectMapper;
        startProducerIfEnabled();
    }

    public PublishResult publish(IncrementalSyncEvent event) {
        event = withResolvedType(event);
        CompanyBinding binding = event.tenantId() == null ? null : bindingRepository.findByTenantId(event.tenantId());
        if (binding == null && event.sourceCompanyId() != null) {
            binding = bindingRepository.findActiveByCompanyId(event.sourceCompanyId());
        }
        if (binding != null && event.sourceCompanyId() == null) {
            event = new IncrementalSyncEvent(
                    event.direction(), event.type(), event.eventId(), event.traceId(), event.originSystem(),
                    event.sourceSystem(), event.targetSystem(), event.tenantId(), binding.sourceCompanyId(),
                    event.entityType(), event.sourceTable(), event.sourceId(), event.targetId(),
                    event.operation(), event.offset(), event.eventTime(), event.payload(),
                    event.rawPayload(), event.schemaVersion()
            );
        }
        Long bindingId = binding == null ? null : binding.bindingId();
        String payload = toJson(event);
        eventLogRepository.markReceived(event, bindingId, payload);
        if (binding == null) {
            eventLogRepository.markStatus(event, "ignored", "No binding found for AICRM_TO_CRM event.");
            return new PublishResult("ignored", null, "No binding found.");
        }
        if (!Boolean.TRUE.equals(binding.aicrmToCrmEnabled())) {
            eventLogRepository.markStatus(event, "ignored", "AICRM_TO_CRM is disabled for this binding.");
            return new PublishResult("ignored", null, "AICRM_TO_CRM is disabled.");
        }
        if (!properties.getRocketmq().isEnabled()) {
            eventLogRepository.markStatus(event, "failed", "RocketMQ is disabled.");
            return new PublishResult("failed", null, "RocketMQ is disabled.");
        }
        try {
            ensureProducer();
            Message message = new Message(
                    topic(binding),
                    tag(),
                    event.dedupKey(),
                    payload.getBytes(StandardCharsets.UTF_8)
            );
            message.putUserProperty("direction", SyncDirection.AICRM_TO_CRM.name());
            message.putUserProperty("type", event.resolvedType());
            message.putUserProperty("eventId", event.resolvedEventId());
            message.putUserProperty("traceId", event.traceId());
            SendResult result = producer.send(message);
            eventLogRepository.markStatus(event, "waiting_ack", "Published to RocketMQ. msgId=" + result.getMsgId());
            bindingRepository.updateDirectionalCheckpoint(bindingId, SyncDirection.AICRM_TO_CRM.name(), result.getMsgId());
            Long targetId = parseLong(event.targetId() == null ? event.sourceId() : event.targetId());
            if (targetId != null && event.sourceTable() != null && event.sourceId() != null) {
                entityStateRepository.markWaitingAck(event, bindingId, event.sourceTable(), targetId, binding.tenantId());
            }
            return new PublishResult("waiting_ack", result.getMsgId(), "Published to RocketMQ.");
        } catch (Exception ex) {
            eventLogRepository.markStatus(event, "failed", ex.getMessage());
            return new PublishResult("failed", null, ex.getMessage());
        }
    }

    private void startProducerIfEnabled() {
        if (!properties.getRocketmq().isEnabled()) {
            return;
        }
        try {
            ensureProducer();
        } catch (Exception ex) {
            log.warn("RocketMQ producer startup failed: {}", ex.getMessage());
        }
    }

    private synchronized void ensureProducer() throws Exception {
        if (producer != null) {
            return;
        }
        producer = new DefaultMQProducer(RocketMqSyncSettings.aicrmToCrmGroup(properties));
        producer.setNamesrvAddr(properties.getRocketmq().getNameServer());
        producer.start();
    }

    private String topic(CompanyBinding binding) {
        return RocketMqSyncSettings.firstNonBlank(
                binding.mqTopic(),
                binding.aicrmToCrmTopic(),
                binding.crmToAicrmTopic(),
                RocketMqSyncSettings.topic(properties)
        );
    }

    private String tag() {
        return RocketMqSyncSettings.aicrmToCrmTag(properties);
    }

    private IncrementalSyncEvent withResolvedType(IncrementalSyncEvent event) {
        if (event.type() != null && !event.type().isBlank()) {
            return event;
        }
        return new IncrementalSyncEvent(
                event.direction(), event.resolvedType(), event.eventId(), event.traceId(), event.originSystem(),
                event.sourceSystem(), event.targetSystem(), event.tenantId(), event.sourceCompanyId(),
                event.entityType(), event.sourceTable(), event.sourceId(), event.targetId(),
                event.operation(), event.offset(), event.eventTime(), event.payload(),
                event.rawPayload(), event.schemaVersion()
        );
    }

    private String toJson(IncrementalSyncEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            return String.valueOf(event.payload());
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    @PreDestroy
    public void shutdown() {
        if (producer != null) {
            producer.shutdown();
        }
    }

    public record PublishResult(String status, String messageId, String message) {
    }
}
