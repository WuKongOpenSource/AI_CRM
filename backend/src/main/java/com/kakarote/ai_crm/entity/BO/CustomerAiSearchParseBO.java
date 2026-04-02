package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 客户搜索解析请求
 */
@Data
@Schema(name = "CustomerAiSearchParseBO", description = "AI 客户搜索解析请求")
public class CustomerAiSearchParseBO {

    @NotBlank(message = "搜索内容不能为空")
    @Schema(description = "自然语言搜索内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;
}
