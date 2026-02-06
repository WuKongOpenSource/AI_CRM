package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘统计视图对象
 */
@Data
@Schema(name = "DashboardStatsVO", description = "仪表盘统计视图对象")
public class DashboardStatsVO {

    @Schema(description = "客户总数")
    private Long totalCustomers;

    @Schema(description = "本月新增客户")
    private Long newCustomersThisMonth;

    @Schema(description = "活跃商机数")
    private Long activeDeals;

    @Schema(description = "待处理任务数")
    private Long pendingTasks;

    @Schema(description = "本月跟进次数")
    private Long followUpsThisMonth;

    @Schema(description = "总合同金额")
    private BigDecimal totalContractAmount;

    @Schema(description = "总收入金额")
    private BigDecimal totalRevenue;

    @Schema(description = "按阶段统计")
    private List<StageCountVO> customersByStage;

    @Schema(description = "按等级统计")
    private List<LevelCountVO> customersByLevel;

    /**
     * 阶段统计
     */
    @Data
    public static class StageCountVO {
        @Schema(description = "阶段")
        private String stage;

        @Schema(description = "阶段名称")
        private String stageName;

        @Schema(description = "数量")
        private Long count;

        @Schema(description = "金额")
        private BigDecimal amount;
    }

    /**
     * 等级统计
     */
    @Data
    public static class LevelCountVO {
        @Schema(description = "等级")
        private String level;

        @Schema(description = "数量")
        private Long count;
    }
}
