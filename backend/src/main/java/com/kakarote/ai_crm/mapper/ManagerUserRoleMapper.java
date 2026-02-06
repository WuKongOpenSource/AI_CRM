package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 用户与角色对应表 Mapper 接口
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
public interface ManagerUserRoleMapper extends BaseMapper<ManagerUserRole> {
    /**
     * 通过条件查询用户和角色关联关系
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 关系列表
     */
    @Select("SELECT * FROM manager_user_role where user_id=#{userId} and role_id =#{roleId}")
    List<ManagerUserRole> queryUserRoleByParams(@Param("roleId") Long roleId,@Param("userId") Long userId);

    /**
     * 查询出不等于当前角色的用户与角色关系数量
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 数量
     */
    @Select("SELECT count(user_id) FROM manager_user_role where user_id=#{userId} and role_id !=#{roleId}")
    Integer queryUserRoleNoCurrentRole(@Param("userId")Long userId,@Param("roleId") Long roleId);
    /**
     * 查询出等于当前角色的用户与角色关系数量
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 数量
     */
    @Select("SELECT count(user_id) FROM manager_user_role where user_id=#{userId} and role_id =#{roleId}")
    Integer queryCurrentUserRole(@Param("userId")Long userId, @Param("roleId")Long roleId);
}
