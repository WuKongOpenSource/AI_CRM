package com.kakarote.ai_crm.service.support;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.entity.VO.UserPreferenceVO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户 UI 偏好解析与规范化。
 */
public final class UserPreferenceSupport {

    public static final String SIDEBAR_RECENT = "recent";
    public static final String SIDEBAR_CUSTOMER = "customer";
    public static final String SIDEBAR_CANDIDATE = "candidate";
    public static final String SIDEBAR_PRODUCT = "product";
    public static final String SIDEBAR_PROJECT = "project";
    public static final String SIDEBAR_RELATION = "relation";
    public static final String SIDEBAR_ADDRESS_BOOK = "addressBook";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> DEFAULT_SIDEBAR_MODULE_ORDER = List.of(
            SIDEBAR_RECENT,
            SIDEBAR_CUSTOMER,
            SIDEBAR_CANDIDATE,
            SIDEBAR_PRODUCT,
            SIDEBAR_PROJECT,
            SIDEBAR_RELATION,
            SIDEBAR_ADDRESS_BOOK
    );
    private static final Set<String> ALLOWED_SIDEBAR_MODULE_KEYS = Set.copyOf(DEFAULT_SIDEBAR_MODULE_ORDER);

    private UserPreferenceSupport() {
    }

    public static List<String> defaultSidebarModuleOrder() {
        return new ArrayList<>(DEFAULT_SIDEBAR_MODULE_ORDER);
    }

    public static List<String> normalizeSidebarModuleOrder(Collection<String> rawOrder) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (rawOrder != null) {
            for (String item : rawOrder) {
                String key = StrUtil.trim(item);
                if (ALLOWED_SIDEBAR_MODULE_KEYS.contains(key)) {
                    normalized.add(key);
                }
            }
        }
        for (String key : DEFAULT_SIDEBAR_MODULE_ORDER) {
            normalized.add(key);
        }
        return new ArrayList<>(normalized);
    }

    public static UserPreferenceVO parsePreferences(String rawJson) {
        UserPreferenceVO preferences = new UserPreferenceVO();
        preferences.setSidebarModuleOrder(defaultSidebarModuleOrder());
        if (StrUtil.isBlank(rawJson)) {
            return preferences;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawJson);
            JsonNode orderNode = root == null ? null : root.get("sidebarModuleOrder");
            if (orderNode == null || !orderNode.isArray()) {
                return preferences;
            }
            List<String> order = new ArrayList<>();
            orderNode.forEach(node -> {
                if (node != null && node.isTextual()) {
                    order.add(node.asText());
                }
            });
            preferences.setSidebarModuleOrder(normalizeSidebarModuleOrder(order));
            return preferences;
        } catch (Exception ignored) {
            return preferences;
        }
    }

    public static String serializePreferences(Collection<String> sidebarModuleOrder) {
        return serializePreferences(toPreferenceVO(sidebarModuleOrder));
    }

    public static String serializePreferences(UserPreferenceVO preferences) {
        List<String> sidebarModuleOrder = preferences == null
                ? defaultSidebarModuleOrder()
                : normalizeSidebarModuleOrder(preferences.getSidebarModuleOrder());
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of("sidebarModuleOrder", sidebarModuleOrder));
        } catch (Exception e) {
            throw new IllegalStateException("用户偏好序列化失败", e);
        }
    }

    public static UserPreferenceVO toPreferenceVO(Collection<String> sidebarModuleOrder) {
        UserPreferenceVO preferences = new UserPreferenceVO();
        preferences.setSidebarModuleOrder(normalizeSidebarModuleOrder(sidebarModuleOrder));
        return preferences;
    }
}
