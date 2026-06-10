package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关系人查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RelationQueryBO", description = "关系人查询参数")
public class RelationQueryBO extends PageEntity {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "关系类型")
    private String relationType;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "来源客户ID")
    private Long sourceCustomerId;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "来源客户联系人ID")
    private Long sourceContactId;
}
