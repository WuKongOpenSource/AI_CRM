package com.kakarote.ai_crm.ai.tools.support;

import com.kakarote.ai_crm.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Shared permission guard for AI tools.
 * Returns user-friendly denial messages instead of throwing, so chat can continue gracefully.
 */
@Component
public class AiToolPermissionSupport {

    @Autowired
    private PermissionService permissionService;

    public String denyMessage(String permission, String actionLabel) {
        if (permissionService.hasPermission(permission)) {
            return null;
        }
        return "您没有「" + actionLabel + "」所需的权限，AI 助手无法继续执行。";
    }
}
