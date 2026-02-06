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

    @Schema(description = "会话标题")
    private String title;

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
