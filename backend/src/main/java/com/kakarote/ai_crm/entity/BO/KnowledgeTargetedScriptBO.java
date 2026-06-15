package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "KnowledgeTargetedScriptBO", description = "Targeted sales script request")
public class KnowledgeTargetedScriptBO {

    @NotEmpty(message = "Please select at least one reference document")
    @Size(max = 4, message = "At most 4 reference documents can be selected")
    @Schema(description = "Reference knowledge IDs", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> knowledgeIds;

    @NotNull(message = "Please select target customer")
    @Schema(description = "Target customer ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;
}
