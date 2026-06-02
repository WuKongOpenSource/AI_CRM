package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class WecomCustomerBindingVO {

    private Long id;

    private Long customerId;

    private String customerName;

    private Long externalCustomerId;

    private String externalUserId;

    private String externalCustomerName;

    private String externalCustomerAvatar;

    private String corpId;

    private Long bindUserId;

    private Date bindTime;

    private Date unbindTime;

    private Integer status;

    private String remark;
}
