package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(name = "GlobalSearchResultVO", description = "Global search result")
public class GlobalSearchResultVO {

    @Schema(description = "Entity type")
    private String entityType;

    @Schema(description = "Entity id")
    private Long entityId;

    @Schema(description = "Title")
    private String title;

    @Schema(description = "Subtitle")
    private String subtitle;

    @Schema(description = "Summary")
    private String summary;

    @Schema(description = "Customer id")
    private Long customerId;

    @Schema(description = "Customer name")
    private String customerName;

    @Schema(description = "Route path")
    private String routePath;

    @Schema(description = "Sort time")
    private Date sortTime;

    @Schema(description = "Search relevance score")
    private Double score;
}
