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
@TableName("crm_wecom_sync_log")
public class WecomSyncLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private String corpId;

    private String syncType;

    private String status;

    private Integer fetchedCount;

    private Integer savedCount;

    private Integer failedCount;

    private Date startedAt;

    private Date finishedAt;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
