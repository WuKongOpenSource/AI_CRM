package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI follow-up parse request")
public class FollowUpAiParseBO {

    @NotBlank(message = "Follow-up content cannot be blank")
    @Schema(description = "User input content")
    private String content;

    @Schema(description = "Customer name")
    private String customerName;

    @Schema(description = "Customer ID")
    private Long customerId;

    @Schema(description = "Uploaded attachments")
    private List<ChatSendBO.AttachmentDTO> attachments;
}
