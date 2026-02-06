package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 知识库视图对象
 */
@Data
@Schema(name = "KnowledgeVO", description = "知识库视图对象")
public class KnowledgeVO {

    @Schema(description = "知识ID")
    private Long knowledgeId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件大小(格式化)")
    private String fileSizeFormatted;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "关联客户ID")
    private Long customerId;

    @Schema(description = "关联客户名称")
    private String customerName;

    @Schema(description = "AI摘要")
    private String summary;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "上传人ID")
    private Long uploadUserId;

    @Schema(description = "上传人姓名")
    private String uploadUserName;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "创建时间")
    private Date createTime;
}
