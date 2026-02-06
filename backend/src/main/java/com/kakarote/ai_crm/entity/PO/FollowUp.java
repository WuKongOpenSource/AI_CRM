package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 跟进记录表
 */
@Data
@TableName("crm_follow_up")
@Schema(name = "FollowUp", description = "跟进记录表")
public class FollowUp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 跟进ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "跟进ID")
    private Long followUpId;

    /**
     * 客户ID
     */
    @Schema(description = "客户ID")
    private Long customerId;

    /**
     * 联系人ID
     */
    @Schema(description = "联系人ID")
    private Long contactId;

    /**
     * 类型: call, meeting, email, visit
     */
    @Schema(description = "类型: call, meeting, email, visit")
    private String type;

    /**
     * 跟进内容
     */
    @Schema(description = "跟进内容")
    private String content;

    /**
     * 跟进时间
     */
    @Schema(description = "跟进时间")
    private Date followTime;

    /**
     * 下次跟进时间
     */
    @Schema(description = "下次跟进时间")
    private Date nextFollowTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createUserId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;
}
