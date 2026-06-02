package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class WecomCustomerUnbindBO {

    private Long bindingId;

    private Long customerId;

    private Long externalCustomerId;
}
