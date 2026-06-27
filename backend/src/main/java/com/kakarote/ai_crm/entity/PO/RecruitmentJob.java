package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("crm_recruitment_job")
@Schema(name = "RecruitmentJob", description = "招聘岗位")
public class RecruitmentJob implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "招聘岗位ID")
    private Long recruitmentJobId;

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

    @Schema(description = "状态: open-招聘中, paused-暂停, closed-关闭")
    private String status;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "删除标记: 0-正常, 1-删除")
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人ID")
    private Long updateUserId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private Date updateTime;
}
