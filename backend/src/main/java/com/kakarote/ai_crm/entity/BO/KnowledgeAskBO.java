package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "知识库文档提问请求")
public class KnowledgeAskBO {

    @NotBlank(message = "问题不能为空")
    @Schema(description = "用户问题")
    private String question;

    @Schema(description = "对话历史")
    private List<ChatHistoryItem> history;

    @Data
    @Schema(description = "对话历史条目")
    public static class ChatHistoryItem {

        @Schema(description = "角色: user 或 assistant")
        private String role;

        @Schema(description = "内容")
        private String content;
    }
}
