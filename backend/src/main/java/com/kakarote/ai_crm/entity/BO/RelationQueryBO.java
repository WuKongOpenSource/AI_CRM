package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "RelationQueryBO", description = "Relation query request")
public class RelationQueryBO extends PageEntity {

    @Schema(description = "Keyword")
    private String keyword;

    @Schema(description = "Relation type")
    private String relationType;

    @Schema(description = "Source")
    private String source;

    @Schema(description = "Source customer ID")
    private Long sourceCustomerId;

    @Schema(description = "Linked customer ID")
    private Long customerId;

    @Schema(description = "Source contact ID")
    private Long sourceContactId;
}
