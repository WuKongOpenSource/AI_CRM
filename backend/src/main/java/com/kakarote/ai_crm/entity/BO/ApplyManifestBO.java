package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 为某客户端应用一份声明式 manifest 的入参。
 */
@Data
@Schema(name = "ApplyManifestBO", description = "Apply a declarative manifest to a plugin client")
public class ApplyManifestBO {

    private String clientId;

    private PluginManifest manifest;
}
