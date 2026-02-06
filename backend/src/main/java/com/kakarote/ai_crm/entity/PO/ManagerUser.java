package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户表
 *
 * @author hmb
 * @date 2020-12-16 13:51:10
 */
@Data
@TableName("manager_user")
@Schema(name="用户表对象", description="用户表")
public class ManagerUser implements Serializable {

	@Serial
    private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@TableId
	@Schema(description = "主键")
	private Long userId;
	/**
	 * 用户名
	 */
	@Schema(description = "用户名")
	private String username;
	/**
	 * 密码
	 */
	@Schema(description = "密码")
	private String password;
	/**
	 * 安全符
	 */
	@Schema(description = "安全符")
	private String salt;
	/**
	 * 头像
	 */
	@Schema(description = "头像")
	private String img;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createTime;
	/**
	 * 真实姓名
	 */
	@Schema(description = "真实姓名")
	private String realname;
	/**
	 * 员工编号
	 */
	@Schema(description = "员工编号")
	private String num;
	/**
	 * 手机号
	 */
	@Schema(description = "手机号")
	private String mobile;
	/**
	 * 邮箱
	 */
	@Schema(description = "邮箱")
	private String email;
	/**
	 * 0 未选择 1 男 2 女
	 */
	@Schema(description = "0 未选择 1 男 2 女 ")
	private Integer sex;
	/**
	 * 部门
	 */
	@Schema(description = "部门")
	private Integer deptId;
	/**
	 * 岗位
	 */
	@Schema(description = "岗位")
	private String post;
	/**
	 * 状态,0禁用,1正常,2未激活
	 */
	@Schema(description = "状态,0禁用,1正常,2未激活")
	private Integer status;
	/**
	 * 直属上级ID
	 */
	@Schema(description = "直属上级ID")
	private Long parentId;

}
