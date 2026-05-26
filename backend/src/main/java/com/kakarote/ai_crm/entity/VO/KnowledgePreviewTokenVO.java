package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "KnowledgePreviewTokenVO", description = "Knowledge media preview token result")
public class KnowledgePreviewTokenVO {

    @Schema(description = "Short-lived media preview URL")
    private String url;

    @Schema(description = "Preview URL expiration time in ISO-8601 format")
    private String expiresAt;

    @Schema(description = "Preview URL TTL in seconds")
    private long expiresInSeconds;
}
