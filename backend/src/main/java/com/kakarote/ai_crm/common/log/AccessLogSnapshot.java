package com.kakarote.ai_crm.common.log;

/**
 * Immutable access-log payload captured at the end of a request.
 */
public record AccessLogSnapshot(
        Long tenantId,
        Long userId,
        String username,
        String method,
        String requestUri,
        String queryString,
        String requestHeaders,
        String requestBody,
        String responseBody,
        Integer statusCode,
        Integer businessCode,
        Boolean success,
        String ipAddress,
        String userAgent,
        String traceId,
        Long costMs,
        Boolean requestTruncated,
        Boolean responseTruncated,
        Boolean resultResponse,
        ErrorSnapshot systemError
) {

    public record ErrorSnapshot(
            String exceptionName,
            String errorMessage,
            String stackTrace
    ) {
    }
}
