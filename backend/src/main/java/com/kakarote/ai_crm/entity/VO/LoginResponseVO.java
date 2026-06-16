package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录响应")
public class LoginResponseVO {

    @Schema(description = "登录令牌")
    private String token;

    @Schema(description = "当前登录用户")
    private ManageUserVO userInfo;
}
