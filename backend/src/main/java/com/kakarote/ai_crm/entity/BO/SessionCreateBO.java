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

    @Schema(description = "关联员工ID")
    private Long employeeId;

    @Schema(description = "关联关系人ID")
    private Long relationId;

    @Schema(description = "关联产品ID")
    private Long productId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联项目任务ID")
    private Long projectTaskId;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "聊天应用编码: general/crm/knowledge/address_book/relation/product")
    private String appCode;
}
