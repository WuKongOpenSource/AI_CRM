package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 关系人新增参数。
 */
@Data
@Schema(name = "RelationAddBO", description = "关系人新增参数")
public class RelationAddBO {

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "微信号")
    private String wechat;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "关系类型")
    private String relationType;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "自定义字段值")
    private Map<String, Object> customFields;
}
