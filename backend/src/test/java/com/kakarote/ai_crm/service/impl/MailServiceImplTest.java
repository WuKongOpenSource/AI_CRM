package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.MailImapBindBO;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.MailAccount;
import com.kakarote.ai_crm.entity.PO.MailSyncLog;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.MailSyncResultVO;
import com.kakarote.ai_crm.mapper.MailAccountMapper;
import com.kakarote.ai_crm.mapper.MailSyncLogMapper;
import com.kakarote.ai_crm.service.support.SyncTaskExecutor;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MailServiceImplTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        AiContextHolder.clearThreadContext();
    }

    @Test
    void syncAccountShouldReturnRunningAndSubmitBackgroundTask() {
        MailServiceImpl service = new MailServiceImpl();
        MailAccountMapper accountMapper = mock(MailAccountMapper.class);
        MailSyncLogMapper syncLogMapper = mock(MailSyncLogMapper.class);
        SyncTaskExecutor syncTaskExecutor = mock(SyncTaskExecutor.class);
        MailAccount account = new MailAccount();
        account.setAccountId(11L);
        account.setUserId(10L);
        account.setProvider("imap");

        org.mockito.Mockito.when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        doAnswer(invocation -> {
            MailSyncLog syncLog = invocation.getArgument(0);
            syncLog.setLogId(99L);
            return 1;
        }).when(syncLogMapper).insert(any(MailSyncLog.class));

        ReflectionTestUtils.setField(service, "baseMapper", accountMapper);
        ReflectionTestUtils.setField(service, "syncLogMapper", syncLogMapper);
        ReflectionTestUtils.setField(service, "syncTaskExecutor", syncTaskExecutor);
        bindLoginUser(10L);

        MailSyncResultVO result = service.syncAccount(11L);

        assertThat(result.getAccountId()).isEqualTo(11L);
        assertThat(result.getLogId()).isEqualTo(99L);
        assertThat(result.getStatus()).isEqualTo("running");
        assertThat(account.getLastSyncTime()).isNotNull();
        assertThat(account.getLastSyncStatus()).isEqualTo("running");
        assertThat(account.getLastSyncError()).isNull();
        verify(accountMapper).updateById(account);
        verify(syncTaskExecutor).submit(anyString(), any(Runnable.class));
    }

    @Test
    void bindImapAccountShouldReportMissingCredentialKeyInChinese() {
        MailServiceImpl service = new MailServiceImpl();
        MailAccountMapper accountMapper = mock(MailAccountMapper.class);
        org.mockito.Mockito.when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ReflectionTestUtils.setField(service, "baseMapper", accountMapper);
        ReflectionTestUtils.setField(service, "secretTextCipher", new SecretTextCipher(""));
        bindLoginUser(10L);

        MailImapBindBO bindBO = new MailImapBindBO();
        bindBO.setEmailAddress("user@example.com");
        bindBO.setImapHost("imap.example.com");
        bindBO.setImapPort(993);
        bindBO.setImapSsl(true);
        bindBO.setSmtpHost("smtp.example.com");
        bindBO.setSmtpPort(465);
        bindBO.setSmtpSsl(true);
        bindBO.setPassword("mail-password");
        bindBO.setTestConnection(false);

        assertThatThrownBy(() -> service.bindImapAccount(bindBO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("邮箱凭据加密密钥未配置");
    }

    @Test
    void oauthFetchersShouldReceiveSyncLogForStageTracing() throws Exception {
        assertThat(MailServiceImpl.class.getDeclaredMethod(
                "fetchGmailMessages",
                MailAccount.class,
                Map.class,
                MailSyncLog.class
        )).isNotNull();
        assertThat(MailServiceImpl.class.getDeclaredMethod(
                "fetchGraphMessages",
                MailAccount.class,
                Map.class,
                MailSyncLog.class
        )).isNotNull();
    }

    private static void bindLoginUser(Long userId) {
        ManagerUser user = new ManagerUser();
        user.setUserId(userId);
        user.setUsername("mail-user");
        user.setPassword("secret");
        user.setStatus(1);

        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }
}
