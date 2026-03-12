package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI解析跟进内容响应")
public class FollowUpAiParseVO {

    @Schema(description = "核心摘要")
    private String summary;

    @Schema(description = "跟进类型: call/meeting/email/visit/other")
    private String type;

    @Schema(description = "跟进时间 yyyy-MM-dd HH:mm")
    private String followTime;

    @Schema(description = "建议下次跟进时间 yyyy-MM-dd HH:mm")
    private String nextFollowTime;

    @Schema(description = "关键要点")
    private List<String> keyPoints;

    @Schema(description = "自动生成的待办")
    private List<String> todos;
}
