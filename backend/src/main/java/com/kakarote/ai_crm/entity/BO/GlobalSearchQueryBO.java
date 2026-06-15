package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "全局搜索查询")
public class GlobalSearchQueryBO extends PageEntity {

    @Schema(description = "搜索关键词")
    private String keyword;

    @Schema(description = "搜索类型：all/customer/contact/task/schedule/knowledge")
    private String type;

    @Schema(description = "Entity type: customer/contact/relation/product/task/schedule/knowledge")
    private String entityType;
}
