package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class ImConversationVO {
    private String id;
    private String peerUserId;
    private String peerName;
    private String peerAvatarUrl;
    private ImMessageVO lastMessage;
    private long unreadCount;
    private Date updateTime;
    private String type;          // direct | channel
    private String name;          // channel name (null for direct)
    private String visibility;    // public | private (channels)
    private Integer memberCount;  // channels
}
