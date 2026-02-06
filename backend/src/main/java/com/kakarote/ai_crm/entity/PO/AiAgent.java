package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * AI智能体表
 */
@Data
@TableName("crm_ai_agent")
@Schema(name = "AiAgent", description = "AI智能体表")
public class AiAgent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 智能体ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "智能体ID")
    private Long agentId;

    /**
     * 显示名称
     */
    @Schema(description = "显示名称")
    private String label;

    /**
     * 图标名称
     */
    @Schema(description = "图标名称")
    private String iconName;

    /**
     * 系统提示词
     */
    @Schema(description = "系统提示词")
    private String prompt;

    /**
     * 角色人设
     */
    @Schema(description = "角色人设")
    private String persona;

    /**
     * 知识库类型(JSON数组)
     */
    @Schema(description = "知识库类型(JSON数组)")
    private String knowledgeBaseTypes;

    /**
     * 是否启用: 0-否, 1-是
     */
    @Schema(description = "是否启用: 0-否, 1-是")
    private Integer enabled;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sortOrder;

    /**
     * 分类: default, custom
     */
    @Schema(description = "分类: default, custom")
    private String category;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人ID")
    private Long createUserId;

    /**
     * 修改人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改人ID")
    private Long updateUserId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "修改时间")
    private Date updateTime;
}
