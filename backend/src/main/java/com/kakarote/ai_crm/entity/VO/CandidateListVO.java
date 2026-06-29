package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Data
@Schema(name = "CandidateListVO", description = "候选人列表视图")
public class CandidateListVO {

    @Schema(description = "候选人ID")
    private Long candidateId;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "头像对象Key或外部地址")
    private String avatar;

    @Schema(description = "头像访问地址")
    private String avatarUrl;

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

    @Schema(description = "关联招聘岗位名称")
    private String recruitmentJobName;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "阶段名称")
    private String stageName;

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

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "最近沟通时间")
    private Date lastContactTime;

    @Schema(description = "下一步时间")
    private Date nextStepTime;

    @Schema(description = "简历附件数量")
    private Integer resumeCount;

    @Schema(description = "面试日程数量")
    private Integer interviewCount;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "自定义字段")
    private Map<String, Object> customFields;
}
