package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_wecom_conversation")
public class WecomConversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private String corpId;

    private String conversationType;

    private Long employeeId;

    private String employeeUserId;

    private Long externalCustomerId;

    private String externalUserId;

    private String archiveExternalUserId;

    private String contactEmployeeUserId;

    private Long groupChatId;

    private String chatId;

    private String title;

    private String peerName;

    private String peerAvatar;

    private Long customerId;

    private Long ownerUserId;

    private String lastMsgId;

    private Date lastMsgTime;

    private String lastMsgPreview;

    private Integer messageCount;

    private String matchStatus;

    private String matchError;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
