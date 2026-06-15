package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "全局搜索结果")
public class GlobalSearchResultVO {

    @Schema(description = "结果类型")
    private String type;

    @Schema(description = "Entity type")
    private String entityType;

    @Schema(description = "业务记录 ID")
    private Long recordId;

    @Schema(description = "Entity id")
    private Long entityId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "摘要内容")
    private String content;

    @Schema(description = "Summary")
    private String summary;

    @Schema(description = "关联客户 ID")
    private Long customerId;

    @Schema(description = "Customer name")
    private String customerName;

    @Schema(description = "关联联系人 ID")
    private Long contactId;

    @Schema(description = "排序时间")
    private LocalDateTime eventTime;

    @Schema(description = "Route path")
    private String routePath;

    @Schema(description = "Sort time")
    private LocalDateTime sortTime;

    @Schema(description = "Search relevance score")
    private Double score;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
