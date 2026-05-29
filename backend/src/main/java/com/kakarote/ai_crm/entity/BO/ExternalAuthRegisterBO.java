package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExternalAuthRegisterBO {

    @NotBlank
    private String ticket;

    private String companyName;

    private String realname;

    private String email;

    private String verificationCode;

    private LoginTypeEnum loginType;
}
