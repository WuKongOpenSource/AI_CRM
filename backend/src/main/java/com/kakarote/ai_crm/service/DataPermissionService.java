package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.common.auth.DataPermissionContext;

public interface DataPermissionService {

    /**
     * 创建上下文。
     */
    DataPermissionContext createContext(String module);

    /**
     * 创建上下文按权限。
     */
    DataPermissionContext createContextByPermission(String permission);

    /**
     * 判断是否存在用户数据访问。
     */
    boolean hasUserDataAccess(String module, Long targetUserId);

    /**
     * 判断是否存在用户数据访问按权限。
     */
    boolean hasUserDataAccessByPermission(String permission, Long targetUserId);

    /**
     * 处理assertUser数据访问方法逻辑。
     */
    void assertUserDataAccess(String module, Long targetUserId);

    /**
     * 处理assertUser数据访问ByPermission方法逻辑。
     */
    void assertUserDataAccessByPermission(String permission, Long targetUserId);
}
