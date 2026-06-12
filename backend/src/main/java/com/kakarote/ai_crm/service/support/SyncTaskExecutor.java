package com.kakarote.ai_crm.service.support;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.auth.DataPermissionHolder;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class SyncTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(SyncTaskExecutor.class);

    private final ExecutorService executor;

    public SyncTaskExecutor() {
        this(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("crm-sync-", 0).factory()));
    }

    SyncTaskExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public Future<?> submit(String taskName, Runnable task) {
        return submit(taskName, currentLoginUser(), task);
    }

    public Future<?> submit(String taskName, LoginUser loginUser, Runnable task) {
        Long tenantId = loginUser == null || loginUser.getUser() == null ? TenantContextHolder.getTenantId() : loginUser.getUser().getTenantId();
        return executor.submit(wrap(taskName, loginUser, tenantId, task));
    }

    public Future<?> submitWithTenant(String taskName, Long tenantId, Runnable task) {
        return executor.submit(wrap(taskName, null, tenantId, task));
    }

    @PreDestroy
    public void shutdownNow() {
        executor.shutdownNow();
    }

    private Runnable wrap(String taskName, LoginUser loginUser, Long tenantId, Runnable task) {
        return () -> {
            long startedAt = System.currentTimeMillis();
            bindContext(loginUser, tenantId);
            try {
                log.debug("Background sync task started: taskName={}, tenantId={}, userId={}",
                        taskName, tenantId, loginUserId(loginUser));
                task.run();
                log.debug("Background sync task finished: taskName={}, tenantId={}, elapsedMs={}",
                        taskName, tenantId, Math.max(0L, System.currentTimeMillis() - startedAt));
            } catch (Throwable throwable) {
                log.error("Background sync task failed: taskName={}, error={}", taskName, throwable.getMessage(), throwable);
                throw throwable;
            } finally {
                clearContext();
            }
        };
    }

    private void bindContext(LoginUser loginUser, Long tenantId) {
        if (loginUser != null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            Long userId = loginUser.getUser() == null ? null : loginUser.getUser().getUserId();
            Long actualTenantId = loginUser.getUser() == null ? tenantId : loginUser.getUser().getTenantId();
            AiContextHolder.bindThreadContext(userId, actualTenantId);
            return;
        }
        if (tenantId != null) {
            AiContextHolder.bindThreadContext(null, tenantId);
        }
    }

    private void clearContext() {
        SecurityContextHolder.clearContext();
        DataPermissionHolder.clear();
        AiContextHolder.clearThreadContext();
        TenantContextHolder.clear();
    }

    private LoginUser currentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    private Long loginUserId(LoginUser loginUser) {
        return loginUser == null || loginUser.getUser() == null ? null : loginUser.getUser().getUserId();
    }
}
