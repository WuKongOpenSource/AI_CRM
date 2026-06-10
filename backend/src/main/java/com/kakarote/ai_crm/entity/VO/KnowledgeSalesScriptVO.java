package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(name = "KnowledgeSalesScriptVO", description = "知识库销售话术生成结果")
public class KnowledgeSalesScriptVO implements Serializable {

    @Schema(description = "生成后的销售话术正文")
    private String script;

    @Schema(description = "话术参考的知识来源")
    private List<String> sources;
}
