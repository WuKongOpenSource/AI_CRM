package com.kakarote.ai_crm.utils;


import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全服务工具类
 */
public class UserUtil {

    private static final Logger log = LoggerFactory.getLogger(UserUtil.class);

    /**
     * 获取用户账户
     **/
    public static String getUsername() {
        try {
            return getLoginUser().getUsername();
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 获取用户ID。
     */
    public static Long getUserId() {
        // 优先从 SecurityContext 获取（正常 HTTP 请求线程）
        try {
            Authentication auth = getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
                return loginUser.getUser().getUserId();
            }
        } catch (Exception ignored) {
        }

        // 回退到 AI 上下文（工具调用线程没有 SecurityContext）
        Long aiContextUserId = AiContextHolder.getCurrentUserId();
        if (aiContextUserId != null) {
            log.debug("从 AiContextHolder 获取用户ID: {}", aiContextUserId);
            return aiContextUserId;
        }

        log.error("无法获取用户ID, AiContext sessionId: {}", AiContextHolder.getCurrentSessionId());
        throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR);
    }

    /**
     * 安全获取用户ID，失败时返回null而不是抛出异常
     * @return 用户ID或null
     */
    public static Long getUserIdOrNull() {
        try {
            Authentication auth = getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
                return loginUser.getUser().getUserId();
            }
        } catch (Exception ignored) {
        }
        return AiContextHolder.getCurrentUserId();
    }

    /**
     * 获取用户
     **/
    public static LoginUser getLoginUser() {
        try {
            return (LoginUser) getAuthentication().getPrincipal();
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR);
        }
    }

    /**
     * 获取当前租户ID
     * 优先从 TenantContextHolder 获取（用于工具调用线程等场景）
     */
    public static Long getTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            return tenantId;
        }
        try {
            return getLoginUser().getUser().getTenantId();
        } catch (Exception e) {
            log.debug("无法获取租户ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取超级管理员ID
     * @return
     */
    public static Long getSuperUserId() {
       return  1L;
    }
    /**
     * 获取Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

}
