package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 企业信息配置更新参数
 */
@Data
@Schema(name = "EnterpriseConfigUpdateBO", description = "企业信息配置更新参数")
public class EnterpriseConfigUpdateBO implements Serializable {

    @Schema(description = "企业名称", example = "我的企业")
    private String name;

    @Schema(description = "企业Logo（MinIO objectKey）")
    private String logo;

    @Schema(description = "企业说明")
    private String description;
}
