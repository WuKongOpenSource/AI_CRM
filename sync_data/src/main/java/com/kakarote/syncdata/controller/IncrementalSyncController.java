package com.kakarote.syncdata.controller;

import com.kakarote.syncdata.incremental.IncrementalSyncEvent;
import com.kakarote.syncdata.incremental.IncrementalSyncService;
import com.kakarote.syncdata.incremental.SyncDirection;
import com.kakarote.syncdata.incremental.SyncOperation;
import com.kakarote.syncdata.mq.AicrmToCrmEventPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/sync/incremental")
public class IncrementalSyncController {

    private final IncrementalSyncService incrementalSyncService;
    private final AicrmToCrmEventPublisher aicrmToCrmEventPublisher;

    public IncrementalSyncController(IncrementalSyncService incrementalSyncService,
                                     AicrmToCrmEventPublisher aicrmToCrmEventPublisher) {
        this.incrementalSyncService = incrementalSyncService;
        this.aicrmToCrmEventPublisher = aicrmToCrmEventPublisher;
    }

    @PostMapping("/events")
    public Map<String, Object> receive(@Valid @RequestBody IncrementalEventRequest request) {
        IncrementalSyncEvent event = toEvent(request, SyncDirection.CRM_TO_AICRM);
        String status = incrementalSyncService.handleEvent(event);
        return Map.of(
                "status", status,
                "incrementalApplicationAvailable", true,
                "message", messageFor(status)
        );
    }

    @PostMapping("/publish")
    public AicrmToCrmEventPublisher.PublishResult publish(@Valid @RequestBody IncrementalEventRequest request) {
        return aicrmToCrmEventPublisher.publish(toEvent(request, SyncDirection.AICRM_TO_CRM));
    }

    private IncrementalSyncEvent toEvent(IncrementalEventRequest request, SyncDirection defaultDirection) {
        SyncDirection direction = request.direction() == null || request.direction().isBlank()
                ? defaultDirection
                : SyncDirection.valueOf(request.direction().trim().toUpperCase());
        boolean crmToAicrm = direction == SyncDirection.CRM_TO_AICRM;
        return new IncrementalSyncEvent(
                direction.name(),
                request.type(),
                request.eventId(),
                request.traceId(),
                request.originSystem() == null ? (crmToAicrm ? "crm" : "aicrm") : request.originSystem(),
                request.sourceSystem() == null ? (crmToAicrm ? "wk_crm" : "aicrm") : request.sourceSystem(),
                request.targetSystem() == null ? (crmToAicrm ? "aicrm" : "wk_crm") : request.targetSystem(),
                request.tenantId(),
                request.sourceCompanyId(),
                request.entityType(),
                request.sourceTable(),
                request.sourceId(),
                request.targetId(),
                request.operation() == null ? SyncOperation.UPDATE.name() : request.operation(),
                request.offset(),
                request.eventTime() == null ? Instant.now() : request.eventTime(),
                request.payload(),
                request.rawPayload(),
                request.schemaVersion() == null ? "1" : request.schemaVersion()
        );
    }

    private String messageFor(String status) {
        return switch (status) {
            case "applied" -> "增量事件已应用到 AICRM 目标业务表。";
            case "waiting_ack" -> "AICRM 到 CRM 事件已发布，等待 CRM ACK 回写映射。";
            case "conflict_skipped" -> "事件时间早于目标侧最近变更，已按冲突策略跳过。";
            case "pending_manual", "pending_mapping" -> "事件已记录，但需要补齐映射或人工处理。";
            case "ignored" -> "事件已记录但被配置或回环保护忽略。";
            case "failed" -> "事件处理失败，请查看增量事件日志。";
            default -> "事件已处理。";
        };
    }

    public record IncrementalEventRequest(
            String direction,
            String type,
            String eventId,
            String traceId,
            String originSystem,
            String sourceSystem,
            String targetSystem,
            Long tenantId,
            Long sourceCompanyId,
            String entityType,
            @NotNull String sourceTable,
            String sourceId,
            String targetId,
            @NotNull String operation,
            String offset,
            Instant eventTime,
            Map<String, Object> payload,
            String rawPayload,
            String schemaVersion
    ) {
    }
}
