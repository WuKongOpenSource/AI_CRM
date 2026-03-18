package com.kakarote.ai_crm.entity.BO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户更新BO
 *
 * @author guomenghao
 */
@Schema(description = "用户更新BO")
@Data
public class UserUpdateBO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "头像")
    private String img;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "真实姓名")
    private String realname;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "部门ID")
    private Long deptId;

    @Schema(description = "岗位")
    private String post;

    @Schema(description = "0 未选择 1 男 2 女")
    private Integer sex;

    @Schema(description = "状态,0禁用,1正常,2未激活")
    private Integer status;

    @Schema(description = "直属上级ID")
    private Long parentId;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;
}
