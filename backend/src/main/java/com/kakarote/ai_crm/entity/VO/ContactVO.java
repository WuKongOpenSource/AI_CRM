package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 联系人视图对象
 */
@Data
@Schema(name = "ContactVO", description = "联系人视图对象")
public class ContactVO {

    @Schema(description = "联系人ID")
    private Long contactId;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "微信")
    private String wechat;

    @Schema(description = "是否主联系人")
    private Integer isPrimary;

    @Schema(description = "最后联系时间")
    private Date lastContactTime;

    @Schema(description = "备注")
    private String notes;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "自定义字段值")
    private Map<String, Object> customFields;

    @Schema(description = "创建时间")
    private Date createTime;
}
