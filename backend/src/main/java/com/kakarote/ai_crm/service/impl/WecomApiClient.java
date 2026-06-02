package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
public class WecomApiClient {

    private static final String BASE_URL = "https://qyapi.weixin.qq.com/cgi-bin";

    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchAccessToken(String corpId, String secret) {
        if (StrUtil.isBlank(corpId) || StrUtil.isBlank(secret)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom CorpID and Secret are required");
        }
        String raw = restTemplate.getForObject(UriComponentsBuilder.fromHttpUrl(BASE_URL + "/gettoken")
                .queryParam("corpid", corpId)
                .queryParam("corpsecret", secret)
                .build()
                .toUriString(), String.class);
        JSONObject json = JSON.parseObject(raw);
        assertWecomOk(json, "Fetch WeCom access token failed");
        return json.getString("access_token");
    }

    public List<JSONObject> listEmployees(String accessToken) {
        JSONObject departmentResponse = get("/department/simplelist", accessToken, null);
        JSONArray departments = departmentResponse.getJSONArray("department_id");
        if (departments == null || departments.isEmpty()) {
            return List.of();
        }
        List<JSONObject> result = new ArrayList<>();
        for (Object item : departments) {
            JSONObject department = (JSONObject) item;
            String departmentId = department.getString("id");
            JSONObject users = get("/user/simplelist", accessToken, builder -> builder
                    .queryParam("department_id", departmentId)
                    .queryParam("fetch_child", 0));
            JSONArray userList = users.getJSONArray("userlist");
            if (userList != null) {
                for (Object user : userList) {
                    result.add((JSONObject) user);
                }
            }
        }
        return result;
    }

    public List<String> listFollowUsers(String accessToken) {
        JSONObject response = get("/externalcontact/get_follow_user_list", accessToken, null);
        JSONArray userIds = response.getJSONArray("follow_user");
        return userIds == null ? List.of() : userIds.toJavaList(String.class);
    }

    public List<String> listExternalUserIds(String accessToken, String userId) {
        JSONObject response = get("/externalcontact/list", accessToken,
                builder -> builder.queryParam("userid", userId));
        JSONArray externalUserIds = response.getJSONArray("external_userid");
        return externalUserIds == null ? List.of() : externalUserIds.toJavaList(String.class);
    }

    public JSONObject getExternalCustomer(String accessToken, String externalUserId) {
        return get("/externalcontact/get", accessToken, builder -> builder.queryParam("external_userid", externalUserId));
    }

    private JSONObject get(String path, String accessToken, QueryCustomizer customizer) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + path)
                .queryParam("access_token", accessToken);
        if (customizer != null) {
            customizer.customize(builder);
        }
        String raw = restTemplate.getForObject(builder.build().toUriString(), String.class);
        JSONObject json = JSON.parseObject(raw);
        assertWecomOk(json, "WeCom API request failed");
        return json;
    }

    private void assertWecomOk(JSONObject json, String message) {
        Integer errCode = json == null ? null : json.getInteger("errcode");
        if (errCode != null && errCode != 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    message + ": " + errCode + " " + json.getString("errmsg"));
        }
    }

    @FunctionalInterface
    private interface QueryCustomizer {
        void customize(UriComponentsBuilder builder);
    }
}
