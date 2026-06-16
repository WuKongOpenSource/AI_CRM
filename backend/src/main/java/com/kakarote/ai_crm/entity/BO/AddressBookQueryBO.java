package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "AddressBookQueryBO", description = "通讯录员工查询参数")
public class AddressBookQueryBO extends PageEntity {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "员工状态: active在职, resigned离职, disabled停用")
    private String employeeStatus;

    @Schema(description = "部门ID列表（含下级部门）", hidden = true)
    private List<Long> deptIds;

    @Schema(hidden = true)
    private Boolean allData;

    @Schema(hidden = true)
    private List<Long> userIds;
}
