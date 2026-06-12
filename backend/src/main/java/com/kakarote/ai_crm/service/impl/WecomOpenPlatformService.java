package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.auth.DataPermissionHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.WecomOpenPlatformProperties;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.ExternalTenantRegisterBO;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ExternalAuthIdentity;
import com.kakarote.ai_crm.entity.PO.ExternalTenantBinding;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.entity.PO.WecomEmployee;
import com.kakarote.ai_crm.entity.PO.WecomSuiteTicket;
import com.kakarote.ai_crm.entity.VO.WecomOpenAuthorizeVO;
import com.kakarote.ai_crm.mapper.ExternalAuthIdentityMapper;
import com.kakarote.ai_crm.mapper.ExternalTenantBindingMapper;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.WecomCorpConfigMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.mapper.WecomSuiteTicketMapper;
import com.kakarote.ai_crm.service.RegistrationService;
import com.kakarote.ai_crm.service.support.SyncTaskExecutor;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import com.kakarote.ai_crm.utils.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@Service
public class WecomOpenPlatformService {

    public static final String AUTH_STATUS_AUTHORIZED = "AUTHORIZED";
    public static final String AUTH_STATUS_UNAUTHORIZED = "UNAUTHORIZED";

    private static final int ENABLED_STATUS = 1;
    private static final String SUITE_TOKEN_KEY_PREFIX = "wecom:open:suite-token:";
    private static final String PROVIDER_TOKEN_KEY_PREFIX = "wecom:open:provider-token:";
    private static final String CORP_TOKEN_KEY_PREFIX = "wecom:open:corp-token:";
    private static final String AUTH_STATE_KEY_PREFIX = "wecom:open:auth-state:";
    private static final String AUTH_RESULT_KEY_PREFIX = "wecom:open:auth-result:";
    private static final String EXTERNAL_LOGIN_TICKET_KEY_PREFIX = "external-auth:login-ticket:";
    private static final String INSTALL_URL = "https://open.work.weixin.qq.com/3rdapp/install";
    private static final String LOGIN_URL = "https://open.work.weixin.qq.com/wwopen/sso/3rd_qrConnect";
    private static final String WECOM_EMPLOYEE_PERMISSION_MODULE = "wecomEmployeeSession";

    @Autowired
    private WecomOpenPlatformProperties properties;

    @Autowired
    private WecomOpenApiClient apiClient;

    @Autowired
    private WecomCallbackCryptoService cryptoService;

    @Autowired
    private WecomSuiteTicketMapper suiteTicketMapper;

    @Autowired
    private WecomCorpConfigMapper configMapper;

    @Autowired
    private ExternalTenantBindingMapper tenantBindingMapper;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ExternalAuthIdentityMapper identityMapper;

    @Autowired
    private ManageUserMapper managerUserMapper;

    @Autowired
    private WecomEmployeeMapper employeeMapper;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Autowired
    private Redis redis;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    @Lazy
    private WecomSyncServiceImpl syncService;

    @Autowired
    private SyncTaskExecutor syncTaskExecutor;

    public boolean isUsable() {
        return properties != null && properties.isUsable();
    }

    public boolean isLoginUsable() {
        return properties != null && properties.isLoginUsable();
    }

    public boolean isAuthorized(WecomCorpConfig config) {
        return config != null
                && AUTH_STATUS_AUTHORIZED.equals(config.getAuthStatus())
                && StrUtil.isNotBlank(config.getPermanentCodeEncrypted());
    }

    public boolean isEnterpriseAuthorized(String corpId) {
        if (StrUtil.isBlank(corpId)) {
            return false;
        }
        return isAuthorized(configMapper.selectAuthorizedThirdPartyByCorpIdIgnoreTenant(corpId));
    }

    public WecomOpenAuthorizeVO createAuthorizeUrl(String redirect, HttpServletRequest request) {
        requireUsable();
        LoginUser loginUser = UserUtil.getLoginUser();
        AuthState state = new AuthState();
        state.setTenantId(loginUser.getUser().getTenantId());
        state.setRedirect(resolveFrontendRedirect(redirect, request));
        return createInstallAuthorizeUrl(state, request);
    }

    public WecomOpenAuthorizeVO createDirectInstallAuthorizeUrl(String redirect, HttpServletRequest request) {
        requireUsable();
        AuthState state = new AuthState();
        state.setDirectInstall(Boolean.TRUE);
        state.setRedirect(resolveFrontendRedirect(redirect, request));
        return createInstallAuthorizeUrl(state, request);
    }

    private WecomOpenAuthorizeVO createInstallAuthorizeUrl(AuthState state, HttpServletRequest request) {
        String stateId = IdUtil.fastSimpleUUID();
        redis.setex(authStateKey(stateId), safeTtl(properties.getStateTtlSeconds()), JSON.toJSONString(state));

        String suiteAccessToken = fetchSuiteAccessToken();
        JSONObject preAuthCode = apiClient.fetchPreAuthCode(suiteAccessToken);
        String preAuthCodeValue = preAuthCode.getString("pre_auth_code");
        apiClient.setSessionInfo(suiteAccessToken, preAuthCodeValue, buildSessionInfo());
        String authorizeUrl = UriComponentsBuilder.fromHttpUrl(INSTALL_URL)
                .queryParam("suite_id", properties.getSuiteId())
                .queryParam("pre_auth_code", preAuthCodeValue)
                .queryParam("redirect_uri", resolveAuthCallbackUri(request))
                .queryParam("state", stateId)
                .build()
                .encode()
                .toUriString();
        WecomOpenAuthorizeVO vo = new WecomOpenAuthorizeVO();
        vo.setAuthorizeUrl(authorizeUrl);
        vo.setState(stateId);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public String handleAuthCallback(String authCode, String state, String error, HttpServletRequest request) {
        AuthState authState;
        try {
            authState = readAuthState(state);
        } catch (BusinessException e) {
            String message = StrUtil.isBlank(state) ? "missing_state" : "state_expired";
            log.warn("WeCom third-party auth callback ignored: message={}, state={}", message, state);
            return appendQuery(resolveFrontendRedirect(null, request), "wecomAuth", "error", "message", message);
        }
        if (StrUtil.isNotBlank(error)) {
            return appendQuery(authState.getRedirect(), "wecomAuth", "error", "message", error);
        }
        AuthResult cachedResult = readAuthResult(state);
        if (cachedResult != null) {
            redis.del(authStateKey(state));
            return appendAuthResult(authState.getRedirect(), cachedResult);
        }
        if (StrUtil.isBlank(authCode)) {
            return appendQuery(authState.getRedirect(), "wecomAuth", "error", "message", "missing_auth_code");
        }
        Long previousTenantId = TenantContextHolder.getTenantId();
        try {
            JSONObject permanentData = apiClient.fetchPermanentCode(fetchSuiteAccessToken(), authCode);
            AuthProcessResult result = processAuthorizedEnterprise(authState, permanentData);
            storeAuthResult(state, result.getTenantId(), result.getIdentityId(), "success", null);
            redis.del(authStateKey(state));
            return appendAuthResult(authState.getRedirect(), result);
        } catch (Exception e) {
            log.warn("WeCom third-party auth callback failed: {}", e.getMessage());
            storeAuthResult(state, null, null, "error", "callback_failed");
            return appendQuery(authState.getRedirect(), "wecomAuth", "error", "message", "callback_failed");
        } finally {
            restoreTenantContext(previousTenantId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String body,
                                 String msgSignature,
                                 String timestamp,
                                 String nonce) {
        JSONObject event = parseCallbackEvent(body, msgSignature, timestamp, nonce);
        String infoType = event.getString("InfoType");
        if (StrUtil.isBlank(infoType)) {
            return "success";
        }
        switch (infoType) {
            case "suite_ticket" -> saveSuiteTicket(event);
            case "create_auth", "change_auth" -> handleAuthEvent(event);
            case "cancel_auth" -> markUnauthorized(event.getString("AuthCorpId"));
            default -> log.debug("Ignored WeCom callback event: {}", infoType);
        }
        return "success";
    }

    public String verifyCallbackUrl(String msgSignature,
                                    String timestamp,
                                    String nonce,
                                    String echoStr) {
        requireUsable();
        return cryptoService.decrypt(msgSignature, timestamp, nonce, echoStr);
    }

    public String fetchCorpAccessToken(WecomCorpConfig config) {
        if (!isAuthorized(config)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom enterprise is not authorized");
        }
        String cacheKey = CORP_TOKEN_KEY_PREFIX + config.getCorpId();
        String cached = redis.get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        JSONObject json = apiClient.fetchCorpAccessToken(fetchSuiteAccessToken(),
                config.getCorpId(),
                decryptRequired(config.getPermanentCodeEncrypted(), "permanent code"));
        String token = json.getString("access_token");
        redis.setex(cacheKey, tokenTtl(json.getInteger("expires_in")), token);
        return token;
    }

    public ExternalAuthServiceImpl.ExternalProfile fetchLoginProfile(String code) {
        requireLoginUsable();
        JSONObject loginInfo = apiClient.fetchLoginInfo(fetchProviderAccessToken(), code);
        JSONObject corpInfo = loginInfo.getJSONObject("corp_info");
        JSONObject userInfo = loginInfo.getJSONObject("user_info");
        String corpId = firstNotBlank(
                loginInfo.getString("corpid"),
                loginInfo.getString("CorpId"),
                corpInfo == null ? null : corpInfo.getString("corpid")
        );
        String userId = firstNotBlank(
                loginInfo.getString("userid"),
                loginInfo.getString("UserId"),
                userInfo == null ? null : userInfo.getString("userid"),
                userInfo == null ? null : userInfo.getString("open_userid")
        );
        if (StrUtil.isBlank(corpId) || StrUtil.isBlank(userId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom login profile is incomplete");
        }
        WecomCorpConfig config = configMapper.selectAuthorizedThirdPartyByCorpIdIgnoreTenant(corpId);
        String corpName = firstNotBlank(
                config == null ? null : config.getCorpName(),
                corpInfo == null ? null : corpInfo.getString("corp_name"),
                corpInfo == null ? null : corpInfo.getString("corp_full_name")
        );

        JSONObject rawProfile = new JSONObject();
        rawProfile.put("loginInfo", loginInfo);
        String displayName = firstNotBlank(
                userInfo == null ? null : userInfo.getString("name"),
                userInfo == null ? null : userInfo.getString("english_name"),
                userId
        );
        String email = normalizeEmail(firstNotBlank(
                userInfo == null ? null : userInfo.getString("email"),
                userInfo == null ? null : userInfo.getString("biz_mail")
        ));
        String mobile = StrUtil.trim(userInfo == null ? null : userInfo.getString("mobile"));

        ExternalAuthServiceImpl.ExternalProfile profile = new ExternalAuthServiceImpl.ExternalProfile();
        profile.setProvider("wecom");
        profile.setSubject(corpId + ":" + userId);
        profile.setEmail(email);
        profile.setMobile(StrUtil.isBlank(mobile) ? null : mobile);
        profile.setDisplayName(displayName);
        profile.setAvatarUrl(userInfo == null ? null : userInfo.getString("avatar"));
        profile.setExternalTenantKey(corpId);
        profile.setExternalTenantName(corpName);
        profile.setEmailVerified(Boolean.FALSE);
        profile.setRawJson(rawProfile.toJSONString());
        return profile;
    }

    public String buildLoginAuthorizeUrl(String callbackUri, String state) {
        requireLoginUsable();
        return LOGIN_URL
                + "?appid=" + encode(properties.getProviderCorpId())
                + "&redirect_uri=" + encode(callbackUri)
                + "&state=" + encode(state)
                + "&usertype=member";
    }

    public String resolveLoginCallbackUri(HttpServletRequest request) {
        if (StrUtil.isNotBlank(properties.getLoginRedirectUri())) {
            return properties.getLoginRedirectUri();
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/auth/external/wecom/callback")
                .build()
                .toUriString();
    }

    private String fetchSuiteAccessToken() {
        requireUsable();
        String cacheKey = SUITE_TOKEN_KEY_PREFIX + properties.getSuiteId();
        String cached = redis.get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        WecomSuiteTicket latestTicket = suiteTicketMapper.selectLatestBySuiteIdIgnoreTenant(properties.getSuiteId());
        if (latestTicket == null || StrUtil.isBlank(latestTicket.getSuiteTicketEncrypted())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom suite ticket is not received yet");
        }
        JSONObject json = apiClient.fetchSuiteAccessToken(properties.getSuiteId(),
                properties.getSuiteSecret(),
                decryptRequired(latestTicket.getSuiteTicketEncrypted(), "suite ticket"));
        String token = json.getString("suite_access_token");
        redis.setex(cacheKey, tokenTtl(json.getInteger("expires_in")), token);
        return token;
    }

    private JSONObject buildSessionInfo() {
        JSONObject sessionInfo = new JSONObject();
        Integer authType = properties.getAuthType();
        sessionInfo.put("auth_type", authType == null ? 0 : authType);
        return sessionInfo;
    }

    private String fetchProviderAccessToken() {
        requireLoginUsable();
        String cacheKey = PROVIDER_TOKEN_KEY_PREFIX + properties.getProviderCorpId();
        String cached = redis.get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        JSONObject json = apiClient.fetchProviderAccessToken(properties.getProviderCorpId(), properties.getProviderSecret());
        String token = json.getString("provider_access_token");
        redis.setex(cacheKey, tokenTtl(json.getInteger("expires_in")), token);
        return token;
    }

    private void saveSuiteTicket(JSONObject event) {
        String suiteId = StrUtil.blankToDefault(event.getString("SuiteId"), properties.getSuiteId());
        String ticket = event.getString("SuiteTicket");
        if (StrUtil.isBlank(suiteId) || StrUtil.isBlank(ticket)) {
            return;
        }
        WecomSuiteTicket suiteTicket = new WecomSuiteTicket();
        suiteTicket.setSuiteId(suiteId);
        suiteTicket.setSuiteTicketEncrypted(secretTextCipher.encrypt(ticket));
        suiteTicket.setReceivedAt(new Date());
        suiteTicket.setRawEventXml(event.getString("_rawXml"));
        suiteTicketMapper.insert(suiteTicket);
        redis.del(SUITE_TOKEN_KEY_PREFIX + suiteId);
    }

    private void handleAuthEvent(JSONObject event) {
        String authCode = event.getString("AuthCode");
        String stateId = event.getString("State");
        if (StrUtil.isBlank(authCode)) {
            return;
        }
        AuthState state;
        if (StrUtil.isBlank(stateId)) {
            state = null;
        } else {
            try {
                state = readAuthState(stateId);
            } catch (BusinessException e) {
                AuthResult result = readAuthResult(stateId);
                if (result != null && "success".equals(result.getStatus())) {
                    return;
                }
                log.debug("WeCom auth event local state is unavailable, fallback to direct install: {}", stateId);
                state = null;
            }
        }
        JSONObject permanentData = apiClient.fetchPermanentCode(fetchSuiteAccessToken(), authCode);
        Long previousTenantId = TenantContextHolder.getTenantId();
        try {
            AuthProcessResult result = processAuthorizedEnterprise(state, permanentData);
            if (StrUtil.isNotBlank(stateId)) {
                storeAuthResult(stateId, result.getTenantId(), result.getIdentityId(), "success", null);
            }
        } finally {
            restoreTenantContext(previousTenantId);
        }
    }

    private AuthProcessResult processAuthorizedEnterprise(AuthState state, JSONObject permanentData) {
        boolean directInstall = shouldUseDirectInstall(state);
        DirectInstallResolution directResolution = directInstall
                ? resolveDirectInstallTenant(permanentData, Boolean.TRUE.equals(state == null ? null : state.getDirectInstall()))
                : null;
        Long tenantId = directInstall ? directResolution.getTenantId() : state.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom authorized tenant is empty");
        }
        TenantContextHolder.setTenantId(tenantId);
        WecomCorpConfig config = upsertAuthorizedConfig(tenantId, permanentData);
        syncOrganizationAfterAuthorization(config);
        AuthProcessResult result = new AuthProcessResult();
        result.setTenantId(tenantId);
        result.setIdentityId(directResolution == null ? null : directResolution.getIdentityId());
        return result;
    }

    private DirectInstallResolution resolveDirectInstallTenant(JSONObject permanentData, boolean loginRequested) {
        JSONObject corpInfo = permanentData.getJSONObject("auth_corp_info");
        JSONObject authUserInfo = permanentData.getJSONObject("auth_user_info");
        String corpId = resolveAuthorizedCorpId(permanentData);
        String corpName = corpInfo == null ? corpId : StrUtil.blankToDefault(corpInfo.getString("corp_name"), corpId);
        ExternalTenantBinding existingBinding = tenantBindingMapper.selectOne(Wrappers.<ExternalTenantBinding>lambdaQuery()
                .eq(ExternalTenantBinding::getProvider, "wecom")
                .eq(ExternalTenantBinding::getExternalTenantKey, corpId)
                .eq(ExternalTenantBinding::getStatus, ENABLED_STATUS)
                .last("LIMIT 1"));
        if (existingBinding != null && existingBinding.getTenantId() != null) {
            DirectInstallResolution result = new DirectInstallResolution();
            result.setTenantId(existingBinding.getTenantId());
            if (loginRequested) {
                ExternalAuthIdentity identity = ensureDirectInstallLoginIdentity(permanentData, existingBinding.getTenantId());
                result.setIdentityId(identity == null ? null : identity.getId());
            }
            return result;
        }

        String wecomUserId = authUserInfo == null ? null : authUserInfo.getString("userid");
        if (StrUtil.isBlank(wecomUserId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom direct install auth user is empty");
        }
        String displayName = firstNotBlank(
                authUserInfo.getString("name"),
                authUserInfo.getString("username"),
                wecomUserId
        );
        String email = normalizeEmail(firstNotBlank(
                authUserInfo.getString("email"),
                authUserInfo.getString("biz_mail")
        ));
        String mobile = StrUtil.trim(authUserInfo.getString("mobile"));
        String username = firstNotBlank(email, mobile, wecomUserId);

        ExternalTenantRegisterBO registerBO = new ExternalTenantRegisterBO();
        registerBO.setUsername(username);
        registerBO.setEmail(email);
        registerBO.setMobile(StrUtil.isBlank(mobile) ? null : mobile);
        registerBO.setCompanyName(corpName);
        registerBO.setRealname(displayName);
        registerBO.setEmailVerificationRequired(Boolean.FALSE);
        ManagerUser user = registrationService.registerExternalTenant(registerBO);
        if (user == null || user.getTenantId() == null || user.getUserId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom direct install tenant registration failed");
        }
        ExternalAuthIdentity identity = upsertDirectInstallIdentity(permanentData, user, corpId, wecomUserId, displayName, email);
        upsertDirectInstallEmployee(user, corpId, wecomUserId, displayName, email, StrUtil.isBlank(mobile) ? null : mobile);
        DirectInstallResolution result = new DirectInstallResolution();
        result.setTenantId(user.getTenantId());
        result.setIdentityId(identity == null ? null : identity.getId());
        return result;
    }

    private String resolveAuthorizedCorpId(JSONObject permanentData) {
        JSONObject corpInfo = permanentData.getJSONObject("auth_corp_info");
        String corpId = firstNotBlank(
                permanentData.getString("auth_corpid"),
                corpInfo == null ? null : corpInfo.getString("corpid")
        );
        if (StrUtil.isBlank(corpId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom authorized corp id is empty");
        }
        return corpId;
    }

    private ExternalAuthIdentity ensureDirectInstallLoginIdentity(JSONObject permanentData, Long tenantId) {
        JSONObject authUserInfo = permanentData.getJSONObject("auth_user_info");
        String corpId = resolveAuthorizedCorpId(permanentData);
        String wecomUserId = authUserInfo == null ? null : authUserInfo.getString("userid");
        if (StrUtil.isBlank(wecomUserId)) {
            return null;
        }
        String displayName = firstNotBlank(
                authUserInfo.getString("name"),
                authUserInfo.getString("username"),
                wecomUserId
        );
        String email = normalizeEmail(firstNotBlank(
                authUserInfo.getString("email"),
                authUserInfo.getString("biz_mail")
        ));
        String mobile = StrUtil.trim(authUserInfo.getString("mobile"));
        ManagerUser user = findDirectInstallManagerUser(tenantId, corpId, wecomUserId);
        if (user == null) {
            user = createDirectInstallManagerUser(tenantId, corpId, wecomUserId, displayName, email,
                    StrUtil.isBlank(mobile) ? null : mobile);
        }
        ExternalAuthIdentity identity = upsertDirectInstallIdentity(permanentData, user, corpId, wecomUserId, displayName, email);
        upsertDirectInstallEmployee(user, corpId, wecomUserId, displayName, email, StrUtil.isBlank(mobile) ? null : mobile);
        return identity;
    }

    private ManagerUser findDirectInstallManagerUser(Long tenantId, String corpId, String wecomUserId) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            WecomEmployee employee = withWecomEmployeeDataPermission(() ->
                    employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                            .eq(WecomEmployee::getCorpId, corpId)
                            .eq(WecomEmployee::getUserId, wecomUserId)
                            .isNotNull(WecomEmployee::getCrmUserId)
                            .last("LIMIT 1")));
            if (employee != null && employee.getCrmUserId() != null) {
                ManagerUser mappedUser = managerUserMapper.selectById(employee.getCrmUserId());
                if (mappedUser != null) {
                    return mappedUser;
                }
            }
            return managerUserMapper.selectOne(Wrappers.<ManagerUser>lambdaQuery()
                    .eq(ManagerUser::getWecomCorpId, corpId)
                    .eq(ManagerUser::getWecomUserId, wecomUserId)
                    .last("LIMIT 1"));
        } finally {
            restoreTenantContext(previousTenantId);
        }
    }

    private ManagerUser createDirectInstallManagerUser(Long tenantId,
                                                       String corpId,
                                                       String wecomUserId,
                                                       String displayName,
                                                       String email,
                                                       String mobile) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            ManagerUser user = new ManagerUser();
            user.setUsername(resolveDirectInstallUsername(wecomUserId, email, mobile));
            user.setPassword(encodeGeneratedPassword());
            user.setRealname(StrUtil.blankToDefault(displayName, wecomUserId));
            user.setEmail(email);
            user.setMobile(mobile);
            user.setStatus(ENABLED_STATUS);
            user.setWecomCorpId(corpId);
            user.setWecomUserId(wecomUserId);
            user.setWecomSyncedAt(new Date());
            user.setCreateTime(new Date());
            managerUserMapper.insert(user);
            return user;
        } finally {
            restoreTenantContext(previousTenantId);
        }
    }

    private String resolveDirectInstallUsername(String wecomUserId, String email, String mobile) {
        String username = firstNotBlank(email, mobile, wecomUserId, "wecom_user");
        if (username.length() > 64) {
            username = firstNotBlank(wecomUserId, "wecom_user");
        }
        if (username.length() > 64) {
            username = username.substring(0, 64);
        }
        if (managerUserMapper.selectCount(Wrappers.<ManagerUser>lambdaQuery()
                .eq(ManagerUser::getUsername, username)) == 0) {
            return username;
        }
        return "wecom_" + IdUtil.fastSimpleUUID().substring(0, 12);
    }

    private String encodeGeneratedPassword() {
        String password = "Wecom" + IdUtil.fastSimpleUUID().substring(0, 12);
        return passwordEncoder == null ? password : passwordEncoder.encode(password);
    }

    private ExternalAuthIdentity upsertDirectInstallIdentity(JSONObject permanentData,
                                                             ManagerUser user,
                                                             String corpId,
                                                             String wecomUserId,
                                                             String displayName,
                                                             String email) {
        String subject = corpId + ":" + wecomUserId;
        ExternalAuthIdentity identity = identityMapper.selectOne(Wrappers.<ExternalAuthIdentity>lambdaQuery()
                .eq(ExternalAuthIdentity::getProvider, "wecom")
                .eq(ExternalAuthIdentity::getSubject, subject)
                .last("LIMIT 1"));
        if (identity == null) {
            identity = new ExternalAuthIdentity();
            identity.setProvider("wecom");
            identity.setSubject(subject);
            identity.setBindTime(new Date());
            identity.setCreateTime(new Date());
        }
        identity.setTenantId(user.getTenantId());
        identity.setUserId(user.getUserId());
        identity.setEmail(email);
        identity.setEmailVerified(Boolean.FALSE);
        identity.setDisplayName(displayName);
        identity.setExternalTenantKey(corpId);
        identity.setRawProfile(permanentData.toJSONString());
        identity.setStatus(ENABLED_STATUS);
        identity.setUpdateTime(new Date());
        if (identity.getId() == null) {
            identityMapper.insert(identity);
        } else {
            identityMapper.updateById(identity);
        }
        return identity;
    }

    private void upsertDirectInstallEmployee(ManagerUser user,
                                             String corpId,
                                             String wecomUserId,
                                             String displayName,
                                             String email,
                                             String mobile) {
        Long previousTenantId = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(user.getTenantId());
        try {
            runWithWecomEmployeeDataPermission(() -> {
                WecomEmployee employee = employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                        .eq(WecomEmployee::getCorpId, corpId)
                        .eq(WecomEmployee::getUserId, wecomUserId)
                        .last("LIMIT 1"));
                if (employee == null) {
                    employee = new WecomEmployee();
                    employee.setCorpId(corpId);
                    employee.setUserId(wecomUserId);
                }
                employee.setCrmUserId(user.getUserId());
                employee.setName(StrUtil.blankToDefault(displayName, wecomUserId));
                employee.setEmail(email);
                employee.setMobile(mobile);
                employee.setStatus(ENABLED_STATUS);
                employee.setSyncedAt(new Date());
                if (employee.getId() == null) {
                    employeeMapper.insert(employee);
                } else {
                    employeeMapper.updateById(employee);
                }
            });
        } finally {
            restoreTenantContext(previousTenantId);
        }
    }

    private WecomCorpConfig upsertAuthorizedConfig(Long tenantId, JSONObject permanentData) {
        JSONObject corpInfo = permanentData.getJSONObject("auth_corp_info");
        JSONObject authInfo = permanentData.getJSONObject("auth_info");
        JSONObject authUserInfo = permanentData.getJSONObject("auth_user_info");
        String corpId = resolveAuthorizedCorpId(permanentData);
        WecomCorpConfig config = configMapper.selectOne(Wrappers.<WecomCorpConfig>lambdaQuery()
                .eq(WecomCorpConfig::getCorpId, corpId)
                .last("LIMIT 1"));
        if (config == null) {
            config = new WecomCorpConfig();
            config.setArchiveEnabled(Boolean.FALSE);
            config.setCustomerContactEnabled(Boolean.TRUE);
            config.setSyncEnabled(Boolean.TRUE);
        }
        config.setTenantId(tenantId);
        config.setCorpId(corpId);
        config.setCorpName(corpInfo == null ? corpId : StrUtil.blankToDefault(corpInfo.getString("corp_name"), corpId));
        config.setAgentId(resolveAgentId(authInfo));
        config.setSuiteId(properties.getSuiteId());
        config.setPermanentCodeEncrypted(secretTextCipher.encrypt(permanentData.getString("permanent_code")));
        config.setAuthInfoJson(authInfo == null ? null : authInfo.toJSONString());
        config.setAuthCorpInfoJson(corpInfo == null ? null : corpInfo.toJSONString());
        config.setAuthStatus(AUTH_STATUS_AUTHORIZED);
        config.setAuthorizedAt(new Date());
        config.setUnauthorizedAt(null);
        if (authUserInfo != null) {
            config.setAuthUserId(authUserInfo.getString("userid"));
            config.setAuthUserName(authUserInfo.getString("name"));
        }
        if (config.getId() == null) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        upsertTenantBinding(tenantId, corpId, config.getCorpName());
        redis.del(CORP_TOKEN_KEY_PREFIX + corpId);
        return config;
    }

    private void syncOrganizationAfterAuthorization(WecomCorpConfig config) {
        if (syncService == null || config == null) {
            return;
        }
        syncTaskExecutor.submitWithTenant("wecom-auth-org-sync-" + config.getCorpId(), config.getTenantId(), () -> {
            try {
                runWithWecomEmployeeDataPermission(() -> syncService.syncOrganization(config));
            } catch (Exception e) {
                log.warn("WeCom organization sync after authorization failed: corpId={}, error={}",
                        config.getCorpId(), e.getMessage());
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException(e);
            }
        });
    }

    private void upsertTenantBinding(Long tenantId, String corpId, String corpName) {
        ExternalTenantBinding binding = tenantBindingMapper.selectOne(Wrappers.<ExternalTenantBinding>lambdaQuery()
                .eq(ExternalTenantBinding::getProvider, "wecom")
                .eq(ExternalTenantBinding::getExternalTenantKey, corpId)
                .last("LIMIT 1"));
        if (binding == null) {
            binding = new ExternalTenantBinding();
            binding.setProvider("wecom");
            binding.setExternalTenantKey(corpId);
            binding.setCreateTime(new Date());
        }
        binding.setExternalTenantName(StrUtil.blankToDefault(corpName, corpId));
        binding.setTenantId(tenantId);
        binding.setStatus(ENABLED_STATUS);
        binding.setUpdateTime(new Date());
        if (binding.getId() == null) {
            tenantBindingMapper.insert(binding);
        } else {
            tenantBindingMapper.updateById(binding);
        }
    }

    private void markUnauthorized(String corpId) {
        if (StrUtil.isBlank(corpId)) {
            return;
        }
        List<WecomCorpConfig> configs = configMapper.selectThirdPartyByCorpIdIgnoreTenant(corpId);
        for (WecomCorpConfig config : configs) {
            Long previousTenantId = TenantContextHolder.getTenantId();
            TenantContextHolder.setTenantId(config.getTenantId());
            try {
                config.setAuthStatus(AUTH_STATUS_UNAUTHORIZED);
                config.setUnauthorizedAt(new Date());
                configMapper.updateById(config);
            } finally {
                restoreTenantContext(previousTenantId);
            }
        }
        redis.del(CORP_TOKEN_KEY_PREFIX + corpId);
    }

    private String resolveAgentId(JSONObject authInfo) {
        if (authInfo == null) {
            return null;
        }
        JSONArray agents = authInfo.getJSONArray("agent");
        if (agents == null || agents.isEmpty()) {
            return null;
        }
        JSONObject first = agents.getJSONObject(0);
        String agentId = first.getString("agentid");
        if (StrUtil.isBlank(agentId)) {
            agentId = first.getString("appid");
        }
        return agentId;
    }

    private JSONObject parseCallbackEvent(String body,
                                          String msgSignature,
                                          String timestamp,
                                          String nonce) {
        String encryptedText = text(parseXml(body), "Encrypt");
        String rawXml = StrUtil.isBlank(encryptedText)
                ? body
                : cryptoService.decrypt(msgSignature, timestamp, nonce, encryptedText);
        JSONObject event = xmlToJson(rawXml);
        event.put("_rawXml", rawXml);
        return event;
    }

    private JSONObject xmlToJson(String xml) {
        Element root = parseXml(xml);
        JSONObject json = new JSONObject();
        for (int i = 0; i < root.getChildNodes().getLength(); i++) {
            if (root.getChildNodes().item(i) instanceof Element child) {
                json.put(child.getTagName(), child.getTextContent());
            }
        }
        return json;
    }

    private Element parseXml(String xml) {
        if (StrUtil.isBlank(xml)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom callback body is empty");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            return document.getDocumentElement();
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom callback xml parse failed");
        }
    }

    private String text(Element root, String tagName) {
        if (root == null || root.getElementsByTagName(tagName).getLength() == 0) {
            return null;
        }
        return root.getElementsByTagName(tagName).item(0).getTextContent();
    }

    private String resolveAuthCallbackUri(HttpServletRequest request) {
        if (StrUtil.isNotBlank(properties.getAuthRedirectUri())) {
            return properties.getAuthRedirectUri();
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/wecom/open/auth/callback")
                .build()
                .toUriString();
    }

    private String resolveFrontendRedirect(String redirect, HttpServletRequest request) {
        if (StrUtil.isNotBlank(redirect)) {
            return redirect;
        }
        if (StrUtil.isNotBlank(properties.getFrontendRedirectUri())) {
            return properties.getFrontendRedirectUri();
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/").build().toUriString();
    }

    private AuthState readAuthState(String state) {
        if (StrUtil.isBlank(state)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Missing WeCom auth state");
        }
        String raw = redis.get(authStateKey(state));
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom auth state expired");
        }
        return JSON.parseObject(raw, AuthState.class);
    }

    private AuthResult readAuthResult(String state) {
        if (StrUtil.isBlank(state)) {
            return null;
        }
        String raw = redis.get(authResultKey(state));
        return StrUtil.isBlank(raw) ? null : JSON.parseObject(raw, AuthResult.class);
    }

    private void storeAuthResult(String state, Long tenantId, Long identityId, String status, String message) {
        if (StrUtil.isBlank(state)) {
            return;
        }
        AuthResult result = new AuthResult();
        result.setTenantId(tenantId);
        result.setIdentityId(identityId);
        result.setStatus(status);
        result.setMessage(message);
        redis.setex(authResultKey(state), safeTtl(properties.getStateTtlSeconds()), JSON.toJSONString(result));
    }

    private String appendAuthResult(String redirect, AuthResult result) {
        if (result == null || "success".equals(result.getStatus())) {
            if (result != null && result.getIdentityId() != null) {
                return appendQuery(redirect,
                        "externalLoginTicket", createExternalLoginTicket(result.getIdentityId()),
                        "provider", "wecom");
            }
            return appendQuery(redirect, "wecomAuth", "success");
        }
        return appendQuery(redirect, "wecomAuth", "error", "message",
                StrUtil.blankToDefault(result.getMessage(), "callback_failed"));
    }

    private String appendAuthResult(String redirect, AuthProcessResult result) {
        AuthResult authResult = new AuthResult();
        authResult.setTenantId(result == null ? null : result.getTenantId());
        authResult.setIdentityId(result == null ? null : result.getIdentityId());
        authResult.setStatus("success");
        return appendAuthResult(redirect, authResult);
    }

    private String createExternalLoginTicket(Long identityId) {
        String ticketValue = IdUtil.fastSimpleUUID();
        ExternalAuthServiceImpl.ExternalLoginTicket ticket = new ExternalAuthServiceImpl.ExternalLoginTicket();
        ticket.setProvider("wecom");
        ticket.setIdentityId(identityId);
        redis.setex(EXTERNAL_LOGIN_TICKET_KEY_PREFIX + ticketValue,
                safeTtl(properties.getStateTtlSeconds()),
                JSON.toJSONString(ticket));
        return ticketValue;
    }

    private String decryptRequired(String encrypted, String label) {
        if (StrUtil.isBlank(encrypted)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom " + label + " is not configured");
        }
        try {
            return secretTextCipher.decrypt(encrypted);
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom " + label + " decrypt failed");
        }
    }

    private void requireUsable() {
        if (!isUsable()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom third-party app is not configured");
        }
    }

    private void requireLoginUsable() {
        if (!isLoginUsable()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom provider login is not configured");
        }
    }

    private int tokenTtl(Integer expiresIn) {
        int expires = expiresIn == null || expiresIn <= 0 ? 7200 : expiresIn;
        int buffer = properties.getTokenCacheTtlBufferSeconds() == null ? 300 : properties.getTokenCacheTtlBufferSeconds();
        return Math.max(60, expires - Math.max(buffer, 0));
    }

    private Integer safeTtl(Integer ttl) {
        return ttl == null || ttl <= 0 ? 600 : ttl;
    }

    private String authStateKey(String state) {
        return AUTH_STATE_KEY_PREFIX + state;
    }

    private String authResultKey(String state) {
        return AUTH_RESULT_KEY_PREFIX + state;
    }

    private String appendQuery(String redirect, String... pairs) {
        StringBuilder builder = new StringBuilder(StrUtil.blankToDefault(redirect, "/"));
        String separator = builder.toString().contains("?") ? "&" : "?";
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (StrUtil.isBlank(pairs[i + 1])) {
                continue;
            }
            builder.append(separator)
                    .append(encode(pairs[i]))
                    .append("=")
                    .append(encode(pairs[i + 1]));
            separator = "&";
        }
        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalizeEmail(String email) {
        return StrUtil.isBlank(email) ? null : StrUtil.trim(email).toLowerCase(Locale.ROOT);
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean shouldUseDirectInstall(AuthState state) {
        return state == null || state.getTenantId() == null || Boolean.TRUE.equals(state.getDirectInstall());
    }

    private void runWithWecomEmployeeDataPermission(Runnable action) {
        withWecomEmployeeDataPermission(() -> {
            action.run();
            return null;
        });
    }

    private <T> T withWecomEmployeeDataPermission(Supplier<T> action) {
        DataPermissionContext previousContext = DataPermissionHolder.get(WECOM_EMPLOYEE_PERMISSION_MODULE);
        DataPermissionHolder.put(WECOM_EMPLOYEE_PERMISSION_MODULE, DataPermissionContext.all());
        try {
            return action.get();
        } finally {
            if (previousContext == null) {
                DataPermissionHolder.remove(WECOM_EMPLOYEE_PERMISSION_MODULE);
            } else {
                DataPermissionHolder.put(WECOM_EMPLOYEE_PERMISSION_MODULE, previousContext);
            }
        }
    }

    private void restoreTenantContext(Long previousTenantId) {
        if (previousTenantId != null) {
            TenantContextHolder.setTenantId(previousTenantId);
        } else {
            TenantContextHolder.clear();
        }
    }

    @Data
    public static class AuthState {
        private Long tenantId;
        private String redirect;
        private Boolean directInstall;
    }

    @Data
    public static class AuthResult {
        private Long tenantId;
        private Long identityId;
        private String status;
        private String message;
    }

    @Data
    private static class AuthProcessResult {
        private Long tenantId;
        private Long identityId;
    }

    @Data
    private static class DirectInstallResolution {
        private Long tenantId;
        private Long identityId;
    }
}
