package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MailAccountVO {

    private Long accountId;

    private String provider;

    private String authType;

    private String emailAddress;

    private String displayName;

    private String imapHost;

    private Integer imapPort;

    private Boolean enabled;

    private Boolean isDefault;

    private String connectionStatus;

    private Date lastUsedTime;

    private String smtpHost;

    private Integer smtpPort;

    private List<String> folders;

    private Integer syncDays;

    private Integer syncLimit;

    private String bodySyncMode;

    private String attachmentSyncMode;

    private Long maxAutoAttachmentSize;

    private Integer retentionDays;

    private Boolean extractActions;

    private Date lastSyncTime;

    private String lastSyncStatus;

    private String lastSyncError;

    private Date createTime;
}
