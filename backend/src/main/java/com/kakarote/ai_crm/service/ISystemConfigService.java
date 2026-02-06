package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.AiConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.WeKnoraConfigUpdateBO;
import com.kakarote.ai_crm.entity.PO.SystemConfig;
import com.kakarote.ai_crm.entity.VO.AiConfigVO;
import com.kakarote.ai_crm.entity.VO.AiConnectionTestVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraConfigVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraConnectionTestVO;

import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface ISystemConfigService extends IService<SystemConfig> {

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键获取配置值，带默认值
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 根据配置类型获取所有配置
     *
     * @param configType 配置类型
     * @return 配置键值对
     */
    Map<String, String> getConfigsByType(String configType);

    /**
     * 更新配置（单个）
     *
     * @param configKey   配置键
     * @param configValue 配置值
     */
    void updateConfig(String configKey, String configValue);

    /**
     * 批量更新配置
     *
     * @param configs 配置键值对
     */
    void updateConfigs(Map<String, String> configs);

    /**
     * 获取 AI 配置
     *
     * @return AI 配置信息
     */
    AiConfigVO getAiConfig();

    /**
     * 更新 AI 配置
     *
     * @param updateBO 更新参数
     */
    void updateAiConfig(AiConfigUpdateBO updateBO);

    /**
     * 测试 AI 连接
     *
     * @param configBO 配置参数
     * @return 测试结果
     */
    AiConnectionTestVO testAiConnection(AiConfigUpdateBO configBO);

    /**
     * 清除配置缓存
     */
    void clearConfigCache();

    /**
     * 获取 WeKnora 配置
     *
     * @return WeKnora 配置信息
     */
    WeKnoraConfigVO getWeKnoraConfig();

    /**
     * 更新 WeKnora 配置
     *
     * @param updateBO 更新参数
     */
    void updateWeKnoraConfig(WeKnoraConfigUpdateBO updateBO);

    /**
     * 测试 WeKnora 连接
     *
     * @param configBO 配置参数
     * @return 测试结果
     */
    WeKnoraConnectionTestVO testWeKnoraConnection(WeKnoraConfigUpdateBO configBO);
}
