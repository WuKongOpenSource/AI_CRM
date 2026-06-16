package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Schema(name = "KnowledgeAiSearchVO", description = "Knowledge AI search result")
public class KnowledgeAiSearchVO {

    @Schema(description = "Original keyword")
    private String keyword;

    @Schema(description = "AI answer in markdown")
    private String answer;

    @Schema(description = "Elapsed milliseconds")
    private Long tookMs;

    @Schema(description = "Overall match percent")
    private Integer matchPercent;

    @Schema(description = "Matched document count")
    private Integer totalHits;

    @Schema(description = "Referenced documents")
    private List<ReferenceItem> references;

    @Data
    @Schema(name = "KnowledgeAiSearchReferenceItem", description = "Referenced knowledge document")
    public static class ReferenceItem {

        @Schema(description = "Knowledge ID")
        private Long knowledgeId;

        @Schema(description = "Document name")
        private String name;

        @Schema(description = "Knowledge type")
        private String type;

        @Schema(description = "Customer name")
        private String customerName;

        @Schema(description = "Document summary")
        private String summary;

        @Schema(description = "Matched excerpt")
        private String excerpt;

        @Schema(description = "Match percent")
        private Integer matchPercent;

        @Schema(description = "Document size in bytes")
        private Long fileSize;

        @Schema(description = "Created time")
        private Date createTime;
    }
}
