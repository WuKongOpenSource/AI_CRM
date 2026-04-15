package com.kakarote.ai_crm.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "token-purchase")
public class TokenPurchaseProperties {

    private boolean enabled = true;

    private int orderExpireMinutes = 30;

    private List<Plan> plans = new ArrayList<>();

    private Wechat wechat = new Wechat();

    private Alipay alipay = new Alipay();

    public List<Plan> getResolvedPlans() {
        List<Plan> resolved = new ArrayList<>();
        if (plans != null) {
            for (Plan plan : plans) {
                if (plan == null || !plan.isEnabled() || StrUtil.hasBlank(plan.getId(), plan.getName())
                        || plan.getTokenAmount() == null || plan.getTokenAmount() <= 0
                        || plan.getPriceFen() == null || plan.getPriceFen() <= 0) {
                    continue;
                }
                resolved.add(plan);
            }
        }

        if (resolved.isEmpty()) {
            Plan defaultPlan = new Plan();
            defaultPlan.setId("starter-100w");
            defaultPlan.setName("快速选购方案");
            defaultPlan.setDescription("1,000,000 Token");
            defaultPlan.setTokenAmount(1_000_000L);
            defaultPlan.setPriceFen(6000);
            defaultPlan.setSort(1);
            resolved.add(defaultPlan);
        }

        resolved.sort(Comparator.comparingInt(Plan::getSort));
        return resolved;
    }

    @Data
    public static class Plan {
        private String id;
        private String name;
        private String description;
        private Long tokenAmount;
        private Integer priceFen;
        private int sort;
        private boolean enabled = true;
    }

    @Data
    public static class Wechat {
        private boolean enabled;
        private String gateway = "https://api.mch.weixin.qq.com";
        private String appId;
        private String merchantId;
        private String merchantSerialNo;
        private String privateKey;
        private String platformCertificate;
        private String apiV3Key;
        private String notifyUrl;

        public boolean isReady() {
            return enabled
                    && StrUtil.isNotBlank(appId)
                    && StrUtil.isNotBlank(merchantId)
                    && StrUtil.isNotBlank(merchantSerialNo)
                    && StrUtil.isNotBlank(privateKey)
                    && StrUtil.isNotBlank(platformCertificate)
                    && StrUtil.isNotBlank(apiV3Key);
        }
    }

    @Data
    public static class Alipay {
        private boolean enabled;
        private String gateway = "https://openapi.alipay.com/gateway.do";
        private String appId;
        private String privateKey;
        private String alipayPublicKey;
        private String notifyUrl;

        public boolean isReady() {
            return enabled
                    && StrUtil.isNotBlank(appId)
                    && StrUtil.isNotBlank(privateKey)
                    && StrUtil.isNotBlank(alipayPublicKey);
        }
    }
}
