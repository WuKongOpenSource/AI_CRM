package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.entity.BO.PluginManifest;
import com.kakarote.ai_crm.entity.VO.ManifestApplyResultVO;

/**
 * 声明式 manifest 解析与应用（P1-a）。当前应用 customFields（复用 CustomFieldService），
 * 幂等性约定：同一插件需先卸载再重新应用；卸载时按记录精确反向。
 */
public interface PluginManifestService {

    /** 校验并应用 manifest 的声明式扩展；记录已应用引用以便反向。 */
    ManifestApplyResultVO applyManifest(String clientId, PluginManifest manifest);

    /** 反向某插件已应用的声明式扩展（删除其新建的自定义字段等）。 */
    void reverseForPlugin(String pluginId);
}
