package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @创建人 xuepengfei
 * @创建时间 2022/4/22
 * @描述
 */

@Data
@Schema(description = "系统日志")
public class ManageLogVo {

    @Schema(description = "请求路径")
    private String requestUri;

    @Schema(description = "请求参数")
    private String parameterList;

    @Schema(description = "请求参数名称")
    private String parameterName;

    @Schema(description = "请求IP")
    private String clientIp;

    @Schema(description = "返回结果")
    private String resultData;

    @Schema(description = "返回结果状态")
    private Integer resultCode;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "企业ID")
    private Long companyId;

    @Schema(description = "企业名称")
    private String companyName;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "错误数据")
    private String errorData;

    @Schema(description = "错误名称")
    private String exceptionName;

    @Schema(description = "时长")
    private String executionTime;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "模块名称")
    private String model;
}
