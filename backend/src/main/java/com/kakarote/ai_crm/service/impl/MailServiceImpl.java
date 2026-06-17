package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.MailIntegrationProperties;
import com.kakarote.ai_crm.entity.BO.MailAssociateCustomerBO;
import com.kakarote.ai_crm.entity.BO.MailDraftCreateBO;
import com.kakarote.ai_crm.entity.BO.MailDraftQueryBO;
import com.kakarote.ai_crm.entity.BO.MailImapBindBO;
import com.kakarote.ai_crm.entity.BO.MailMessageQueryBO;
import com.kakarote.ai_crm.entity.BO.MailSendBO;
import com.kakarote.ai_crm.entity.BO.MailSyncPolicyBO;
import com.kakarote.ai_crm.entity.BO.MailTemplateQueryBO;
import com.kakarote.ai_crm.entity.BO.MailTemplateSaveBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.MailAccount;
import com.kakarote.ai_crm.entity.PO.MailAttachment;
import com.kakarote.ai_crm.entity.PO.MailDraft;
import com.kakarote.ai_crm.entity.PO.MailMessage;
import com.kakarote.ai_crm.entity.PO.MailSyncCursor;
import com.kakarote.ai_crm.entity.PO.MailSyncLog;
import com.kakarote.ai_crm.entity.PO.MailTemplate;
import com.kakarote.ai_crm.entity.VO.MailAccountVO;
import com.kakarote.ai_crm.entity.VO.MailAttachmentVO;
import com.kakarote.ai_crm.entity.VO.MailAuthStatusVO;
import com.kakarote.ai_crm.entity.VO.MailDraftVO;
import com.kakarote.ai_crm.entity.VO.MailMessageVO;
import com.kakarote.ai_crm.entity.VO.MailOAuthStartVO;
import com.kakarote.ai_crm.entity.VO.MailSyncLogVO;
import com.kakarote.ai_crm.entity.VO.MailSyncResultVO;
import com.kakarote.ai_crm.entity.VO.MailTemplateVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.MailAccountMapper;
import com.kakarote.ai_crm.mapper.MailAttachmentMapper;
import com.kakarote.ai_crm.mapper.MailDraftMapper;
import com.kakarote.ai_crm.mapper.MailMessageMapper;
import com.kakarote.ai_crm.mapper.MailSyncCursorMapper;
import com.kakarote.ai_crm.mapper.MailSyncLogMapper;
import com.kakarote.ai_crm.mapper.MailTemplateMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.IMailService;
import com.kakarote.ai_crm.service.support.SyncTaskExecutor;
import com.kakarote.ai_crm.utils.MailMimeParser;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import com.kakarote.ai_crm.utils.UserUtil;
import jakarta.annotation.PostConstruct;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MailServiceImpl extends ServiceImpl<MailAccountMapper, MailAccount> implements IMailService {

    private static final String PROVIDER_IMAP = "imap";
    private static final String PROVIDER_GMAIL = "gmail";
    private static final String PROVIDER_OUTLOOK = "outlook";
    private static final String AUTH_IMAP_PASSWORD = "imap_password";
    private static final String AUTH_OAUTH2 = "oauth2";
    private static final String BODY_SYNC_METADATA = "metadata";
    private static final String BODY_SYNC_SUMMARY = "summary";
    private static final String BODY_SYNC_FULL = "full";
    private static final String ATTACHMENT_SYNC_METADATA = "metadata";
    private static final String ATTACHMENT_SYNC_AUTO = "auto";
    private static final String ATTACHMENT_SYNC_FULL = "full";
    private static final String OAUTH_STATE_PREFIX = "mail:oauth:state:";
    private static final String MAIL_CREDENTIAL_KEY_REQUIRED_MESSAGE =
            "邮箱凭据加密密钥未配置，请设置 mail.integration.encryption-key 或 MAIL_CREDENTIAL_ENCRYPTION_KEY（至少16位）";
    private static final String MAIL_CREDENTIAL_ENCRYPT_FAILED_MESSAGE = "邮箱凭据加密失败，请检查邮箱加密密钥配置";
    private static final String MAIL_CREDENTIAL_DECRYPT_FAILED_MESSAGE =
            "邮箱凭据解密失败，请确认邮箱加密密钥与绑定邮箱时使用的密钥一致";
    private static final List<String> DEFAULT_IMAP_FOLDERS = List.of("INBOX", "Sent");
    private static final List<String> DEFAULT_GMAIL_FOLDERS = List.of("INBOX", "SENT");
    private static final List<String> DEFAULT_GRAPH_FOLDERS = List.of("inbox", "sentitems");
    private static final int IMAP_FETCH_BATCH_SIZE = 20;
    private static final Map<String, String> IMAP_CLIENT_ID = Map.of(
            "name", "AICRM",
            "version", "1.0.0",
            "vendor", "Kakarote"
    );
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("(20\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})");
    private static final Pattern CN_DATE_PATTERN = Pattern.compile("(\\d{1,2})月(\\d{1,2})日");
    private static final List<String> ACTION_HINTS = List.of(
            "请", "麻烦", "需要", "确认", "安排", "回复", "尽快", "截止", "提醒",
            "please", "need", "confirm", "reply", "schedule", "asap", "before", "by "
    );
    private static final List<String> INTENT_HINTS = List.of(
            "报价", "合同", "发票", "付款", "会议", "方案", "需求", "问题", "投诉",
            "quote", "contract", "invoice", "payment", "meeting", "proposal", "requirement", "issue", "complaint"
    );

    @Autowired
    private MailIntegrationProperties properties;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Autowired
    private MailMessageMapper mailMessageMapper;

    @Autowired
    private MailAttachmentMapper mailAttachmentMapper;

    @Autowired
    private MailSyncCursorMapper syncCursorMapper;

    @Autowired
    private MailSyncLogMapper syncLogMapper;

    @Autowired
    private MailDraftMapper mailDraftMapper;

    @Autowired
    private MailTemplateMapper mailTemplateMapper;

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private IKnowledgeService knowledgeService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SyncTaskExecutor syncTaskExecutor;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConcurrentHashMap<Long, ReentrantLock> accountSyncLocks = new ConcurrentHashMap<>();

    @PostConstruct
    void configureRestTemplateProxy() {
        MailIntegrationProperties.ProxyConfig proxyConfig = properties.getProxy();
        if (proxyConfig == null || !proxyConfig.isUsable()) {
            return;
        }
        URI proxyUri;
        try {
            proxyUri = parseProxyUri(proxyConfig.getUrl());
        } catch (IllegalArgumentException e) {
            log.warn("Mail HTTP proxy is ignored because url is invalid: {}", proxyConfig.getUrl());
            return;
        }
        String host = proxyUri.getHost();
        int port = proxyUri.getPort();
        if (StrUtil.isBlank(host) || port < 0) {
            log.warn("Mail HTTP proxy is ignored because url is invalid: {}", proxyConfig.getUrl());
            return;
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(new Proxy(resolveProxyType(proxyUri.getScheme()), new InetSocketAddress(host, port)));
        restTemplate.setRequestFactory(requestFactory);
        log.info("Mail HTTP proxy enabled: {}://{}:{}", StrUtil.blankToDefault(proxyUri.getScheme(), "http"), host, port);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailAccountVO bindImapAccount(MailImapBindBO bindBO) {
        Long userId = UserUtil.getUserId();
        String email = normalizeEmail(bindBO.getEmailAddress());
        String username = StrUtil.blankToDefault(bindBO.getUsername(), email);
        List<String> folders = normalizeFolders(bindBO.getFolders(), DEFAULT_IMAP_FOLDERS);

        if (Boolean.TRUE.equals(bindBO.getTestConnection())) {
            testImapConnection(bindBO.getImapHost(), resolveImapPort(bindBO.getImapPort()), Boolean.TRUE.equals(bindBO.getImapSsl()), username, bindBO.getPassword());
        }

        MailAccount account = findAccountByEmail(userId, email);
        if (account == null) {
            account = new MailAccount();
            account.setUserId(userId);
        }
        account.setProvider(PROVIDER_IMAP);
        account.setAuthType(AUTH_IMAP_PASSWORD);
        account.setEmailAddress(email);
        account.setDisplayName(StrUtil.trimToNull(bindBO.getDisplayName()));
        account.setImapHost(StrUtil.trim(bindBO.getImapHost()));
        account.setImapPort(resolveImapPort(bindBO.getImapPort()));
        account.setImapSsl(Boolean.TRUE.equals(bindBO.getImapSsl()));
        account.setSmtpHost(StrUtil.trimToNull(bindBO.getSmtpHost()));
        account.setSmtpPort(resolveSmtpPort(bindBO.getSmtpPort()));
        account.setSmtpSsl(Boolean.TRUE.equals(bindBO.getSmtpSsl()));
        account.setUsername(username);
        account.setCredentialJson(encryptCredentials(Map.of("password", bindBO.getPassword())));
        account.setFolders(String.join(",", folders));
        account.setSyncDays(resolveSyncDays(bindBO.getSyncDays()));
        account.setSyncLimit(resolveSyncLimit(bindBO.getSyncLimit()));
        account.setBodySyncMode(resolveBodySyncMode(bindBO.getBodySyncMode()));
        account.setAttachmentSyncMode(resolveAttachmentSyncMode(bindBO.getAttachmentSyncMode()));
        account.setMaxAutoAttachmentSize(resolveMaxAutoAttachmentSize(bindBO.getMaxAutoAttachmentSize()));
        account.setRetentionDays(resolveRetentionDays(bindBO.getRetentionDays()));
        account.setExtractActions(bindBO.getExtractActions() == null || Boolean.TRUE.equals(bindBO.getExtractActions()));
        account.setEnabled(true);
        account.setConnectionStatus("connected");
        account.setIsDefault(shouldBecomeDefaultAccount(userId, account.getAccountId()));
        account.setLastUsedTime(new Date());
        account.setLastSyncStatus("ready");
        account.setLastSyncError(null);
        saveOrUpdate(account);
        return toAccountVO(account);
    }

    @Override
    public void testImapConnection(MailImapBindBO bindBO) {
        String email = normalizeEmail(bindBO.getEmailAddress());
        String username = StrUtil.blankToDefault(bindBO.getUsername(), email);
        testImapConnection(bindBO.getImapHost(), resolveImapPort(bindBO.getImapPort()), Boolean.TRUE.equals(bindBO.getImapSsl()), username, bindBO.getPassword());
    }

    @Override
    public MailOAuthStartVO startOAuth(String provider) {
        Long userId = UserUtil.getUserId();
        String normalizedProvider = normalizeOAuthProvider(provider);
        MailIntegrationProperties.OAuthProvider oauthProvider = oauthProvider(normalizedProvider);
        requireOAuthConfig(normalizedProvider, oauthProvider);

        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(OAUTH_STATE_PREFIX + state,
                normalizedProvider + "|" + userId,
                10, TimeUnit.MINUTES);

        String authorizeUrl;
        if (PROVIDER_GMAIL.equals(normalizedProvider)) {
            authorizeUrl = UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                    .queryParam("client_id", oauthProvider.getClientId())
                    .queryParam("redirect_uri", oauthProvider.getRedirectUri())
                    .queryParam("response_type", "code")
                    .queryParam("access_type", "offline")
                    .queryParam("prompt", "consent")
                    .queryParam("scope", "https://www.googleapis.com/auth/gmail.readonly https://www.googleapis.com/auth/gmail.send")
                    .queryParam("state", state)
                    .build(true)
                    .toUriString();
        } else {
            authorizeUrl = UriComponentsBuilder.fromUriString(outlookOAuthUrl(oauthProvider, "authorize"))
                    .queryParam("client_id", oauthProvider.getClientId())
                    .queryParam("redirect_uri", oauthProvider.getRedirectUri())
                    .queryParam("response_type", "code")
                    .queryParam("scope", "offline_access Mail.Read Mail.Send User.Read")
                    .queryParam("state", state)
                    .build(true)
                    .toUriString();
        }
        return new MailOAuthStartVO(normalizedProvider, authorizeUrl, state);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailAccountVO handleOAuthCallback(String provider, String code, String state) {
        String normalizedProvider = normalizeOAuthProvider(provider);
        String stateValue = redisTemplate.opsForValue().get(OAUTH_STATE_PREFIX + state);
        if (StrUtil.isBlank(stateValue)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱授权状态已失效，请重新授权");
        }
        redisTemplate.delete(OAUTH_STATE_PREFIX + state);

        String[] parts = stateValue.split("\\|");
        if (parts.length != 2 || !Objects.equals(parts[0], normalizedProvider)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱授权状态不匹配");
        }
        Long userId = Long.valueOf(parts[1]);
        try {
            OAuthToken token = exchangeOAuthCode(normalizedProvider, code);
            OAuthProfile profile = fetchOAuthProfile(normalizedProvider, token.accessToken());
            MailAccount account = findAccountByEmail(userId, profile.emailAddress());
            if (account == null) {
                account = new MailAccount();
                account.setUserId(userId);
            }
            account.setProvider(normalizedProvider);
            account.setAuthType(AUTH_OAUTH2);
            account.setEmailAddress(profile.emailAddress());
            account.setDisplayName(profile.displayName());
            account.setCredentialJson(encryptCredentials(token.toCredentialMap()));
            account.setFolders(String.join(",", PROVIDER_GMAIL.equals(normalizedProvider) ? DEFAULT_GMAIL_FOLDERS : DEFAULT_GRAPH_FOLDERS));
            account.setSyncDays(properties.getDefaultSyncDays());
            account.setSyncLimit(properties.getDefaultSyncLimit());
            account.setBodySyncMode(resolveBodySyncMode(null));
            account.setAttachmentSyncMode(resolveAttachmentSyncMode(null));
            account.setMaxAutoAttachmentSize(resolveMaxAutoAttachmentSize(null));
            account.setRetentionDays(resolveRetentionDays(null));
            account.setExtractActions(true);
            account.setEnabled(true);
            account.setConnectionStatus("connected");
            account.setIsDefault(shouldBecomeDefaultAccount(userId, account.getAccountId()));
            account.setLastUsedTime(new Date());
            account.setLastSyncStatus("ready");
            account.setLastSyncError(null);
            saveOrUpdate(account);
            return toAccountVO(account);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱授权失败: " + e.getMessage());
        }
    }

    @Override
    public List<MailAccountVO> listAccounts() {
        Long userId = UserUtil.getUserId();
        return baseMapper.selectList(new LambdaQueryWrapper<MailAccount>()
                        .eq(MailAccount::getUserId, userId)
                        .orderByDesc(MailAccount::getCreateTime))
                .stream()
                .map(this::toAccountVO)
                .toList();
    }

    @Override
    public MailAuthStatusVO getAuthStatus() {
        List<MailAccountVO> accounts = listAccounts();
        MailAuthStatusVO statusVO = new MailAuthStatusVO();
        statusVO.setAccounts(accounts);
        statusVO.setAuthorized(accounts.stream().anyMatch(account -> Boolean.TRUE.equals(account.getEnabled())));
        statusVO.setCurrentAccount(accounts.stream()
                .filter(account -> Boolean.TRUE.equals(account.getEnabled()) && Boolean.TRUE.equals(account.getIsDefault()))
                .findFirst()
                .orElse(accounts.stream().filter(account -> Boolean.TRUE.equals(account.getEnabled())).findFirst().orElse(null)));
        return statusVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailAccountVO setDefaultAccount(Long accountId) {
        MailAccount account = loadUserAccount(accountId);
        if (!Boolean.TRUE.equals(account.getEnabled())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱账号已断开连接");
        }
        baseMapper.selectList(new LambdaQueryWrapper<MailAccount>()
                        .eq(MailAccount::getUserId, account.getUserId()))
                .forEach(item -> {
                    item.setIsDefault(Objects.equals(item.getAccountId(), accountId));
                    item.setLastUsedTime(Objects.equals(item.getAccountId(), accountId) ? new Date() : item.getLastUsedTime());
                    baseMapper.updateById(item);
                });
        account.setIsDefault(true);
        account.setLastUsedTime(new Date());
        return toAccountVO(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailAccountVO updateAccountPolicy(MailSyncPolicyBO policyBO) {
        MailAccount account = loadUserAccount(policyBO.getAccountId());
        if (policyBO.getFolders() != null) {
            List<String> defaults = PROVIDER_IMAP.equals(account.getProvider()) ? DEFAULT_IMAP_FOLDERS
                    : PROVIDER_GMAIL.equals(account.getProvider()) ? DEFAULT_GMAIL_FOLDERS : DEFAULT_GRAPH_FOLDERS;
            account.setFolders(String.join(",", normalizeFolders(policyBO.getFolders(), defaults)));
        }
        if (policyBO.getSyncDays() != null) {
            account.setSyncDays(resolveSyncDays(policyBO.getSyncDays()));
        }
        if (policyBO.getSyncLimit() != null) {
            account.setSyncLimit(resolveSyncLimit(policyBO.getSyncLimit()));
        }
        if (policyBO.getBodySyncMode() != null) {
            account.setBodySyncMode(resolveBodySyncMode(policyBO.getBodySyncMode()));
        }
        if (policyBO.getAttachmentSyncMode() != null) {
            account.setAttachmentSyncMode(resolveAttachmentSyncMode(policyBO.getAttachmentSyncMode()));
        }
        if (policyBO.getMaxAutoAttachmentSize() != null) {
            account.setMaxAutoAttachmentSize(resolveMaxAutoAttachmentSize(policyBO.getMaxAutoAttachmentSize()));
        }
        if (policyBO.getRetentionDays() != null) {
            account.setRetentionDays(resolveRetentionDays(policyBO.getRetentionDays()));
        }
        if (policyBO.getExtractActions() != null) {
            account.setExtractActions(policyBO.getExtractActions());
        }
        updateById(account);
        return toAccountVO(account);
    }

    @Override
    public MailSyncResultVO syncAccount(Long accountId) {
        MailAccount account = loadUserAccount(accountId);
        MailSyncLog syncLog = startSyncLog(account, "mailbox");
        markAccountSyncRunning(account);
        MailSyncResultVO result = buildRunningSyncResult(account, syncLog);
        log.debug("mail sync accepted: logId={}, accountId={}, email={}, provider={}",
                syncLog.getLogId(), account.getAccountId(), account.getEmailAddress(), account.getProvider());
        syncTaskExecutor.submit("mail-sync-" + account.getAccountId(),
                () -> syncAccountWithLock(account, syncLog));
        return result;
    }

    @Override
    @Scheduled(fixedDelayString = "${mail.integration.scheduler-fixed-delay-millis:600000}")
    public void syncDueAccounts() {
        if (!properties.isEnabled()) {
            return;
        }
        Date threshold = Date.from(Instant.now().minus(10, ChronoUnit.MINUTES));
        int maxConcurrentSync = resolveMaxConcurrentSync();
        List<MailAccount> accounts = baseMapper.selectList(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getEnabled, true)
                .and(wrapper -> wrapper.isNull(MailAccount::getLastSyncTime)
                        .or()
                        .lt(MailAccount::getLastSyncTime, threshold))
                .last("LIMIT " + maxConcurrentSync));
        for (MailAccount account : accounts) {
            try {
                MailSyncLog syncLog = startSyncLog(account, "scheduled");
                markAccountSyncRunning(account);
                syncTaskExecutor.submit("mail-scheduled-sync-" + account.getAccountId(),
                        () -> syncAccountWithLock(account, syncLog));
            } catch (Exception e) {
                log.warn("自动同步邮箱失败: accountId={}, error={}", account.getAccountId(), e.getMessage());
            }
        }
    }

    private int resolveMaxConcurrentSync() {
        int configured = properties.getMaxConcurrentSync();
        if (configured <= 0) {
            return 1;
        }
        return Math.min(configured, 50);
    }

    @Override
    public BasePage<MailMessageVO> queryMessages(MailMessageQueryBO queryBO) {
        BasePage<MailMessageVO> page = queryBO.parse();
        mailMessageMapper.queryPageList(page, queryBO, UserUtil.getUserId());
        return page;
    }

    @Override
    public MailMessageVO getMessageDetail(Long messageId) {
        MailMessageVO detail = mailMessageMapper.selectDetail(messageId, UserUtil.getUserId());
        if (detail == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮件不存在或无权限访问");
        }
        ensureMessageBody(detail);
        detail.setAttachments(mailAttachmentMapper.selectList(new LambdaQueryWrapper<MailAttachment>()
                        .eq(MailAttachment::getMessageId, messageId)
                        .orderByAsc(MailAttachment::getCreateTime))
                .stream()
                .map(this::toAttachmentVO)
                .toList());
        return detail;
    }

    /**
     * 详情打开时按需补全正文：summary/metadata 模式下未存 HTML 原文，
     * 首次打开时从邮箱服务商按 providerMessageId 拉取单封完整正文并回写缓存。
     */
    private void ensureMessageBody(MailMessageVO detail) {
        if (StrUtil.isNotBlank(detail.getBodyHtml())) {
            return;
        }
        if (!"received".equalsIgnoreCase(detail.getDirection())
                || StrUtil.isBlank(detail.getProviderMessageId())
                || BODY_SYNC_FULL.equalsIgnoreCase(detail.getBodySyncStatus())) {
            return;
        }
        MailAccount account = getById(detail.getAccountId());
        if (account == null) {
            return;
        }
        try {
            ResolvedBody body = fetchOriginalBody(account, detail.getProviderMessageId());
            if (body == null || (StrUtil.isBlank(body.text()) && StrUtil.isBlank(body.html()))) {
                return;
            }

            String text = StrUtil.maxLength(body.text(), 200_000);
            String html = StrUtil.maxLength(body.html(), 200_000);
            MailMessage update = new MailMessage();
            update.setMessageId(detail.getMessageId());
            if (StrUtil.isNotBlank(text)) {
                update.setBodyText(text);
            }
            if (StrUtil.isNotBlank(html)) {
                update.setBodyHtml(html);
            }
            update.setBodySyncStatus(BODY_SYNC_FULL);
            mailMessageMapper.updateById(update);

            if (StrUtil.isNotBlank(text)) {
                detail.setBodyText(text);
            }
            if (StrUtil.isNotBlank(html)) {
                detail.setBodyHtml(html);
            }
            detail.setBodySyncStatus(BODY_SYNC_FULL);
        } catch (Exception e) {
            log.warn("按需拉取邮件正文失败: messageId={}, providerMessageId={}, error={}",
                    detail.getMessageId(), detail.getProviderMessageId(), e.getMessage());
        }
    }

    private ResolvedBody fetchOriginalBody(MailAccount account, String providerMessageId) throws Exception {
        Map<String, Object> credentials = decryptCredentials(account);
        return switch (account.getProvider()) {
            case PROVIDER_IMAP -> fetchImapBody(account, credentials, providerMessageId);
            case PROVIDER_GMAIL -> fetchGmailBody(account, credentials, providerMessageId);
            case PROVIDER_OUTLOOK -> fetchGraphBody(account, credentials, providerMessageId);
            default -> null;
        };
    }

    private ResolvedBody fetchImapBody(MailAccount account, Map<String, Object> credentials,
                                       String providerMessageId) throws Exception {
        int idx = providerMessageId.lastIndexOf(':');
        if (idx <= 0 || idx >= providerMessageId.length() - 1) {
            return null;
        }
        String folderName = providerMessageId.substring(0, idx);
        long uid;
        try {
            uid = Long.parseLong(providerMessageId.substring(idx + 1));
        } catch (NumberFormatException e) {
            return null;
        }

        String password = asString(credentials.get("password"));
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.ssl.enable", String.valueOf(Boolean.TRUE.equals(account.getImapSsl())));
        props.put("mail.imap.connectiontimeout", "15000");
        props.put("mail.imap.timeout", "30000");
        Session session = Session.getInstance(props);
        try (Store store = session.getStore("imap")) {
            store.connect(account.getImapHost(), resolveImapPort(account.getImapPort()), account.getUsername(), password);
            sendImapClientId(store);
            Folder folder = store.getFolder(folderName);
            if (folder == null || !folder.exists() || !(folder instanceof UIDFolder uidFolder)) {
                return null;
            }
            folder.open(Folder.READ_ONLY);
            try {
                Message message = uidFolder.getMessageByUID(uid);
                if (!(message instanceof MimeMessage mimeMessage)) {
                    return null;
                }
                MailMimeParser.ParsedMail parsed = MailMimeParser.parse(mimeMessage);
                return new ResolvedBody(parsed.text(), parsed.html());
            } finally {
                folder.close(false);
            }
        }
    }

    private ResolvedBody fetchGmailBody(MailAccount account, Map<String, Object> credentials,
                                        String providerMessageId) throws Exception {
        String accessToken = refreshOAuthIfNeeded(account, credentials);
        JsonNode detail = getJson("https://gmail.googleapis.com/gmail/v1/users/me/messages/"
                + providerMessageId + "?format=raw", accessToken);
        String raw = detail.path("raw").asText();
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        byte[] rawBytes = Base64.getUrlDecoder().decode(raw);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(rawBytes));
        MailMimeParser.ParsedMail parsed = MailMimeParser.parse(mimeMessage);
        return new ResolvedBody(parsed.text(), parsed.html());
    }

    private ResolvedBody fetchGraphBody(MailAccount account, Map<String, Object> credentials,
                                        String providerMessageId) throws Exception {
        String accessToken = refreshOAuthIfNeeded(account, credentials);
        JsonNode node = getJson("https://graph.microsoft.com/v1.0/me/messages/"
                + providerMessageId + "?$select=body", accessToken);
        String bodyContent = node.path("body").path("content").asText(null);
        String bodyType = node.path("body").path("contentType").asText("");
        if (StrUtil.isBlank(bodyContent)) {
            return null;
        }
        if ("html".equalsIgnoreCase(bodyType)) {
            return new ResolvedBody(htmlToText(bodyContent), bodyContent);
        }
        return new ResolvedBody(bodyContent, null);
    }

    private record ResolvedBody(String text, String html) {
    }

    @Override
    public List<MailMessageVO> queryThread(Long messageId) {
        MailMessage message = loadUserMessage(messageId);
        return mailMessageMapper.selectThread(
                UserUtil.getUserId(),
                message.getAccountId(),
                message.getThreadId(),
                messageId
        );
    }

    @Override
    public List<MailMessageVO> queryCustomerTimeline(Long customerId, int limit) {
        if (customerId == null || customerMapper.selectById(customerId) == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
        }
        int actualLimit = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        return mailMessageMapper.selectCustomerTimeline(UserUtil.getUserId(), customerId, actualLimit);
    }

    @Override
    public List<MailSyncLogVO> listSyncLogs(Long accountId, int limit) {
        MailAccount account = loadUserAccount(accountId);
        int actualLimit = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 100));
        return syncLogMapper.selectList(new LambdaQueryWrapper<MailSyncLog>()
                        .eq(MailSyncLog::getAccountId, account.getAccountId())
                        .orderByDesc(MailSyncLog::getStartedAt)
                        .last("LIMIT " + actualLimit))
                .stream()
                .map(this::toSyncLogVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailAttachmentVO downloadAttachment(Long attachmentId) {
        MailAttachment attachment = mailAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "附件不存在");
        }
        MailMessage message = loadUserMessage(attachment.getMessageId());
        if (StrUtil.isNotBlank(attachment.getFilePath())) {
            return toAttachmentVO(attachment);
        }
        if (StrUtil.isBlank(message.getRawFilePath())) {
            attachment.setDownloadStatus("external_required");
            attachment.setDownloadError("当前同步策略未保留附件源文件，请使用附件自动/完整模式重新同步，或从源邮箱获取。");
            mailAttachmentMapper.updateById(attachment);
            return toAttachmentVO(attachment);
        }
        try (InputStream inputStream = fileStorageService.getFileStream(message.getRawFilePath())) {
            MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), inputStream);
            MailMimeParser.ParsedMail parsed = MailMimeParser.parse(mimeMessage);
            MailMimeParser.ParsedAttachment matched = parsed.attachments().stream()
                    .filter(item -> Objects.equals(item.fileName(), attachment.getFileName()))
                    .findFirst()
                    .orElse(null);
            if (matched == null || matched.bytes() == null || matched.bytes().length == 0) {
                attachment.setDownloadStatus("failed");
                attachment.setDownloadError("保留的原始邮件中未找到该附件。");
                mailAttachmentMapper.updateById(attachment);
                return toAttachmentVO(attachment);
            }
            String path = "mail/" + message.getAccountId() + "/attachments/"
                    + IdUtil.fastSimpleUUID() + "-" + safeFileName(matched.fileName());
            fileStorageService.upload(new ByteArrayInputStream(matched.bytes()), matched.bytes().length, path, matched.contentType());
            attachment.setFilePath(path);
            attachment.setFileSize((long) matched.bytes().length);
            attachment.setContentText(StrUtil.maxLength(matched.text(), 100_000));
            attachment.setDownloadStatus("downloaded");
            attachment.setScanStatus("pending");
            attachment.setDownloadError(null);
            mailAttachmentMapper.updateById(attachment);
            return toAttachmentVO(attachment);
        } catch (Exception e) {
            attachment.setDownloadStatus("failed");
            attachment.setDownloadError(StrUtil.maxLength(e.getMessage(), 1000));
            mailAttachmentMapper.updateById(attachment);
            return toAttachmentVO(attachment);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailDraftVO createDraft(MailDraftCreateBO draftBO) {
        MailAccount account = draftBO.getAccountId() != null ? loadUserAccount(draftBO.getAccountId()) : findDefaultAccount();
        if (draftBO.getCustomerId() != null && customerMapper.selectById(draftBO.getCustomerId()) == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
        }
        if (draftBO.getSourceMessageId() != null) {
            loadUserMessage(draftBO.getSourceMessageId());
        }
        MailDraft draft = new MailDraft();
        draft.setUserId(UserUtil.getUserId());
        draft.setAccountId(account == null ? null : account.getAccountId());
        draft.setCustomerId(draftBO.getCustomerId());
        draft.setContactId(draftBO.getContactId());
        draft.setSourceMessageId(draftBO.getSourceMessageId());
        draft.setToAddresses(StrUtil.maxLength(draftBO.getToAddresses(), 8000));
        draft.setCcAddresses(StrUtil.maxLength(draftBO.getCcAddresses(), 8000));
        draft.setBccAddresses(StrUtil.maxLength(draftBO.getBccAddresses(), 8000));
        draft.setSubject(StrUtil.maxLength(draftBO.getSubject(), 500));
        draft.setBodyText(StrUtil.maxLength(draftBO.getBodyText(), 200_000));
        draft.setAttachmentRefs(StrUtil.maxLength(draftBO.getAttachmentRefs(), 8000));
        draft.setStatus("draft");
        List<String> risks = detectDraftRisks(draft);
        draft.setRiskStatus(risks.isEmpty() ? "clear" : "pending_review");
        draft.setRiskReasons(risks.isEmpty() ? null : String.join("; ", risks));
        mailDraftMapper.insert(draft);
        return toDraftVO(draft);
    }

    @Override
    public BasePage<MailDraftVO> queryDrafts(MailDraftQueryBO queryBO) {
        BasePage<MailDraft> page = queryBO.parse();
        LambdaQueryWrapper<MailDraft> wrapper = new LambdaQueryWrapper<MailDraft>()
                .eq(MailDraft::getUserId, UserUtil.getUserId())
                .orderByDesc(MailDraft::getUpdateTime);
        if (queryBO.getAccountId() != null) {
            wrapper.eq(MailDraft::getAccountId, queryBO.getAccountId());
        }
        if (StrUtil.isNotBlank(queryBO.getStatus())) {
            wrapper.eq(MailDraft::getStatus, queryBO.getStatus());
        } else {
            wrapper.ne(MailDraft::getStatus, "deleted");
        }
        if (StrUtil.isNotBlank(queryBO.getKeyword())) {
            wrapper.and(item -> item.like(MailDraft::getSubject, queryBO.getKeyword())
                    .or()
                    .like(MailDraft::getToAddresses, queryBO.getKeyword())
                    .or()
                    .like(MailDraft::getBodyText, queryBO.getKeyword()));
        }
        mailDraftMapper.selectPage(page, wrapper);
        return page.copy(MailDraftVO.class, this::toDraftVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailDraftVO updateDraft(Long draftId, MailDraftCreateBO draftBO) {
        MailDraft draft = loadUserDraft(draftId);
        if (draftBO.getAccountId() != null) {
            loadUserAccount(draftBO.getAccountId());
            draft.setAccountId(draftBO.getAccountId());
        }
        draft.setCustomerId(draftBO.getCustomerId());
        draft.setContactId(draftBO.getContactId());
        draft.setSourceMessageId(draftBO.getSourceMessageId());
        draft.setToAddresses(StrUtil.maxLength(draftBO.getToAddresses(), 8000));
        draft.setCcAddresses(StrUtil.maxLength(draftBO.getCcAddresses(), 8000));
        draft.setBccAddresses(StrUtil.maxLength(draftBO.getBccAddresses(), 8000));
        draft.setSubject(StrUtil.maxLength(draftBO.getSubject(), 500));
        draft.setBodyText(StrUtil.maxLength(draftBO.getBodyText(), 200_000));
        draft.setAttachmentRefs(StrUtil.maxLength(draftBO.getAttachmentRefs(), 8000));
        draft.setStatus("draft");
        List<String> risks = detectDraftRisks(draft);
        draft.setRiskStatus(risks.isEmpty() ? "clear" : "pending_review");
        draft.setRiskReasons(risks.isEmpty() ? null : String.join("; ", risks));
        mailDraftMapper.updateById(draft);
        return toDraftVO(draft);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long draftId) {
        MailDraft draft = loadUserDraft(draftId);
        draft.setStatus("deleted");
        mailDraftMapper.updateById(draft);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailMessageVO sendMail(MailSendBO sendBO) {
        MailDraft draft;
        if (sendBO.getDraftId() != null) {
            draft = loadUserDraft(sendBO.getDraftId());
        } else if (sendBO.getDraft() != null) {
            MailDraftVO created = createDraft(sendBO.getDraft());
            draft = loadUserDraft(created.getDraftId());
        } else {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "草稿不能为空");
        }
        MailAccount account = draft.getAccountId() != null ? loadUserAccount(draft.getAccountId()) : findDefaultAccount();
        if (account == null || !Boolean.TRUE.equals(account.getEnabled())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "没有已连接的邮箱账号");
        }
        try {
            byte[] rawBytes = buildDraftRawBytes(account, draft);
            String providerMessageId = sendRawMail(account, draft, rawBytes);
            MailMessage message = saveSentMessage(account, draft, providerMessageId, rawBytes);
            draft.setStatus("sent");
            mailDraftMapper.updateById(draft);
            account.setLastUsedTime(new Date());
            updateById(account);
            return getMessageDetail(message.getMessageId());
        } catch (BusinessException e) {
            draft.setStatus("send_failed");
            draft.setRiskStatus("send_failed");
            draft.setRiskReasons(StrUtil.maxLength(e.getMessage(), 1000));
            mailDraftMapper.updateById(draft);
            throw e;
        } catch (Exception e) {
            draft.setStatus("send_failed");
            draft.setRiskStatus("send_failed");
            draft.setRiskReasons(StrUtil.maxLength(e.getMessage(), 1000));
            mailDraftMapper.updateById(draft);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮件发送失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMessageRead(Long messageId, boolean read) {
        MailMessage message = loadUserMessage(messageId);
        message.setReadStatus(read ? "read" : "unread");
        mailMessageMapper.updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void starMessage(Long messageId, boolean starred) {
        MailMessage message = loadUserMessage(messageId);
        message.setStarred(starred);
        mailMessageMapper.updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long messageId) {
        MailMessage message = loadUserMessage(messageId);
        message.setDeleted(true);
        mailMessageMapper.updateById(message);
    }

    @Override
    public BasePage<MailTemplateVO> queryTemplates(MailTemplateQueryBO queryBO) {
        BasePage<MailTemplate> page = queryBO.parse();
        LambdaQueryWrapper<MailTemplate> wrapper = new LambdaQueryWrapper<MailTemplate>()
                .eq(MailTemplate::getUserId, UserUtil.getUserId())
                .orderByDesc(MailTemplate::getIsCommon)
                .orderByDesc(MailTemplate::getUpdateTime);
        if (StrUtil.isNotBlank(queryBO.getCategory())) {
            wrapper.eq(MailTemplate::getCategory, queryBO.getCategory());
        }
        if (Boolean.TRUE.equals(queryBO.getCommonOnly())) {
            wrapper.eq(MailTemplate::getIsCommon, true);
        }
        if (StrUtil.isNotBlank(queryBO.getKeyword())) {
            wrapper.and(item -> item.like(MailTemplate::getName, queryBO.getKeyword())
                    .or()
                    .like(MailTemplate::getSubject, queryBO.getKeyword())
                    .or()
                    .like(MailTemplate::getBodyText, queryBO.getKeyword()));
        }
        mailTemplateMapper.selectPage(page, wrapper);
        return page.copy(MailTemplateVO.class, this::toTemplateVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailTemplateVO saveTemplate(MailTemplateSaveBO saveBO) {
        MailTemplate template = saveBO.getTemplateId() == null ? new MailTemplate() : loadUserTemplate(saveBO.getTemplateId());
        template.setUserId(UserUtil.getUserId());
        template.setName(StrUtil.maxLength(saveBO.getName(), 255));
        template.setCategory(StrUtil.blankToDefault(StrUtil.trimToNull(saveBO.getCategory()), "custom"));
        template.setSubject(StrUtil.maxLength(saveBO.getSubject(), 500));
        template.setBodyText(StrUtil.maxLength(saveBO.getBodyText(), 200_000));
        template.setVariables(StrUtil.maxLength(saveBO.getVariables(), 1000));
        template.setIsCommon(Boolean.TRUE.equals(saveBO.getIsCommon()));
        if (template.getTemplateId() == null) {
            mailTemplateMapper.insert(template);
        } else {
            mailTemplateMapper.updateById(template);
        }
        return toTemplateVO(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        MailTemplate template = loadUserTemplate(templateId);
        mailTemplateMapper.deleteById(template.getTemplateId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MailTemplateVO copyTemplate(Long templateId) {
        MailTemplate source = loadUserTemplate(templateId);
        MailTemplate copy = new MailTemplate();
        copy.setUserId(source.getUserId());
        copy.setName(StrUtil.maxLength(source.getName() + " 副本", 255));
        copy.setCategory(source.getCategory());
        copy.setSubject(source.getSubject());
        copy.setBodyText(source.getBodyText());
        copy.setVariables(source.getVariables());
        copy.setIsCommon(false);
        mailTemplateMapper.insert(copy);
        return toTemplateVO(copy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void associateCustomer(MailAssociateCustomerBO associateBO) {
        MailMessage message = loadUserMessage(associateBO.getMessageId());
        if (associateBO.getCustomerId() != null && customerMapper.selectById(associateBO.getCustomerId()) == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
        }
        message.setCustomerId(associateBO.getCustomerId());
        mailMessageMapper.updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disconnectAccount(Long accountId) {
        MailAccount account = loadUserAccount(accountId);
        account.setEnabled(false);
        account.setCredentialJson(null);
        account.setConnectionStatus("disconnected");
        boolean wasDefault = Boolean.TRUE.equals(account.getIsDefault());
        account.setIsDefault(false);
        account.setLastSyncStatus("disconnected");
        updateById(account);
        if (wasDefault) {
            MailAccount next = baseMapper.selectOne(new LambdaQueryWrapper<MailAccount>()
                    .eq(MailAccount::getUserId, account.getUserId())
                    .eq(MailAccount::getEnabled, true)
                    .orderByDesc(MailAccount::getLastUsedTime)
                    .last("LIMIT 1"));
            if (next != null) {
                next.setIsDefault(true);
                next.setLastUsedTime(new Date());
                updateById(next);
            }
        }
    }

    private byte[] buildDraftRawBytes(MailAccount account, MailDraft draft) throws Exception {
        Session session = Session.getInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        InternetAddress from = StrUtil.isBlank(account.getDisplayName())
                ? new InternetAddress(account.getEmailAddress())
                : new InternetAddress(account.getEmailAddress(), account.getDisplayName(), StandardCharsets.UTF_8.name());
        message.setFrom(from);
        message.setRecipients(Message.RecipientType.TO, parseMailAddresses(draft.getToAddresses()));
        if (StrUtil.isNotBlank(draft.getCcAddresses())) {
            message.setRecipients(Message.RecipientType.CC, parseMailAddresses(draft.getCcAddresses()));
        }
        if (StrUtil.isNotBlank(draft.getBccAddresses())) {
            message.setRecipients(Message.RecipientType.BCC, parseMailAddresses(draft.getBccAddresses()));
        }
        message.setSubject(StrUtil.blankToDefault(draft.getSubject(), "(No subject)"), StandardCharsets.UTF_8.name());
        message.setSentDate(new Date());
        String body = StrUtil.blankToDefault(draft.getBodyText(), "");
        if (looksLikeHtml(body)) {
            message.setContent(body, "text/html; charset=UTF-8");
        } else {
            message.setText(body, StandardCharsets.UTF_8.name());
        }
        message.saveChanges();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);
        return outputStream.toByteArray();
    }

    private String sendRawMail(MailAccount account, MailDraft draft, byte[] rawBytes) throws Exception {
        Map<String, Object> credentials = decryptCredentials(account);
        return switch (account.getProvider()) {
            case PROVIDER_GMAIL -> sendGmail(account, rawBytes, credentials);
            case PROVIDER_OUTLOOK -> sendGraph(account, draft, credentials);
            case PROVIDER_IMAP -> sendSmtp(account, rawBytes, credentials);
            default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "不支持的邮箱服务商");
        };
    }

    private String sendGmail(MailAccount account, byte[] rawBytes, Map<String, Object> credentials) throws Exception {
        String accessToken = refreshOAuthIfNeeded(account, credentials);
        Map<String, String> body = Map.of("raw", Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes));
        JsonNode response = postJson("https://gmail.googleapis.com/gmail/v1/users/me/messages/send", accessToken, body);
        return StrUtil.blankToDefault(response.path("id").asText(null), "gmail-" + IdUtil.fastSimpleUUID());
    }

    private String sendGraph(MailAccount account, MailDraft draft, Map<String, Object> credentials) throws Exception {
        String accessToken = refreshOAuthIfNeeded(account, credentials);
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("subject", StrUtil.blankToDefault(draft.getSubject(), ""));
        String body = StrUtil.blankToDefault(draft.getBodyText(), "");
        message.put("body", Map.of(
                "contentType", looksLikeHtml(body) ? "HTML" : "Text",
                "content", body
        ));
        message.put("toRecipients", toGraphRecipients(draft.getToAddresses()));
        if (StrUtil.isNotBlank(draft.getCcAddresses())) {
            message.put("ccRecipients", toGraphRecipients(draft.getCcAddresses()));
        }
        if (StrUtil.isNotBlank(draft.getBccAddresses())) {
            message.put("bccRecipients", toGraphRecipients(draft.getBccAddresses()));
        }
        postJsonNoResponse("https://graph.microsoft.com/v1.0/me/sendMail", accessToken, Map.of(
                "message", message,
                "saveToSentItems", true
        ));
        return "graph-" + IdUtil.fastSimpleUUID();
    }

    private String sendSmtp(MailAccount account, byte[] rawBytes, Map<String, Object> credentials) throws Exception {
        String password = asString(credentials.get("password"));
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱授权已过期，请重新连接");
        }
        String smtpHost = resolveSmtpHost(account);
        int smtpPort = resolveSmtpPort(account.getSmtpPort());
        boolean ssl = account.getSmtpSsl() == null || Boolean.TRUE.equals(account.getSmtpSsl());

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.starttls.enable", String.valueOf(!ssl));
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "30000");
        Session session = Session.getInstance(props);
        MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(rawBytes));
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(smtpHost, smtpPort, StrUtil.blankToDefault(account.getUsername(), account.getEmailAddress()), password);
            transport.sendMessage(message, message.getAllRecipients());
        }
        return "smtp-" + IdUtil.fastSimpleUUID();
    }

    private MailMessage saveSentMessage(MailAccount account, MailDraft draft, String providerMessageId, byte[] rawBytes) {
        String rawPath = null;
        if (rawBytes != null && rawBytes.length > 0) {
            rawPath = "mail/" + account.getAccountId() + "/sent/" + IdUtil.fastSimpleUUID() + ".eml";
            fileStorageService.upload(new ByteArrayInputStream(rawBytes), rawBytes.length, rawPath, "message/rfc822");
        }
        String body = StrUtil.blankToDefault(draft.getBodyText(), "");
        String bodyText = looksLikeHtml(body) ? htmlToText(body) : body;
        MailMessage message = new MailMessage();
        message.setAccountId(account.getAccountId());
        message.setUserId(account.getUserId());
        message.setProvider(account.getProvider());
        message.setProviderMessageId(providerMessageId);
        message.setInternetMessageId(providerMessageId);
        message.setFolder("SENT");
        message.setDirection("sent");
        message.setSubject(StrUtil.maxLength(draft.getSubject(), 500));
        message.setFromName(StrUtil.maxLength(account.getDisplayName(), 255));
        message.setFromAddress(StrUtil.maxLength(account.getEmailAddress(), 255));
        message.setToAddresses(StrUtil.maxLength(draft.getToAddresses(), 8000));
        message.setCcAddresses(StrUtil.maxLength(draft.getCcAddresses(), 8000));
        message.setBccAddresses(StrUtil.maxLength(draft.getBccAddresses(), 8000));
        message.setSentTime(new Date());
        message.setReceivedTime(new Date());
        message.setBodySyncMode(BODY_SYNC_FULL);
        message.setBodySyncStatus(BODY_SYNC_FULL);
        message.setSummary(StrUtil.maxLength(bodyText, 4000));
        message.setBodyText(StrUtil.maxLength(bodyText, 200_000));
        message.setBodyHtml(looksLikeHtml(body) ? StrUtil.maxLength(body, 200_000) : null);
        message.setRawFilePath(rawPath);
        message.setRawFileSize(rawBytes == null ? 0L : (long) rawBytes.length);
        message.setHasAttachments(StrUtil.isNotBlank(draft.getAttachmentRefs()));
        message.setReadStatus("read");
        message.setStarred(false);
        message.setDeleted(false);
        message.setCustomerId(draft.getCustomerId());
        message.setContactId(draft.getContactId());
        message.setSyncStatus("sent_success");
        mailMessageMapper.insert(message);
        return message;
    }

    private InternetAddress[] parseMailAddresses(String addresses) throws Exception {
        if (StrUtil.isBlank(addresses)) {
            return new InternetAddress[0];
        }
        InternetAddress[] parsed = InternetAddress.parse(addresses, false);
        if (parsed.length == 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "收件人地址不能为空");
        }
        return parsed;
    }

    private List<Map<String, Object>> toGraphRecipients(String addresses) throws Exception {
        List<Map<String, Object>> recipients = new ArrayList<>();
        for (InternetAddress address : parseMailAddresses(addresses)) {
            Map<String, Object> emailAddress = new LinkedHashMap<>();
            emailAddress.put("address", address.getAddress());
            if (StrUtil.isNotBlank(address.getPersonal())) {
                emailAddress.put("name", address.getPersonal());
            }
            recipients.add(Map.of("emailAddress", emailAddress));
        }
        return recipients;
    }

    private boolean looksLikeHtml(String body) {
        return StrUtil.isNotBlank(body) && Pattern.compile("<\\s*(p|br|div|span|table|ul|ol|li|strong|b|em|i|a)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(body)
                .find();
    }

    private JsonNode postJson(String url, String accessToken, Object body) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            String responseBody = response.getBody();
            return StrUtil.isBlank(responseBody) ? objectMapper.createObjectNode() : objectMapper.readTree(responseBody);
        } catch (HttpClientErrorException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮件服务商 API 调用失败: " + e.getStatusCode());
        }
    }

    private void postJsonNoResponse(String url, String accessToken, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Void.class);
        } catch (HttpClientErrorException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮件服务商 API 调用失败: " + e.getStatusCode());
        }
    }

    private MailSyncResultVO syncAccountInternal(MailAccount account) {
        return syncAccountInternal(account, null);
    }

    private MailSyncResultVO syncAccountInternal(MailAccount account, MailSyncLog existingSyncLog) {
        MailSyncResultVO result = new MailSyncResultVO();
        result.setAccountId(account.getAccountId());
        MailSyncLog syncLog = existingSyncLog == null ? startSyncLog(account, "mailbox") : existingSyncLog;
        result.setLogId(syncLog.getLogId());
        try {
            Map<String, Object> credentials = decryptCredentials(account);
            if (PROVIDER_IMAP.equals(account.getProvider())) {
                syncImapMessages(account, credentials, syncLog, result);
            } else {
                List<SyncedMail> messages = switch (account.getProvider()) {
                    case PROVIDER_GMAIL -> fetchGmailMessages(account, credentials, syncLog);
                    case PROVIDER_OUTLOOK -> fetchGraphMessages(account, credentials, syncLog);
                    default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "不支持的邮箱服务商");
                };
                processFetchedMessages(account, syncLog, result, messages);
            }
            account.setLastSyncTime(new Date());
            account.setLastSyncStatus(result.getFailedCount() > 0 ? "partial" : "success");
            account.setLastSyncError(null);
            result.setStatus(account.getLastSyncStatus());
            updateById(account);
            finishSyncLog(syncLog, result);
            return result;
        } catch (Exception e) {
            account.setLastSyncTime(new Date());
            account.setLastSyncStatus("failed");
            account.setLastSyncError(StrUtil.maxLength(e.getMessage(), 1000));
            updateById(account);
            result.setStatus("failed");
            result.setErrorMessage(e.getMessage());
            finishSyncLog(syncLog, result);
            return result;
        }
    }

    private MailSyncResultVO syncAccountWithLock(MailAccount account) {
        return syncAccountWithLock(account, null);
    }

    private MailSyncResultVO syncAccountWithLock(MailAccount account, MailSyncLog syncLog) {
        if (account == null || account.getAccountId() == null) {
            return syncAccountInternal(account, syncLog);
        }
        ReentrantLock lock = accountSyncLocks.computeIfAbsent(account.getAccountId(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            return syncAccountInternal(account, syncLog);
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                accountSyncLocks.remove(account.getAccountId(), lock);
            }
        }
    }

    private boolean saveSyncedMail(MailAccount account, SyncedMail syncedMail) {
        Long existing = mailMessageMapper.selectCount(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getAccountId, account.getAccountId())
                .eq(MailMessage::getProviderMessageId, syncedMail.providerMessageId()));
        if (existing != null && existing > 0) {
            return false;
        }

        MatchResult match = matchContactAndCustomer(syncedMail.fromAddress());
        String bodySyncMode = resolveBodySyncMode(account.getBodySyncMode());
        String attachmentSyncMode = resolveAttachmentSyncMode(account.getAttachmentSyncMode());
        MailExtraction extraction = extractMail(syncedMail, Boolean.TRUE.equals(account.getExtractActions()));
        byte[] rawBytes = syncedMail.rawBytes() == null ? buildGeneratedRaw(syncedMail) : syncedMail.rawBytes();
        boolean retainRaw = BODY_SYNC_FULL.equals(bodySyncMode) || ATTACHMENT_SYNC_FULL.equals(attachmentSyncMode);
        String rawPath = null;
        if (retainRaw && rawBytes != null && rawBytes.length > 0) {
            rawPath = "mail/" + account.getAccountId() + "/" + IdUtil.fastSimpleUUID() + ".eml";
            fileStorageService.upload(new ByteArrayInputStream(rawBytes), rawBytes.length, rawPath, "message/rfc822");
        }

        MailMessage message = new MailMessage();
        message.setAccountId(account.getAccountId());
        message.setUserId(account.getUserId());
        message.setProvider(account.getProvider());
        message.setProviderMessageId(syncedMail.providerMessageId());
        message.setInternetMessageId(syncedMail.internetMessageId());
        message.setThreadId(syncedMail.threadId());
        message.setFolder(syncedMail.folder());
        message.setDirection(resolveDirection(syncedMail.folder()));
        message.setSubject(StrUtil.maxLength(syncedMail.subject(), 500));
        message.setFromName(StrUtil.maxLength(syncedMail.fromName(), 255));
        message.setFromAddress(StrUtil.maxLength(normalizeEmailOrNull(syncedMail.fromAddress()), 255));
        message.setToAddresses(StrUtil.maxLength(syncedMail.toAddresses(), 8000));
        message.setCcAddresses(StrUtil.maxLength(syncedMail.ccAddresses(), 8000));
        message.setSentTime(syncedMail.sentTime());
        message.setReceivedTime(syncedMail.receivedTime());
        message.setBodySyncMode(bodySyncMode);
        message.setBodySyncStatus(bodySyncMode);
        message.setSummary(StrUtil.maxLength(extraction.summary(), 4000));
        message.setKeywords(StrUtil.maxLength(extraction.keywords(), 1000));
        message.setIntent(StrUtil.maxLength(extraction.intent(), 255));
        message.setActionItemsJson(StrUtil.maxLength(extraction.actionItemsJson(), 8000));
        message.setReplyDeadlineTime(extraction.replyDeadlineTime());
        message.setExtractionStatus(extraction.status());
        message.setExtractionError(extraction.error());
        if (BODY_SYNC_FULL.equals(bodySyncMode)) {
            message.setBodyText(StrUtil.maxLength(syncedMail.bodyText(), 200_000));
            message.setBodyHtml(StrUtil.maxLength(syncedMail.bodyHtml(), 200_000));
        } else if (BODY_SYNC_SUMMARY.equals(bodySyncMode)) {
            message.setBodyText(StrUtil.maxLength(extraction.summary(), 4000));
            message.setBodyHtml(null);
        } else {
            message.setBodyText(null);
            message.setBodyHtml(null);
        }
        message.setRawFilePath(rawPath);
        message.setRawFileSize(rawPath == null || rawBytes == null ? 0L : (long) rawBytes.length);
        message.setHasAttachments(!syncedMail.attachments().isEmpty());
        message.setReadStatus("sent".equalsIgnoreCase(resolveDirection(syncedMail.folder())) ? "read" : "unread");
        message.setStarred(false);
        message.setDeleted(false);
        message.setCustomerId(match.customerId());
        message.setContactId(match.contactId());
        message.setSyncStatus("success");
        mailMessageMapper.insert(message);

        saveAttachments(account, message, syncedMail.attachments());
        String knowledgeText = buildKnowledgeText(message);
        if (StrUtil.isNotBlank(knowledgeText)) {
            archiveMailKnowledgeAsync(account, message, knowledgeText);
        }
        return true;
    }

    private void processFetchedMessages(MailAccount account, MailSyncLog syncLog, MailSyncResultVO result,
                                        List<SyncedMail> messages) {
        result.setFetchedCount(messages.size());
        int processedCount = 0;
        for (SyncedMail syncedMail : messages) {
            processedCount++;
            processSyncedMail(account, syncLog, result, syncedMail, processedCount, messages.size());
        }
    }

    private boolean processSyncedMail(MailAccount account, MailSyncLog syncLog, MailSyncResultVO result,
                                      SyncedMail syncedMail, int processedCount, int totalCount) {
        boolean processed = true;
        String messageRef = buildMessageLogRef(syncedMail, processedCount, totalCount);
        try {
            logSyncStage(syncLog, account, "save.start", messageRef);
            if (saveSyncedMail(account, syncedMail)) {
                result.setSavedCount(result.getSavedCount() + 1);
                logSyncStage(syncLog, account, "save.inserted", messageRef);
            } else {
                result.setSkippedCount(result.getSkippedCount() + 1);
                logSyncStage(syncLog, account, "save.skipped", messageRef);
            }
        } catch (Exception e) {
            result.setFailedCount(result.getFailedCount() + 1);
            log.warn("保存同步邮件失败: accountId={}, providerMessageId={}, error={}",
                    account.getAccountId(), syncedMail.providerMessageId(), e.getMessage());
            logSyncStage(syncLog, account, "save.failed", messageRef + ", error=" + e.getMessage());
            processed = false;
        }
        if (processedCount == 1 || processedCount % IMAP_FETCH_BATCH_SIZE == 0 || processedCount == totalCount) {
            logSyncStage(syncLog, account, "save.progress",
                    "Processed messages=" + processedCount + "/" + totalCount
                            + ", fetched=" + result.getFetchedCount()
                            + ", saved=" + result.getSavedCount()
                            + ", skipped=" + result.getSkippedCount()
                            + ", failed=" + result.getFailedCount());
        }
        return processed;
    }

    private String buildMessageLogRef(SyncedMail syncedMail, int processedCount, int totalCount) {
        return "index=" + processedCount + "/" + totalCount
                + ", providerMessageId=" + syncedMail.providerMessageId()
                + ", subject=" + StrUtil.maxLength(StrUtil.blankToDefault(syncedMail.subject(), ""), 120);
    }

    private void archiveMailKnowledgeAsync(MailAccount account, MailMessage message, String knowledgeText) {
        Long accountId = account == null ? null : account.getAccountId();
        Long userId = account == null ? null : account.getUserId();
        Long messageId = message.getMessageId();
        String fileName = buildKnowledgeFileName(message);
        String subject = StrUtil.maxLength(message.getSubject(), 500);
        Long customerId = message.getCustomerId();
        syncTaskExecutor.submit("mail-knowledge-archive-" + messageId, () -> {
            if (userId != null) {
                AiContextHolder.bindThreadContext(messageId, userId);
            }
            try {
                log.info("mail knowledge archive start: accountId={}, messageId={}, subject={}",
                        accountId, messageId, subject);
                Long knowledgeId = knowledgeService.archiveText(fileName, knowledgeText, "email", customerId, subject);
                MailMessage update = new MailMessage();
                update.setMessageId(messageId);
                update.setKnowledgeId(knowledgeId);
                mailMessageMapper.updateById(update);
                log.info("mail knowledge archive finished: accountId={}, messageId={}, knowledgeId={}",
                        accountId, messageId, knowledgeId);
            } catch (Exception e) {
                log.warn("mail knowledge archive failed: accountId={}, messageId={}, error={}",
                        accountId, messageId, e.getMessage(), e);
            } finally {
                AiContextHolder.clearThreadContext();
            }
        });
    }

    private void saveAttachments(MailAccount account, MailMessage message, List<SyncedAttachment> attachments) {
        String syncMode = resolveAttachmentSyncMode(account.getAttachmentSyncMode());
        long maxAutoSize = resolveMaxAutoAttachmentSize(account.getMaxAutoAttachmentSize());
        for (SyncedAttachment attachment : attachments) {
            byte[] bytes = attachment.bytes();
            String path = null;
            boolean shouldStore = shouldStoreAttachment(syncMode, bytes, maxAutoSize);
            if (shouldStore && bytes != null && bytes.length > 0) {
                path = "mail/" + account.getAccountId() + "/attachments/"
                        + IdUtil.fastSimpleUUID() + "-" + safeFileName(attachment.fileName());
                fileStorageService.upload(new ByteArrayInputStream(bytes), bytes.length, path, attachment.contentType());
            }
            MailAttachment entity = new MailAttachment();
            entity.setMessageId(message.getMessageId());
            entity.setProviderAttachmentId(StrUtil.maxLength(attachment.providerAttachmentId(), 500));
            entity.setFileName(StrUtil.maxLength(attachment.fileName(), 500));
            entity.setContentType(StrUtil.maxLength(attachment.contentType(), 255));
            entity.setFileSize(bytes == null ? 0L : (long) bytes.length);
            entity.setFilePath(path);
            entity.setContentText(shouldStore ? StrUtil.maxLength(attachment.text(), 100_000) : null);
            entity.setSyncMode(syncMode);
            entity.setDownloadStatus(path == null ? resolveAttachmentMetadataStatus(syncMode, bytes, maxAutoSize) : "downloaded");
            entity.setScanStatus(path == null ? "not_scanned" : "pending");
            mailAttachmentMapper.insert(entity);
        }
    }

    private void syncImapMessages(MailAccount account, Map<String, Object> credentials,
                                  MailSyncLog syncLog, MailSyncResultVO result) throws Exception {
        String password = asString(credentials.get("password"));
        List<String> folders = normalizeFolders(splitFolders(account.getFolders()), DEFAULT_IMAP_FOLDERS);
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.ssl.enable", String.valueOf(Boolean.TRUE.equals(account.getImapSsl())));
        props.put("mail.imap.connectiontimeout", "15000");
        props.put("mail.imap.timeout", "30000");
        Session session = Session.getInstance(props);
        logSyncStage(syncLog, account, "imap.connect",
                "Connecting host=" + account.getImapHost() + ", port=" + account.getImapPort()
                        + ", ssl=" + Boolean.TRUE.equals(account.getImapSsl()) + ", folders=" + folders);
        try (Store store = session.getStore("imap")) {
            store.connect(account.getImapHost(), account.getImapPort(), account.getUsername(), password);
            sendImapClientId(store);
            logSyncStage(syncLog, account, "imap.connected", "IMAP store connected");
            for (String folderName : folders) {
                logSyncStage(syncLog, account, "imap.folder.lookup", "Looking up folder=" + folderName);
                Folder folder = store.getFolder(folderName);
                if (folder == null || !folder.exists()) {
                    logSyncStage(syncLog, account, "imap.folder.missing", "Folder not found=" + folderName);
                    continue;
                }
                logSyncStage(syncLog, account, "imap.folder.open", "Opening folder=" + folderName);
                folder.open(Folder.READ_ONLY);
                try {
                    logSyncStage(syncLog, account, "imap.folder.candidates", "Resolving candidates for folder=" + folderName);
                    Message[] messages = resolveImapCandidates(account, folder, folderName);
                    logSyncStage(syncLog, account, "imap.folder.candidates.done",
                            "Folder=" + folderName + ", candidates=" + messages.length);

                    FetchProfile profile = new FetchProfile();
                    profile.add(FetchProfile.Item.ENVELOPE);
                    profile.add(FetchProfile.Item.CONTENT_INFO);
                    long maxUid = 0L;
                    UIDFolder uidFolder = folder instanceof UIDFolder value ? value : null;
                    int parsedCount = 0;
                    boolean folderHadFailure = false;
                    int totalBatches = (messages.length + IMAP_FETCH_BATCH_SIZE - 1) / IMAP_FETCH_BATCH_SIZE;
                    for (int start = 0; start < messages.length; start += IMAP_FETCH_BATCH_SIZE) {
                        int end = Math.min(start + IMAP_FETCH_BATCH_SIZE, messages.length);
                        Message[] batch = new Message[end - start];
                        System.arraycopy(messages, start, batch, 0, batch.length);
                        int batchNumber = start / IMAP_FETCH_BATCH_SIZE + 1;
                        logSyncStage(syncLog, account, "imap.batch.fetch_profile",
                                "Fetching envelope/profile folder=" + folderName
                                        + ", batch=" + batchNumber + "/" + totalBatches
                                        + ", range=" + (start + 1) + "-" + end + "/" + messages.length
                                        + ", batchSize=" + batch.length);
                        folder.fetch(batch, profile);
                        logSyncStage(syncLog, account, "imap.batch.fetch_profile.done",
                                "Fetched envelope/profile folder=" + folderName
                                        + ", batch=" + batchNumber + "/" + totalBatches);
                        for (Message message : batch) {
                            if (!(message instanceof MimeMessage mimeMessage)) {
                                continue;
                            }
                            long uid = uidFolder == null ? message.getMessageNumber() : uidFolder.getUID(message);
                            maxUid = Math.max(maxUid, uid);
                            parsedCount++;
                            if (parsedCount == 1 || parsedCount % IMAP_FETCH_BATCH_SIZE == 0
                                    || parsedCount == messages.length) {
                                logSyncStage(syncLog, account, "imap.message.parse",
                                        "Parsing folder=" + folderName + ", index=" + parsedCount + "/" + messages.length
                                                + ", uid=" + uid);
                            }
                            try {
                                SyncedMail syncedMail = toSyncedMail(folderName, uid, mimeMessage);
                                result.setFetchedCount(result.getFetchedCount() + 1);
                                if (!processSyncedMail(account, syncLog, result, syncedMail, parsedCount, messages.length)) {
                                    folderHadFailure = true;
                                }
                            } catch (Exception e) {
                                folderHadFailure = true;
                                result.setFailedCount(result.getFailedCount() + 1);
                                log.warn("解析同步邮件失败: accountId={}, folder={}, uid={}, error={}",
                                        account.getAccountId(), folderName, uid, e.getMessage());
                                logSyncStage(syncLog, account, "imap.message.parse.failed",
                                        "Parse failed folder=" + folderName + ", uid=" + uid + ", error=" + e.getMessage());
                            }
                        }
                    }
                    if (maxUid > 0 && !folderHadFailure) {
                        logSyncStage(syncLog, account, "imap.cursor.update",
                                "Updating cursor folder=" + folderName + ", maxUid=" + maxUid);
                        upsertCursor(account, folderName, "imap_uid", String.valueOf(maxUid), maxUid, null, null);
                    } else if (folderHadFailure) {
                        logSyncStage(syncLog, account, "imap.cursor.skip",
                                "Skip cursor update because folder had failed messages, folder=" + folderName
                                        + ", maxUid=" + maxUid);
                    }
                    logSyncStage(syncLog, account, "imap.folder.done",
                            "Folder=" + folderName + " done, parsed=" + parsedCount
                                    + ", totalFetched=" + result.getFetchedCount());
                } finally {
                    logSyncStage(syncLog, account, "imap.folder.close", "Closing folder=" + folderName);
                    folder.close(false);
                }
            }
        }
        logSyncStage(syncLog, account, "imap.done", "IMAP fetch done, messages=" + result.getFetchedCount());
    }

    private List<SyncedMail> fetchImapMessages(MailAccount account, Map<String, Object> credentials) throws Exception {
        String password = asString(credentials.get("password"));
        List<String> folders = normalizeFolders(splitFolders(account.getFolders()), DEFAULT_IMAP_FOLDERS);
        List<SyncedMail> result = new ArrayList<>();
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.ssl.enable", String.valueOf(Boolean.TRUE.equals(account.getImapSsl())));
        props.put("mail.imap.connectiontimeout", "15000");
        props.put("mail.imap.timeout", "30000");
        Session session = Session.getInstance(props);
        try (Store store = session.getStore("imap")) {
            store.connect(account.getImapHost(), account.getImapPort(), account.getUsername(), password);
            sendImapClientId(store);
            for (String folderName : folders) {
                Folder folder = store.getFolder(folderName);
                if (folder == null || !folder.exists()) {
                    continue;
                }
                folder.open(Folder.READ_ONLY);
                try {
                    Message[] messages = resolveImapCandidates(account, folder, folderName);
                    FetchProfile profile = new FetchProfile();
                    profile.add(FetchProfile.Item.ENVELOPE);
                    profile.add(FetchProfile.Item.CONTENT_INFO);
                    folder.fetch(messages, profile);
                    long maxUid = 0L;
                    UIDFolder uidFolder = folder instanceof UIDFolder value ? value : null;
                    for (Message message : messages) {
                        if (!(message instanceof MimeMessage mimeMessage)) {
                            continue;
                        }
                        long uid = uidFolder == null ? message.getMessageNumber() : uidFolder.getUID(message);
                        maxUid = Math.max(maxUid, uid);
                        result.add(toSyncedMail(folderName, uid, mimeMessage));
                    }
                    if (maxUid > 0) {
                        upsertCursor(account, folderName, "imap_uid", String.valueOf(maxUid), maxUid, null, null);
                    }
                } finally {
                    folder.close(false);
                }
            }
        }
        return result;
    }

    private Message[] resolveImapCandidates(MailAccount account, Folder folder, String folderName) throws Exception {
        UIDFolder uidFolder = folder instanceof UIDFolder value ? value : null;
        MailSyncCursor cursor = findCursor(account.getAccountId(), folderName);
        Message[] messages;
        if (uidFolder != null && cursor != null && cursor.getLastUid() != null && cursor.getLastUid() > 0) {
            messages = uidFolder.getMessagesByUID(cursor.getLastUid() + 1, UIDFolder.LASTUID);
        } else {
            Date since = Date.from(Instant.now().minus(resolveSyncDays(account.getSyncDays()), ChronoUnit.DAYS));
            SearchTerm searchTerm = new ReceivedDateTerm(ReceivedDateTerm.GE, since);
            messages = folder.search(searchTerm);
        }
        int limit = resolveSyncLimit(account.getSyncLimit());
        if (messages.length <= limit) {
            return messages;
        }
        Message[] limited = new Message[limit];
        System.arraycopy(messages, messages.length - limit, limited, 0, limit);
        return limited;
    }

    private SyncedMail toSyncedMail(String folderName, long uid, MimeMessage message) throws Exception {
        MailMimeParser.ParsedMail parsed = MailMimeParser.parse(message);
        List<SyncedAttachment> attachments = parsed.attachments().stream()
                .map(item -> new SyncedAttachment(null, item.fileName(), item.contentType(), item.bytes(), item.text()))
                .toList();
        return new SyncedMail(
                folderName + ":" + uid,
                message.getHeader("Message-ID", null),
                message.getHeader("Thread-Index", null),
                folderName,
                StrUtil.trimToNull(message.getSubject()),
                MailMimeParser.firstName(message.getFrom()),
                MailMimeParser.firstAddress(message.getFrom()),
                MailMimeParser.toAddressList(message.getRecipients(Message.RecipientType.TO)),
                MailMimeParser.toAddressList(message.getRecipients(Message.RecipientType.CC)),
                message.getSentDate(),
                message.getReceivedDate(),
                parsed.text(),
                parsed.html(),
                MailMimeParser.toRawBytes(message),
                attachments
        );
    }

    private List<SyncedMail> fetchGmailMessages(MailAccount account, Map<String, Object> credentials,
                                                MailSyncLog syncLog) throws Exception {
        logSyncStage(syncLog, account, "gmail.token", "Refreshing or reusing Gmail access token");
        String accessToken = refreshOAuthIfNeeded(account, credentials);
        List<String> folders = normalizeFolders(splitFolders(account.getFolders()), DEFAULT_GMAIL_FOLDERS);
        List<SyncedMail> messages = new ArrayList<>();
        logSyncStage(syncLog, account, "gmail.prepare", "Gmail labels=" + folders);
        for (String folder : folders) {
            String label = "SENT".equalsIgnoreCase(folder) ? "SENT" : "INBOX";
            String url = UriComponentsBuilder.fromUriString("https://gmail.googleapis.com/gmail/v1/users/me/messages")
                    .queryParam("labelIds", label)
                    .queryParam("maxResults", resolveSyncLimit(account.getSyncLimit()))
                    .queryParam("q", "newer_than:" + resolveSyncDays(account.getSyncDays()) + "d")
                    .build()
                    .toUriString();
            logSyncStage(syncLog, account, "gmail.label.list", "Listing Gmail label=" + label);
            JsonNode listNode = getJson(url, accessToken);
            JsonNode messageNodes = listNode.path("messages");
            if (!messageNodes.isArray()) {
                logSyncStage(syncLog, account, "gmail.label.empty", "No Gmail messages for label=" + label);
                continue;
            }
            logSyncStage(syncLog, account, "gmail.label.list.done",
                    "Label=" + label + ", candidates=" + messageNodes.size());
            int fetchedCount = 0;
            for (JsonNode messageNode : messageNodes) {
                String messageId = messageNode.path("id").asText();
                fetchedCount++;
                if (fetchedCount == 1 || fetchedCount % IMAP_FETCH_BATCH_SIZE == 0 || fetchedCount == messageNodes.size()) {
                    logSyncStage(syncLog, account, "gmail.message.fetch",
                            "Fetching Gmail message label=" + label + ", index=" + fetchedCount + "/" + messageNodes.size()
                                    + ", messageId=" + messageId);
                }
                JsonNode detail = getJson("https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId + "?format=raw", accessToken);
                String raw = detail.path("raw").asText();
                if (StrUtil.isBlank(raw)) {
                    logSyncStage(syncLog, account, "gmail.message.skip",
                            "Gmail message has empty raw body, messageId=" + messageId);
                    continue;
                }
                byte[] rawBytes = Base64.getUrlDecoder().decode(raw);
                MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()), new ByteArrayInputStream(rawBytes));
                MailMimeParser.ParsedMail parsed = MailMimeParser.parse(mimeMessage);
                List<SyncedAttachment> attachments = parsed.attachments().stream()
                        .map(item -> new SyncedAttachment(null, item.fileName(), item.contentType(), item.bytes(), item.text()))
                        .toList();
                messages.add(new SyncedMail(
                        messageId,
                        mimeMessage.getHeader("Message-ID", null),
                        detail.path("threadId").asText(null),
                        label,
                        StrUtil.trimToNull(mimeMessage.getSubject()),
                        MailMimeParser.firstName(mimeMessage.getFrom()),
                        MailMimeParser.firstAddress(mimeMessage.getFrom()),
                        MailMimeParser.toAddressList(mimeMessage.getRecipients(Message.RecipientType.TO)),
                        MailMimeParser.toAddressList(mimeMessage.getRecipients(Message.RecipientType.CC)),
                        mimeMessage.getSentDate(),
                        mimeMessage.getReceivedDate(),
                        parsed.text(),
                        parsed.html(),
                        rawBytes,
                        attachments
                ));
                String historyId = detail.path("historyId").asText(null);
                if (historyId != null) {
                    upsertCursor(account, label, "gmail_history", historyId, null, historyId, null);
                }
            }
            logSyncStage(syncLog, account, "gmail.label.done",
                    "Label=" + label + " done, fetched=" + fetchedCount + ", totalFetched=" + messages.size());
        }
        logSyncStage(syncLog, account, "gmail.done", "Gmail fetch done, messages=" + messages.size());
        return messages;
    }

    private List<SyncedMail> fetchGraphMessages(MailAccount account, Map<String, Object> credentials,
                                                MailSyncLog syncLog) throws Exception {
        logSyncStage(syncLog, account, "graph.token", "Refreshing or reusing Graph access token");
        String accessToken = refreshOAuthIfNeeded(account, credentials);
        List<String> folders = normalizeFolders(splitFolders(account.getFolders()), DEFAULT_GRAPH_FOLDERS);
        List<SyncedMail> messages = new ArrayList<>();
        logSyncStage(syncLog, account, "graph.prepare", "Graph folders=" + folders);
        for (String folder : folders) {
            String url = UriComponentsBuilder.fromUriString("https://graph.microsoft.com/v1.0/me/mailFolders/" + folder + "/messages")
                    .queryParam("$top", Math.min(resolveSyncLimit(account.getSyncLimit()), 100))
                    .queryParam("$orderby", "receivedDateTime desc")
                    .queryParam("$select", "id,internetMessageId,conversationId,subject,from,toRecipients,ccRecipients,receivedDateTime,sentDateTime,body,hasAttachments")
                    .build()
                    .toUriString();
            logSyncStage(syncLog, account, "graph.folder.list", "Listing Graph folder=" + folder);
            JsonNode root = getJson(url, accessToken);
            JsonNode values = root.path("value");
            if (!values.isArray()) {
                logSyncStage(syncLog, account, "graph.folder.empty", "No Graph messages for folder=" + folder);
                continue;
            }
            logSyncStage(syncLog, account, "graph.folder.list.done",
                    "Folder=" + folder + ", candidates=" + values.size());
            int fetchedCount = 0;
            for (JsonNode node : values) {
                String bodyContent = node.path("body").path("content").asText(null);
                String bodyType = node.path("body").path("contentType").asText("");
                String bodyText = "html".equalsIgnoreCase(bodyType) ? htmlToText(bodyContent) : bodyContent;
                String messageId = node.path("id").asText();
                fetchedCount++;
                if (fetchedCount == 1 || fetchedCount % IMAP_FETCH_BATCH_SIZE == 0 || fetchedCount == values.size()) {
                    logSyncStage(syncLog, account, "graph.message.fetch",
                            "Fetching Graph message folder=" + folder + ", index=" + fetchedCount + "/" + values.size()
                                    + ", messageId=" + messageId);
                }
                messages.add(new SyncedMail(
                        messageId,
                        node.path("internetMessageId").asText(null),
                        node.path("conversationId").asText(null),
                        folder,
                        node.path("subject").asText(null),
                        node.path("from").path("emailAddress").path("name").asText(null),
                        node.path("from").path("emailAddress").path("address").asText(null),
                        graphRecipients(node.path("toRecipients")),
                        graphRecipients(node.path("ccRecipients")),
                        parseGraphDate(node.path("sentDateTime").asText(null)),
                        parseGraphDate(node.path("receivedDateTime").asText(null)),
                        bodyText,
                        "html".equalsIgnoreCase(bodyType) ? bodyContent : null,
                        fetchGraphRawBytes(messageId, accessToken),
                        fetchGraphAttachments(messageId, accessToken)
                ));
            }
            String nextLink = root.path("@odata.nextLink").asText(null);
            if (nextLink != null) {
                upsertCursor(account, folder, "graph_delta", nextLink, null, null, nextLink);
            }
            logSyncStage(syncLog, account, "graph.folder.done",
                    "Folder=" + folder + " done, fetched=" + fetchedCount + ", totalFetched=" + messages.size());
        }
        logSyncStage(syncLog, account, "graph.done", "Graph fetch done, messages=" + messages.size());
        return messages;
    }

    private List<SyncedAttachment> fetchGraphAttachments(String messageId, String accessToken) {
        try {
            JsonNode root = getJson("https://graph.microsoft.com/v1.0/me/messages/" + messageId + "/attachments", accessToken);
            JsonNode values = root.path("value");
            if (!values.isArray()) {
                return List.of();
            }
            List<SyncedAttachment> attachments = new ArrayList<>();
            for (JsonNode node : values) {
                String contentBytes = node.path("contentBytes").asText(null);
                byte[] bytes = StrUtil.isBlank(contentBytes) ? null : Base64.getDecoder().decode(contentBytes);
                attachments.add(new SyncedAttachment(
                        node.path("id").asText(null),
                        node.path("name").asText("attachment"),
                        node.path("contentType").asText("application/octet-stream"),
                        bytes,
                        null
                ));
            }
            return attachments;
        } catch (Exception e) {
            log.debug("读取 Graph 邮件附件失败: messageId={}, error={}", messageId, e.getMessage());
            return List.of();
        }
    }

    private byte[] fetchGraphRawBytes(String messageId, String accessToken) {
        try {
            return getBytes("https://graph.microsoft.com/v1.0/me/messages/" + messageId + "/$value", accessToken);
        } catch (Exception e) {
            log.debug("读取 Graph 邮件原文失败: messageId={}, error={}", messageId, e.getMessage());
            return null;
        }
    }

    private String refreshOAuthIfNeeded(MailAccount account, Map<String, Object> credentials) throws Exception {
        String accessToken = asString(credentials.get("access_token"));
        Number expiresAtNumber = credentials.get("expires_at") instanceof Number number ? number : null;
        long expiresAt = expiresAtNumber == null ? 0L : expiresAtNumber.longValue();
        if (StrUtil.isNotBlank(accessToken) && expiresAt > Instant.now().plus(2, ChronoUnit.MINUTES).toEpochMilli()) {
            return accessToken;
        }
        String refreshToken = asString(credentials.get("refresh_token"));
        if (StrUtil.isBlank(refreshToken)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱授权已过期，请重新授权");
        }
        OAuthToken refreshed = refreshOAuthToken(account.getProvider(), refreshToken);
        Map<String, Object> nextCredentials = new LinkedHashMap<>(credentials);
        nextCredentials.putAll(refreshed.toCredentialMap());
        if (StrUtil.isBlank(refreshed.refreshToken())) {
            nextCredentials.put("refresh_token", refreshToken);
        }
        account.setCredentialJson(encryptCredentials(nextCredentials));
        updateById(account);
        return refreshed.accessToken();
    }

    private OAuthToken exchangeOAuthCode(String provider, String code) {
        MailIntegrationProperties.OAuthProvider oauthProvider = oauthProvider(provider);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", oauthProvider.getClientId());
        form.add("client_secret", oauthProvider.getClientSecret());
        form.add("code", code);
        form.add("redirect_uri", oauthProvider.getRedirectUri());
        form.add("grant_type", "authorization_code");
        return postToken(provider, form);
    }

    private OAuthToken refreshOAuthToken(String provider, String refreshToken) {
        MailIntegrationProperties.OAuthProvider oauthProvider = oauthProvider(provider);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", oauthProvider.getClientId());
        form.add("client_secret", oauthProvider.getClientSecret());
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");
        return postToken(provider, form);
    }

    private OAuthToken postToken(String provider, MultiValueMap<String, String> form) {
        String tokenUrl = PROVIDER_GMAIL.equals(provider)
                ? "https://oauth2.googleapis.com/token"
                : outlookOAuthUrl(oauthProvider(provider), "token");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, new HttpEntity<>(form, headers), Map.class);
        Map<?, ?> body = response.getBody();
        if (body == null || body.get("access_token") == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱授权失败");
        }
        long expiresIn = body.get("expires_in") instanceof Number number ? number.longValue() : 3600L;
        return new OAuthToken(
                asString(body.get("access_token")),
                asString(body.get("refresh_token")),
                Instant.now().plusSeconds(Math.max(60, expiresIn)).toEpochMilli()
        );
    }

    private String outlookOAuthUrl(MailIntegrationProperties.OAuthProvider oauthProvider, String action) {
        String tenant = StrUtil.blankToDefault(oauthProvider.getTenant(), "common");
        return "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/" + action;
    }

    private OAuthProfile fetchOAuthProfile(String provider, String accessToken) throws Exception {
        if (PROVIDER_GMAIL.equals(provider)) {
            JsonNode profile = getJson("https://gmail.googleapis.com/gmail/v1/users/me/profile", accessToken);
            String email = normalizeEmail(profile.path("emailAddress").asText());
            return new OAuthProfile(email, email);
        }
        JsonNode profile = getJson("https://graph.microsoft.com/v1.0/me?$select=displayName,mail,userPrincipalName", accessToken);
        String email = normalizeEmail(StrUtil.blankToDefault(profile.path("mail").asText(null), profile.path("userPrincipalName").asText()));
        return new OAuthProfile(email, profile.path("displayName").asText(email));
    }

    private JsonNode getJson(String url, String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return objectMapper.readTree(response.getBody());
        } catch (HttpClientErrorException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "读取邮箱接口失败: " + e.getStatusCode());
        }
    }

    private byte[] getBytes(String url, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "读取邮箱原文失败: " + e.getStatusCode());
        }
    }

    private void testImapConnection(String host, int port, boolean ssl, String username, String password) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imap");
        props.put("mail.imap.ssl.enable", String.valueOf(ssl));
        props.put("mail.imap.connectiontimeout", "15000");
        props.put("mail.imap.timeout", "15000");
        Session session = Session.getInstance(props);
        try (Store store = session.getStore("imap")) {
            store.connect(host, port, username, password);
            sendImapClientId(store);
            validateReadableInbox(store);
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "IMAP 连接失败，请检查服务器、账号和授权码");
        }
    }

    private void sendImapClientId(Store store) {
        if (store instanceof IMAPStore imapStore) {
            sendImapClientId(imapStore);
        }
    }

    private void sendImapClientId(IMAPStore store) {
        try {
            store.id(IMAP_CLIENT_ID);
        } catch (MessagingException e) {
            log.debug("IMAP ID command ignored: {}", e.getMessage());
        }
    }

    private void validateReadableInbox(Store store) throws MessagingException {
        Folder inbox = store.getFolder("INBOX");
        if (inbox == null || !inbox.exists()) {
            throw new MessagingException("未找到 INBOX 文件夹");
        }
        inbox.open(Folder.READ_ONLY);
        try {
            // Opening the folder is enough to prove the account can read via IMAP.
        } finally {
            inbox.close(false);
        }
    }

    private int resolveSmtpPort(Integer port) {
        return port == null || port <= 0 ? 465 : port;
    }

    private String resolveSmtpHost(MailAccount account) {
        String smtpHost = StrUtil.trimToNull(account.getSmtpHost());
        if (smtpHost != null) {
            return smtpHost;
        }
        String imapHost = StrUtil.trimToNull(account.getImapHost());
        if (imapHost != null && imapHost.toLowerCase(Locale.ROOT).startsWith("imap.")) {
            return "smtp." + imapHost.substring(5);
        }
        throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "使用 IMAP 邮箱发送邮件时必须配置 SMTP 服务器");
    }

    private MatchResult matchContactAndCustomer(String fromAddress) {
        String email = normalizeEmailOrNull(fromAddress);
        if (email == null) {
            return new MatchResult(null, null);
        }
        List<Contact> contacts = contactMapper.selectList(new LambdaQueryWrapper<Contact>()
                .apply("LOWER(email) = {0}", email)
                .last("LIMIT 2"));
        if (contacts.size() == 1) {
            Contact contact = contacts.get(0);
            return new MatchResult(contact.getCustomerId(), contact.getContactId());
        }
        return new MatchResult(null, null);
    }

    private boolean shouldBecomeDefaultAccount(Long userId, Long currentAccountId) {
        LambdaQueryWrapper<MailAccount> wrapper = new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getUserId, userId)
                .eq(MailAccount::getEnabled, true)
                .eq(MailAccount::getIsDefault, true);
        if (currentAccountId != null) {
            wrapper.ne(MailAccount::getAccountId, currentAccountId);
        }
        Long count = baseMapper.selectCount(wrapper);
        return count == null || count == 0;
    }

    private MailAccount findDefaultAccount() {
        Long userId = UserUtil.getUserId();
        MailAccount account = baseMapper.selectOne(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getUserId, userId)
                .eq(MailAccount::getEnabled, true)
                .eq(MailAccount::getIsDefault, true)
                .orderByDesc(MailAccount::getLastUsedTime)
                .last("LIMIT 1"));
        if (account != null) {
            return account;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getUserId, userId)
                .eq(MailAccount::getEnabled, true)
                .orderByDesc(MailAccount::getLastUsedTime)
                .orderByDesc(MailAccount::getCreateTime)
                .last("LIMIT 1"));
    }

    private MailAccount loadUserAccount(Long accountId) {
        MailAccount account = baseMapper.selectOne(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getAccountId, accountId)
                .eq(MailAccount::getUserId, UserUtil.getUserId()));
        if (account == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱账号不存在或无权限访问");
        }
        return account;
    }

    private MailDraft loadUserDraft(Long draftId) {
        MailDraft draft = mailDraftMapper.selectOne(new LambdaQueryWrapper<MailDraft>()
                .eq(MailDraft::getDraftId, draftId)
                .eq(MailDraft::getUserId, UserUtil.getUserId())
                .ne(MailDraft::getStatus, "deleted"));
        if (draft == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "草稿不存在或无权限访问");
        }
        return draft;
    }

    private MailTemplate loadUserTemplate(Long templateId) {
        MailTemplate template = mailTemplateMapper.selectOne(new LambdaQueryWrapper<MailTemplate>()
                .eq(MailTemplate::getTemplateId, templateId)
                .eq(MailTemplate::getUserId, UserUtil.getUserId()));
        if (template == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "模板不存在或无权限访问");
        }
        return template;
    }

    private MailMessage loadUserMessage(Long messageId) {
        MailMessage message = mailMessageMapper.selectOne(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getMessageId, messageId)
                .eq(MailMessage::getUserId, UserUtil.getUserId()));
        if (message == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮件不存在或无权限访问");
        }
        return message;
    }

    private MailAccount findAccountByEmail(Long userId, String email) {
        return baseMapper.selectOne(new LambdaQueryWrapper<MailAccount>()
                .eq(MailAccount::getUserId, userId)
                .apply("LOWER(email_address) = {0}", email)
                .last("LIMIT 1"));
    }

    private MailSyncCursor findCursor(Long accountId, String folder) {
        return syncCursorMapper.selectOne(new LambdaQueryWrapper<MailSyncCursor>()
                .eq(MailSyncCursor::getAccountId, accountId)
                .eq(MailSyncCursor::getFolder, folder)
                .last("LIMIT 1"));
    }

    private void upsertCursor(MailAccount account, String folder, String cursorType, String cursorValue,
                              Long lastUid, String historyId, String deltaLink) {
        MailSyncCursor cursor = findCursor(account.getAccountId(), folder);
        if (cursor == null) {
            cursor = new MailSyncCursor();
            cursor.setAccountId(account.getAccountId());
            cursor.setFolder(folder);
        }
        cursor.setCursorType(cursorType);
        cursor.setCursorValue(cursorValue);
        cursor.setLastUid(lastUid);
        cursor.setLastHistoryId(historyId);
        cursor.setDeltaLink(deltaLink);
        if (cursor.getCursorId() == null) {
            syncCursorMapper.insert(cursor);
        } else {
            syncCursorMapper.updateById(cursor);
        }
    }

    private String encryptCredentials(Map<String, ?> values) {
        try {
            return secretTextCipher.encrypt(objectMapper.writeValueAsString(values));
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, resolveCredentialCipherMessage(e, true));
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱凭据序列化失败");
        }
    }

    private Map<String, Object> decryptCredentials(MailAccount account) throws Exception {
        try {
            String json = secretTextCipher.decrypt(account.getCredentialJson());
            if (StrUtil.isBlank(json)) {
                return new HashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (IllegalStateException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, resolveCredentialCipherMessage(e, false));
        }
    }

    private String resolveCredentialCipherMessage(Throwable throwable, boolean encrypting) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (containsIgnoreCase(message, "mail.integration.encryption-key")
                    || containsIgnoreCase(message, "MAIL_CREDENTIAL_ENCRYPTION_KEY")) {
                return MAIL_CREDENTIAL_KEY_REQUIRED_MESSAGE;
            }
            current = current.getCause();
        }
        return encrypting ? MAIL_CREDENTIAL_ENCRYPT_FAILED_MESSAGE : MAIL_CREDENTIAL_DECRYPT_FAILED_MESSAGE;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private MailAccountVO toAccountVO(MailAccount account) {
        MailAccountVO vo = new MailAccountVO();
        vo.setAccountId(account.getAccountId());
        vo.setProvider(account.getProvider());
        vo.setAuthType(account.getAuthType());
        vo.setEmailAddress(account.getEmailAddress());
        vo.setDisplayName(account.getDisplayName());
        vo.setImapHost(account.getImapHost());
        vo.setImapPort(account.getImapPort());
        vo.setEnabled(account.getEnabled());
        vo.setIsDefault(account.getIsDefault());
        vo.setConnectionStatus(account.getConnectionStatus());
        vo.setLastUsedTime(account.getLastUsedTime());
        vo.setSmtpHost(account.getSmtpHost());
        vo.setSmtpPort(account.getSmtpPort());
        vo.setFolders(splitFolders(account.getFolders()));
        vo.setSyncDays(account.getSyncDays());
        vo.setSyncLimit(account.getSyncLimit());
        vo.setBodySyncMode(resolveBodySyncMode(account.getBodySyncMode()));
        vo.setAttachmentSyncMode(resolveAttachmentSyncMode(account.getAttachmentSyncMode()));
        vo.setMaxAutoAttachmentSize(resolveMaxAutoAttachmentSize(account.getMaxAutoAttachmentSize()));
        vo.setRetentionDays(resolveRetentionDays(account.getRetentionDays()));
        vo.setExtractActions(account.getExtractActions());
        vo.setLastSyncTime(account.getLastSyncTime());
        vo.setLastSyncStatus(account.getLastSyncStatus());
        vo.setLastSyncError(account.getLastSyncError());
        vo.setCreateTime(account.getCreateTime());
        return vo;
    }

    private MailAttachmentVO toAttachmentVO(MailAttachment attachment) {
        MailAttachmentVO vo = new MailAttachmentVO();
        vo.setAttachmentId(attachment.getAttachmentId());
        vo.setMessageId(attachment.getMessageId());
        vo.setProviderAttachmentId(attachment.getProviderAttachmentId());
        vo.setFileName(attachment.getFileName());
        vo.setContentType(attachment.getContentType());
        vo.setFileSize(attachment.getFileSize());
        vo.setFilePath(attachment.getFilePath());
        vo.setKnowledgeId(attachment.getKnowledgeId());
        vo.setDownloadStatus(attachment.getDownloadStatus());
        vo.setScanStatus(attachment.getScanStatus());
        vo.setSyncMode(attachment.getSyncMode());
        vo.setDownloadError(attachment.getDownloadError());
        return vo;
    }

    private MailSyncLogVO toSyncLogVO(MailSyncLog log) {
        MailSyncLogVO vo = new MailSyncLogVO();
        vo.setLogId(log.getLogId());
        vo.setAccountId(log.getAccountId());
        vo.setUserId(log.getUserId());
        vo.setSyncType(log.getSyncType());
        vo.setStatus(log.getStatus());
        vo.setFetchedCount(log.getFetchedCount());
        vo.setSavedCount(log.getSavedCount());
        vo.setSkippedCount(log.getSkippedCount());
        vo.setFailedCount(log.getFailedCount());
        vo.setStartedAt(log.getStartedAt());
        vo.setFinishedAt(log.getFinishedAt());
        vo.setErrorMessage(log.getErrorMessage());
        return vo;
    }

    private MailDraftVO toDraftVO(MailDraft draft) {
        MailDraftVO vo = new MailDraftVO();
        vo.setDraftId(draft.getDraftId());
        vo.setAccountId(draft.getAccountId());
        vo.setCustomerId(draft.getCustomerId());
        vo.setContactId(draft.getContactId());
        vo.setSourceMessageId(draft.getSourceMessageId());
        vo.setToAddresses(draft.getToAddresses());
        vo.setCcAddresses(draft.getCcAddresses());
        vo.setBccAddresses(draft.getBccAddresses());
        vo.setSubject(draft.getSubject());
        if (draft.getAccountId() != null) {
            MailAccount account = baseMapper.selectById(draft.getAccountId());
            if (account != null && Objects.equals(account.getUserId(), draft.getUserId())) {
                vo.setAccountEmail(account.getEmailAddress());
            }
        }
        vo.setBodyText(draft.getBodyText());
        vo.setAttachmentRefs(draft.getAttachmentRefs());
        vo.setStatus(draft.getStatus());
        vo.setRiskStatus(draft.getRiskStatus());
        vo.setRiskReasons(draft.getRiskReasons());
        vo.setCreateTime(draft.getCreateTime());
        vo.setUpdateTime(draft.getUpdateTime());
        return vo;
    }

    private MailTemplateVO toTemplateVO(MailTemplate template) {
        MailTemplateVO vo = new MailTemplateVO();
        vo.setTemplateId(template.getTemplateId());
        vo.setName(template.getName());
        vo.setCategory(template.getCategory());
        vo.setSubject(template.getSubject());
        vo.setBodyText(template.getBodyText());
        vo.setVariables(template.getVariables());
        vo.setIsCommon(template.getIsCommon());
        vo.setCreateTime(template.getCreateTime());
        vo.setUpdateTime(template.getUpdateTime());
        return vo;
    }

    private MailSyncLog startSyncLog(MailAccount account, String syncType) {
        MailSyncLog syncLog = new MailSyncLog();
        syncLog.setAccountId(account.getAccountId());
        syncLog.setUserId(account.getUserId());
        syncLog.setSyncType(syncType);
        syncLog.setStatus("running");
        syncLog.setFetchedCount(0);
        syncLog.setSavedCount(0);
        syncLog.setSkippedCount(0);
        syncLog.setFailedCount(0);
        syncLog.setStartedAt(new Date());
        syncLogMapper.insert(syncLog);
        logSyncStage(syncLog, account, "sync.start", "Sync log created, syncType=" + syncType);
        return syncLog;
    }

    private void markAccountSyncRunning(MailAccount account) {
        account.setLastSyncTime(new Date());
        account.setLastSyncStatus("running");
        account.setLastSyncError(null);
        updateById(account);
    }

    private MailSyncResultVO buildRunningSyncResult(MailAccount account, MailSyncLog syncLog) {
        MailSyncResultVO result = new MailSyncResultVO();
        result.setAccountId(account.getAccountId());
        result.setLogId(syncLog.getLogId());
        result.setFetchedCount(0);
        result.setSavedCount(0);
        result.setSkippedCount(0);
        result.setFailedCount(0);
        result.setStatus("running");
        return result;
    }

    private void finishSyncLog(MailSyncLog syncLog, MailSyncResultVO result) {
        syncLog.setStatus(result.getStatus());
        syncLog.setFetchedCount(result.getFetchedCount());
        syncLog.setSavedCount(result.getSavedCount());
        syncLog.setSkippedCount(result.getSkippedCount());
        syncLog.setFailedCount(result.getFailedCount());
        syncLog.setFinishedAt(new Date());
        syncLog.setErrorMessage(StrUtil.maxLength(result.getErrorMessage(), 1000));
        syncLogMapper.updateById(syncLog);
        log.info("mail sync finished: logId={}, accountId={}, status={}, fetched={}, saved={}, skipped={}, failed={}, error={}",
                syncLog.getLogId(), syncLog.getAccountId(), result.getStatus(), result.getFetchedCount(),
                result.getSavedCount(), result.getSkippedCount(), result.getFailedCount(),
                StrUtil.maxLength(result.getErrorMessage(), 500));
    }

    private void logSyncStage(MailSyncLog syncLog, MailAccount account, String stage, String detail) {
        Date startedAt = syncLog == null ? null : syncLog.getStartedAt();
        Long elapsedMs = startedAt == null ? null : Math.max(0L, System.currentTimeMillis() - startedAt.getTime());
        log.debug("mail sync stage: logId={}, accountId={}, email={}, provider={}, stage={}, elapsedMs={}, detail={}",
                syncLog == null ? null : syncLog.getLogId(),
                account == null ? null : account.getAccountId(),
                account == null ? null : account.getEmailAddress(),
                account == null ? null : account.getProvider(),
                stage,
                elapsedMs,
                StrUtil.maxLength(StrUtil.blankToDefault(detail, ""), 500));
    }

    private MailExtraction extractMail(SyncedMail mail, boolean extractActions) {
        try {
            String text = StrUtil.blankToDefault(mail.bodyText(), "");
            String compact = text.replaceAll("\\s+", " ").trim();
            String summarySource = StrUtil.isNotBlank(compact) ? compact : StrUtil.blankToDefault(mail.subject(), "");
            String summary = StrUtil.maxLength(summarySource, 500);
            String lower = (StrUtil.blankToDefault(mail.subject(), "") + " " + compact).toLowerCase(Locale.ROOT);
            List<String> keywords = new ArrayList<>();
            for (String hint : INTENT_HINTS) {
                if (lower.contains(hint.toLowerCase(Locale.ROOT))) {
                    keywords.add(hint);
                }
            }
            String intent = keywords.isEmpty() ? null : keywords.get(0);
            List<String> actionItems = extractActions ? extractActionItems(text) : List.of();
            String actionItemsJson = actionItems.isEmpty() ? null : objectMapper.writeValueAsString(actionItems);
            return new MailExtraction(
                    summary,
                    keywords.isEmpty() ? null : String.join(",", keywords),
                    intent,
                    actionItemsJson,
                    extractReplyDeadline(text),
                    "heuristic",
                    null
            );
        } catch (Exception e) {
            return new MailExtraction(null, null, null, null, null, "failed", StrUtil.maxLength(e.getMessage(), 1000));
        }
    }

    private List<String> extractActionItems(String text) {
        if (StrUtil.isBlank(text)) {
            return List.of();
        }
        List<String> items = new ArrayList<>();
        String[] sentences = text.split("[。！？!?\\r\\n]+");
        for (String sentence : sentences) {
            String normalized = sentence.replaceAll("\\s+", " ").trim();
            if (StrUtil.isBlank(normalized)) {
                continue;
            }
            String lower = normalized.toLowerCase(Locale.ROOT);
            boolean matched = ACTION_HINTS.stream().anyMatch(hint -> lower.contains(hint.toLowerCase(Locale.ROOT)));
            if (matched) {
                items.add(StrUtil.maxLength(normalized, 300));
            }
            if (items.size() >= 6) {
                break;
            }
        }
        return items;
    }

    private Date extractReplyDeadline(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(text);
        if (isoMatcher.find()) {
            int year = Integer.parseInt(isoMatcher.group(1));
            int month = Integer.parseInt(isoMatcher.group(2));
            int day = Integer.parseInt(isoMatcher.group(3));
            return toDate(year, month, day);
        }
        Matcher cnMatcher = CN_DATE_PATTERN.matcher(text);
        if (cnMatcher.find()) {
            int year = LocalDate.now().getYear();
            int month = Integer.parseInt(cnMatcher.group(1));
            int day = Integer.parseInt(cnMatcher.group(2));
            return toDate(year, month, day);
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (text.contains("明天") || lower.contains("tomorrow")) {
            return Date.from(LocalDate.now().plusDays(1).atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant());
        }
        return null;
    }

    private Date toDate(int year, int month, int day) {
        try {
            return Date.from(LocalDate.of(year, month, day).atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean shouldStoreAttachment(String syncMode, byte[] bytes, long maxAutoSize) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        return ATTACHMENT_SYNC_FULL.equals(syncMode)
                || (ATTACHMENT_SYNC_AUTO.equals(syncMode) && bytes.length <= maxAutoSize);
    }

    private String resolveAttachmentMetadataStatus(String syncMode, byte[] bytes, long maxAutoSize) {
        if (bytes == null || bytes.length == 0) {
            return "metadata";
        }
        if (ATTACHMENT_SYNC_METADATA.equals(syncMode)) {
            return "metadata";
        }
        if (ATTACHMENT_SYNC_AUTO.equals(syncMode) && bytes.length > maxAutoSize) {
            return "skipped_large";
        }
        return "metadata";
    }

    private List<String> detectDraftRisks(MailDraft draft) {
        List<String> risks = new ArrayList<>();
        String text = (StrUtil.blankToDefault(draft.getSubject(), "") + " " + StrUtil.blankToDefault(draft.getBodyText(), "")).toLowerCase(Locale.ROOT);
        if (StrUtil.isNotBlank(draft.getAttachmentRefs())) {
            risks.add("attachment_requires_review");
        }
        if (text.contains("合同") || text.contains("contract")) {
            risks.add("contract_content");
        }
        if (text.contains("报价") || text.contains("金额") || text.contains("quote") || text.contains("price")) {
            risks.add("commercial_terms");
        }
        if (text.contains("付款") || text.contains("invoice") || text.contains("payment")) {
            risks.add("payment_terms");
        }
        if (text.matches(".*(\\d+(\\.\\d+)?\\s*(万|元|rmb|cny|usd|\\$)).*")) {
            risks.add("amount_detected");
        }
        return risks.stream().distinct().toList();
    }

    private String buildKnowledgeText(MailMessage message) {
        return """
                邮件主题: %s
                发件人: %s <%s>
                收件人: %s
                抄送: %s
                时间: %s

                %s
                """.formatted(
                StrUtil.blankToDefault(message.getSubject(), "(无主题)"),
                StrUtil.blankToDefault(message.getFromName(), ""),
                StrUtil.blankToDefault(message.getFromAddress(), ""),
                StrUtil.blankToDefault(message.getToAddresses(), ""),
                StrUtil.blankToDefault(message.getCcAddresses(), ""),
                message.getReceivedTime() != null ? message.getReceivedTime() : message.getSentTime(),
                StrUtil.blankToDefault(message.getBodyText(), StrUtil.blankToDefault(message.getSummary(), ""))
        );
    }

    private String buildKnowledgeFileName(MailMessage message) {
        String subject = StrUtil.blankToDefault(message.getSubject(), "mail");
        return safeFileName("邮件-" + subject + "-" + message.getMessageId() + ".txt");
    }

    private byte[] buildGeneratedRaw(SyncedMail mail) {
        return buildGeneratedRawString(mail).getBytes(StandardCharsets.UTF_8);
    }

    private String buildGeneratedRawString(SyncedMail mail) {
        return """
                Subject: %s
                From: %s
                To: %s
                Date: %s

                %s
                """.formatted(
                StrUtil.blankToDefault(mail.subject(), ""),
                StrUtil.blankToDefault(mail.fromAddress(), ""),
                StrUtil.blankToDefault(mail.toAddresses(), ""),
                mail.receivedTime() != null ? mail.receivedTime() : mail.sentTime(),
                StrUtil.blankToDefault(mail.bodyText(), "")
        );
    }

    private MailIntegrationProperties.OAuthProvider oauthProvider(String provider) {
        return PROVIDER_GMAIL.equals(provider) ? properties.getGmail() : properties.getOutlook();
    }

    private void requireOAuthConfig(String provider, MailIntegrationProperties.OAuthProvider oauthProvider) {
        if (oauthProvider == null
                || StrUtil.isBlank(oauthProvider.getClientId())
                || StrUtil.isBlank(oauthProvider.getClientSecret())
                || StrUtil.isBlank(oauthProvider.getRedirectUri())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, provider + " OAuth 配置未完成");
        }
    }

    private URI parseProxyUri(String proxyUrl) {
        String trimmed = StrUtil.trim(proxyUrl);
        if (!trimmed.contains("://")) {
            trimmed = "http://" + trimmed;
        }
        return URI.create(trimmed);
    }

    private Proxy.Type resolveProxyType(String scheme) {
        if (StrUtil.equalsAnyIgnoreCase(scheme, "socks", "socks5")) {
            return Proxy.Type.SOCKS;
        }
        return Proxy.Type.HTTP;
    }

    private String normalizeOAuthProvider(String provider) {
        String normalized = StrUtil.blankToDefault(provider, "").trim().toLowerCase(Locale.ROOT);
        if (Set.of(PROVIDER_GMAIL, PROVIDER_OUTLOOK).contains(normalized)) {
            return normalized;
        }
        throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "不支持的邮箱 OAuth 服务商");
    }

    private String resolveDirection(String folder) {
        String normalized = StrUtil.blankToDefault(folder, "").toLowerCase(Locale.ROOT);
        return normalized.contains("sent") ? "sent" : "received";
    }

    private int resolveImapPort(Integer port) {
        return port == null || port <= 0 ? 993 : port;
    }

    private int resolveSyncDays(Integer syncDays) {
        return syncDays == null || syncDays <= 0 ? properties.getDefaultSyncDays() : Math.min(syncDays, 365);
    }

    private int resolveSyncLimit(Integer syncLimit) {
        return syncLimit == null || syncLimit <= 0 ? properties.getDefaultSyncLimit() : Math.min(syncLimit, 2000);
    }

    private String resolveBodySyncMode(String mode) {
        String normalized = StrUtil.blankToDefault(mode, properties.getDefaultBodySyncMode()).trim().toLowerCase(Locale.ROOT);
        if (Set.of(BODY_SYNC_METADATA, BODY_SYNC_SUMMARY, BODY_SYNC_FULL).contains(normalized)) {
            return normalized;
        }
        return BODY_SYNC_SUMMARY;
    }

    private String resolveAttachmentSyncMode(String mode) {
        String normalized = StrUtil.blankToDefault(mode, properties.getDefaultAttachmentSyncMode()).trim().toLowerCase(Locale.ROOT);
        if (Set.of(ATTACHMENT_SYNC_METADATA, ATTACHMENT_SYNC_AUTO, ATTACHMENT_SYNC_FULL).contains(normalized)) {
            return normalized;
        }
        return ATTACHMENT_SYNC_METADATA;
    }

    private long resolveMaxAutoAttachmentSize(Long size) {
        long fallback = properties.getDefaultMaxAutoAttachmentSize() <= 0
                ? 10 * 1024 * 1024L
                : properties.getDefaultMaxAutoAttachmentSize();
        if (size == null || size <= 0) {
            return fallback;
        }
        return Math.min(size, 150 * 1024 * 1024L);
    }

    private int resolveRetentionDays(Integer retentionDays) {
        int fallback = properties.getDefaultRetentionDays() <= 0 ? 180 : properties.getDefaultRetentionDays();
        if (retentionDays == null || retentionDays <= 0) {
            return fallback;
        }
        return Math.min(retentionDays, 3650);
    }

    private List<String> normalizeFolders(List<String> folders, List<String> defaults) {
        List<String> source = folders == null || folders.isEmpty() ? defaults : folders;
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String folder : source) {
            if (StrUtil.isNotBlank(folder)) {
                normalized.add(folder.trim());
            }
        }
        return normalized.isEmpty() ? defaults : new ArrayList<>(normalized);
    }

    private List<String> splitFolders(String folders) {
        if (StrUtil.isBlank(folders)) {
            return List.of();
        }
        return StrUtil.split(folders, ',').stream()
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private Date parseGraphDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return Date.from(OffsetDateTime.parse(value).toInstant());
    }

    private String graphRecipients(JsonNode nodes) {
        if (nodes == null || !nodes.isArray()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode node : nodes) {
            JsonNode email = node.path("emailAddress");
            String address = email.path("address").asText("");
            String name = email.path("name").asText("");
            values.add(StrUtil.isBlank(name) ? address : name + " <" + address + ">");
        }
        return String.join(", ", values);
    }

    private String htmlToText(String html) {
        if (html == null) {
            return null;
        }
        return html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeEmailOrNull(email);
        if (normalized == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱格式不正确");
        }
        return normalized;
    }

    private String normalizeEmailOrNull(String email) {
        String trimmed = StrUtil.trimToNull(email);
        if (trimmed == null) {
            return null;
        }
        int start = trimmed.indexOf('<');
        int end = trimmed.indexOf('>');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start + 1, end);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String safeFileName(String fileName) {
        String normalized = StrUtil.blankToDefault(fileName, "mail").replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
        return StrUtil.maxLength(normalized, 180);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record SyncedMail(
            String providerMessageId,
            String internetMessageId,
            String threadId,
            String folder,
            String subject,
            String fromName,
            String fromAddress,
            String toAddresses,
            String ccAddresses,
            Date sentTime,
            Date receivedTime,
            String bodyText,
            String bodyHtml,
            byte[] rawBytes,
            List<SyncedAttachment> attachments
    ) {
    }

    private record SyncedAttachment(String providerAttachmentId, String fileName, String contentType, byte[] bytes, String text) {
    }

    private record MatchResult(Long customerId, Long contactId) {
    }

    private record MailExtraction(String summary, String keywords, String intent, String actionItemsJson,
                                  Date replyDeadlineTime, String status, String error) {
    }

    private record OAuthToken(String accessToken, String refreshToken, long expiresAt) {
        Map<String, Object> toCredentialMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("access_token", accessToken);
            map.put("refresh_token", refreshToken);
            map.put("expires_at", expiresAt);
            return map;
        }
    }

    private record OAuthProfile(String emailAddress, String displayName) {
    }
}
