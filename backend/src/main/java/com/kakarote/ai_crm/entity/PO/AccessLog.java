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
 * Backend HTTP access log.
 */
@Data
@TableName("crm_access_log")
@Schema(name = "AccessLog", description = "Backend HTTP access log")
public class AccessLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "Log ID")
    private Long logId;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "HTTP method")
    private String method;

    @Schema(description = "Request URI")
    private String requestUri;

    @Schema(description = "Query string")
    private String queryString;

    @Schema(description = "Sanitized request headers")
    private String requestHeaders;

    @Schema(description = "Sanitized request body")
    private String requestBody;

    @Schema(description = "Sanitized Result response summary")
    private String responseBody;

    @Schema(description = "HTTP status code")
    private Integer statusCode;

    @Schema(description = "Business code from Result")
    private Integer businessCode;

    @Schema(description = "Whether the request succeeded")
    private Boolean success;

    @Schema(description = "IP address")
    private String ipAddress;

    @Schema(description = "User Agent")
    private String userAgent;

    @Schema(description = "Trace ID")
    private String traceId;

    @Schema(description = "Request cost in milliseconds")
    private Long costMs;

    @Schema(description = "Whether request body was truncated")
    private Boolean requestTruncated;

    @Schema(description = "Whether response body was truncated")
    private Boolean responseTruncated;

    @Schema(description = "Whether response body is unified Result JSON")
    private Boolean resultResponse;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "Create time")
    private Date createTime;
}
