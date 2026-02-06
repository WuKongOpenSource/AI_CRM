package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 智能体视图对象
 */
@Data
@Schema(name = "AgentVO", description = "智能体视图对象")
public class AgentVO {

    @Schema(description = "智能体ID")
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
    private Integer enabled;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;
}
