package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI解析任务请求")
public class TaskAiParseBO {

    @NotBlank(message = "任务描述不能为空")
    @Schema(description = "用户输入的自然语言任务描述")
    private String content;
}
