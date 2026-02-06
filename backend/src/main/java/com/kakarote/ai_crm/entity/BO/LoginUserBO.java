package com.kakarote.ai_crm.entity.BO;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


/**
 * 登录BO
 * @author zhangzhiwei
 */
@Data
public class LoginUserBO {

    /**
     * 用户名
     */
    @NotEmpty
    private String username;

    /**
     * 用户密码
     */
    @NotEmpty
    private String password;
}
