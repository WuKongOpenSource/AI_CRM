package com.kakarote.ai_crm.service.support;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.auth.DataPermissionHolder;
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
        return executor.submit(wrap(taskName, loginUser, task));
    }

    @PreDestroy
    public void shutdownNow() {
        executor.shutdownNow();
    }

    private Runnable wrap(String taskName, LoginUser loginUser, Runnable task) {
        return () -> {
            long startedAt = System.currentTimeMillis();
            bindContext(loginUser);
            try {
                log.debug("Background sync task started: taskName={}, userId={}",
                        taskName, loginUserId(loginUser));
                task.run();
                log.debug("Background sync task finished: taskName={}, elapsedMs={}",
                        taskName, Math.max(0L, System.currentTimeMillis() - startedAt));
            } catch (Throwable throwable) {
                log.error("Background sync task failed: taskName={}, error={}", taskName, throwable.getMessage(), throwable);
                throw throwable;
            } finally {
                clearContext();
            }
        };
    }

    private void bindContext(LoginUser loginUser) {
        if (loginUser == null) {
            return;
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void clearContext() {
        SecurityContextHolder.clearContext();
        DataPermissionHolder.clear();
        AiContextHolder.clear();
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
