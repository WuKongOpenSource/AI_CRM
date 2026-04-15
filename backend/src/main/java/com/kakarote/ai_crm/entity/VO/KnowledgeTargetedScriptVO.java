package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "KnowledgeTargetedScriptVO", description = "定向销售话术生成响应")
public class KnowledgeTargetedScriptVO {

    @Schema(description = "结果标题")
    private String title;

    @Schema(description = "结果副标题")
    private String subtitle;

    @Schema(description = "Markdown 格式的话术内容")
    private String content;

    @Schema(description = "目标客户 ID")
    private Long customerId;

    @Schema(description = "目标客户名称")
    private String customerName;

    @Schema(description = "参考文档 ID 列表")
    private List<Long> knowledgeIds;

    @Schema(description = "参考文档名称列表")
    private List<String> knowledgeNames;
}
