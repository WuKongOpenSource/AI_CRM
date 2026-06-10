package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WecomApiClient {

    private static final String BASE_URL = "https://qyapi.weixin.qq.com/cgi-bin";

    private final RestTemplate restTemplate;

    public WecomApiClient() {
        this(new RestTemplate());
    }

    WecomApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<JSONObject> listDepartments(String accessToken) {
        JSONObject response = get("/department/list", accessToken, null);
        JSONArray departments = response.getJSONArray("department");
        if (departments == null || departments.isEmpty()) {
            return List.of();
        }
        List<JSONObject> result = new ArrayList<>();
        for (Object department : departments) {
            result.add((JSONObject) department);
        }
        return result;
    }

    public List<JSONObject> listDepartmentUsers(String accessToken, Long departmentId) {
        JSONObject users = get("/user/list", accessToken, builder -> builder
                .queryParam("department_id", departmentId)
                .queryParam("fetch_child", 0));
        JSONArray userList = users.getJSONArray("userlist");
        if (userList == null || userList.isEmpty()) {
            return List.of();
        }
        List<JSONObject> result = new ArrayList<>();
        for (Object user : userList) {
            result.add((JSONObject) user);
        }
        return result;
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

    public Map<String, String> convertExternalUserIds(String accessToken, List<String> externalUserIds) {
        if (externalUserIds == null || externalUserIds.isEmpty()) {
            return Map.of();
        }
        List<String> normalizedExternalUserIds = externalUserIds.stream()
                .filter(externalUserId -> externalUserId != null && !externalUserId.isBlank())
                .distinct()
                .toList();
        if (normalizedExternalUserIds.isEmpty()) {
            return Map.of();
        }
        JSONObject body = new JSONObject();
        body.put("external_userid_list", normalizedExternalUserIds);
        JSONObject response = post("/externalcontact/get_new_external_userid", accessToken, body);
        JSONArray items = response.getJSONArray("items");
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Object item : items) {
            if (!(item instanceof JSONObject json)) {
                continue;
            }
            String externalUserId = json.getString("external_userid");
            String newExternalUserId = json.getString("new_external_userid");
            if (externalUserId != null && !externalUserId.isBlank()
                    && newExternalUserId != null && !newExternalUserId.isBlank()) {
                result.put(externalUserId, newExternalUserId);
            }
        }
        return result;
    }

    public JSONObject getCustomerGroupChat(String accessToken, String chatId) {
        JSONObject body = new JSONObject();
        body.put("chat_id", chatId);
        body.put("need_name", 1);
        return post("/externalcontact/groupchat/get", accessToken, body);
    }

    public JSONObject getArchiveInternalGroupChat(String accessToken, String roomId) {
        JSONObject body = new JSONObject();
        body.put("roomid", roomId);
        return post("/msgaudit/groupchat/get", accessToken, body);
    }

    public String getAgentJsApiTicket(String accessToken) {
        JSONObject response = get("/ticket/get", accessToken,
                builder -> builder.queryParam("type", "agent_config"));
        return response.getString("ticket");
    }

    public Map<String, String> convertUserIdsToOpenUserIds(String accessToken, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<String> normalizedUserIds = userIds.stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .distinct()
                .toList();
        if (normalizedUserIds.isEmpty()) {
            return Map.of();
        }
        JSONObject body = new JSONObject();
        body.put("userid_list", normalizedUserIds);
        JSONObject response = post("/batch/userid_to_openuserid", accessToken, body);
        JSONArray openUserIds = response.getJSONArray("open_userid_list");
        if (openUserIds == null || openUserIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Object item : openUserIds) {
            if (!(item instanceof JSONObject json)) {
                continue;
            }
            String userId = json.getString("userid");
            String openUserId = json.getString("open_userid");
            if (userId != null && !userId.isBlank() && openUserId != null && !openUserId.isBlank()) {
                result.put(userId, openUserId);
            }
        }
        return result;
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

    private JSONObject post(String path, String accessToken, JSONObject body) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL + path)
                .queryParam("access_token", accessToken);
        String raw = restTemplate.postForObject(builder.build().toUriString(), body, String.class);
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
