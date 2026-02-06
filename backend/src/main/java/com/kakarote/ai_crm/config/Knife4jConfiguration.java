package com.kakarote.ai_crm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置
 * @author hmb
 */
@Configuration
public class Knife4jConfiguration {

    @Value("${spring.application.name:common-web}")
    private String desc;

    @Bean
    public OpenAPI createRestApi() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.info(apiInfo());
        return openAPI;
    }

    private Info apiInfo() {
        return new Info()
                .title(desc)
                .version("1.0.0")
                .description("");
    }

    /**
     * GroupedOpenApi 是对接口文档分组，类似于 swagger 的 Docket
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                // 组名
                .group(desc)
                // 扫描的包
                .packagesToScan("com.kakarote.ai_crm.controller")
                .build();
    }

}
