package com.kakarote.ai_crm.common.auth;

import java.util.HashMap;
import java.util.Map;

public final class DataPermissionHolder {

    private static final ThreadLocal<Map<String, DataPermissionContext>> CACHE =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * 初始化数据权限持有器实例。
     */
    private DataPermissionHolder() {
    }

    /**
     * 获取数据权限持有器。
     */
    public static DataPermissionContext get(String module) {
        return CACHE.get().get(module);
    }

    /**
     * 处理put方法逻辑。
     */
    public static void put(String module, DataPermissionContext context) {
        CACHE.get().put(module, context);
    }

    public static void remove(String module) {
        CACHE.get().remove(module);
    }

    /**
     * 清理数据权限持有器。
     */
    public static void clear() {
        CACHE.remove();
    }
}
