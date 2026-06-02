package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WecomConversationQueryBO extends PageEntity {

    private String conversationType;

    private Long employeeId;

    private String employeeUserId;

    private Long externalCustomerId;

    private Long groupChatId;

    private Long customerId;

    private String keyword;
}
