package com.kakarote.ai_crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * CORS跨域配置
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的前端域名（需要支持 Cookie 跨域传输，所以不能用 *）
        // 包含：前端开发服务器、MinIO Console
        config.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.*.*:*",
            "https://aicrm.5kcrm.cn"
        ));

        // 允许的请求方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        config.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "Manager-Token",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Cache-Control",
            "Cookie"
        ));

        // 暴露的响应头
        config.setExposedHeaders(Arrays.asList(
            "Content-Type",
            "Manager-Token",
            "Content-Disposition",
            "Set-Cookie"
        ));

        // 允许携带凭证（Cookie）- OIDC 需要
        config.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用CORS配置
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
