package com.kakarote.ai_crm.entity.BO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class MailImapBindBO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String emailAddress;

    private String displayName;

    @NotBlank(message = "IMAP 主机不能为空")
    private String imapHost;

    private Integer imapPort = 993;

    private Boolean imapSsl = true;

    private String smtpHost;

    private Integer smtpPort = 465;

    private Boolean smtpSsl = true;

    private String username;

    @NotBlank(message = "授权码或密码不能为空")
    private String password;

    private List<String> folders;

    private Integer syncDays;

    private Integer syncLimit;

    private String bodySyncMode;

    private String attachmentSyncMode;

    private Long maxAutoAttachmentSize;

    private Integer retentionDays;

    private Boolean extractActions = true;

    private Boolean testConnection = true;
}
