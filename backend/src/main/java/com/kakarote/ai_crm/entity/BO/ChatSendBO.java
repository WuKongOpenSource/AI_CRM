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

    @Schema(description = "鏄惁鍚敤 RAG 妫€绱㈣矾鐢憋紝榛樿 false")
    private Boolean ragEnabled;

    @Schema(description = "聊天应用编码: general/crm/knowledge/product")
    private String appCode;

    @Schema(description = "产品对话绑定的产品ID")
    private Long productId;

    @Schema(description = "候选人对话绑定的候选人ID")
    private Long candidateId;

    @Schema(description = "项目对话绑定的项目ID")
    private Long projectId;

    @Schema(description = "任务对话绑定的项目任务ID")
    private Long projectTaskId;

    @Schema(description = "本次对话选择的 AI 服务商编码")
    private String modelProvider;

    @Schema(description = "本次对话选择的模型名称")
    private String modelName;

    @Schema(description = "Model source: custom/system")
    private String modelSource;
    @Schema(description = "知识库文件 ID 列表（限定 RAG 范围等），无需上传文件本体")
    private List<Long> knowledgeIds;

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
