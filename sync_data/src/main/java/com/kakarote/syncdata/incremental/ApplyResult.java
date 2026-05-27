package com.kakarote.syncdata.incremental;

public record ApplyResult(
        String status,
        String message,
        String targetTable,
        Long targetId,
        Long tenantId
) {
    public static ApplyResult applied(String targetTable, Long targetId, Long tenantId) {
        return new ApplyResult("applied", "applied", targetTable, targetId, tenantId);
    }

    public static ApplyResult skipped(String status, String message) {
        return new ApplyResult(status, message, null, null, null);
    }
}
