package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
/**
 * 角色关联BO
 *
 * @author guomenghao
 */
@Schema(description = "角色关联BO")
@Getter
@Setter
public class RoleBO {

    @Schema(description = "人员id列表")
    private List<Long> userIds;

    @Schema(description = "权限id列表")
    private List<Long> roleIds;
}
