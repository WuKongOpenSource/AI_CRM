package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "ChatAppOptionVO", description = "聊天应用选项")
public class ChatAppOptionVO {

    @Schema(description = "应用编码")
    private String code;

    @Schema(description = "显示名称")
    private String label;

    @Schema(description = "图标名称")
    private String iconName;

    @Schema(description = "应用说明")
    private String description;

    @Schema(description = "是否默认启用知识库 RAG")
    private Boolean defaultRagEnabled;

    @Schema(description = "推荐问题")
    private List<String> recommendedQuestions;
}
