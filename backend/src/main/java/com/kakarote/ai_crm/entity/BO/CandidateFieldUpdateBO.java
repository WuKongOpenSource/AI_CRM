package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "CandidateFieldUpdateBO", description = "候选人单字段更新参数")
public class CandidateFieldUpdateBO {

    @NotNull(message = "候选人ID不能为空")
    @Schema(description = "候选人ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long candidateId;

    @NotBlank(message = "字段名不能为空")
    @Schema(description = "字段名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @Schema(description = "字段值")
    private Object value;
}
