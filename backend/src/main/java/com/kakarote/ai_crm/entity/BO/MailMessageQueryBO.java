package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class MailMessageQueryBO extends PageEntity {

    private Long accountId;

    private Long customerId;

    private String keyword;

    private String folder;

    private String direction;

    private String readStatus;

    private Boolean starred;

    private Date startTime;

    private Date endTime;
}
