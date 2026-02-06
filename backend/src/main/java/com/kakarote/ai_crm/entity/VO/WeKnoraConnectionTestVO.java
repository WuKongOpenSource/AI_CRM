package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * WeKnora 连接测试结果
 */
@Data
@Schema(name = "WeKnoraConnectionTestVO", description = "WeKnora连接测试结果")
public class WeKnoraConnectionTestVO implements Serializable {

    @Schema(description = "是否连接成功")
    private Boolean success;

    @Schema(description = "响应时间(毫秒)")
    private Long responseTime;

    @Schema(description = "测试结果消息")
    private String message;

    @Schema(description = "知识库中的文档数量")
    private Integer knowledgeCount;
}
