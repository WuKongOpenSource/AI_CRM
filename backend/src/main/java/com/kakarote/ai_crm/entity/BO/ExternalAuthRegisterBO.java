package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExternalAuthRegisterBO {

    @NotBlank
    private String ticket;

    private String companyName;

    @Size(min = 6, max = 20, message = "Password length must be 6-20")
    private String password;

    private LoginTypeEnum loginType;
}
