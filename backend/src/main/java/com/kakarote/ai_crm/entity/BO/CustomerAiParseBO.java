package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "AI智能录入客户请求")
public class CustomerAiParseBO {

    @NotBlank(message = "输入内容不能为空")
    @Schema(description = "用户输入的文本内容（名片文字、客户描述等）")
    private String content;

    @Schema(description = "图片在MinIO中的objectKey（通过presigned上传后获得）")
    private String imageObjectKey;

    @Schema(description = "图片MIME类型")
    private String imageMimeType;
}
