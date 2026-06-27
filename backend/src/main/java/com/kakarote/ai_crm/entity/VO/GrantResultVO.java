package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 授权结果：实际授予的 scope 与被拒绝(管理员本身不持有)的 scope。
 */
@Data
@Schema(name = "GrantResultVO", description = "Result of granting scopes to a plugin")
public class GrantResultVO {

    private String clientId;

    /** 实际授予（管理员持有的） */
    private List<String> granted;

    /** 被拒（管理员本身不持有,下放原则禁止越权） */
    private List<String> rejected;
}
