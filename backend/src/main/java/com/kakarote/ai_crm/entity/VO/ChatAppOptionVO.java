package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "ChatAppOptionVO", description = "Chat application option")
public class ChatAppOptionVO {

    @Schema(description = "Application code")
    private String code;

    @Schema(description = "Display label")
    private String label;

    @Schema(description = "Icon name")
    private String iconName;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Default RAG enabled")
    private Boolean defaultRagEnabled;

    @Schema(description = "Recommended questions")
    private List<String> recommendedQuestions;
}
