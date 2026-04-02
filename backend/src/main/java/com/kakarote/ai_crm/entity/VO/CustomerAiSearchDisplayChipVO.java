package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 搜索展示标签
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CustomerAiSearchDisplayChipVO", description = "AI 搜索展示标签")
public class CustomerAiSearchDisplayChipVO {

    @Schema(description = "标签字段 key")
    private String key;

    @Schema(description = "标签展示文案")
    private String label;
}
