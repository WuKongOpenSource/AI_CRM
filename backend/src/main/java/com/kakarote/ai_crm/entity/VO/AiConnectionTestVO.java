package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AI 连接测试结果。
 */
@Data
@Schema(name = "AiConnectionTestVO", description = "AI 连接测试结果")
public class AiConnectionTestVO implements Serializable {

    @Schema(description = "是否连接成功")
    private Boolean success;

    @Schema(description = "响应时间，单位毫秒")
    private Long responseTime;

    @Schema(description = "返回消息或错误信息")
    private String message;

    @Schema(description = "使用的模型")
    private String model;

    @Schema(description = "使用的服务商编码")
    private String provider;
}
