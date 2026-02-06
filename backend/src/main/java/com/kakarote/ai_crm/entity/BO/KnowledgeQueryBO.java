package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "KnowledgeQueryBO", description = "知识库查询参数")
public class KnowledgeQueryBO extends PageEntity {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "上传人ID")
    private Long uploadUserId;
}
