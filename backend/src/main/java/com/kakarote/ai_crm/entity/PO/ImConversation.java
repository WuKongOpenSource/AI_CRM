package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_im_conversation")
public class ImConversation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String type;
    private String memberKey;
    private Long lastMessageId;
    private Date createTime;
    private Date updateTime;
    private String name;
    private String description;
    private String visibility;
    private Long ownerId;
}
