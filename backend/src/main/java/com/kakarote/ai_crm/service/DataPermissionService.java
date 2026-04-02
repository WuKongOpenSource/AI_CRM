package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.common.auth.DataPermissionContext;

public interface DataPermissionService {

    DataPermissionContext createContext(String module);

    boolean hasUserDataAccess(String module, Long targetUserId);

    void assertUserDataAccess(String module, Long targetUserId);
}
