package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class TencentMeetingQueryBO extends PageEntity {

    private String keyword;

    private String status;

    private String bindStatus;

    private Long customerId;

    private Date startTimeFrom;

    private Date startTimeTo;
}
