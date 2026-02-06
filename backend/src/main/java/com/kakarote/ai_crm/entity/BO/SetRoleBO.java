package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色BO
 *
 * @author guomenghao
 */
@Schema(description = "角色BO")
@Getter
@Setter
public class SetRoleBO {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色标识符")
    private String realm;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "数据权限 1、本人，2、本人及下属，3、本部门，4、本部门及下属部门，5、全部 ")
    private Integer dataType;
}
