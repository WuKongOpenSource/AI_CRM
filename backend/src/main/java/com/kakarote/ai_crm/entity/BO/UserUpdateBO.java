package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户更新BO
 *
 * @author guomenghao
 */
@Schema(description = "用户更新BO")
@Data
public class UserUpdateBO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "真实姓名")
    private String realname;

    @Schema(description = "密码")
    private String password;
}
