package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TencentMeetingCustomerBindingVO {

    private Long id;

    private Long meetingId;

    private String meetingExternalId;

    private Long customerId;

    private String customerName;

    private Date bindTime;

    private Integer status;
}
