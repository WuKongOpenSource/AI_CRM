package com.kakarote.ai_crm.config;

import com.kakarote.ai_crm.config.security.filter.AccessLogFilter;
import com.kakarote.ai_crm.config.security.filter.JwtAuthenticationTokenFilter;
import com.kakarote.ai_crm.config.security.handle.AuthenticationEntryPointImpl;
import com.kakarote.ai_crm.config.security.handle.LogoutSuccessHandlerImpl;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

import java.util.List;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    @Autowired
    private JwtAuthenticationTokenFilter authenticationTokenFilter;

    @Autowired
    private AccessLogFilter accessLogFilter;

    @Autowired
    private CorsFilter corsFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/login", "/auth/login", "/captcha/get", "/captcha/check").anonymous()
                        .requestMatchers(
                                "/auth/external/providers",
                                "/auth/external/*/authorize",
                                "/auth/external/*/callback",
                                "/auth/external/login-ticket"
                        ).permitAll()
                        .requestMatchers(
                                "/",
                                "/index",
                                "/static/**",
                                "/assets/**",
                                "/favicon.ico",
                                "/logo.png",
                                "/*.txt"
                        ).anonymous()
                        .requestMatchers(
                                "/index.html",
                                "/doc.html",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/instrument/callback"
                        ).anonymous()
                        .requestMatchers(
                                "/legal-document/**",
                                "/mail/oauth/*/callback",
                                "/email/oauth/*/callback",
                                "/api/enum/**",
                                "/knowledge/preview-range/**",
                                "/.well-known/openid-configuration",
                                "/oauth2/authorize",
                                "/oauth2/token",
                                "/oauth2/userinfo",
                                "/oauth2/jwks",
                                "/oauth2/minio-sso",
                                "/plugin/gateway/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler));

        http.addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(accessLogFilter, JwtAuthenticationTokenFilter.class);
        http.addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class);
        http.addFilterBefore(corsFilter, LogoutFilter.class);

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<AccessLogFilter> accessLogFilterRegistration(AccessLogFilter filter) {
        FilterRegistrationBean<AccessLogFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
    public AuthenticationManager authenticationManagerBean() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(List.of(authProvider));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
