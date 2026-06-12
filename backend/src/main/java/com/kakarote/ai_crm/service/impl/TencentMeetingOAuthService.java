package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.TencentMeeting;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCorpConfig;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCustomerBinding;
import com.kakarote.ai_crm.entity.PO.TencentMeetingParticipant;
import com.kakarote.ai_crm.entity.PO.TencentMeetingRecording;
import com.kakarote.ai_crm.entity.PO.TencentMeetingTranscriptSegment;
import com.kakarote.ai_crm.entity.PO.TencentMeetingUserMapping;
import com.kakarote.ai_crm.entity.VO.TencentMeetingOAuthAccountVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingOAuthAuthorizeVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingOAuthStatusVO;
import com.kakarote.ai_crm.mapper.TencentMeetingCorpConfigMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingCustomerBindingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingParticipantMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingRecordingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingTranscriptSegmentMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingUserMappingMapper;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import com.kakarote.ai_crm.utils.UserUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class TencentMeetingOAuthService {

    static final String AUTH_STATUS_ACTIVE = "ACTIVE";
    static final String AUTH_STATUS_EXPIRED = "EXPIRED";
    private static final String STATE_KEY_PREFIX = "tencent-meeting:oauth:state:";
    private static final int STATE_TTL_SECONDS = 600;
    private static final String AUTHORIZE_URL = "https://meeting.tencent.com/marketplace/authorize.html";
    private static final String ACCESS_TOKEN_URL = "https://meeting.tencent.com/wemeet-webapi/v2/oauth2/oauth/access_token";
    private static final String USER_INFO_URL = "https://meeting.tencent.com/wemeet-webapi/v2/oauth2/oauth/user_info";
    private static final String USER_BASIC_URL = "https://api.meeting.qq.com/v1/users/info/basic";

    @Autowired
    private TencentMeetingCorpConfigMapper configMapper;

    @Autowired
    private TencentMeetingUserMappingMapper userMappingMapper;

    @Autowired
    private TencentMeetingMapper meetingMapper;

    @Autowired
    private TencentMeetingParticipantMapper participantMapper;

    @Autowired
    private TencentMeetingRecordingMapper recordingMapper;

    @Autowired
    private TencentMeetingTranscriptSegmentMapper transcriptMapper;

    @Autowired
    private TencentMeetingCustomerBindingMapper bindingMapper;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Autowired
    private Redis redis;

    @Autowired
    private ManageUserService manageUserService;

    @Value("${tencent-meeting.oauth.callback-url:}")
    private String configuredCallbackUrl;

    @Value("${tencent-meeting.oauth.frontend-redirect-url:}")
    private String configuredFrontendRedirectUrl;

    private final RestTemplate restTemplate;

    public TencentMeetingOAuthService() {
        this(new RestTemplate());
    }

    TencentMeetingOAuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TencentMeetingOAuthAuthorizeVO createAuthorizeUrl(String redirect, HttpServletRequest request) {
        TencentMeetingCorpConfig config = requireOAuthConfig();
        LoginUser loginUser = UserUtil.getLoginUser();
        OAuthState state = new OAuthState();
        state.setTenantId(loginUser.getUser().getTenantId());
        state.setUserId(loginUser.getUser().getUserId());
        state.setRedirect(resolveRedirect(redirect, request));
        String stateId = IdUtil.fastSimpleUUID();
        redis.setex(STATE_KEY_PREFIX + stateId, STATE_TTL_SECONDS, JSON.toJSONString(state));

        String authorizeUrl = UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("corp_id", config.getAppId())
                .queryParam("sdk_id", config.getSdkId())
                .queryParam("redirect_uri", callbackUri(request))
                .queryParam("state", stateId)
                .build()
                .encode()
                .toUriString();

        TencentMeetingOAuthAuthorizeVO vo = new TencentMeetingOAuthAuthorizeVO();
        vo.setAuthorizeUrl(authorizeUrl);
        vo.setState(stateId);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(String authCode, String state, String error, HttpServletRequest request) {
        OAuthState oauthState = consumeState(state);
        if (StrUtil.isNotBlank(error)) {
            return appendQuery(oauthState.getRedirect(), "tencentMeetingOAuth", "error", "message", error);
        }
        if (StrUtil.isBlank(authCode)) {
            return appendQuery(oauthState.getRedirect(), "tencentMeetingOAuth", "error", "message", "missing_auth_code");
        }

        Long previousTenantId = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(oauthState.getTenantId());
        try {
            TencentMeetingCorpConfig config = requireOAuthConfig();
            JSONObject tokenData = exchangeAuthCode(config, authCode);
            String openId = tokenData.getString("open_id");
            JSONObject profileData = verifyToken(tokenData);
            JSONObject basicInfo = fetchUserBasicInfo(tokenData.getString("access_token"), openId);
            upsertAuthorizedAccount(config, oauthState, tokenData, profileData, basicInfo);
            return appendQuery(oauthState.getRedirect(), "tencentMeetingOAuth", "success");
        } catch (Exception e) {
            log.warn("Tencent Meeting OAuth callback failed: {}", e.getMessage());
            return appendQuery(oauthState.getRedirect(), "tencentMeetingOAuth", "error", "message", "callback_failed");
        } finally {
            if (previousTenantId == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(previousTenantId);
            }
        }
    }

    public TencentMeetingOAuthStatusVO getStatus() {
        TencentMeetingOAuthStatusVO status = new TencentMeetingOAuthStatusVO();
        TencentMeetingCorpConfig config = findConfig();
        status.setConfigured(config != null && StrUtil.isNotBlank(config.getAppId()) && StrUtil.isNotBlank(config.getSdkId()));
        if (config == null) {
            status.setAuthorized(Boolean.FALSE);
            return status;
        }
        Long userId = UserUtil.getUserIdOrNull();
        TencentMeetingUserMapping account = findAccountByCrmUser(config, userId);
        status.setAuthorized(account != null && AUTH_STATUS_ACTIVE.equals(account.getAuthStatus()));
        status.setAccount(toAccountVO(account));
        return status;
    }

    public TencentMeetingUserMapping requireCurrentAuthorizedAccount(TencentMeetingCorpConfig config) {
        TencentMeetingUserMapping account = findAccountByCrmUser(config, UserUtil.getUserIdOrNull());
        if (account == null || !AUTH_STATUS_ACTIVE.equals(account.getAuthStatus()) || account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "请先授权腾讯会议账号");
        }
        return account;
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindCurrentUser() {
        TencentMeetingCorpConfig config = requireOAuthConfig();
        TencentMeetingUserMapping account = findAccountByCrmUser(config, UserUtil.getUserIdOrNull());
        if (account != null) {
            deleteCurrentAccountMeetingRecords(config, account);
            userMappingMapper.deleteById(account.getId());
        }
    }

    private void deleteCurrentAccountMeetingRecords(TencentMeetingCorpConfig config, TencentMeetingUserMapping account) {
        if (config == null || account == null || account.getCrmUserId() == null) {
            return;
        }
        List<Long> meetingIds = meetingMapper.selectList(Wrappers.<TencentMeeting>lambdaQuery()
                        .select(TencentMeeting::getId)
                        .eq(TencentMeeting::getAppId, config.getAppId())
                        .eq(TencentMeeting::getCrmCreatorUserId, account.getCrmUserId()))
                .stream()
                .map(TencentMeeting::getId)
                .filter(Objects::nonNull)
                .toList();
        if (meetingIds.isEmpty()) {
            return;
        }
        transcriptMapper.delete(Wrappers.<TencentMeetingTranscriptSegment>lambdaQuery()
                .in(TencentMeetingTranscriptSegment::getMeetingDbId, meetingIds));
        recordingMapper.delete(Wrappers.<TencentMeetingRecording>lambdaQuery()
                .eq(TencentMeetingRecording::getAppId, config.getAppId())
                .in(TencentMeetingRecording::getMeetingDbId, meetingIds));
        participantMapper.delete(Wrappers.<TencentMeetingParticipant>lambdaQuery()
                .eq(TencentMeetingParticipant::getAppId, config.getAppId())
                .in(TencentMeetingParticipant::getMeetingDbId, meetingIds));
        bindingMapper.delete(Wrappers.<TencentMeetingCustomerBinding>lambdaQuery()
                .in(TencentMeetingCustomerBinding::getMeetingId, meetingIds));
        meetingMapper.delete(Wrappers.<TencentMeeting>lambdaQuery()
                .in(TencentMeeting::getId, meetingIds));
    }

    private void upsertAuthorizedAccount(TencentMeetingCorpConfig config,
                                         OAuthState state,
                                         JSONObject tokenData,
                                         JSONObject profileData,
                                         JSONObject basicInfo) {
        String openId = tokenData.getString("open_id");
        if (StrUtil.isBlank(openId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent Meeting OAuth response has no open_id");
        }
        TencentMeetingUserMapping existing = userMappingMapper.selectOne(Wrappers.<TencentMeetingUserMapping>lambdaQuery()
                .eq(TencentMeetingUserMapping::getAppId, config.getAppId())
                .eq(TencentMeetingUserMapping::getMeetingUserId, openId)
                .last("LIMIT 1"));
        if (existing != null && existing.getCrmUserId() != null && !Objects.equals(existing.getCrmUserId(), state.getUserId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "该腾讯会议账号已绑定其他AICRM用户");
        }
        TencentMeetingUserMapping account = existing == null ? new TencentMeetingUserMapping() : existing;
        account.setAppId(config.getAppId());
        account.setMeetingUserId(openId);
        account.setUserName(resolveAccountUserName(openId, state.getUserId(), basicInfo, profileData, existing == null ? null : existing.getUserName()));
        account.setOpenCorpId(firstNotBlank(basicInfo.getString("open_corp_id"), tokenData.getString("open_corp_id")));
        account.setOpenCorpName(basicInfo.getString("open_corp_name"));
        account.setAvatarUrl(basicInfo.getString("avatar_url"));
        account.setCrmUserId(state.getUserId());
        account.setAccessTokenEncrypted(secretTextCipher.encrypt(tokenData.getString("access_token")));
        account.setRefreshTokenEncrypted(secretTextCipher.encrypt(tokenData.getString("refresh_token")));
        account.setTokenExpiresAt(resolveExpiresAt(tokenData));
        account.setScopes(tokenData.getJSONArray("scopes") == null ? profileData.getString("scopes") : tokenData.getJSONArray("scopes").toJSONString());
        account.setAuthStatus(AUTH_STATUS_ACTIVE);
        account.setStatus(1);
        account.setLastAuthTime(new Date());
        account.setLastSyncError(null);
        account.setSyncedAt(new Date());
        if (account.getId() == null) {
            userMappingMapper.insert(account);
        } else {
            userMappingMapper.updateById(account);
        }
    }

    private String resolveAccountUserName(String openId,
                                          Long crmUserId,
                                          JSONObject basicInfo,
                                          JSONObject profileData,
                                          String existingUserName) {
        String name = firstNotOpenId(openId,
                jsonText(basicInfo, "username"),
                jsonText(basicInfo, "user_name"),
                jsonText(basicInfo, "nick_name"),
                jsonText(basicInfo, "nickname"),
                jsonText(profileData, "username"),
                jsonText(profileData, "user_name"),
                jsonText(profileData, "nick_name"),
                jsonText(profileData, "nickname"),
                existingUserName,
                crmUserDisplayName(crmUserId));
        return StrUtil.blankToDefault(name, openId);
    }

    private String firstNotOpenId(String openId, String... values) {
        for (String value : values) {
            if (StrUtil.isBlank(value)) {
                continue;
            }
            String candidate = value.trim();
            if (!candidate.equals(openId)) {
                return candidate;
            }
        }
        return null;
    }

    private String jsonText(JSONObject object, String key) {
        return object == null ? null : object.getString(key);
    }

    private String crmUserDisplayName(Long userId) {
        if (userId == null || manageUserService == null) {
            return null;
        }
        ManagerUser user = manageUserService.getById(userId);
        if (user == null) {
            return null;
        }
        return firstNotBlank(user.getRealname(), user.getUsername(), user.getMobile(), user.getEmail());
    }

    private JSONObject exchangeAuthCode(TencentMeetingCorpConfig config, String authCode) {
        JSONObject body = new JSONObject();
        body.put("sdk_id", config.getSdkId());
        body.put("secret", secretTextCipher.decrypt(config.getAppSecretEncrypted()));
        body.put("auth_code", authCode);
        return postOAuth(ACCESS_TOKEN_URL, body, "Tencent Meeting OAuth token exchange failed", true);
    }

    private JSONObject verifyToken(JSONObject tokenData) {
        JSONObject body = new JSONObject();
        body.put("access_token", tokenData.getString("access_token"));
        body.put("open_id", tokenData.getString("open_id"));
        return postOAuth(USER_INFO_URL, body, "Tencent Meeting OAuth user_info failed", false);
    }

    private JSONObject postOAuth(String url, JSONObject body, String errorMessage, boolean requireAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            String raw = restTemplate.postForObject(url, new HttpEntity<>(body.toJSONString(), headers), String.class);
            if (requireAccessToken) {
                return TencentMeetingOAuthTokenProvider.extractData(raw, errorMessage);
            }
            JSONObject root = JSON.parseObject(StrUtil.blankToDefault(raw, "{}"));
            JSONObject data = root.getJSONObject("data");
            return data == null ? root : data;
        } catch (RestClientResponseException e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, errorMessage + ": " + e.getResponseBodyAsString());
        }
    }

    private JSONObject fetchUserBasicInfo(String accessToken, String openId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("AccessToken", accessToken);
        headers.set("OpenId", openId);
        headers.set("X-TC-Timestamp", String.valueOf(System.currentTimeMillis() / 1000L));
        headers.set("X-TC-Nonce", IdUtil.fastSimpleUUID());
        String url = UriComponentsBuilder.fromHttpUrl(USER_BASIC_URL)
                .queryParam("operator_id", openId)
                .queryParam("operator_id_type", 2)
                .build()
                .toUriString();
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>("", headers), String.class);
            JSONObject root = JSON.parseObject(StrUtil.blankToDefault(response.getBody(), "{}"));
            JSONObject data = root.getJSONObject("data");
            return data == null ? root : data;
        } catch (RestClientResponseException e) {
            log.warn("Tencent Meeting user basic info failed: openId={}, error={}", openId, e.getResponseBodyAsString());
            return new JSONObject();
        }
    }

    private TencentMeetingUserMapping findAccountByCrmUser(TencentMeetingCorpConfig config, Long userId) {
        if (config == null || userId == null) {
            return null;
        }
        return userMappingMapper.selectOne(Wrappers.<TencentMeetingUserMapping>lambdaQuery()
                .eq(TencentMeetingUserMapping::getAppId, config.getAppId())
                .eq(TencentMeetingUserMapping::getCrmUserId, userId)
                .eq(TencentMeetingUserMapping::getAuthStatus, AUTH_STATUS_ACTIVE)
                .eq(TencentMeetingUserMapping::getStatus, 1)
                .orderByDesc(TencentMeetingUserMapping::getLastAuthTime)
                .last("LIMIT 1"));
    }

    private TencentMeetingCorpConfig findConfig() {
        return configMapper.selectLatestOAuthConfigIgnoreTenant();
    }

    private TencentMeetingCorpConfig requireOAuthConfig() {
        TencentMeetingCorpConfig config = findConfig();
        if (config == null || StrUtil.isBlank(config.getAppId()) || StrUtil.isBlank(config.getSdkId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent Meeting OAuth config is not configured");
        }
        if (StrUtil.isBlank(config.getAppSecretEncrypted())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent Meeting app secret is not configured");
        }
        return config;
    }

    private OAuthState consumeState(String state) {
        if (StrUtil.isBlank(state)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Missing Tencent Meeting OAuth state");
        }
        String key = STATE_KEY_PREFIX + state;
        String raw = redis.get(key);
        if (StrUtil.isBlank(raw)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent Meeting OAuth state expired");
        }
        redis.del(key);
        return JSON.parseObject(raw, OAuthState.class);
    }

    private String callbackUri(HttpServletRequest request) {
        if (StrUtil.isNotBlank(configuredCallbackUrl)) {
            return StrUtil.trim(configuredCallbackUrl);
        }
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/tencent-meeting/oauth/callback")
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    private String resolveRedirect(String redirect, HttpServletRequest request) {
        if (isSafeFrontendRedirect(redirect)) {
            return StrUtil.trim(redirect);
        }
        if (StrUtil.isNotBlank(configuredFrontendRedirectUrl)) {
            return StrUtil.trim(configuredFrontendRedirectUrl);
        }
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/#/tencent-meetings")
                .replaceQuery(null)
                .build()
                .toUriString();
    }

    private boolean isSafeFrontendRedirect(String redirect) {
        if (StrUtil.isBlank(redirect)) {
            return false;
        }
        return !StrUtil.containsIgnoreCase(redirect, "/tencent-meeting/oauth/callback");
    }

    private String appendQuery(String url, String... keyValues) {
        // 用 replaceQueryParam（幂等）而非 queryParam（追加）：前端会把当前完整 URL 当作 redirect 回传，
        // 若该 URL 已带 tencentMeetingOAuth/message，再次授权时不能层层叠加，否则地址栏会出现
        // ?tencentMeetingOAuth=success&tencentMeetingOAuth=success&... 这类重复参数。
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            builder.replaceQueryParam(keyValues[i], keyValues[i + 1]);
        }
        return builder.build().toUriString();
    }

    private TencentMeetingOAuthAccountVO toAccountVO(TencentMeetingUserMapping account) {
        if (account == null) {
            return null;
        }
        TencentMeetingOAuthAccountVO vo = new TencentMeetingOAuthAccountVO();
        vo.setId(account.getId());
        vo.setAppId(account.getAppId());
        vo.setOpenId(account.getMeetingUserId());
        vo.setUserName(account.getUserName());
        vo.setOpenCorpId(account.getOpenCorpId());
        vo.setOpenCorpName(account.getOpenCorpName());
        vo.setAvatarUrl(account.getAvatarUrl());
        vo.setCrmUserId(account.getCrmUserId());
        if (account.getCrmUserId() != null) {
            ManagerUser user = manageUserService.getById(account.getCrmUserId());
            if (user != null) {
                vo.setCrmUserName(StrUtil.blankToDefault(user.getRealname(), user.getUsername()));
            }
        }
        vo.setAuthStatus(account.getAuthStatus());
        vo.setScopes(account.getScopes());
        vo.setTokenExpiresAt(account.getTokenExpiresAt());
        vo.setLastAuthTime(account.getLastAuthTime());
        vo.setLastRefreshTime(account.getLastRefreshTime());
        vo.setLastSyncTime(account.getLastSyncTime());
        vo.setLastSyncError(account.getLastSyncError());
        return vo;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private Date resolveExpiresAt(JSONObject data) {
        Long epochSeconds = data.getLong("expires");
        if (epochSeconds != null) {
            return new Date(epochSeconds * 1000L);
        }
        Long expiresIn = data.getLong("expires_in");
        return expiresIn == null ? null : new Date(System.currentTimeMillis() + expiresIn * 1000L);
    }

    @Data
    static class OAuthState {
        private Long tenantId;
        private Long userId;
        private String redirect;
    }
}
