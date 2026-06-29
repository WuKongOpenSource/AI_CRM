package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Schema(name = "CandidateResumeParseVO", description = "候选人简历解析结果")
public class CandidateResumeParseVO {

    @Schema(description = "候选人ID")
    private Long candidateId;

    @Schema(description = "候选人姓名")
    private String candidateName;

    @Schema(description = "是否新建候选人")
    private Boolean created = false;

    @Schema(description = "是否更新候选人")
    private Boolean updated = false;

    @Schema(description = "提取字段")
    private Map<String, Object> parsedFields = new LinkedHashMap<>();

    @Schema(description = "冲突字段")
    private Map<String, Object> conflicts = new LinkedHashMap<>();

    @Schema(description = "简历摘要")
    private String resumeSummary;

    @Schema(description = "AI评估")
    private String aiEvaluation;

    @Schema(description = "原始文本片段")
    private String rawText;
}
