package com.kakarote.ai_crm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步配置类
 * 启用 @Async 注解支持，用于异步上传文件到 WeKnora
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
