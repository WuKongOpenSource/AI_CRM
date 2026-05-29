package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.log.AccessLogSnapshot;
import com.kakarote.ai_crm.entity.PO.AccessLog;
import com.kakarote.ai_crm.entity.PO.ErrorLog;
import com.kakarote.ai_crm.mapper.AccessLogMapper;
import com.kakarote.ai_crm.mapper.ErrorLogMapper;
import com.kakarote.ai_crm.service.AccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class AccessLogServiceImpl implements AccessLogService {

    @Autowired
    private AccessLogMapper accessLogMapper;

    @Autowired
    private ErrorLogMapper errorLogMapper;

    @Override
    @Async("accessLogExecutor")
    public void recordAsync(AccessLogSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        try {
            AccessLog accessLog = toAccessLog(snapshot);
            accessLogMapper.insert(accessLog);
            if (snapshot.systemError() != null) {
                ErrorLog errorLog = toErrorLog(snapshot, accessLog.getLogId());
                errorLogMapper.insert(errorLog);
            }
        } catch (Exception e) {
            log.warn("Persist access log failed: traceId={}, uri={}, error={}",
                    snapshot.traceId(), snapshot.requestUri(), e.getMessage(), e);
        }
    }

    private AccessLog toAccessLog(AccessLogSnapshot snapshot) {
        AccessLog log = new AccessLog();
        log.setTenantId(defaultTenantId(snapshot.tenantId()));
        log.setUserId(snapshot.userId());
        log.setUsername(snapshot.username());
        log.setMethod(snapshot.method());
        log.setRequestUri(snapshot.requestUri());
        log.setQueryString(snapshot.queryString());
        log.setRequestHeaders(snapshot.requestHeaders());
        log.setRequestBody(snapshot.requestBody());
        log.setResponseBody(snapshot.responseBody());
        log.setStatusCode(snapshot.statusCode());
        log.setBusinessCode(snapshot.businessCode());
        log.setSuccess(Boolean.TRUE.equals(snapshot.success()));
        log.setIpAddress(snapshot.ipAddress());
        log.setUserAgent(snapshot.userAgent());
        log.setTraceId(snapshot.traceId());
        log.setCostMs(snapshot.costMs());
        log.setRequestTruncated(Boolean.TRUE.equals(snapshot.requestTruncated()));
        log.setResponseTruncated(Boolean.TRUE.equals(snapshot.responseTruncated()));
        log.setResultResponse(Boolean.TRUE.equals(snapshot.resultResponse()));
        log.setCreateTime(new Date());
        return log;
    }

    private ErrorLog toErrorLog(AccessLogSnapshot snapshot, Long accessLogId) {
        AccessLogSnapshot.ErrorSnapshot error = snapshot.systemError();
        ErrorLog log = new ErrorLog();
        log.setAccessLogId(accessLogId);
        log.setTenantId(defaultTenantId(snapshot.tenantId()));
        log.setUserId(snapshot.userId());
        log.setTraceId(snapshot.traceId());
        log.setExceptionName(error.exceptionName());
        log.setErrorMessage(error.errorMessage());
        log.setStackTrace(error.stackTrace());
        log.setCreateTime(new Date());
        return log;
    }

    private Long defaultTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }
}
