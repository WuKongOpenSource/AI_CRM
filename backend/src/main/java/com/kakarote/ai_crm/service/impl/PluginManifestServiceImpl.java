package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.CustomFieldAddBO;
import com.kakarote.ai_crm.entity.BO.PluginManifest;
import com.kakarote.ai_crm.entity.PO.CrmAiApplication;
import com.kakarote.ai_crm.entity.PO.CrmOauthClient;
import com.kakarote.ai_crm.entity.PO.CrmPluginInstall;
import com.kakarote.ai_crm.entity.VO.ManifestApplyResultVO;
import com.kakarote.ai_crm.mapper.CrmAiApplicationMapper;
import com.kakarote.ai_crm.mapper.CrmOauthClientMapper;
import com.kakarote.ai_crm.mapper.CrmPluginInstallMapper;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.PluginManifestService;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PluginManifestServiceImpl implements PluginManifestService {

    private static final String SUPPORTED_SCHEMA_VERSION = "v1";
    private static final String REF_FIELDS = "fields";
    private static final String REF_APPS = "apps";

    @Autowired
    private CrmOauthClientMapper oauthClientMapper;

    @Autowired
    private CrmPluginInstallMapper pluginInstallMapper;

    @Autowired
    private CrmAiApplicationMapper aiApplicationMapper;

    @Autowired
    private ICustomFieldService customFieldService;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManifestApplyResultVO applyManifest(String clientId, PluginManifest manifest) {
        if (manifest == null || !SUPPORTED_SCHEMA_VERSION.equals(manifest.getSchemaVersion())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "manifest schemaVersion 必须为 v1");
        }
        CrmOauthClient client = oauthClientMapper.selectOne(
                new LambdaQueryWrapper<CrmOauthClient>().eq(CrmOauthClient::getClientId, clientId));
        if (client == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST, "客户端不存在");
        }
        String pluginId = client.getPluginId();

        CrmPluginInstall existing = pluginInstallMapper.selectOne(
                new LambdaQueryWrapper<CrmPluginInstall>().eq(CrmPluginInstall::getPluginId, pluginId));
        if (existing != null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "该插件已安装,请先卸载再重新应用 manifest");
        }

        // 1) 自定义字段
        List<Long> appliedFieldIds = new ArrayList<>();
        if (manifest.getCustomFields() != null) {
            for (PluginManifest.ManifestCustomField cf : manifest.getCustomFields()) {
                CustomFieldAddBO bo = new CustomFieldAddBO();
                bo.setEntityType(cf.getEntity());
                bo.setFieldLabel(cf.getLabel());
                bo.setFieldType(cf.getType());
                bo.setDefaultValue(cf.getDefaultValue());
                bo.setPlaceholder(cf.getPlaceholder());
                bo.setIsRequired(cf.getRequired());
                bo.setIsSearchable(cf.getSearchable());
                bo.setIsShowInList(cf.getShowInList());
                bo.setIsUnique(cf.getUnique());
                appliedFieldIds.add(customFieldService.addField(bo));
            }
        }

        // 2) AI 应用
        List<Long> appliedAppIds = new ArrayList<>();
        if (manifest.getAiApplications() != null) {
            for (PluginManifest.ManifestAiApplication aiApp : manifest.getAiApplications()) {
                CrmAiApplication row = new CrmAiApplication();
                row.setPluginId(pluginId);
                row.setCode(aiApp.getCode());
                row.setLabel(aiApp.getLabel());
                row.setIconName(aiApp.getIconName());
                row.setDescription(aiApp.getDescription());
                row.setSystemPrompt(aiApp.getSystemPrompt());
                row.setDefaultRagEnabled(Boolean.TRUE.equals(aiApp.getDefaultRagEnabled()) ? 1 : 0);
                row.setToolGroups(aiApp.getToolGroups() != null ? JSON.toJSONString(aiApp.getToolGroups()) : null);
                row.setRecommendedQuestions(aiApp.getRecommendedQuestions() != null ? JSON.toJSONString(aiApp.getRecommendedQuestions()) : null);
                row.setStatus(1);
                aiApplicationMapper.insert(row);
                appliedAppIds.add(row.getAppId());
            }
        }

        String version = manifest.getPlugin() != null ? manifest.getPlugin().getVersion() : null;
        JSONObject refs = new JSONObject();
        refs.put(REF_FIELDS, appliedFieldIds);
        refs.put(REF_APPS, appliedAppIds);

        CrmPluginInstall install = new CrmPluginInstall();
        install.setPluginId(pluginId);
        install.setVersion(version);
        install.setEnabled(1);
        install.setInstallSource("local_manifest");
        install.setConfigEncrypted(secretTextCipher.encrypt(refs.toJSONString()));
        pluginInstallMapper.insert(install);

        ManifestApplyResultVO vo = new ManifestApplyResultVO();
        vo.setPluginId(pluginId);
        vo.setAppliedFieldIds(appliedFieldIds);
        vo.setAppliedAiApplicationIds(appliedAppIds);
        vo.setAppliedCount(appliedFieldIds.size() + appliedAppIds.size());
        log.info("应用插件 manifest: pluginId={}, fields={}, aiApps={}", pluginId, appliedFieldIds, appliedAppIds);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseForPlugin(String pluginId) {
        if (StrUtil.isBlank(pluginId)) {
            return;
        }
        CrmPluginInstall install = pluginInstallMapper.selectOne(
                new LambdaQueryWrapper<CrmPluginInstall>().eq(CrmPluginInstall::getPluginId, pluginId));
        if (install == null) {
            return;
        }
        String dec = secretTextCipher.decrypt(install.getConfigEncrypted());
        if (StrUtil.isNotBlank(dec)) {
            try {
                JSONObject refs = JSON.parseObject(dec);
                reverseFields(pluginId, refs.getJSONArray(REF_FIELDS));
                reverseApps(pluginId, refs.getJSONArray(REF_APPS));
            } catch (Exception e) {
                log.warn("解析已应用扩展引用失败: pluginId={}, err={}", pluginId, e.getMessage());
            }
        }
        pluginInstallMapper.deleteById(install.getInstallId());
        log.info("反向插件声明式扩展完成: pluginId={}", pluginId);
    }

    private void reverseFields(String pluginId, JSONArray fieldIds) {
        if (fieldIds == null) {
            return;
        }
        for (int i = 0; i < fieldIds.size(); i++) {
            try {
                customFieldService.deleteField(fieldIds.getLong(i));
            } catch (Exception e) {
                log.warn("反向删除自定义字段失败: pluginId={}, fieldId={}, err={}", pluginId, fieldIds.get(i), e.getMessage());
            }
        }
    }

    private void reverseApps(String pluginId, JSONArray appIds) {
        if (appIds == null) {
            return;
        }
        for (int i = 0; i < appIds.size(); i++) {
            try {
                aiApplicationMapper.deleteById(appIds.getLong(i));
            } catch (Exception e) {
                log.warn("反向删除 AI 应用失败: pluginId={}, appId={}, err={}", pluginId, appIds.get(i), e.getMessage());
            }
        }
    }
}
