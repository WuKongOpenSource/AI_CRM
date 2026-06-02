package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WecomCustomerQueryBO extends PageEntity {

    private String keyword;

    private String bindStatus;

    private Long customerId;
}
