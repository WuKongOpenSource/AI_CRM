package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息表
 */
@Data
@TableName("crm_chat_message")
@Schema(name = "ChatMessage", description = "聊天消息表")
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "消息ID")
    private Long messageId;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID")
    private Long sessionId;

    /**
     * 角色: user, assistant, system
     */
    @Schema(description = "角色: user, assistant, system")
    private String role;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容")
    private String content;

    /**
     * 使用token数
     */
    @Schema(description = "使用token数")
    private Integer tokensUsed;

    /**
     * 使用的模型
     */
    @Schema(description = "使用的模型")
    private String modelName;

    /**
     * 函数调用(JSON)
     */
    @Schema(description = "函数调用(JSON)")
    private String functionCall;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;
}
