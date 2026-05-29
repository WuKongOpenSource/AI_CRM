package com.kakarote.ai_crm.common.log;

import com.kakarote.ai_crm.mapper.AccessLogMapper;
import com.kakarote.ai_crm.mapper.ErrorLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@Slf4j
public class AccessLogCleanupScheduler {

    private static final int RETENTION_DAYS = 90;
    private static final int CLEANUP_BATCH_SIZE = 1000;

    @Autowired
    private AccessLogMapper accessLogMapper;

    @Autowired
    private ErrorLogMapper errorLogMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredLogs() {
        Date cutoff = Date.from(Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS));
        try {
            deleteExpiredErrors(cutoff);
            deleteExpiredAccessLogs(cutoff);
        } catch (Exception e) {
            log.warn("Cleanup expired access logs failed: cutoff={}, error={}", cutoff, e.getMessage(), e);
        }
    }

    private void deleteExpiredErrors(Date cutoff) {
        int deleted;
        do {
            deleted = errorLogMapper.deleteExpiredBefore(cutoff, CLEANUP_BATCH_SIZE);
        } while (deleted >= CLEANUP_BATCH_SIZE);
    }

    private void deleteExpiredAccessLogs(Date cutoff) {
        int deleted;
        do {
            deleted = accessLogMapper.deleteExpiredBefore(cutoff, CLEANUP_BATCH_SIZE);
        } while (deleted >= CLEANUP_BATCH_SIZE);
    }
}
