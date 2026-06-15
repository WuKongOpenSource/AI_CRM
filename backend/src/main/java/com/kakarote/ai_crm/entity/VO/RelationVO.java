package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
@Schema(name = "RelationVO", description = "Relation view")
public class RelationVO {

    @Schema(description = "Relation ID")
    private Long relationId;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Avatar object key")
    private String avatar;

    @Schema(description = "Avatar URL")
    private String avatarUrl;

    @Schema(description = "Phone")
    private String phone;

    @Schema(description = "Wechat")
    private String wechat;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Relation type")
    private String relationType;

    @Schema(description = "Relation type name")
    private String relationTypeName;

    @Schema(description = "Company")
    private String company;

    @Schema(description = "Linked customer ID")
    private Long customerId;

    @Schema(description = "Linked customer name")
    private String customerName;

    @Schema(description = "Linked customer logo")
    private String customerLogo;

    @Schema(description = "Linked customer logo URL")
    private String customerLogoUrl;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Source")
    private String source;

    @Schema(description = "Source name")
    private String sourceName;

    @Schema(description = "Source customer ID")
    private Long sourceCustomerId;

    @Schema(description = "Source customer name")
    private String sourceCustomerName;

    @Schema(description = "Source contact ID")
    private Long sourceContactId;

    @Schema(description = "Custom field values")
    private Map<String, Object> customFields;

    @Schema(description = "Create user ID")
    private Long createUserId;

    @Schema(description = "Create time")
    private Date createTime;

    @Schema(description = "Update time")
    private Date updateTime;
}
