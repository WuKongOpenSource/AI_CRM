package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_global_search_index")
public class GlobalSearchIndex implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long searchId;

    private String entityType;
    private Long entityId;
    private String title;
    private String subtitle;
    private String summary;
    private String searchText;
    private Long customerId;
    private String customerName;
    private Long ownerUserId;
    private Long customerOwnerId;
    private Long assignedUserId;
    private Long uploadUserId;
    private Long createUserId;
    private String participantUserIds;
    private String routePath;
    private Date sortTime;
    private Date createTime;
    private Date updateTime;
}
