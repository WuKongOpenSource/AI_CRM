package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;

import java.util.List;

/**
 * <p>
 * 用户与角色对应表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
public interface IManagerUserRoleService extends IService<ManagerUserRole> {
    /**
     * 通过条件查询用户和角色关联关系
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 用户和角色关联关系列表
     */

    List<ManagerUserRole> queryUserRoleByParams(Long roleId, Long userId);

    /**
     * 查询出不等于当前角色的用户与角色关系数量
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 数量
     */
    Integer queryUserRoleNoCurrentRole(Long userId, Long roleId);
    /**
     * 查询出等于当前角色的用户与角色关系数量
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 数量
     */
    Integer queryCurrentUserRole(Long userId, Long roleId);
}
