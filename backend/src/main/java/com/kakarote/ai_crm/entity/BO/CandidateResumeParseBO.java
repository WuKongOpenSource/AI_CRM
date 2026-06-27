package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "CandidateResumeParseBO", description = "候选人简历解析参数")
public class CandidateResumeParseBO {

    @Schema(description = "候选人ID，传入后优先补全该候选人")
    private Long candidateId;

    @Schema(description = "简历文本内容")
    private String content;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "默认应聘岗位")
    private String appliedPosition;

    @Schema(description = "候选人来源")
    private String source;

    @Schema(description = "是否自动新建或补全候选人")
    private Boolean autoSave;
}
