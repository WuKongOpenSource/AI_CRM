package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WecomOpenApiClient {

    private static final String BASE_URL = "https://qyapi.weixin.qq.com/cgi-bin";

    private final RestTemplate restTemplate;

    public WecomOpenApiClient() {
        this(new RestTemplate());
    }

    WecomOpenApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JSONObject fetchSuiteAccessToken(String suiteId, String suiteSecret, String suiteTicket) {
        JSONObject body = new JSONObject();
        body.put("suite_id", suiteId);
        body.put("suite_secret", suiteSecret);
        body.put("suite_ticket", suiteTicket);
        JSONObject json = post("/service/get_suite_token", null, body);
        assertWecomOk(json, "Fetch WeCom suite token failed");
        requireField(json, "suite_access_token", "WeCom suite token is empty");
        return json;
    }

    public JSONObject fetchProviderAccessToken(String providerCorpId, String providerSecret) {
        JSONObject body = new JSONObject();
        body.put("corpid", providerCorpId);
        body.put("provider_secret", providerSecret);
        JSONObject json = postWithTokenParam("/service/get_provider_token", null, null, body);
        assertWecomOk(json, "Fetch WeCom provider token failed");
        requireField(json, "provider_access_token", "WeCom provider token is empty");
        return json;
    }

    public JSONObject fetchPreAuthCode(String suiteAccessToken) {
        JSONObject json = post("/service/get_pre_auth_code", suiteAccessToken, new JSONObject());
        assertWecomOk(json, "Fetch WeCom pre auth code failed");
        requireField(json, "pre_auth_code", "WeCom pre auth code is empty");
        return json;
    }

    public JSONObject setSessionInfo(String suiteAccessToken, String preAuthCode, JSONObject sessionInfo) {
        JSONObject body = new JSONObject();
        body.put("pre_auth_code", preAuthCode);
        body.put("session_info", sessionInfo == null ? new JSONObject() : sessionInfo);
        JSONObject json = post("/service/set_session_info", suiteAccessToken, body);
        assertWecomOk(json, "Set WeCom auth session info failed");
        return json;
    }

    public JSONObject fetchPermanentCode(String suiteAccessToken, String authCode) {
        JSONObject body = new JSONObject();
        body.put("auth_code", authCode);
        JSONObject json = post("/service/get_permanent_code", suiteAccessToken, body);
        assertWecomOk(json, "Fetch WeCom permanent code failed");
        requireField(json, "permanent_code", "WeCom permanent code is empty");
        return json;
    }

    public JSONObject fetchAuthInfo(String suiteAccessToken, String authCorpId, String permanentCode) {
        JSONObject body = new JSONObject();
        body.put("auth_corpid", authCorpId);
        body.put("permanent_code", permanentCode);
        return post("/service/get_auth_info", suiteAccessToken, body);
    }

    public JSONObject fetchCorpAccessToken(String suiteAccessToken, String authCorpId, String permanentCode) {
        JSONObject body = new JSONObject();
        body.put("auth_corpid", authCorpId);
        body.put("permanent_code", permanentCode);
        JSONObject json = post("/service/get_corp_token", suiteAccessToken, body);
        assertWecomOk(json, "Fetch WeCom corp token failed");
        requireField(json, "access_token", "WeCom corp token is empty");
        return json;
    }

    /**
     * 代开发/自建式取企业 access_token：GET /cgi-bin/gettoken?corpid=&amp;corpsecret=permanent_code。
     * 与第三方应用的 get_corp_token 不同（代开发拿到 permanent_code 后按自建方式取 token）。
     */
    public JSONObject fetchSelfBuiltCorpToken(String corpId, String corpSecret) {
        JSONObject json = get("/gettoken", null, null,
                builder -> builder.queryParam("corpid", corpId).queryParam("corpsecret", corpSecret));
        assertWecomOk(json, "Fetch WeCom self-built corp token failed");
        requireField(json, "access_token", "WeCom corp token is empty");
        return json;
    }

    public JSONObject fetchUserInfo3rd(String suiteAccessToken, String code) {
        JSONObject json = get("/service/auth/getuserinfo3rd", suiteAccessToken, "suite_access_token",
                builder -> builder.queryParam("code", code));
        assertWecomOk(json, "Fetch WeCom third-party user info failed");
        return json;
    }

    public JSONObject fetchUserDetail3rd(String suiteAccessToken, String userTicket) {
        JSONObject body = new JSONObject();
        body.put("user_ticket", userTicket);
        JSONObject json = postWithTokenParam("/service/getuserdetail3rd", "suite_access_token", suiteAccessToken, body);
        assertWecomOk(json, "Fetch WeCom third-party user detail failed");
        return json;
    }

    public JSONObject fetchLoginInfo(String providerAccessToken, String authCode) {
        JSONObject body = new JSONObject();
        body.put("auth_code", authCode);
        JSONObject json = postWithTokenParam("/service/get_login_info", "access_token", providerAccessToken, body);
        assertWecomOk(json, "Fetch WeCom login info failed");
        return json;
    }

    private JSONObject post(String path, String suiteAccessToken, JSONObject body) {
        return postWithTokenParam(path, "suite_access_token", suiteAccessToken, body);
    }

    private JSONObject postWithTokenParam(String path, String tokenParam, String token, JSONObject body) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + path);
        if (StrUtil.isNotBlank(token)) {
            builder.queryParam(tokenParam, token);
        }
        String raw = restTemplate.postForObject(builder.build().toUriString(), body, String.class);
        return JSON.parseObject(raw);
    }

    private JSONObject get(String path,
                           String token,
                           String tokenParam,
                           QueryCustomizer customizer) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + path);
        if (StrUtil.isNotBlank(token)) {
            builder.queryParam(tokenParam, token);
        }
        if (customizer != null) {
            customizer.customize(builder);
        }
        String raw = restTemplate.getForObject(builder.build().toUriString(), String.class);
        return JSON.parseObject(raw);
    }

    private void assertWecomOk(JSONObject json, String message) {
        Integer errCode = json == null ? null : json.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    message + ": " + errCode + " " + json.getString("errmsg"));
        }
    }

    private void requireField(JSONObject json, String field, String message) {
        if (json == null || StrUtil.isBlank(json.getString(field))) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, message);
        }
    }

    @FunctionalInterface
    private interface QueryCustomizer {
        void customize(UriComponentsBuilder builder);
    }
}
