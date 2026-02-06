package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 操作日志表
 */
@Data
@TableName("crm_operation_log")
@Schema(name = "OperationLog", description = "操作日志表")
public class OperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "日志ID")
    private Long logId;

    /**
     * 模块
     */
    @Schema(description = "模块")
    private String module;

    /**
     * 操作类型
     */
    @Schema(description = "操作类型")
    private String operation;

    /**
     * 目标ID
     */
    @Schema(description = "目标ID")
    private Long targetId;

    /**
     * 目标类型
     */
    @Schema(description = "目标类型")
    private String targetType;

    /**
     * 操作内容
     */
    @Schema(description = "操作内容")
    private String content;

    /**
     * IP地址
     */
    @Schema(description = "IP地址")
    private String ipAddress;

    /**
     * User Agent
     */
    @Schema(description = "User Agent")
    private String userAgent;

    /**
     * 操作人ID
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "操作人ID")
    private Long createUserId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;
}
