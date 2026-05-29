package com.kakarote.ai_crm.entity.BO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MailAssociateCustomerBO {

    @NotNull(message = "邮件ID不能为空")
    private Long messageId;

    private Long customerId;
}
