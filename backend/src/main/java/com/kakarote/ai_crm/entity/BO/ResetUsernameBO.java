package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "重置用户名BO")
@Data
public class ResetUsernameBO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "新用户名")
    private String username;

    @Schema(description = "当前登录密码")
    private String currentPassword;
}
