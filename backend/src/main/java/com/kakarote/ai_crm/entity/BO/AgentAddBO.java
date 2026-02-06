package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 智能体新增参数
 */
@Data
@Schema(name = "AgentAddBO", description = "智能体新增参数")
public class AgentAddBO {

    @NotBlank(message = "名称不能为空")
    @Schema(description = "显示名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String label;

    @Schema(description = "图标名称")
    private String iconName;

    @NotBlank(message = "系统提示词不能为空")
    @Schema(description = "系统提示词", requiredMode = Schema.RequiredMode.REQUIRED)
    private String prompt;

    @Schema(description = "角色人设")
    private String persona;

    @Schema(description = "知识库类型列表")
    private List<String> knowledgeBaseTypes;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "排序")
    private Integer sortOrder;
}
