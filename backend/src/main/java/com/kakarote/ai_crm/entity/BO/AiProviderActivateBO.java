package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 激活已保存的 AI 服务商。
 */
@Data
@Schema(name = "AiProviderActivateBO", description = "激活已保存的 AI 服务商")
public class AiProviderActivateBO implements Serializable {

    @NotBlank(message = "服务商不能为空")
    @Schema(description = "AI 服务商编码", example = "dashscope")
    private String provider;
}
