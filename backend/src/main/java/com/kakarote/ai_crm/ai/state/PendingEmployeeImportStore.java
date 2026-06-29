package com.kakarote.ai_crm.ai.state;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 AI 会话暂存待确认的员工批量导入草稿。
 */
@Component
public class PendingEmployeeImportStore {

    private static final Duration PENDING_TTL = Duration.ofMinutes(30);

    private final Map<Long, PendingEmployeeImport> pendingRequests = new ConcurrentHashMap<>();

    public void save(Long sessionId, List<EmployeeImportRow> rows) {
        if (sessionId == null || rows == null) {
            return;
        }
        pendingRequests.put(sessionId, new PendingEmployeeImport(List.copyOf(rows), Instant.now().plus(PENDING_TTL)));
    }

    public PendingEmployeeImport get(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        PendingEmployeeImport pending = pendingRequests.get(sessionId);
        if (pending == null) {
            return null;
        }
        if (pending.isExpired()) {
            pendingRequests.remove(sessionId);
            return null;
        }
        return pending;
    }

    public PendingEmployeeImport remove(Long sessionId) {
        PendingEmployeeImport pending = get(sessionId);
        if (pending != null) {
            pendingRequests.remove(sessionId);
        }
        return pending;
    }

    public void clear(Long sessionId) {
        if (sessionId != null) {
            pendingRequests.remove(sessionId);
        }
    }

    public record PendingEmployeeImport(List<EmployeeImportRow> rows, Instant expiresAt) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
