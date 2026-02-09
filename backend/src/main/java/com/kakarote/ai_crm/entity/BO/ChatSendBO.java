package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 聊天发送参数
 */
@Data
@Schema(name = "ChatSendBO", description = "聊天发送参数")
public class ChatSendBO {

    @NotNull(message = "会话ID不能为空")
    @Schema(description = "会话ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "附件列表")
    private List<AttachmentDTO> attachments;

    @Data
    @Schema(description = "附件信息")
    public static class AttachmentDTO {

        @Schema(description = "文件名")
        private String fileName;

        @Schema(description = "文件路径（MinIO objectKey）")
        private String filePath;

        @Schema(description = "文件大小（字节）")
        private Long fileSize;

        @Schema(description = "MIME类型")
        private String mimeType;
    }
}
