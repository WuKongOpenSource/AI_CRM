package com.kakarote.ai_crm.service;

public interface PermissionService {

    /**
     * 判断是否存在权限。
     */
    boolean hasPermission(String permission);
}
