package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import com.kakarote.ai_crm.entity.BO.CustomerFieldFilterBO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * AI 客户搜索解析后的结构化查询
 */
@Data
@Schema(name = "CustomerAiSearchQueryVO", description = "AI 客户搜索解析后的结构化查询")
public class CustomerAiSearchQueryVO {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "单个阶段")
    private String stage;

    @Schema(description = "阶段列表")
    private List<String> stages;

    @Schema(description = "客户级别")
    private String level;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "标签")
    private String tag;

    @Schema(description = "客户来源")
    private String source;

    @Schema(description = "预计成交金额下限")
    private BigDecimal quotationMin;

    @Schema(description = "预计成交金额上限")
    private BigDecimal quotationMax;

    @Schema(description = "最后联系时间开始")
    private Date lastContactStart;

    @Schema(description = "最后联系时间结束")
    private Date lastContactEnd;

    @Schema(description = "是否包含未跟进客户")
    private Boolean includeNoLastContact;

    @Schema(description = "下次跟进时间开始")
    private Date nextFollowStart;

    @Schema(description = "下次跟进时间结束")
    private Date nextFollowEnd;

    @Schema(description = "创建时间开始")
    private Date createTimeStart;

    @Schema(description = "创建时间结束")
    private Date createTimeEnd;

    @Schema(description = "联系人数量下限")
    private Integer contactCountMin;

    @Schema(description = "联系人数量上限")
    private Integer contactCountMax;

    @Schema(description = "通用字段筛选条件")
    private List<CustomerFieldFilterBO> filters;

    @Schema(description = "排序字段")
    private String sortBy;

    @Schema(description = "排序方向")
    private String sortOrder;
}
