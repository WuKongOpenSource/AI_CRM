package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.List;

@Data
public class MailAuthStatusVO {

    private Boolean authorized;

    private MailAccountVO currentAccount;

    private List<MailAccountVO> accounts;
}
