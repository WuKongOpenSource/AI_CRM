package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Backend system exception log.
 */
@Data
@TableName("crm_error_log")
@Schema(name = "ErrorLog", description = "Backend system exception log")
public class ErrorLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Error ID")
    private Long errorId;

    @Schema(description = "Related access log ID")
    private Long accessLogId;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Trace ID")
    private String traceId;

    @Schema(description = "Exception class name")
    private String exceptionName;

    @Schema(description = "Error message")
    private String errorMessage;

    @Schema(description = "Stack trace")
    private String stackTrace;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "Create time")
    private Date createTime;
}
