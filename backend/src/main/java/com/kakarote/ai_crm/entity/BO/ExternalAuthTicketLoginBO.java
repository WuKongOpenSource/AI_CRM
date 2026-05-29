package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExternalAuthTicketLoginBO {

    @NotBlank
    private String ticket;

    private LoginTypeEnum loginType;
}
