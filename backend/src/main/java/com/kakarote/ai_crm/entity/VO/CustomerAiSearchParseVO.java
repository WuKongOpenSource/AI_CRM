package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI 客户搜索解析结果
 */
@Data
@Schema(name = "CustomerAiSearchParseVO", description = "AI 客户搜索解析结果")
public class CustomerAiSearchParseVO {

    @Schema(description = "原始搜索内容")
    private String originalQuery;

    @Schema(description = "规范化后的搜索内容")
    private String normalizedQuery;

    @Schema(description = "结构化查询条件")
    private CustomerAiSearchQueryVO parsedQuery;

    @Schema(description = "展示标签")
    private List<CustomerAiSearchDisplayChipVO> displayChips;

    @Schema(description = "解析说明")
    private String explanation;

    @Schema(description = "解析置信度 0-1")
    private Double confidence;

    @Schema(description = "是否回退为关键词搜索")
    private Boolean fallbackKeywordSearch;
}
