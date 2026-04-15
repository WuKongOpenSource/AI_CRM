package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TokenPurchaseOptionVO implements Serializable {

    private boolean enabled;

    private int orderExpireMinutes;

    private Long giftTokenRemaining;

    private Long purchasedTokenRemaining;

    private Long tokenRemaining;

    private List<PlanVO> plans;

    private List<ChannelVO> channels;

    @Data
    public static class PlanVO implements Serializable {
        private String id;
        private String name;
        private String description;
        private Long tokenAmount;
        private Integer priceFen;
    }

    @Data
    public static class ChannelVO implements Serializable {
        private String code;
        private String label;
        private boolean enabled;
        private String unavailableReason;
    }
}
