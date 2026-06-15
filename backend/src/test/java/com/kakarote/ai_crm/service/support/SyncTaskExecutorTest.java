package com.kakarote.ai_crm.service.support;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.utils.UserUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SyncTaskExecutorTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        AiContextHolder.clear();
    }

    @Test
    void submitShouldRestoreLoginUserContextAndCleanThreadAfterTask() throws Exception {
        ExecutorService delegate = Executors.newSingleThreadExecutor();
        SyncTaskExecutor executor = new SyncTaskExecutor(delegate);
        LoginUser loginUser = loginUser(101L);

        try {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));

            Future<?> first = executor.submit("test-sync", () -> {
                assertThat(UserUtil.getLoginUser()).isSameAs(loginUser);
                assertThat(UserUtil.getUserId()).isEqualTo(101L);
            });
            first.get(2, TimeUnit.SECONDS);

            SecurityContextHolder.clearContext();
            AiContextHolder.clear();

            Future<?> second = executor.submit("test-clean", () ->
                    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull());
            second.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            delegate.shutdownNow();
        }
    }

    private static LoginUser loginUser(Long userId) {
        ManagerUser user = new ManagerUser();
        user.setUserId(userId);
        user.setUsername("sync-user");
        user.setPassword("secret");
        user.setStatus(1);

        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        return loginUser;
    }
}
