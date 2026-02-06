package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 跟进记录视图对象
 */
@Data
@Schema(name = "FollowUpVO", description = "跟进记录视图对象")
public class FollowUpVO {

    @Schema(description = "跟进ID")
    private Long followUpId;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "联系人ID")
    private Long contactId;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "跟进内容")
    private String content;

    @Schema(description = "跟进时间")
    private Date followTime;

    @Schema(description = "下次跟进时间")
    private Date nextFollowTime;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private Date createTime;
}
