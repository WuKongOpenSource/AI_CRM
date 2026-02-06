package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户状态BO
 *
 * @author guomenghao
 */
@Schema(description = "用户状态BO")
@Data
public class UserStatusBO {

    @Schema(description = "ids")
    private List<Long> ids;

    @Schema(description = "状态 0 禁用,1 正常")
    private Integer status;
}
