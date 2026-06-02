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
@TableName("crm_tencent_meeting")
public class TencentMeeting implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private String appId;

    private String meetingId;

    private String meetingCode;

    private String subject;

    private String status;

    private String creatorUserId;

    private String creatorName;

    private Long crmCreatorUserId;

    private String participantNames;

    private Integer participantCount;

    private Date startTime;

    private Date endTime;

    private Long durationSeconds;

    private String bindStatus;

    private Long customerId;

    private String customerName;

    private String summary;

    private String todoText;

    private String transcriptText;

    private String rawJson;

    private Long knowledgeId;

    private Date syncedAt;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
