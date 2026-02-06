package com.kakarote.ai_crm.config;

import com.kakarote.ai_crm.config.security.filter.JwtAuthenticationTokenFilter;
import com.kakarote.ai_crm.config.security.handle.AuthenticationEntryPointImpl;
import com.kakarote.ai_crm.config.security.handle.LogoutSuccessHandlerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;

import jakarta.servlet.DispatcherType;
import java.util.List;

/**
 * spring security配置
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {
    /**
     * 自定义用户认证逻辑
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 认证失败处理类
     */
    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    /**
     * 退出处理类
     */
    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    /**
     * token认证过滤器
     */
    @Autowired
    private JwtAuthenticationTokenFilter authenticationTokenFilter;

    /**
     * 跨域过滤器
     */
    @Autowired
    private CorsFilter corsFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF，因为使用的是 Token 认证，不依赖 Session
                .csrf(AbstractHttpConfigurer::disable)
                // 认证失败处理（自定义 UnauthorizedHandler）
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                // 不使用 Session 进行认证，采用无状态机制
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 允许异步 dispatch（流式响应需要）
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/login", "/auth/login").anonymous() // 允许匿名访问
                        .requestMatchers("/", "/index", "/static/**", "/assets/**").anonymous()
                        .requestMatchers("/index.html", "/doc.html", "/swagger-resources/**", "/webjars/**", "/v3/api-docs/**", "/instrument/callback").anonymous()
                        .requestMatchers("/api/enum/**").permitAll() // 枚举值接口允许访问
                        // OIDC 端点允许访问（用于 MinIO SSO）
                        .requestMatchers("/.well-known/openid-configuration", "/oauth2/authorize", "/oauth2/token", "/oauth2/userinfo", "/oauth2/jwks", "/oauth2/minio-sso").permitAll()
                        .anyRequest().authenticated() // 其他所有请求都需要认证
                )
                // 允许 iframe 加载（避免 X-Frame-Options 限制）
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                // 配置登出处理
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler));

        // 添加 JWT 认证过滤器
        http.addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        // 添加 CORS 过滤器（放在 JWT 过滤器之前）
        http.addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class);
        http.addFilterBefore(corsFilter, LogoutFilter.class);

        return http.build();
    }


    @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
    public AuthenticationManager authenticationManagerBean() throws Exception {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(List.of(authProvider));
    }

    /**
     * 强散列哈希加密实现
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
