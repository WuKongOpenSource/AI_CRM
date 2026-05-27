package com.kakarote.syncdata.incremental;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.syncdata.db.CompanyBindingRepository;
import com.kakarote.syncdata.model.CompanyBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class IncrementalSyncService {

    private static final Logger log = LoggerFactory.getLogger(IncrementalSyncService.class);

    private final CompanyBindingRepository bindingRepository;
    private final IncrementalEventLogRepository eventLogRepository;
    private final EntitySyncStateRepository entityStateRepository;
    private final CrmToAicrmEventApplier crmToAicrmEventApplier;
    private final ObjectMapper objectMapper;

    public IncrementalSyncService(CompanyBindingRepository bindingRepository,
                                  IncrementalEventLogRepository eventLogRepository,
                                  EntitySyncStateRepository entityStateRepository,
                                  CrmToAicrmEventApplier crmToAicrmEventApplier,
                                  ObjectMapper objectMapper) {
        this.bindingRepository = bindingRepository;
        this.eventLogRepository = eventLogRepository;
        this.entityStateRepository = entityStateRepository;
        this.crmToAicrmEventApplier = crmToAicrmEventApplier;
        this.objectMapper = objectMapper;
    }

    public String handleEvent(IncrementalSyncEvent event) {
        event = withPayloadCompanyId(event);
        CompanyBinding binding = resolveBinding(event);
        Long bindingId = binding == null ? null : binding.bindingId();
        String rawPayload = event.rawPayload() == null ? toJson(event.payload()) : event.rawPayload();
        String terminalStatus = eventLogRepository.existingTerminalStatus(event.dedupKey());
        if (terminalStatus != null) {
            return terminalStatus;
        }
        eventLogRepository.markReceived(event, bindingId, rawPayload);

        try {
            if (binding == null) {
                return finish(event, "ignored", "No active binding for event source.", null, null, null);
            }
            if (isOwnLoopback(event)) {
                return finish(event, "ignored", "Ignored loopback event from sync_data.", null, null, null);
            }
            if (SyncOperation.ACK.name().equals(event.resolvedOperation())) {
                ApplyResult result = crmToAicrmEventApplier.applyAck(event, binding);
                entityStateRepository.markAcked(event.resolvedSourceSystem(), binding.sourceCompanyId(),
                        event.sourceTable(), event.sourceId(), result.targetTable(), result.targetId());
                bindingRepository.updateDirectionalCheckpoint(bindingId, event.resolvedDirection().name(), event.offset());
                return finish(event, result.status(), result.message(), result.targetTable(), result.targetId(), result.tenantId());
            }
            if (event.resolvedDirection() == SyncDirection.AICRM_TO_CRM) {
                return finish(event, "ignored", "AICRM_TO_CRM events are published by this service and applied by CRM.", null, null, null);
            }
            if (!Boolean.TRUE.equals(binding.crmToAicrmEnabled()) && !Boolean.TRUE.equals(binding.incrementalEnabled())) {
                return finish(event, "ignored", "CRM_TO_AICRM is disabled for this binding.", null, null, null);
            }

            ApplyResult existingTarget = crmToAicrmEventApplier.resolveTarget(event, binding);
            if (existingTarget.targetId() != null
                    && !entityStateRepository.shouldApplyCrmEvent(event, existingTarget.targetTable(), existingTarget.targetId())) {
                return finish(event, "conflict_skipped",
                        "Skipped because a newer AICRM event exists for this entity.",
                        existingTarget.targetTable(), existingTarget.targetId(), existingTarget.tenantId());
            }
            ApplyResult preview = crmToAicrmEventApplier.apply(event, binding);
            entityStateRepository.markApplied(event, bindingId, preview.targetTable(), preview.targetId(), preview.tenantId());
            bindingRepository.updateDirectionalCheckpoint(bindingId, event.resolvedDirection().name(), event.offset());
            return finish(event, preview.status(), preview.message(), preview.targetTable(), preview.targetId(), preview.tenantId());
        } catch (Exception ex) {
            log.warn("Incremental event failed. direction={}, table={}, id={}, message={}",
                    event.direction(), event.sourceTable(), event.sourceId(), ex.getMessage());
            return finish(event, "failed", ex.getMessage(), null, null, null);
        }
    }

    public String rejectEvent(IncrementalSyncEvent event, String message) {
        event = withPayloadCompanyId(event);
        CompanyBinding binding = resolveBinding(event);
        Long bindingId = binding == null ? null : binding.bindingId();
        String rawPayload = event.rawPayload() == null ? toJson(event.payload()) : event.rawPayload();
        String terminalStatus = eventLogRepository.existingTerminalStatus(event.dedupKey());
        if (terminalStatus != null) {
            return terminalStatus;
        }
        eventLogRepository.markReceived(event, bindingId, rawPayload);
        return finish(event, "failed", message, null, null, null);
    }

    private String finish(IncrementalSyncEvent event, String status, String message,
                          String targetTable, Long targetId, Long tenantId) {
        eventLogRepository.markStatus(event, status, message);
        return status;
    }

    private CompanyBinding resolveBinding(IncrementalSyncEvent event) {
        if (event.sourceCompanyId() != null) {
            CompanyBinding binding = bindingRepository.findActiveByCompanyId(event.sourceCompanyId());
            if (binding != null) {
                return binding;
            }
        }
        if (event.tenantId() != null) {
            return bindingRepository.findByTenantId(event.tenantId());
        }
        return null;
    }

    private boolean isOwnLoopback(IncrementalSyncEvent event) {
        return "sync_data".equalsIgnoreCase(event.originSystem());
    }

    private IncrementalSyncEvent withPayloadCompanyId(IncrementalSyncEvent event) {
        if (event.sourceCompanyId() != null || event.payload() == null) {
            return event;
        }
        Object value = event.payload().get("company_id");
        if (value == null) {
            value = event.payload().get("sourceCompanyId");
        }
        Long companyId = null;
        if (value instanceof Number number) {
            companyId = number.longValue();
        } else if (value != null && !String.valueOf(value).isBlank()) {
            companyId = Long.valueOf(String.valueOf(value));
        }
        if (companyId == null) {
            return event;
        }
        return new IncrementalSyncEvent(
                event.direction(), event.type(), event.eventId(), event.traceId(), event.originSystem(),
                event.sourceSystem(), event.targetSystem(), event.tenantId(), companyId,
                event.entityType(), event.sourceTable(), event.sourceId(), event.targetId(),
                event.operation(), event.offset(), event.eventTime(), event.payload(),
                event.rawPayload(), event.schemaVersion()
        );
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return payload.toString();
        }
    }
}
