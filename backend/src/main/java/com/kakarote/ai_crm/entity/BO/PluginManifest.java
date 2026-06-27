package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 插件声明式 manifest（schema v1，P1-a 仅应用 customFields；其余字段预留）。
 * 未知字段忽略(Spring Boot 默认 fail-on-unknown=false)。
 */
@Data
@Schema(name = "PluginManifest", description = "Declarative plugin manifest (v1)")
public class PluginManifest {

    private String schemaVersion;

    private PluginMeta plugin;

    private List<ManifestCustomField> customFields;

    private List<ManifestAiApplication> aiApplications;

    @Data
    @Schema(name = "PluginManifest.PluginMeta")
    public static class PluginMeta {
        private String id;
        private String name;
        private String version;
    }

    @Data
    @Schema(name = "PluginManifest.ManifestCustomField")
    public static class ManifestCustomField {
        /** 实体：customer / contact / relation / product */
        private String entity;
        /** 显示标签 */
        private String label;
        /** 字段类型：text/textarea/number/date/datetime/select/multiselect/checkbox */
        private String type;
        private String defaultValue;
        private String placeholder;
        private Boolean required;
        private Boolean searchable;
        private Boolean showInList;
        private Boolean unique;
    }

    @Data
    @Schema(name = "PluginManifest.ManifestAiApplication")
    public static class ManifestAiApplication {
        /** 应用编码（与内置冲突时内置优先,安装无效） */
        private String code;
        private String label;
        private String iconName;
        private String description;
        private String systemPrompt;
        private Boolean defaultRagEnabled;
        /** 关联工具组（内置工具组才会真正加载工具；自定义工具组属 P1-b 后续） */
        private List<String> toolGroups;
        private List<String> recommendedQuestions;
    }
}
