package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 会话表
 */
@Data
@TableName("crm_chat_session")
@Schema(name = "ChatSession", description = "会话表")
public class ChatSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "会话ID")
    private Long sessionId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 智能体ID
     */
    @Schema(description = "智能体ID")
    private Long agentId;

    /**
     * 关联客户ID
     */
    @Schema(description = "关联客户ID")
    private Long customerId;

    /**
     * 会话标题
     */
    @Schema(description = "会话标题")
    private String title;

    /**
     * 状态: 0-已归档, 1-活跃
     */
    @Schema(description = "状态: 0-已归档, 1-活跃")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改时间")
    private Date updateTime;
}
