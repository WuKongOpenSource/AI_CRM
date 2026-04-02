package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ManagerRoleMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 角色菜单对应关系表 Mapper 接口
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
public interface ManagerRoleMenuMapper extends BaseMapper<ManagerRoleMenu> {

    /**
     * 通过角色查询角色菜单对应关系
     *
     * @param roleId 角色ID
     * @return
     */
    @Select("SELECT * FROM manager_role_menu  where role_id=#{roleId}")
    List<ManagerRoleMenu> queryRoleMenuByRoleId(@Param("roleId") Long roleId);

    /**
     * 通过角色ID菜单ID列表
     * @param
     * @return 菜单ID列表
     */
    @Select("SELECT menu_id FROM manager_role_menu  where role_id=#{roleId}")
    List<Long> queryMenuIdListByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT id, menu_id, role_id, data_scope FROM manager_role_menu WHERE role_id=#{roleId}")
    List<ManagerRoleMenu> queryRoleMenuWithScopeByRoleId(@Param("roleId") Long roleId);

    @Select("""
            SELECT rm.data_scope
            FROM manager_role_menu rm
            INNER JOIN manager_user_role ur ON ur.role_id = rm.role_id
            INNER JOIN manager_menu m ON m.menu_id = rm.menu_id
            WHERE ur.user_id = #{userId}
              AND rm.data_scope IS NOT NULL
              AND m.realm LIKE CONCAT(#{module}, ':%')
            """)
    List<Integer> queryDataScopesByUserIdAndModule(@Param("userId") Long userId, @Param("module") String module);
}
