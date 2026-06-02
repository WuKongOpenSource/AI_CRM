package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class WecomCustomerBindBO {

    private Long customerId;

    private Long externalCustomerId;

    private String remark;
}
