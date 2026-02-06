package com.kakarote.ai_crm.entity.VO;

import com.kakarote.ai_crm.entity.PO.CustomerTag;
import com.kakarote.ai_crm.entity.PO.Task;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 客户详情视图对象
 */
@Data
@Schema(name = "CustomerDetailVO", description = "客户详情视图对象")
public class CustomerDetailVO {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "行业")
    private String industry;

    @Schema(description = "阶段")
    private String stage;

    @Schema(description = "阶段名称")
    private String stageName;

    @Schema(description = "客户等级")
    private String level;

    @Schema(description = "客户来源")
    private String source;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "网站")
    private String website;

    @Schema(description = "报价金额")
    private BigDecimal quotation;

    @Schema(description = "合同金额")
    private BigDecimal contractAmount;

    @Schema(description = "收入金额")
    private BigDecimal revenue;

    @Schema(description = "最后联系时间")
    private Date lastContactTime;

    @Schema(description = "下次跟进时间")
    private Date nextFollowTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "负责人ID")
    private Long ownerId;

    @Schema(description = "负责人姓名")
    private String ownerName;

    @Schema(description = "负责人头像")
    private String ownerAvatar;

    @Schema(description = "标签列表")
    private List<CustomerTag> tags;

    @Schema(description = "相关任务")
    private List<Task> tasks;

    @Schema(description = "相关文档")
    private List<KnowledgeVO> documents;

    @Schema(description = "团队成员列表")
    private List<TeamMemberVO> teamMembers;

    @Schema(description = "联系人列表")
    private List<ContactVO> contacts;

    @Schema(description = "最近跟进记录")
    private List<FollowUpVO> recentFollowUps;

    @Schema(description = "自定义字段值")
    private Map<String, Object> customFields;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date updateTime;

    /**
     * 团队成员视图
     */
    @Data
    public static class TeamMemberVO {
        @Schema(description = "用户ID")
        private Long userId;

        @Schema(description = "姓名")
        private String name;

        @Schema(description = "头像")
        private String avatar;

        @Schema(description = "角色")
        private String role;
    }
}
