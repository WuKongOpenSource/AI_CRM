package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * AI 配置信息
 */
@Data
@Schema(name = "AiConfigVO", description = "AI 配置信息")
public class AiConfigVO implements Serializable {

    @Schema(description = "AI 服务商编码")
    private String provider;

    @Schema(description = "AI 服务商显示名称")
    private String providerLabel;

    @Schema(description = "API 基础 URL")
    private String apiUrl;

    @Schema(description = "API 密钥，返回时为脱敏值")
    private String apiKey;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "Temperature 参数")
    private Double temperature;

    @Schema(description = "最大 Token 数")
    private Integer maxTokens;

    @Schema(description = "额外请求头是否已配置")
    private Boolean extraHeadersConfigured;

    @Schema(description = "额外请求头 JSON，仅管理接口可见")
    private String extraHeadersJson;

    @Schema(description = "当前模型能力")
    private CapabilitiesVO capabilities;

    @Schema(description = "当前服务商模型填写提示")
    private String modelHint;

    @Schema(description = "当前服务商额外请求头提示")
    private String extraHeadersHint;

    @Schema(description = "系统支持的服务商列表")
    private List<ProviderOptionVO> availableProviders;

    @Schema(description = "当前 AI 使用模式：custom")
    private String mode;

    @Schema(description = "是否已保存自建 AI 配置")
    private Boolean customConfigSaved;

    @Schema(description = "当前模式下 AI 是否可立即使用")
    private Boolean ready;

    @Schema(description = "最后更新时间")
    private Date updateTime;

    @Data
    @Schema(name = "AiConfigCapabilitiesVO", description = "AI 模型能力")
    public static class CapabilitiesVO implements Serializable {
        @Schema(description = "是否支持流式输出")
        private Boolean supportsStream;

        @Schema(description = "是否支持工具调用")
        private Boolean supportsToolCall;

        @Schema(description = "是否支持视觉输入")
        private Boolean supportsVision;
    }

    @Data
    @Schema(name = "AiProviderOptionVO", description = "AI 服务商选项")
    public static class ProviderOptionVO implements Serializable {
        @Schema(description = "服务商编码")
        private String value;

        @Schema(description = "服务商名称")
        private String label;

        @Schema(description = "服务商说明")
        private String description;

        @Schema(description = "默认 API URL")
        private String baseUrl;

        @Schema(description = "推荐模型列表")
        private List<String> models;

        @Schema(description = "模型填写提示")
        private String modelHint;

        @Schema(description = "额外请求头提示")
        private String extraHeadersHint;

        @Schema(description = "默认是否支持流式输出")
        private Boolean supportsStream;

        @Schema(description = "默认是否支持工具调用")
        private Boolean supportsToolCall;

        @Schema(description = "默认是否支持视觉输入")
        private Boolean supportsVision;

        @Schema(description = "是否已保存该服务商配置")
        private Boolean configured;

        @Schema(description = "当前默认激活的服务商是否为该项")
        private Boolean active;

        @Schema(description = "是否已保存该服务商的 API Key")
        private Boolean apiKeyConfigured;

        @Schema(description = "已保存的 API URL")
        private String savedApiUrl;

        @Schema(description = "已保存的模型名称")
        private String savedModel;

        @Schema(description = "已保存的 Temperature")
        private Double savedTemperature;

        @Schema(description = "已保存的最大 Token 数")
        private Integer savedMaxTokens;

        @Schema(description = "是否已保存额外请求头")
        private Boolean savedExtraHeadersConfigured;

        @Schema(description = "已保存的额外请求头 JSON，仅管理接口可见")
        private String savedExtraHeadersJson;
    }
}
