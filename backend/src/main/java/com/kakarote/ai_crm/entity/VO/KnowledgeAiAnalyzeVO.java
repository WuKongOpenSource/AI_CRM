package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI分析知识库文档响应")
public class KnowledgeAiAnalyzeVO {

    @Schema(description = "核心提炼/摘要")
    private String coreHighlights;

    @Schema(description = "推荐话术（3-5条销售建议）")
    private List<String> talkingPoints;

    @Schema(description = "关联实体（客户/商机等）")
    private List<RelatedEntity> relatedEntities;

    @Data
    @Schema(description = "关联实体")
    public static class RelatedEntity {

        @Schema(description = "实体名称")
        private String name;

        @Schema(description = "实体类型: customer, opportunity")
        private String type;
    }
}
