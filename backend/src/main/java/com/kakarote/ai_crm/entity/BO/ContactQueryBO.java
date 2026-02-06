package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 联系人查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ContactQueryBO", description = "联系人查询参数")
public class ContactQueryBO extends PageEntity {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "姓名关键词")
    private String keyword;

    @Schema(description = "是否主联系人")
    private Integer isPrimary;
}
