package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 预签名上传请求参数
 */
@Data
@Schema(name = "PresignedUploadBO", description = "预签名上传请求参数")
public class PresignedUploadBO implements Serializable {

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "文件名（含扩展名）", example = "document.pdf")
    private String fileName;

    @Schema(description = "文件MIME类型", example = "application/pdf")
    private String contentType;

    @Schema(description = "签名有效期（秒），默认3600", example = "3600")
    private Integer expiry;

    @Schema(description = "自定义存储目录前缀", example = "uploads/documents")
    private String prefix;
}
