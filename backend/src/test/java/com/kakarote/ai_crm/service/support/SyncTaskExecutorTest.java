package com.kakarote.ai_crm.service.support;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SyncTaskExecutorTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        AiContextHolder.clearThreadContext();
    }

    @Test
    void submitShouldBindExplicitLoginUserInBackgroundTask() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        SyncTaskExecutor executor = new SyncTaskExecutor(executorService);
        LoginUser loginUser = loginUser(88L);
        AtomicReference<Authentication> authenticationRef = new AtomicReference<>();

        Future<?> future = executor.submit("explicit-login-user", loginUser, () ->
                authenticationRef.set(SecurityContextHolder.getContext().getAuthentication()));
        future.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertThat(authenticationRef.get()).isNotNull();
        assertThat(authenticationRef.get().getPrincipal()).isSameAs(loginUser);
    }

    private static LoginUser loginUser(Long userId) {
        ManagerUser user = new ManagerUser();
        user.setUserId(userId);
        user.setUsername("sync-user");
        user.setStatus(1);

        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        return loginUser;
    }
}
