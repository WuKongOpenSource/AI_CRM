package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识库项目表
 */
@Data
@TableName("crm_knowledge")
@Schema(name = "Knowledge", description = "知识库项目表")
public class Knowledge implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 知识ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "知识ID")
    private Long knowledgeId;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * 类型: meeting, email, recording, document, proposal, contract
     */
    @Schema(description = "类型: meeting, email, recording, document, proposal, contract")
    private String type;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    private String filePath;

    /**
     * 文件大小(字节)
     */
    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    /**
     * MIME类型
     */
    @Schema(description = "MIME类型")
    private String mimeType;

    /**
     * 关联客户ID
     */
    @Schema(description = "关联客户ID")
    private Long customerId;

    /**
     * AI摘要
     */
    @Schema(description = "AI摘要")
    private String summary;

    /**
     * 文本内容(用于搜索)
     */
    @Schema(description = "文本内容(用于搜索)")
    private String contentText;

    /**
     * 状态: 0-处理中, 1-正常, 2-处理失败
     */
    @Schema(description = "状态: 0-处理中, 1-正常, 2-处理失败")
    private Integer status;

    /**
     * 上传人ID
     */
    @Schema(description = "上传人ID")
    private Long uploadUserId;

    /**
     * WeKnora 中的知识ID
     */
    @TableField("weknora_knowledge_id")
    @Schema(description = "WeKnora 中的知识ID")
    private String weKnoraKnowledgeId;

    /**
     * WeKnora 解析状态: pending, processing, completed, failed
     */
    @TableField("weknora_parse_status")
    @Schema(description = "WeKnora 解析状态")
    private String weKnoraParseStatus;

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
