package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
@Schema(name = "RelationAddBO", description = "Relation create request")
public class RelationAddBO {

    @NotBlank(message = "Name is required")
    @Schema(description = "Name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Avatar object key")
    private String avatar;

    @Schema(description = "Phone")
    private String phone;

    @Schema(description = "Wechat")
    private String wechat;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Relation type")
    private String relationType;

    @Schema(description = "Linked customer ID")
    private Long customerId;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Custom field values")
    private Map<String, Object> customFields;
}
