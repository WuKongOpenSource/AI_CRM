package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class ImContactVO {
    private String userId;
    private String name;
    private String avatarUrl;
    private String deptName;
    private boolean online;
}
