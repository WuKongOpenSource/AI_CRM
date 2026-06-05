package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.EnterpriseConfigUpdateBO;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import com.kakarote.ai_crm.entity.VO.AiBillingConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.EnterpriseConfigVO;

import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface ISystemConfigService extends IService<SystemConfig> {

    /**
     * 获取配置值。
     */
    String getConfigValue(String configKey);

    /**
     * 获取配置值。
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 获取配置按类型。
     */
    Map<String, String> getConfigsByType(String configType);

    /**
     * 更新配置。
     */
    void updateConfig(String configKey, String configValue);

    /**
     * 更新配置。
     */
    void updateConfigs(Map<String, String> configs);

    void updateConfigsWithType(Map<String, String> configs, String configType);

    /**
     * 获取AI 配置。
     */
    AiConfigVO getAiConfig();

    /**
     * 获取AI 配置详情。
     */
    AiConfigVO getAiConfigDetail();

    /**
     * 更新AI 配置。
     */
    void updateAiConfig(AiConfigUpdateBO updateBO);

    /**
     * 激活AI 服务商。
     */
    void activateAiProvider(String provider);

    /**
     * 切换使用赠送AI 配置。
     */
    void useGiftAiConfig();

    /**
     * 切换使用自定义AI 配置。
     */
    void useCustomAiConfig();

    /**
     * 获取 AI 积分折算配置。
     */
    AiBillingConfigVO getAiBillingConfig();

    /**
     * 处理testAiConnection方法逻辑。
     */
    AiConnectionTestVO testAiConnection(AiConfigUpdateBO configBO);

    /**
     * 清理配置缓存。
     */
    void clearConfigCache();

    /**
     * 获取企业配置。
     */
    EnterpriseConfigVO getEnterpriseConfig();

    /**
     * 更新企业配置。
     */
    void updateEnterpriseConfig(EnterpriseConfigUpdateBO updateBO);
}
