package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 配置信息
 */
@Data
@Schema(name = "AiConfigVO", description = "AI配置信息")
public class AiConfigVO implements Serializable {

    @Schema(description = "AI服务提供商")
    private String provider;

    @Schema(description = "API基础URL")
    private String apiUrl;

    @Schema(description = "API密钥（脱敏显示）")
    private String apiKey;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "Temperature参数")
    private Double temperature;

    @Schema(description = "最大Token数")
    private Integer maxTokens;

    @Schema(description = "最后更新时间")
    private Date updateTime;
}
