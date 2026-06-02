package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.ExternalAuthProperties;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.ExternalAuthRegisterBO;
import com.kakarote.ai_crm.entity.BO.ExternalAuthTicketLoginBO;
import com.kakarote.ai_crm.entity.BO.ExternalTenantMemberRegisterBO;
import com.kakarote.ai_crm.entity.BO.ExternalTenantRegisterBO;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ExternalAuthIdentity;
import com.kakarote.ai_crm.entity.PO.ExternalTenantBinding;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.ExternalAuthAuthorizeVO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthBindingVO;
import com.kakarote.ai_crm.entity.VO.ExternalAuthProviderVO;
import com.kakarote.ai_crm.entity.VO.LoginResponseVO;
import com.kakarote.ai_crm.mapper.ExternalAuthIdentityMapper;
import com.kakarote.ai_crm.mapper.ExternalTenantBindingMapper;
import com.kakarote.ai_crm.service.AuthSessionService;
import com.kakarote.ai_crm.service.ExternalAuthService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.RegistrationService;
import com.kakarote.ai_crm.utils.UserUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class ExternalAuthServiceImpl implements ExternalAuthService {

    private static final List<String> PROVIDER_ORDER = List.of("google", "outlook", "wechat", "wecom");
    private static final Set<String> PROVIDERS = Set.copyOf(PROVIDER_ORDER);
    private static final String SCENE_LOGIN = "LOGIN";
    private static final String SCENE_BIND = "BIND";
    private static final int ENABLED_STATUS = 1;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ExternalAuthProperties properties;

    @Autowired
    private Redis redis;

    @Autowired
    private ExternalAuthIdentityMapper identityMapper;

    @Autowired
    private ExternalTenantBindingMapper tenantBindingMapper;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private AuthSessionService authSessionService;

    @PostConstruct
    void configureRestTemplateProxy() {
        ExternalAuthProperties.ProxyConfig proxyConfig = properties.getProxy();
        if (proxyConfig == null || !proxyConfig.isUsable()) {
            return;
        }
        URI proxyUri;
        try {
            proxyUri = parseProxyUri(proxyConfig.getUrl());
        } catch (IllegalArgumentException e) {
            log.warn("External auth proxy is ignored because url is invalid: {}", proxyConfig.getUrl());
            return;
        }
        String host = proxyUri.getHost();
        int port = proxyUri.getPort();
        if (StrUtil.isBlank(host) || port < 0) {
            log.warn("External auth proxy is ignored because url is invalid: {}", proxyConfig.getUrl());
            return;
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(new Proxy(resolveProxyType(proxyUri.getScheme()), new InetSocketAddress(host, port)));
        restTemplate.setRequestFactory(requestFactory);
        log.info("External auth proxy enabled: {}://{}:{}", StrUtil.blankToDefault(proxyUri.getScheme(), "http"), host, port);
    }

    @Override
    public List<ExternalAuthProviderVO> listProviders() {
        List<ExternalAuthProviderVO> providers = new ArrayList<>();
        for (String provider : PROVIDER_ORDER) {
            ExternalAuthProviderVO vo = new ExternalAuthProviderVO();
            vo.setProvider(provider);
            vo.setName(providerName(provider));
            ExternalAuthProperties.ProviderConfig config = properties.getProvider(provider);
            vo.setEnabled(config != null && config.isUsable(provider));
            providers.add(vo);
        }
        return providers;
    }

    @Override
    public ExternalAuthAuthorizeVO createAuthorizeUrl(String provider, String redirect, HttpServletRequest request) {
        String normalizedProvider = normalizeProvider(provider);
        ExternalAuthProperties.ProviderConfig config = requireUsableProvider(normalizedProvider);
        AuthState authState = new AuthState();
        authState.setProvider(normalizedProvider);
        authState.setScene(SCENE_LOGIN);
        authState.setRedirect(resolveRedirect(redirect, request));
        return buildAuthorizeVO(normalizedProvider, config, storeState(authState), request);
    }

    @Override
    public ExternalAuthAuthorizeVO createBindAuthorizeUrl(String provider, String redirect, HttpServletRequest request) {
        String normalizedProvider = normalizeProvider(provider);
        ExternalAuthProperties.ProviderConfig config = requireUsableProvider(normalizedProvider);
        LoginUser loginUser = UserUtil.getLoginUser();
        AuthState authState = new AuthState();
        authState.setProvider(normalizedProvider);
        authState.setScene(SCENE_BIND);
        authState.setRedirect(resolveRedirect(redirect, request));
        authState.setUserId(loginUser.getUser().getUserId());
        authState.setTenantId(loginUser.getUser().getTenantId());
        return buildAuthorizeVO(normalizedProvider, config, storeState(authState), request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String provider,
                                 String code,
                                 String state,
                                 String error,
                                 HttpServletRequest request) {
        String normalizedProvider = normalizeProvider(provider);
        AuthState authState = consumeState(state);
        if (!Objects.equals(normalizedProvider, authState.getProvider())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Invalid external auth state");
        }
        if (StrUtil.isNotBlank(error)) {
            return appendQuery(authState.getRedirect(), "externalAuthError", error, "provider", normalizedProvider);
        }
        if (StrUtil.isBlank(code)) {
            return appendQuery(authState.getRedirect(), "externalAuthError", "missing_code", "provider", normalizedProvider);
        }

        ExternalProfile profile = fetchProfile(normalizedProvider, code, request);
        if (SCENE_BIND.equals(authState.getScene())) {
            bindIdentity(authState, profile);
            return appendQuery(authState.getRedirect(), "externalBind", "success", "provider", normalizedProvider);
        }

        ExternalAuthIdentity identity = findIdentity(normalizedProvider, profile.getSubject());
        if (identity != null) {
            String ticket = createLoginTicket(identity);
            return appendQuery(authState.getRedirect(), "externalLoginTicket", ticket, "provider", normalizedProvider);
        }

        if ("wecom".equals(normalizedProvider) && StrUtil.isNotBlank(profile.getExternalTenantKey())) {
            ExternalAuthIdentity provisionedIdentity = autoProvisionWecomIdentity(profile);
            String ticket = createLoginTicket(provisionedIdentity);
            return appendQuery(authState.getRedirect(), "externalLoginTicket", ticket, "provider", normalizedProvider);
        }

        String ticket = createRegisterTicket(profile);
        return appendQuery(authState.getRedirect(), "externalRegisterTicket", ticket, "provider", normalizedProvider);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseVO loginByTicket(ExternalAuthTicketLoginBO loginBO,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        ExternalLoginTicket ticket = consumeTicket(loginTicketKey(loginBO.getTicket()), ExternalLoginTicket.class);
        ExternalAuthIdentity identity = identityMapper.selectById(ticket.getIdentityId());
        if (identity == null || !Objects.equals(identity.getStatus(), ENABLED_STATUS)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
        }

        Long previousTenantId = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(identity.getTenantId());
        try {
            ManagerUser user = manageUserService.getById(identity.getUserId());
            if (user == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
            }
            if (!Integer.valueOf(1).equals(user.getStatus())) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DISABLED);
            }
            identity.setLastLoginTime(new Date());
            identity.setUpdateTime(new Date());
            identityMapper.updateById(identity);
            return authSessionService.createLoginResponse(user, LoginTypeEnum.resolve(loginBO.getLoginType()), request, response);
        } finally {
            restoreTenantContext(previousTenantId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseVO registerByTicket(ExternalAuthRegisterBO registerBO,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        String ticketKey = registerTicketKey(registerBO.getTicket());
        ExternalRegisterTicket ticket = readTicket(ticketKey, ExternalRegisterTicket.class);
        ExternalProfile profile = ticket.getProfile();
        if (findIdentity(profile.getProvider(), profile.getSubject()) != null) {
            redis.del(ticketKey);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "External identity is already bound");
        }

        String email = resolveRegistrationEmail(profile);
        String realname = StrUtil.blankToDefault(StrUtil.trim(profile.getDisplayName()), email);

        ManagerUser user;
        if ("wecom".equals(profile.getProvider()) && StrUtil.isNotBlank(profile.getExternalTenantKey())) {
            ExternalTenantBinding binding = findTenantBinding(profile.getProvider(), profile.getExternalTenantKey());
            if (binding == null) {
                ExternalTenantRegisterBO tenantRegisterBO = new ExternalTenantRegisterBO();
                tenantRegisterBO.setEmail(email);
                tenantRegisterBO.setPassword(registerBO.getPassword());
                tenantRegisterBO.setCompanyName(StrUtil.blankToDefault(
                        StrUtil.trim(registerBO.getCompanyName()),
                        StrUtil.blankToDefault(profile.getExternalTenantName(), "WeCom Enterprise")
                ));
                tenantRegisterBO.setRealname(realname);
                tenantRegisterBO.setEmailVerificationRequired(Boolean.FALSE);
                user = registrationService.registerExternalTenant(tenantRegisterBO);
                saveTenantBinding(profile, user.getTenantId(), tenantRegisterBO.getCompanyName());
            } else {
                ExternalTenantMemberRegisterBO memberRegisterBO = new ExternalTenantMemberRegisterBO();
                memberRegisterBO.setTenantId(binding.getTenantId());
                memberRegisterBO.setEmail(email);
                memberRegisterBO.setPassword(registerBO.getPassword());
                memberRegisterBO.setRealname(realname);
                user = registrationService.registerExternalTenantMember(memberRegisterBO);
            }
        } else {
            ExternalTenantRegisterBO tenantRegisterBO = new ExternalTenantRegisterBO();
            tenantRegisterBO.setEmail(email);
            tenantRegisterBO.setPassword(registerBO.getPassword());
            tenantRegisterBO.setCompanyName(StrUtil.trim(registerBO.getCompanyName()));
            tenantRegisterBO.setRealname(realname);
            tenantRegisterBO.setEmailVerificationRequired(Boolean.FALSE);
            user = registrationService.registerExternalTenant(tenantRegisterBO);
        }

        saveIdentity(profile, user.getTenantId(), user.getUserId());
        redis.del(ticketKey);
        return authSessionService.createLoginResponse(user, LoginTypeEnum.resolve(registerBO.getLoginType()), request, response);
    }

    @Override
    public List<ExternalAuthBindingVO> listBindings() {
        LoginUser loginUser = UserUtil.getLoginUser();
        Long tenantId = loginUser.getUser().getTenantId();
        Long userId = loginUser.getUser().getUserId();
        List<ExternalAuthIdentity> identities = identityMapper.selectList(
                Wrappers.<ExternalAuthIdentity>lambdaQuery()
                        .eq(ExternalAuthIdentity::getTenantId, tenantId)
                        .eq(ExternalAuthIdentity::getUserId, userId)
                        .eq(ExternalAuthIdentity::getStatus, ENABLED_STATUS)
        );

        List<ExternalAuthBindingVO> result = new ArrayList<>();
        for (String provider : PROVIDER_ORDER) {
            ExternalAuthIdentity identity = identities.stream()
                    .filter(item -> provider.equals(item.getProvider()))
                    .findFirst()
                    .orElse(null);
            ExternalAuthBindingVO vo = new ExternalAuthBindingVO();
            vo.setProvider(provider);
            vo.setProviderName(providerName(provider));
            vo.setBound(identity != null);
            if (identity != null) {
                vo.setDisplayName(identity.getDisplayName());
                vo.setEmail(identity.getEmail());
                vo.setBindTime(identity.getBindTime());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        LoginUser loginUser = UserUtil.getLoginUser();
        identityMapper.delete(Wrappers.<ExternalAuthIdentity>lambdaQuery()
                .eq(ExternalAuthIdentity::getProvider, normalizedProvider)
                .eq(ExternalAuthIdentity::getTenantId, loginUser.getUser().getTenantId())
                .eq(ExternalAuthIdentity::getUserId, loginUser.getUser().getUserId()));
    }

    private ExternalAuthAuthorizeVO buildAuthorizeVO(String provider,
                                                     ExternalAuthProperties.ProviderConfig config,
                                                     String state,
                                                     HttpServletRequest request) {
        String callbackUri = resolveCallbackUri(provider, config, request);
        String clientId = config.resolveClientId(provider);
        String authorizeUrl = switch (provider) {
            case "google" -> UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                    .queryParam("client_id", clientId)
                    .queryParam("redirect_uri", callbackUri)
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid email profile")
                    .queryParam("state", state)
                    .queryParam("access_type", "offline")
                    .build()
                    .toUriString();
            case "outlook" -> UriComponentsBuilder.fromHttpUrl(outlookOAuthUrl(config, "authorize"))
                    .queryParam("client_id", clientId)
                    .queryParam("redirect_uri", callbackUri)
                    .queryParam("response_type", "code")
                    .queryParam("response_mode", "query")
                    .queryParam("scope", "openid email profile")
                    .queryParam("state", state)
                    .build()
                    .toUriString();
            case "wechat" -> UriComponentsBuilder.fromHttpUrl("https://open.weixin.qq.com/connect/qrconnect")
                    .queryParam("appid", clientId)
                    .queryParam("redirect_uri", callbackUri)
                    .queryParam("response_type", "code")
                    .queryParam("scope", "snsapi_login")
                    .queryParam("state", state)
                    .build()
                    .toUriString() + "#wechat_redirect";
            case "wecom" -> UriComponentsBuilder.fromHttpUrl("https://open.work.weixin.qq.com/wwopen/sso/qrConnect")
                    .queryParam("appid", clientId)
                    .queryParamIfPresent("agentid", StrUtil.isBlank(config.getAgentId())
                            ? java.util.Optional.empty()
                            : java.util.Optional.of(config.getAgentId()))
                    .queryParam("redirect_uri", callbackUri)
                    .queryParam("state", state)
                    .queryParam("login_type", "CorpApp")
                    .build()
                    .toUriString();
            default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Unsupported provider");
        };

        ExternalAuthAuthorizeVO vo = new ExternalAuthAuthorizeVO();
        vo.setProvider(provider);
        vo.setAuthorizeUrl(authorizeUrl);
        return vo;
    }

    private ExternalProfile fetchProfile(String provider, String code, HttpServletRequest request) {
        ExternalAuthProperties.ProviderConfig config = requireUsableProvider(provider);
        String callbackUri = resolveCallbackUri(provider, config, request);
        try {
            return switch (provider) {
                case "google" -> fetchGoogleProfile(code, callbackUri, config);
                case "outlook" -> fetchOutlookProfile(code, callbackUri, config);
                case "wechat" -> fetchWechatProfile(code, config);
                case "wecom" -> fetchWecomProfile(code, config);
                default -> throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Unsupported provider");
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("External auth profile fetch failed: provider={}, error={}", provider, e.getMessage());
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "External auth failed");
        }
    }

    private ExternalProfile fetchGoogleProfile(String code,
                                               String callbackUri,
                                               ExternalAuthProperties.ProviderConfig config) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("redirect_uri", callbackUri);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String tokenJson = restTemplate.postForObject(
                "https://oauth2.googleapis.com/token",
                new HttpEntity<>(body, headers),
                String.class
        );
        JSONObject token = JSON.parseObject(tokenJson);
        String accessToken = token.getString("access_token");
        if (StrUtil.isBlank(accessToken)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Google token exchange failed");
        }

        String userInfoJson = restTemplate.getForObject(
                UriComponentsBuilder.fromHttpUrl("https://www.googleapis.com/oauth2/v3/userinfo")
                        .queryParam("access_token", accessToken)
                        .build()
                        .toUriString(),
                String.class
        );
        JSONObject userInfo = JSON.parseObject(userInfoJson);
        ExternalProfile profile = new ExternalProfile();
        profile.setProvider("google");
        profile.setSubject(userInfo.getString("sub"));
        profile.setEmail(normalizeEmail(userInfo.getString("email")));
        profile.setEmailVerified(Boolean.TRUE.equals(userInfo.getBoolean("email_verified")));
        profile.setDisplayName(userInfo.getString("name"));
        profile.setAvatarUrl(userInfo.getString("picture"));
        profile.setRawJson(userInfo.toJSONString());
        validateProfile(profile);
        return profile;
    }

    private ExternalProfile fetchOutlookProfile(String code,
                                                String callbackUri,
                                                ExternalAuthProperties.ProviderConfig config) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("redirect_uri", callbackUri);
        body.add("grant_type", "authorization_code");
        body.add("scope", "openid email profile");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String tokenJson = restTemplate.postForObject(
                outlookOAuthUrl(config, "token"),
                new HttpEntity<>(body, headers),
                String.class
        );
        JSONObject token = JSON.parseObject(tokenJson);
        String accessToken = token.getString("access_token");
        if (StrUtil.isBlank(accessToken)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Outlook token exchange failed");
        }

        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.setBearerAuth(accessToken);
        String userInfoJson = restTemplate.exchange(
                "https://graph.microsoft.com/oidc/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(userInfoHeaders),
                String.class
        ).getBody();
        JSONObject userInfo = JSON.parseObject(userInfoJson);
        JSONObject idClaims = parseJwtPayload(token.getString("id_token"));

        String email = firstNotBlank(
                userInfo.getString("email"),
                idClaims.getString("email"),
                idClaims.getString("preferred_username"),
                userInfo.getString("preferred_username")
        );
        String subject = firstNotBlank(userInfo.getString("sub"), idClaims.getString("sub"), idClaims.getString("oid"));
        String displayName = firstNotBlank(userInfo.getString("name"), idClaims.getString("name"), email, subject);

        JSONObject rawProfile = new JSONObject();
        rawProfile.put("userinfo", userInfo);
        if (!idClaims.isEmpty()) {
            rawProfile.put("idTokenClaims", idClaims);
        }

        ExternalProfile profile = new ExternalProfile();
        profile.setProvider("outlook");
        profile.setSubject(subject);
        profile.setEmail(normalizeEmail(email));
        profile.setEmailVerified(Boolean.TRUE.equals(userInfo.getBoolean("email_verified")));
        profile.setDisplayName(displayName);
        profile.setAvatarUrl(userInfo.getString("picture"));
        profile.setExternalTenantKey(idClaims.getString("tid"));
        profile.setRawJson(rawProfile.toJSONString());
        validateProfile(profile);
        return profile;
    }

    private ExternalProfile fetchWechatProfile(String code, ExternalAuthProperties.ProviderConfig config) {
        String tokenJson = restTemplate.getForObject(
                UriComponentsBuilder.fromHttpUrl("https://api.weixin.qq.com/sns/oauth2/access_token")
                        .queryParam("appid", config.getClientId())
                        .queryParam("secret", config.getClientSecret())
                        .queryParam("code", code)
                        .queryParam("grant_type", "authorization_code")
                        .build()
                        .toUriString(),
                String.class
        );
        JSONObject token = JSON.parseObject(tokenJson);
        String openId = token.getString("openid");
        String accessToken = token.getString("access_token");
        JSONObject profileJson = new JSONObject();
        if (StrUtil.isNotBlank(accessToken) && StrUtil.isNotBlank(openId)) {
            String userInfoJson = restTemplate.getForObject(
                    UriComponentsBuilder.fromHttpUrl("https://api.weixin.qq.com/sns/userinfo")
                            .queryParam("access_token", accessToken)
                            .queryParam("openid", openId)
                            .queryParam("lang", "zh_CN")
                            .build()
                            .toUriString(),
                    String.class
            );
            profileJson = JSON.parseObject(userInfoJson);
        }

        ExternalProfile profile = new ExternalProfile();
        profile.setProvider("wechat");
        profile.setSubject(openId);
        profile.setDisplayName(StrUtil.blankToDefault(profileJson.getString("nickname"), openId));
        profile.setAvatarUrl(profileJson.getString("headimgurl"));
        profile.setExternalTenantKey(token.getString("unionid"));
        profile.setEmailVerified(Boolean.FALSE);
        profile.setRawJson(profileJson.isEmpty() ? token.toJSONString() : profileJson.toJSONString());
        validateProfile(profile);
        return profile;
    }

    private ExternalProfile fetchWecomProfile(String code, ExternalAuthProperties.ProviderConfig config) {
        String tokenJson = restTemplate.getForObject(
                UriComponentsBuilder.fromHttpUrl("https://qyapi.weixin.qq.com/cgi-bin/gettoken")
                        .queryParam("corpid", config.getCorpId())
                        .queryParam("corpsecret", config.getClientSecret())
                        .build()
                        .toUriString(),
                String.class
        );
        JSONObject token = JSON.parseObject(tokenJson);
        String accessToken = token.getString("access_token");
        if (StrUtil.isBlank(accessToken)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom token exchange failed");
        }

        String userInfoJson = restTemplate.getForObject(
                UriComponentsBuilder.fromHttpUrl("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo")
                        .queryParam("access_token", accessToken)
                        .queryParam("code", code)
                        .build()
                        .toUriString(),
                String.class
        );
        JSONObject userInfo = JSON.parseObject(userInfoJson);
        String userId = StrUtil.blankToDefault(userInfo.getString("userid"), userInfo.getString("openid"));
        JSONObject userDetail = new JSONObject();
        if (StrUtil.isNotBlank(userInfo.getString("userid"))) {
            String userDetailJson = restTemplate.getForObject(
                    UriComponentsBuilder.fromHttpUrl("https://qyapi.weixin.qq.com/cgi-bin/user/get")
                            .queryParam("access_token", accessToken)
                            .queryParam("userid", userInfo.getString("userid"))
                            .build()
                            .toUriString(),
                    String.class
            );
            userDetail = JSON.parseObject(userDetailJson);
        }

        JSONObject rawProfile = new JSONObject();
        rawProfile.put("userinfo", userInfo);
        if (!userDetail.isEmpty()) {
            rawProfile.put("userDetail", userDetail);
        }
        String displayName = firstNotBlank(userDetail.getString("name"), userDetail.getString("english_name"), userId);
        String email = normalizeEmail(firstNotBlank(
                userDetail.getString("email"),
                userDetail.getString("biz_mail"),
                syntheticWecomEmail(config.getCorpId(), userId)
        ));

        ExternalProfile profile = new ExternalProfile();
        profile.setProvider("wecom");
        profile.setSubject(config.getCorpId() + ":" + userId);
        profile.setEmail(email);
        profile.setDisplayName(displayName);
        profile.setAvatarUrl(userDetail.getString("avatar"));
        profile.setExternalTenantKey(config.getCorpId());
        profile.setExternalTenantName(config.getCorpId());
        profile.setEmailVerified(Boolean.FALSE);
        profile.setRawJson(rawProfile.toJSONString());
        validateProfile(profile);
        return profile;
    }

    private ExternalAuthIdentity autoProvisionWecomIdentity(ExternalProfile profile) {
        String email = resolveRegistrationEmail(profile);
        String realname = StrUtil.blankToDefault(StrUtil.trim(profile.getDisplayName()), email);
        String password = generateExternalPassword();
        String companyName = StrUtil.blankToDefault(profile.getExternalTenantName(), profile.getExternalTenantKey());

        ManagerUser user;
        ExternalTenantBinding binding = findTenantBinding(profile.getProvider(), profile.getExternalTenantKey());
        if (binding == null) {
            ExternalTenantRegisterBO tenantRegisterBO = new ExternalTenantRegisterBO();
            tenantRegisterBO.setEmail(email);
            tenantRegisterBO.setPassword(password);
            tenantRegisterBO.setCompanyName(companyName);
            tenantRegisterBO.setRealname(realname);
            tenantRegisterBO.setEmailVerificationRequired(Boolean.FALSE);
            user = registrationService.registerExternalTenant(tenantRegisterBO);
            saveTenantBinding(profile, user.getTenantId(), companyName);
        } else {
            user = manageUserService.queryUsersByUsername(email)
                    .stream()
                    .filter(item -> Objects.equals(item.getTenantId(), binding.getTenantId()))
                    .findFirst()
                    .orElse(null);
            if (user == null) {
                ExternalTenantMemberRegisterBO memberRegisterBO = new ExternalTenantMemberRegisterBO();
                memberRegisterBO.setTenantId(binding.getTenantId());
                memberRegisterBO.setEmail(email);
                memberRegisterBO.setPassword(password);
                memberRegisterBO.setRealname(realname);
                user = registrationService.registerExternalTenantMember(memberRegisterBO);
            }
        }

        saveIdentity(profile, user.getTenantId(), user.getUserId());
        return findIdentity(profile.getProvider(), profile.getSubject());
    }

    private void bindIdentity(AuthState state, ExternalProfile profile) {
        if (state.getUserId() == null || state.getTenantId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NOT_LOGIN);
        }
        ExternalAuthIdentity existing = findIdentity(profile.getProvider(), profile.getSubject());
        if (existing != null) {
            if (Objects.equals(existing.getTenantId(), state.getTenantId())
                    && Objects.equals(existing.getUserId(), state.getUserId())) {
                fillIdentity(existing, profile, state.getTenantId(), state.getUserId());
                identityMapper.updateById(existing);
                return;
            }
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "External identity is already bound");
        }

        long providerBoundCount = identityMapper.selectCount(Wrappers.<ExternalAuthIdentity>lambdaQuery()
                .eq(ExternalAuthIdentity::getProvider, profile.getProvider())
                .eq(ExternalAuthIdentity::getTenantId, state.getTenantId())
                .eq(ExternalAuthIdentity::getUserId, state.getUserId())
                .eq(ExternalAuthIdentity::getStatus, ENABLED_STATUS));
        if (providerBoundCount > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Please unbind this provider first");
        }

        saveIdentity(profile, state.getTenantId(), state.getUserId());
    }

    private void saveIdentity(ExternalProfile profile, Long tenantId, Long userId) {
        ExternalAuthIdentity identity = new ExternalAuthIdentity();
        fillIdentity(identity, profile, tenantId, userId);
        identityMapper.insert(identity);
    }

    private void fillIdentity(ExternalAuthIdentity identity, ExternalProfile profile, Long tenantId, Long userId) {
        Date now = new Date();
        identity.setProvider(profile.getProvider());
        identity.setSubject(profile.getSubject());
        identity.setTenantId(tenantId);
        identity.setUserId(userId);
        identity.setEmail(profile.getEmail());
        identity.setEmailVerified(profile.getEmailVerified());
        identity.setDisplayName(profile.getDisplayName());
        identity.setAvatarUrl(profile.getAvatarUrl());
        identity.setExternalTenantKey(profile.getExternalTenantKey());
        identity.setRawProfile(profile.getRawJson());
        identity.setStatus(ENABLED_STATUS);
        if (identity.getBindTime() == null) {
            identity.setBindTime(now);
        }
        identity.setLastLoginTime(now);
        identity.setUpdateTime(now);
        if (identity.getCreateTime() == null) {
            identity.setCreateTime(now);
        }
    }

    private void saveTenantBinding(ExternalProfile profile, Long tenantId, String tenantName) {
        if (StrUtil.isBlank(profile.getExternalTenantKey())) {
            return;
        }
        ExternalTenantBinding binding = new ExternalTenantBinding();
        binding.setProvider(profile.getProvider());
        binding.setExternalTenantKey(profile.getExternalTenantKey());
        binding.setExternalTenantName(StrUtil.blankToDefault(profile.getExternalTenantName(), tenantName));
        binding.setTenantId(tenantId);
        binding.setStatus(ENABLED_STATUS);
        binding.setCreateTime(new Date());
        binding.setUpdateTime(new Date());
        tenantBindingMapper.insert(binding);
    }

    private ExternalAuthIdentity findIdentity(String provider, String subject) {
        if (StrUtil.isBlank(subject)) {
            return null;
        }
        return identityMapper.selectOne(Wrappers.<ExternalAuthIdentity>lambdaQuery()
                .eq(ExternalAuthIdentity::getProvider, provider)
                .eq(ExternalAuthIdentity::getSubject, subject)
                .eq(ExternalAuthIdentity::getStatus, ENABLED_STATUS)
                .last("LIMIT 1"));
    }

    private ExternalTenantBinding findTenantBinding(String provider, String externalTenantKey) {
        return tenantBindingMapper.selectOne(Wrappers.<ExternalTenantBinding>lambdaQuery()
                .eq(ExternalTenantBinding::getProvider, provider)
                .eq(ExternalTenantBinding::getExternalTenantKey, externalTenantKey)
                .eq(ExternalTenantBinding::getStatus, ENABLED_STATUS)
                .last("LIMIT 1"));
    }

    private String resolveRegistrationEmail(ExternalProfile profile) {
        String email = normalizeEmail(profile.getEmail());
        if (StrUtil.isBlank(email)) {
            if ("wecom".equals(profile.getProvider())) {
                email = syntheticWecomEmail(profile.getExternalTenantKey(), profile.getSubject());
            }
        }
        if (StrUtil.isBlank(email)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "External account email is required");
        }
        profile.setEmail(email);
        return email;
    }

    private ExternalAuthProperties.ProviderConfig requireUsableProvider(String provider) {
        ExternalAuthProperties.ProviderConfig config = properties.getProvider(provider);
        if (config == null || !config.isUsable(provider)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "External auth provider is not configured");
        }
        return config;
    }

    private String normalizeProvider(String provider) {
        String normalized = StrUtil.trim(provider).toLowerCase(Locale.ROOT);
        if (!PROVIDERS.contains(normalized)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Unsupported external auth provider");
        }
        return normalized;
    }

    private String providerName(String provider) {
        return switch (provider) {
            case "google" -> "Google";
            case "outlook" -> "Outlook";
            case "wechat" -> "WeChat";
            case "wecom" -> "企业微信";
            default -> provider;
        };
    }

    private String resolveRedirect(String redirect, HttpServletRequest request) {
        if (StrUtil.isNotBlank(redirect)) {
            return redirect;
        }
        if (StrUtil.isNotBlank(properties.getFrontendRedirectUri())) {
            return properties.getFrontendRedirectUri();
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/").build().toUriString();
    }

    private String resolveCallbackUri(String provider,
                                      ExternalAuthProperties.ProviderConfig config,
                                      HttpServletRequest request) {
        if (StrUtil.isNotBlank(config.getRedirectUri())) {
            return config.getRedirectUri();
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/auth/external/")
                .path(provider)
                .path("/callback")
                .build()
                .toUriString();
    }

    private String storeState(AuthState state) {
        String stateId = IdUtil.fastSimpleUUID();
        redis.setex(stateKey(stateId), safeTtl(properties.getStateTtlSeconds()), JSON.toJSONString(state));
        return stateId;
    }

    private AuthState consumeState(String state) {
        if (StrUtil.isBlank(state)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Missing external auth state");
        }
        return consumeTicket(stateKey(state), AuthState.class);
    }

    private String createLoginTicket(ExternalAuthIdentity identity) {
        String ticketValue = IdUtil.fastSimpleUUID();
        ExternalLoginTicket ticket = new ExternalLoginTicket();
        ticket.setProvider(identity.getProvider());
        ticket.setIdentityId(identity.getId());
        redis.setex(loginTicketKey(ticketValue), safeTtl(properties.getTicketTtlSeconds()), JSON.toJSONString(ticket));
        return ticketValue;
    }

    private String createRegisterTicket(ExternalProfile profile) {
        String ticketValue = IdUtil.fastSimpleUUID();
        ExternalRegisterTicket ticket = new ExternalRegisterTicket();
        ticket.setProfile(profile);
        redis.setex(registerTicketKey(ticketValue), safeTtl(properties.getTicketTtlSeconds()), JSON.toJSONString(ticket));
        return ticketValue;
    }

    private <T> T consumeTicket(String key, Class<T> type) {
        T ticket = readTicket(key, type);
        redis.del(key);
        return ticket;
    }

    private <T> T readTicket(String key, Class<T> type) {
        String raw = redis.get(key);
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "External auth ticket expired");
        }
        return JSON.parseObject(raw, type);
    }

    private String stateKey(String state) {
        return "external-auth:state:" + state;
    }

    private String loginTicketKey(String ticket) {
        return "external-auth:login-ticket:" + ticket;
    }

    private String registerTicketKey(String ticket) {
        return "external-auth:register-ticket:" + ticket;
    }

    private Integer safeTtl(Integer ttl) {
        return ttl == null || ttl <= 0 ? 300 : ttl;
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

    private String outlookOAuthUrl(ExternalAuthProperties.ProviderConfig config, String action) {
        String tenant = StrUtil.blankToDefault(config.getTenant(), "common");
        return "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/" + action;
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

    private JSONObject parseJwtPayload(String jwt) {
        if (StrUtil.isBlank(jwt)) {
            return new JSONObject();
        }
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            return new JSONObject();
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return JSON.parseObject(json);
        } catch (Exception e) {
            log.debug("Failed to parse Outlook id_token payload: {}", e.getMessage());
            return new JSONObject();
        }
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

    private String syntheticWecomEmail(String corpId, String userId) {
        String safeCorp = sanitizeEmailPart(StrUtil.blankToDefault(corpId, "corp"));
        String safeUser = sanitizeEmailPart(StrUtil.blankToDefault(userId, "user"));
        return "wecom." + safeUser + "." + safeCorp + "@external.wecom.local";
    }

    private String sanitizeEmailPart(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return StrUtil.blankToDefault(sanitized, "unknown");
    }

    private String generateExternalPassword() {
        return "Wecom" + IdUtil.fastSimpleUUID().substring(0, 12);
    }

    private void validateProfile(ExternalProfile profile) {
        if (StrUtil.isBlank(profile.getSubject())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "External identity subject is missing");
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
        private String provider;
        private String scene;
        private String redirect;
        private Long userId;
        private Long tenantId;
    }

    @Data
    public static class ExternalLoginTicket {
        private String provider;
        private Long identityId;
    }

    @Data
    public static class ExternalRegisterTicket {
        private ExternalProfile profile;
    }

    @Data
    public static class ExternalProfile {
        private String provider;
        private String subject;
        private String email;
        private Boolean emailVerified;
        private String displayName;
        private String avatarUrl;
        private String externalTenantKey;
        private String externalTenantName;
        private String rawJson;
    }
}
