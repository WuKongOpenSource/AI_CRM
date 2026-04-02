package com.kakarote.ai_crm.common.auth;

import java.util.HashMap;
import java.util.Map;

public final class DataPermissionHolder {

    private static final ThreadLocal<Map<String, DataPermissionContext>> CACHE =
            ThreadLocal.withInitial(HashMap::new);

    private DataPermissionHolder() {
    }

    public static DataPermissionContext get(String module) {
        return CACHE.get().get(module);
    }

    public static void put(String module, DataPermissionContext context) {
        CACHE.get().put(module, context);
    }

    public static void clear() {
        CACHE.remove();
    }
}
