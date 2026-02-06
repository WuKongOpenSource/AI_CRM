package com.kakarote.ai_crm.entity.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "用户信息")
public class ManageUserVO {

    @Schema(description = "主键")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像")
    private String img;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "真实姓名")
    private String realname;

    @Schema(description = "员工编号")
    private String num;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "0 未选择 1 男 2 女 ")
    private Integer sex;

    @Schema(description = "部门")
    private Integer deptId;

    @Schema(description = "岗位")
    private String post;

    @Schema(description = "状态,0禁用,1正常,2未激活")
    private Integer status;

    @Schema(description = "直属上级ID")
    private Long parentId;

    @Schema(description = "上级名称")
    private String parentName;
}
