package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class WecomExternalCustomerVO {

    private Long id;

    private String externalUserId;

    private String name;

    private String avatar;

    private Integer type;

    private Integer gender;

    private String unionId;

    private String position;

    private String corpName;

    private String corpFullName;

    private String bindStatus;

    private Long customerId;

    private String customerName;

    private Date syncedAt;
}
