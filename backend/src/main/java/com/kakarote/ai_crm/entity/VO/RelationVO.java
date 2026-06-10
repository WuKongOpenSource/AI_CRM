package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 关系人视图对象。
 */
@Data
@Schema(name = "RelationVO", description = "关系人视图对象")
public class RelationVO {

    @Schema(description = "关系人ID")
    private Long relationId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "头像访问URL")
    private String avatarUrl;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "微信号")
    private String wechat;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "关系类型")
    private String relationType;

    @Schema(description = "关系类型名称")
    private String relationTypeName;

    @Schema(description = "所属公司")
    private String company;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "关联客户名称")
    private String customerName;

    @Schema(description = "关联客户Logo")
    private String customerLogo;

    @Schema(description = "关联客户Logo访问URL")
    private String customerLogoUrl;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "来源名称")
    private String sourceName;

    @Schema(description = "来源客户ID")
    private Long sourceCustomerId;

    @Schema(description = "来源客户名称")
    private String sourceCustomerName;

    @Schema(description = "来源客户联系人ID")
    private Long sourceContactId;

    @Schema(description = "自定义字段值")
    private Map<String, Object> customFields;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}
