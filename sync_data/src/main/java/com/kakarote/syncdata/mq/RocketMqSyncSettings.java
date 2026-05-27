package com.kakarote.syncdata.mq;

import com.kakarote.syncdata.SyncProperties;

public final class RocketMqSyncSettings {

    public static final String DEFAULT_TOPIC = "crm-aicrm-sync-events";
    public static final String CRM_TO_AICRM_TAG = "crm-to-aicrm";
    public static final String AICRM_TO_CRM_TAG = "aicrm-to-crm";

    private RocketMqSyncSettings() {
    }

    public static String topic(SyncProperties properties) {
        if (properties == null || properties.getRocketmq() == null) {
            return DEFAULT_TOPIC;
        }
        return firstNonBlank(
                properties.getRocketmq().getTopic(),
                DEFAULT_TOPIC
        );
    }

    public static String crmToAicrmTag(SyncProperties properties) {
        if (properties == null || properties.getRocketmq() == null
                || properties.getRocketmq().getCrmToAicrm() == null) {
            return CRM_TO_AICRM_TAG;
        }
        return firstNonBlank(
                properties.getRocketmq().getCrmToAicrm().getTag(),
                CRM_TO_AICRM_TAG
        );
    }

    public static String aicrmToCrmTag(SyncProperties properties) {
        if (properties == null || properties.getRocketmq() == null
                || properties.getRocketmq().getAicrmToCrm() == null) {
            return AICRM_TO_CRM_TAG;
        }
        return firstNonBlank(
                properties.getRocketmq().getAicrmToCrm().getTag(),
                AICRM_TO_CRM_TAG
        );
    }

    public static String crmToAicrmGroup(SyncProperties properties) {
        if (properties == null || properties.getRocketmq() == null
                || properties.getRocketmq().getCrmToAicrm() == null) {
            return "sync-data-crm-to-aicrm";
        }
        return firstNonBlank(
                properties.getRocketmq().getCrmToAicrm().getConsumerGroup(),
                properties.getIncremental() == null || properties.getIncremental().getMq() == null
                        ? null : properties.getIncremental().getMq().getConsumerGroup(),
                "sync-data-crm-to-aicrm"
        );
    }

    public static String aicrmToCrmGroup(SyncProperties properties) {
        if (properties == null || properties.getRocketmq() == null
                || properties.getRocketmq().getAicrmToCrm() == null) {
            return "sync-data-aicrm-to-crm";
        }
        return firstNonBlank(
                properties.getRocketmq().getAicrmToCrm().getProducerGroup(),
                "sync-data-aicrm-to-crm"
        );
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
