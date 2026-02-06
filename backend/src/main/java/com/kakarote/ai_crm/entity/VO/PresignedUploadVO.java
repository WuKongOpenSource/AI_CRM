package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 预签名上传信息
 */
@Data
@Schema(name = "PresignedUploadVO", description = "预签名上传信息")
public class PresignedUploadVO implements Serializable {

    @Schema(description = "预签名上传URL")
    private String uploadUrl;

    @Schema(description = "HTTP方法（PUT）")
    private String method;

    @Schema(description = "存储桶名称")
    private String bucket;

    @Schema(description = "对象Key（文件路径）")
    private String objectKey;

    @Schema(description = "签名有效期（秒）")
    private Integer expiry;

    @Schema(description = "文件访问URL（上传完成后可用）")
    private String accessUrl;
}
