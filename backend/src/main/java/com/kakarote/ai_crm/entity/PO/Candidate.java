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
@TableName("crm_candidate")
@Schema(name = "Candidate", description = "候选人")
public class Candidate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "候选人ID")
    private Long candidateId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像对象Key或外部地址")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "微信")
    private String wechat;

    @Schema(description = "当前公司")
    private String currentCompany;

    @Schema(description = "当前职位")
    private String currentPosition;

    @Schema(description = "应聘岗位")
    private String appliedPosition;

    @Schema(description = "岗位要求")
    private String jobRequirements;

    @Schema(description = "关联招聘岗位ID")
    private Long recruitmentJobId;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "学历")
    private String education;

    @Schema(description = "学校")
    private String school;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "工作年限")
    private BigDecimal workYears;

    @Schema(description = "期望城市")
    private String expectedCity;

    @Schema(description = "期望薪资")
    private String expectedSalary;

    @Schema(description = "技能标签")
    private String skillTags;

    @Schema(description = "简历摘要")
    private String resumeSummary;

    @Schema(description = "AI评估")
    private String aiEvaluation;

    @Schema(description = "AI解析快照")
    private String aiParseSnapshot;

    @Schema(description = "冲突字段快照")
    private String conflictSnapshot;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "最近沟通时间")
    private Date lastContactTime;

    @Schema(description = "下一步时间")
    private Date nextStepTime;

    @Schema(description = "状态: 0-删除, 1-正常")
    private Integer status;

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
