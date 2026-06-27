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
import java.util.Date;

/**
 * 已安装/自定义 AI 应用（内置应用仍由 ChatApplicationRegistry 构造函数提供,不入库）。
 * tool_groups / recommended_questions 以 JSON 数组字符串存储。
 */
@Data
@TableName("crm_ai_application")
@Schema(name = "CrmAiApplication", description = "Installed/custom AI chat application")
public class CrmAiApplication implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long appId;

    private String pluginId;

    private String code;

    private String label;

    private String iconName;

    private String description;

    private String systemPrompt;

    private Integer defaultRagEnabled;

    /** JSON 数组字符串 */
    private String toolGroups;

    /** JSON 数组字符串 */
    private String recommendedQuestions;

    /** 0=停用 1=启用 */
    private Integer status;

    /** 预留：NULL=单实例；未来多租户用 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
