package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 配置更新参数。
 */
@Data
@Schema(name = "AiConfigUpdateBO", description = "AI 配置更新参数")
public class AiConfigUpdateBO implements Serializable {

    @Schema(description = "AI 服务商编码", example = "dashscope")
    private String provider;

    @NotBlank(message = "API 地址不能为空")
    @Schema(description = "API 基础 URL", example = "https://dashscope.aliyuncs.com/compatible-mode")
    private String apiUrl;

    @Schema(description = "API 密钥；更新已保存服务商时可留空以沿用历史 Key")
    private String apiKey;

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", example = "qwen3.5-plus")
    private String model;

    @DecimalMin(value = "0.0", message = "Temperature 最小值为 0")
    @DecimalMax(value = "2.0", message = "Temperature 最大值为 2")
    @Schema(description = "Temperature 参数，范围 0-2", example = "0.7")
    private Double temperature;

    @Min(value = 100, message = "最大 Token 数最小值为 100")
    @Max(value = 128000, message = "最大 Token 数最大值为 128000")
    @Schema(description = "最大 Token 数", example = "2048")
    private Integer maxTokens;

    @Schema(
            description = "额外请求头 JSON，适用于 OpenAI 兼容服务的自定义头，例如 {\"appid\":\"your-app-id\"}",
            example = "{\"appid\":\"your-app-id\"}"
    )
    private String extraHeadersJson;
}
