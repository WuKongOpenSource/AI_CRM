package com.kakarote.ai_crm.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 请求上下文工具类
 * 用于从当前 HTTP 请求中动态获取访问地址
 * 支持反向代理场景（通过 X-Forwarded-* 头）
 */
public class RequestContextUtil {

    /**
     * 获取当前 HTTP 请求
     */
    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest();
    }

    /**
     * 获取当前请求的基础 URL（scheme + host，不含路径）
     * 示例：http://192.168.1.170 或 https://example.com
     *
     * @return 基础 URL，如果无法获取则返回 null
     */
    public static String getBaseUrl() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        return getBaseUrl(request);
    }

    /**
     * 从指定请求中获取基础 URL
     *
     * @param request HTTP 请求
     * @return 基础 URL
     */
    public static String getBaseUrl(HttpServletRequest request) {

        // 优先使用 X-Forwarded 头（反向代理场景）
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (StrUtil.isBlank(scheme)) {
            scheme = request.getScheme();
        }
        String host = SystemUtil.get("HOST_IP", "");
        if (StrUtil.isEmpty(host)) {
            // 获取 Host（可能包含端口）
            host = request.getHeader("X-Forwarded-Host");
            if (StrUtil.isBlank(host)) {
                host = request.getHeader("Host");
            }
        }

        if (StrUtil.isBlank(host)) {
            host = request.getServerName();
            int port = request.getServerPort();
            // 只有非标准端口才需要显示
            if ((scheme.equals("http") && port != 80) ||
                (scheme.equals("https") && port != 443)) {
                host = host + ":" + port;
            }
        }

        return scheme + "://" + host;
    }

    /**
     * 从基础 URL 提取 host（不含端口）
     *
     * @param baseUrl 基础 URL，如 http://192.168.1.170:8080
     * @return host 部分，如 192.168.1.170
     */
    public static String extractHost(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            return null;
        }
        try {
            URI uri = new URI(baseUrl);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * 从基础 URL 提取 scheme
     *
     * @param baseUrl 基础 URL，如 http://192.168.1.170
     * @return scheme 部分，如 http
     */
    public static String extractScheme(String baseUrl) {
        if (StrUtil.isBlank(baseUrl)) {
            return "http";
        }
        try {
            URI uri = new URI(baseUrl);
            return uri.getScheme();
        } catch (URISyntaxException e) {
            return "http";
        }
    }

    /**
     * 基于当前请求的 host 构建指定端口的 URL
     *
     * @param port 目标端口
     * @param path 路径（可选，以 / 开头）
     * @return 完整 URL，如 http://192.168.1.170:9001/oauth_callback
     */
    public static String buildUrlWithPort(int port, String path) {
        String baseUrl = getBaseUrl();
        return buildUrlWithPort(baseUrl, port, path);
    }

    /**
     * 基于指定基础 URL 构建新端口的 URL
     *
     * @param baseUrl 基础 URL，如 http://192.168.1.170 或 http://192.168.1.170:80
     * @param port    目标端口
     * @param path    路径（可选，以 / 开头）
     * @return 完整 URL
     */
    public static String buildUrlWithPort(String baseUrl, int port, String path) {
        if (StrUtil.isBlank(baseUrl)) {
            return null;
        }

        String scheme = extractScheme(baseUrl);
        String host = extractHost(baseUrl);

        if (host == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(host);

        // 只有非标准端口才添加端口号
        boolean isStandardPort = (scheme.equals("http") && port == 80) ||
                                 (scheme.equals("https") && port == 443);
        if (!isStandardPort && port > 0) {
            sb.append(":").append(port);
        }

        if (StrUtil.isNotBlank(path)) {
            if (!path.startsWith("/")) {
                sb.append("/");
            }
            sb.append(path);
        }

        return sb.toString();
    }

    /**
     * 获取 MinIO Console URL
     *
     * @param minioConsolePort MinIO Console 端口
     * @return MinIO Console URL
     */
    public static String getMinioConsoleUrl(int minioConsolePort) {
        return buildUrlWithPort(minioConsolePort, "");
    }

    /**
     * 获取 MinIO OAuth 回调 URL
     *
     * @param minioConsolePort MinIO Console 端口
     * @return OAuth 回调 URL
     */
    public static String getMinioOAuthCallbackUrl(int minioConsolePort) {
        return buildUrlWithPort(minioConsolePort, "/oauth_callback");
    }

    /**
     * 获取前端登录页面 URL（Vue Hash 路由模式）
     *
     * @return 前端登录页 URL，如 http://192.168.1.170/#/login
     */
    public static String getFrontendLoginUrl() {
        String baseUrl = getBaseUrl();
        if (baseUrl == null) {
            return null;
        }
        return baseUrl + "/#/login";
    }
}
