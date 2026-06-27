package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_im_message")
public class ImMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String contentType;
    private String content;
    private String attachmentName;
    private String attachmentPath;
    private Long attachmentSize;
    private String attachmentMime;
    private String status;
    private Date createTime;
    private Date updateTime;
}
