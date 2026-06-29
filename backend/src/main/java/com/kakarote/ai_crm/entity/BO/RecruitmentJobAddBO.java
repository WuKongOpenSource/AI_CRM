package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "RecruitmentJobAddBO", description = "新增招聘岗位")
public class RecruitmentJobAddBO {

    @NotBlank(message = "岗位名称不能为空")
    @Schema(description = "岗位名称")
    private String jobName;

    @Schema(description = "部门")
    private String department;

    @Schema(description = "招聘人数")
    private Integer headcount;

    @Schema(description = "工作年限")
    private BigDecimal workYears;

    @Schema(description = "学历要求")
    private String education;

    @Schema(description = "工作城市")
    private String city;

    @Schema(description = "薪资范围")
    private String salaryRange;

    @Schema(description = "技能标签")
    private String skillTags;

    @Schema(description = "岗位职责")
    private String responsibilities;

    @Schema(description = "任职要求")
    private String requirements;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "备注")
    private String remark;
}
