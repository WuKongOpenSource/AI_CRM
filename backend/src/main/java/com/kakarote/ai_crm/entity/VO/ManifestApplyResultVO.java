package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 应用 manifest 的结果。
 */
@Data
@Schema(name = "ManifestApplyResultVO", description = "Result of applying a plugin manifest")
public class ManifestApplyResultVO {

    private String pluginId;

    /** 本次新建的自定义字段ID（卸载时按此反向删除） */
    private List<Long> appliedFieldIds;

    /** 本次新建的 AI 应用ID（卸载时按此反向删除） */
    private List<Long> appliedAiApplicationIds;

    private int appliedCount;
}
