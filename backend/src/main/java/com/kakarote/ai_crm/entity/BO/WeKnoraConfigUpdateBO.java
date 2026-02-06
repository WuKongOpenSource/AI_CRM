package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * WeKnora 配置更新参数
 */
@Data
@Schema(name = "WeKnoraConfigUpdateBO", description = "WeKnora配置更新参数")
public class WeKnoraConfigUpdateBO implements Serializable {

    @Schema(description = "是否启用WeKnora")
    private Boolean enabled;

    @Schema(description = "API基础URL", example = "http://localhost:8080/api/v1")
    private String baseUrl;

    @Schema(description = "API Key")
    private String apiKey;

    @Schema(description = "知识库ID")
    private String knowledgeBaseId;

    @Min(value = 1, message = "最大匹配数最小值为1")
    @Max(value = 50, message = "最大匹配数最大值为50")
    @Schema(description = "最大匹配结果数", example = "5")
    private Integer matchCount;

    @DecimalMin(value = "0.0", message = "向量相似度阈值最小值为0")
    @DecimalMax(value = "1.0", message = "向量相似度阈值最大值为1")
    @Schema(description = "向量相似度阈值", example = "0.5")
    private Double vectorThreshold;

    @Schema(description = "是否启用自动RAG")
    private Boolean autoRagEnabled;
}
