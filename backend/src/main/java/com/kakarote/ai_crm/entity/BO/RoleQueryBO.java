package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色查询BO
 *
 * @author guomenghao
 */
@Schema(description = "角色查询BO")
@Data
public class RoleQueryBO extends PageEntity {

    @Schema(description = "查询条件")
    private String search;

}

