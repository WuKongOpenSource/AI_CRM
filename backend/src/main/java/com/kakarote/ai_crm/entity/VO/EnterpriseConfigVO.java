package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 企业信息配置
 */
@Data
@Schema(name = "EnterpriseConfigVO", description = "企业信息配置")
public class EnterpriseConfigVO implements Serializable {

    @Schema(description = "企业名称")
    private String name;

    @Schema(description = "企业Logo（MinIO objectKey）")
    private String logo;

    @Schema(description = "企业Logo访问URL")
    private String logoUrl;

    @Schema(description = "企业说明")
    private String description;

    @Schema(description = "最后更新时间")
    private Date updateTime;
}
