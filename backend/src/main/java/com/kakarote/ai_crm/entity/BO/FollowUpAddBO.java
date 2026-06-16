package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 跟进记录新增参数
 */
@Data
@Schema(name = "FollowUpAddBO", description = "跟进记录新增参数")
public class FollowUpAddBO {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "关系人ID")
    private Long relationId;

    @Schema(description = "联系人ID")
    private Long contactId;

    @NotBlank(message = "类型不能为空")
    @Schema(description = "类型: call, meeting, email, visit", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotBlank(message = "跟进内容不能为空")
    @Schema(description = "跟进内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "AI summary")
    private String summary;

    @Schema(description = "Scene type")
    private String sceneType;

    @Schema(description = "AI generated flag")
    private Integer aiGenerated;

    @NotNull(message = "跟进时间不能为空")
    @Schema(description = "跟进时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date followTime;

    @Schema(description = "下次跟进时间")
    private Date nextFollowTime;

    @Schema(description = "Attachments")
    private List<ChatSendBO.AttachmentDTO> attachments;

    @Schema(description = "Suggested tasks")
    private List<FollowUpSuggestedTaskBO> suggestedTasks;
}
