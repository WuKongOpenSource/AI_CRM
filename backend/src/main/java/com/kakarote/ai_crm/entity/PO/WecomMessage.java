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
@TableName("crm_wecom_message")
public class WecomMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long conversationId;

    private String corpId;

    private String msgId;

    private Long seq;

    private String action;

    private String msgType;

    private String senderId;

    private String senderType;

    private String receiverList;

    private Date msgTime;

    private String contentText;

    private String contentJson;

    private Long mediaId;

    private String sdkFileId;

    private String fileName;

    private Long fileSize;

    private String fileUrl;

    private Boolean recalled;

    private String rawJson;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
