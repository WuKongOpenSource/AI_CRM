package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.EnterpriseConfigUpdateBO;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import com.kakarote.ai_crm.entity.VO.AiConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.EnterpriseConfigVO;

import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface ISystemConfigService extends IService<SystemConfig> {

    String getConfigValue(String configKey);

    String getConfigValue(String configKey, String defaultValue);

    Map<String, String> getConfigsByType(String configType);

    void updateConfigsWithType(Map<String, String> configs, String configType);

    void updateConfig(String configKey, String configValue);

    void updateConfigs(Map<String, String> configs);

    AiConfigVO getAiConfig();

    AiConfigVO getAiConfigDetail();

    void updateAiConfig(AiConfigUpdateBO updateBO);

    void activateAiProvider(String provider);

    void useCustomAiConfig();

    AiConnectionTestVO testAiConnection(AiConfigUpdateBO configBO);

    void clearConfigCache();

    EnterpriseConfigVO getEnterpriseConfig();

    void updateEnterpriseConfig(EnterpriseConfigUpdateBO updateBO);
}
