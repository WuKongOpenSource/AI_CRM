package com.kakarote.ai_crm.entity.VO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "菜单")
public class MenuVO {


    @Schema(description = "菜单ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    @Schema(description = "上级资源ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    @Schema(description = "权限标识")
    private String realm;

    @Schema(description = "权限名称")
    private String realmName;

    @Schema(description = "权限类型 1 数据 2api 3 菜单 4 按钮 5 功能")
    private Integer type;


    @Schema(description = "子菜单列表")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<MenuVO> children;
}
