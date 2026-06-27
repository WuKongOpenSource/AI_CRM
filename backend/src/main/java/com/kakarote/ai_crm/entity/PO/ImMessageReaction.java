package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_im_message_reaction")
public class ImMessageReaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long messageId;
    private Long conversationId;
    private Long userId;
    private String emoji;
    private Date createTime;
}
