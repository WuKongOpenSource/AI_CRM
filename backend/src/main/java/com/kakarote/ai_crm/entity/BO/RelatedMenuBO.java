package com.kakarote.ai_crm.entity.BO;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 关联菜单BO
 *
 * @author guomenghao
 */
@Schema(description = "关联菜单BO")
@Data
public class RelatedMenuBO {

    @Schema(description = "菜单id列表")
    private List<Long> menuIds;

    @Schema(description = "权限id")
    private Long roleId;
}
