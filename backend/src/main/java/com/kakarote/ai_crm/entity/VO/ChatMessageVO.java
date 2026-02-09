package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 聊天消息视图对象
 */
@Data
@Schema(name = "ChatMessageVO", description = "聊天消息视图对象")
public class ChatMessageVO {

    @Schema(description = "消息ID")
    private Long messageId;

    @Schema(description = "会话ID")
    private Long sessionId;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "使用token数")
    private Integer tokensUsed;

    @Schema(description = "使用的模型")
    private String modelName;

    @Schema(description = "附件列表")
    private List<AttachmentVO> attachments;

    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 附件视图
     */
    @Data
    public static class AttachmentVO {
        @Schema(description = "附件ID")
        private Long id;

        @Schema(description = "文件名")
        private String fileName;

        @Schema(description = "文件路径")
        private String filePath;

        @Schema(description = "文件大小")
        private Long fileSize;

        @Schema(description = "MIME类型")
        private String mimeType;

        @Schema(description = "访问URL")
        private String accessUrl;
    }
}
