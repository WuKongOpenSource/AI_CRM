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
 * AI 配置更新参数
 */
@Data
@Schema(name = "AiConfigUpdateBO", description = "AI配置更新参数")
public class AiConfigUpdateBO implements Serializable {

    @Schema(description = "AI服务提供商: openai, dashscope, custom", example = "dashscope")
    private String provider;

    @NotBlank(message = "API地址不能为空")
    @Schema(description = "API基础URL", example = "https://dashscope.aliyuncs.com/compatible-mode/")
    private String apiUrl;

    @NotBlank(message = "API Key不能为空")
    @Schema(description = "API密钥")
    private String apiKey;

    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", example = "qwen-max")
    private String model;

    @DecimalMin(value = "0.0", message = "Temperature最小值为0")
    @DecimalMax(value = "2.0", message = "Temperature最大值为2")
    @Schema(description = "Temperature参数 0-2", example = "0.7")
    private Double temperature;

    @Min(value = 100, message = "最大Token数最小值为100")
    @Max(value = 128000, message = "最大Token数最大值为128000")
    @Schema(description = "最大Token数", example = "2048")
    private Integer maxTokens;
}
