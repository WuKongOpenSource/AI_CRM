package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 客户查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "CustomerQueryBO", description = "客户查询参数")
public class CustomerQueryBO extends PageEntity {

    @Schema(description = "公司名称关键词")
    private String keyword;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "阶段列表")
    private List<String> stages;

    @Schema(description = "客户等级")
    private String level;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "客户来源")
    private String source;
}
