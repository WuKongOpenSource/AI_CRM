package com.kakarote.ai_crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * CORS跨域配置
 */
@Configuration
public class CorsConfig {

    /**
     * 处理corsFilter方法逻辑。
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // ================= 修改重点 =================
        // 使用通配符 * 允许所有域名
        // Spring Boot 会自动在响应头中回显请求的 Origin，从而满足 allowCredentials=true 的要求
        config.setAllowedOriginPatterns(Collections.singletonList("*"));

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

        // 保持开启，因为你需要支持 OIDC 和 Cookie
        config.setAllowCredentials(true);

        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
