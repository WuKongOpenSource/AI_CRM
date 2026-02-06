package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户修改BO
 *
 * @author guomenghao
 */
@Schema(description = "用户添加BO")
@Data
public class UserAddBO {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "真实姓名")
    private String realname;
}
