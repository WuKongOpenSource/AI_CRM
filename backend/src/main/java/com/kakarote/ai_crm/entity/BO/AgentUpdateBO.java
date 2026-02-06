package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 智能体更新参数
 */
@Data
@Schema(name = "AgentUpdateBO", description = "智能体更新参数")
public class AgentUpdateBO {

    @NotNull(message = "智能体ID不能为空")
    @Schema(description = "智能体ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long agentId;

    @Schema(description = "显示名称")
    private String label;

    @Schema(description = "图标名称")
    private String iconName;

    @Schema(description = "系统提示词")
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
