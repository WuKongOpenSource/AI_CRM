package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 联系人更新参数
 */
@Data
@Schema(name = "ContactUpdateBO", description = "联系人更新参数")
public class ContactUpdateBO {

    @NotNull(message = "联系人ID不能为空")
    @Schema(description = "联系人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long contactId;

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

    @Schema(description = "备注")
    private String notes;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "自定义字段值")
    private Map<String, Object> customFields;
}
