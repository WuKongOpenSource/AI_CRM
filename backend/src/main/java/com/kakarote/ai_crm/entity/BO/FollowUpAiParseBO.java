package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI解析跟进内容请求")
public class FollowUpAiParseBO {

    @NotBlank(message = "跟进内容不能为空")
    @Schema(description = "用户输入的跟进文本内容")
    private String content;

    @Schema(description = "客户名称（提供上下文）")
    private String customerName;

    @Schema(description = "客户ID")
    private Long customerId;
}
