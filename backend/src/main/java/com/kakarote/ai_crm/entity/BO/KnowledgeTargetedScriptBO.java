package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "KnowledgeTargetedScriptBO", description = "定向销售话术生成请求")
public class KnowledgeTargetedScriptBO {

    @NotEmpty(message = "请至少选择一份参考文档")
    @Size(max = 4, message = "最多只能选择 4 份参考文档")
    @Schema(description = "参考文档 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> knowledgeIds;

    @NotNull(message = "请选择目标客户")
    @Schema(description = "目标客户 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;
}
