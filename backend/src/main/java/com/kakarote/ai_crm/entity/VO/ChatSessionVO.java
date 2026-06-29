package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 会话视图对象
 */
@Data
@Schema(name = "ChatSessionVO", description = "会话视图对象")
public class ChatSessionVO {

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "智能体ID")
    private Long agentId;

    @Schema(description = "智能体名称")
    private String agentName;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "关联客户名称")
    private String customerName;

    @Schema(description = "关联客户Logo访问地址")
    private String customerLogoUrl;

    @Schema(description = "关联员工ID")
    private Long employeeId;

    @Schema(description = "关联员工姓名")
    private String employeeName;

    @Schema(description = "关联员工头像访问地址")
    private String employeeAvatarUrl;

    @Schema(description = "关联关系人ID")
    private Long relationId;

    @Schema(description = "关联关系人姓名")
    private String relationName;

    @Schema(description = "关联关系人头像访问地址")
    private String relationAvatarUrl;

    @Schema(description = "关联产品ID")
    private Long productId;

    @Schema(description = "关联产品名称")
    private String productName;

    @Schema(description = "关联产品编码")
    private String productCode;

    @Schema(description = "Product image URL")
    private String productImageUrl;

    @Schema(description = "关联候选人ID")
    private Long candidateId;

    @Schema(description = "关联候选人姓名")
    private String candidateName;

    @Schema(description = "关联候选人头像访问地址")
    private String candidateAvatarUrl;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "关联项目名称")
    private String projectName;

    @Schema(description = "关联项目任务ID")
    private Long projectTaskId;

    @Schema(description = "关联项目任务名称")
    private String projectTaskTitle;

    @Schema(description = "会话标题")
    private String title;

    @Schema(description = "聊天应用编码: general/crm/knowledge/address_book/product/hr")
    private String appCode;

    @Schema(description = "Whether the session is pinned")
    private Boolean pinned;

    @Schema(description = "Last time the session was pinned")
    private Date pinnedTime;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "最后一条消息")
    private String lastMessage;

    @Schema(description = "消息数量")
    private Integer messageCount;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}
