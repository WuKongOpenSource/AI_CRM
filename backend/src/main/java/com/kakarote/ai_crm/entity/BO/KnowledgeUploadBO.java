package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 知识库上传参数
 */
@Data
@Schema(name = "KnowledgeUploadBO", description = "知识库上传参数")
public class KnowledgeUploadBO {

    @NotBlank(message = "类型不能为空")
    @Schema(description = "类型: meeting, email, recording, document, proposal, contract", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "AI摘要")
    private String summary;
}
