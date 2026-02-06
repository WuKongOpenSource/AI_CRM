package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 会话创建参数
 */
@Data
@Schema(name = "SessionCreateBO", description = "会话创建参数")
public class SessionCreateBO {

    @Schema(description = "智能体ID")
    private Long agentId;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "会话标题")
    private String title;
}
