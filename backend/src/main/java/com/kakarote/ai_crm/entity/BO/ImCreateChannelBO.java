package com.kakarote.ai_crm.entity.BO;

import lombok.Data;
import java.util.List;

@Data
public class ImCreateChannelBO {
    private String name;
    private String description;
    private String visibility;       // public | private; defaults to public if blank
    private List<Long> memberIds;    // optional initial members (besides creator)
}
