package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 客户查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "CustomerQueryBO", description = "客户查询参数")
public class CustomerQueryBO extends PageEntity {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "单个阶段")
    private String stage;

    @Schema(description = "阶段列表")
    private List<String> stages;

    @Schema(description = "客户级别")
    private String level;

    @Schema(description = "负责人ID")
    private Long ownerId;

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

    @Schema(description = "最后联系时间开始 yyyy-MM-dd HH:mm:ss")
    private Date lastContactStart;

    @Schema(description = "最后联系时间结束 yyyy-MM-dd HH:mm:ss")
    private Date lastContactEnd;

    @Schema(description = "是否包含从未跟进客户")
    private Boolean includeNoLastContact;

    @Schema(description = "下次跟进时间开始 yyyy-MM-dd HH:mm:ss")
    private Date nextFollowStart;

    @Schema(description = "下次跟进时间结束 yyyy-MM-dd HH:mm:ss")
    private Date nextFollowEnd;

    @Schema(description = "创建时间开始 yyyy-MM-dd HH:mm:ss")
    private Date createTimeStart;

    @Schema(description = "创建时间结束 yyyy-MM-dd HH:mm:ss")
    private Date createTimeEnd;

    @Schema(description = "联系人数量下限")
    private Integer contactCountMin;

    @Schema(description = "联系人数量上限")
    private Integer contactCountMax;

    @Schema(description = "通用字段筛选条件")
    private List<CustomerFieldFilterBO> filters;

    @Schema(description = "排序字段: updateTime/createTime/quotation/lastContactTime/nextFollowTime/contactCount")
    private String sortBy;

    @Schema(description = "排序方向: asc/desc")
    private String sortOrder;

    @Schema(hidden = true)
    private List<CustomerResolvedFieldFilterBO> resolvedFieldFilters;
}
