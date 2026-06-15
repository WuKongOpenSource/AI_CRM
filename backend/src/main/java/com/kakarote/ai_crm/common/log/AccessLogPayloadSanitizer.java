package com.kakarote.ai_crm.common.log;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Sanitizes HTTP payloads before they are stored as audit data.
 */
public final class AccessLogPayloadSanitizer {

    public static final int REQUEST_BODY_LIMIT_BYTES = 256 * 1024;
    private static final int RESPONSE_SUMMARY_LIMIT_CHARS = 16 * 1024;
    private static final int HEADER_VALUE_LIMIT_CHARS = 512;
    private static final String MASK = "***";
    private static final Set<String> EXACT_SENSITIVE_KEYS = Set.of(
            "key",
            "token",
            "authorization",
            "manager-token"
    );
    private static final String[] SENSITIVE_KEY_PARTS = {
            "password",
            "passwd",
            "secret",
            "apikey",
            "api_key",
            "captcha",
            "verification",
            "verifycode",
            "access_token",
            "refresh_token",
            "验证码"
    };

    private AccessLogPayloadSanitizer() {
    }

    public static String sanitizeHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            if (isSensitiveKey(name)) {
                headers.put(name, MASK);
                continue;
            }
            headers.put(name, truncateToChars(request.getHeader(name), HEADER_VALUE_LIMIT_CHARS).value());
        }
        return headers.isEmpty() ? null : JSON.toJSONString(headers);
    }

    public static SanitizedText sanitizeQueryString(String queryString) {
        if (StrUtil.isBlank(queryString)) {
            return new SanitizedText(null, false);
        }
        return truncateToChars(sanitizeFormLike(queryString), REQUEST_BODY_LIMIT_BYTES);
    }

    public static SanitizedText sanitizeRequestBody(String body, String contentType) {
        if (StrUtil.isBlank(body)) {
            return new SanitizedText(null, false);
        }
        String sanitized = sanitizeByContentType(body, contentType);
        return truncateToChars(sanitized, REQUEST_BODY_LIMIT_BYTES);
    }

    public static String buildOmittedBodyMessage(String contentType, long contentLength) {
        if (StrUtil.isBlank(contentType) && contentLength <= 0) {
            return null;
        }
        return "[request body omitted; contentType=%s; contentLength=%s]"
                .formatted(StrUtil.blankToDefault(contentType, "unknown"), contentLength);
    }

    public static ResultResponseSummary summarizeResultResponse(String body) {
        if (StrUtil.isBlank(body)) {
            return new ResultResponseSummary(null, null, false, false);
        }
        try {
            Object parsed = JSON.parse(body);
            if (!(parsed instanceof JSONObject jsonObject)
                    || !jsonObject.containsKey("code")
                    || !jsonObject.containsKey("msg")) {
                return new ResultResponseSummary(null, null, false, false);
            }
            JSONObject summary = new JSONObject();
            Integer businessCode = jsonObject.getInteger("code");
            summary.put("code", businessCode);
            summary.put("msg", jsonObject.getString("msg"));
            summary.put("dataSummary", summarizeData(jsonObject.get("data")));
            SanitizedText sanitized = truncateToChars(JSON.toJSONString(summary), RESPONSE_SUMMARY_LIMIT_CHARS);
            return new ResultResponseSummary(businessCode, sanitized.value(), sanitized.truncated(), true);
        } catch (Exception ignored) {
            return new ResultResponseSummary(null, null, false, false);
        }
    }

    public static SanitizedText truncateToChars(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return new SanitizedText(value, false);
        }
        return new SanitizedText(value.substring(0, maxChars) + "...[truncated]", true);
    }

    private static String sanitizeByContentType(String body, String contentType) {
        String normalized = StrUtil.nullToEmpty(contentType).toLowerCase(Locale.ROOT);
        if (normalized.contains("json")) {
            return sanitizeJson(body);
        }
        if (normalized.contains("x-www-form-urlencoded")) {
            return sanitizeFormLike(body);
        }
        return sanitizeLooseText(body);
    }

    private static String sanitizeJson(String body) {
        try {
            Object parsed = JSON.parse(body);
            return JSON.toJSONString(maskJsonValue(parsed, null));
        } catch (Exception ignored) {
            return sanitizeLooseText(body);
        }
    }

    private static Object maskJsonValue(Object value, String key) {
        if (isSensitiveKey(key)) {
            return MASK;
        }
        if (value instanceof JSONObject object) {
            JSONObject masked = new JSONObject();
            for (Map.Entry<String, Object> entry : object.entrySet()) {
                masked.put(entry.getKey(), maskJsonValue(entry.getValue(), entry.getKey()));
            }
            return masked;
        }
        if (value instanceof JSONArray array) {
            JSONArray masked = new JSONArray();
            for (Object item : array) {
                masked.add(maskJsonValue(item, null));
            }
            return masked;
        }
        return value;
    }

    private static String sanitizeFormLike(String payload) {
        String[] pairs = payload.split("&", -1);
        StringBuilder builder = new StringBuilder(payload.length());
        for (int i = 0; i < pairs.length; i++) {
            if (i > 0) {
                builder.append('&');
            }
            String pair = pairs[i];
            int split = pair.indexOf('=');
            if (split < 0) {
                builder.append(isSensitiveKey(pair) ? MASK : pair);
                continue;
            }
            String key = pair.substring(0, split);
            builder.append(key).append('=').append(isSensitiveKey(key) ? MASK : pair.substring(split + 1));
        }
        return builder.toString();
    }

    private static String sanitizeLooseText(String body) {
        String sanitized = body;
        String[] names = {
                "password", "passwd", "token", "authorization", "Manager-Token",
                "apiKey", "api_key", "captcha", "verificationCode", "secret", "key"
        };
        for (String name : names) {
            sanitized = sanitized.replaceAll("(?i)(\"" + name + "\"\\s*:\\s*)\"[^\"]*\"", "$1\"" + MASK + "\"");
            sanitized = sanitized.replaceAll("(?i)(" + name + "\\s*=\\s*)[^&\\s]+", "$1" + MASK);
        }
        return sanitized;
    }

    private static String summarizeData(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof JSONObject object) {
            return "object(keys=%s)".formatted(String.join(",", object.keySet()));
        }
        if (data instanceof JSONArray array) {
            return "array(size=%d)".formatted(array.size());
        }
        if (data instanceof CharSequence text) {
            return truncateToChars(sanitizeLooseText(text.toString()), 512).value();
        }
        return truncateToChars(String.valueOf(data), 512).value();
    }

    private static boolean isSensitiveKey(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        if (EXACT_SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
            return true;
        }
        if ("key".equals(normalized)) {
            return true;
        }
        for (String part : SENSITIVE_KEY_PARTS) {
            String normalizedPart = part.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
            if (normalized.contains(normalizedPart)) {
                return true;
            }
        }
        return false;
    }

    public static String decodeBody(byte[] bytes, String encoding) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return new String(bytes, StrUtil.isBlank(encoding)
                    ? StandardCharsets.UTF_8
                    : java.nio.charset.Charset.forName(encoding));
        } catch (Exception ignored) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public record SanitizedText(String value, boolean truncated) {
    }

    public record ResultResponseSummary(
            Integer businessCode,
            String responseBody,
            boolean truncated,
            boolean resultResponse
    ) {
    }
}
