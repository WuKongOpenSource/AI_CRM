package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.common.log.AccessLogSnapshot;

public interface AccessLogService {

    void recordAsync(AccessLogSnapshot snapshot);
}
