package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import com.kakarote.ai_crm.mapper.ManagerUserRoleMapper;
import com.kakarote.ai_crm.service.IManagerUserRoleService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 用户与角色对应表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
@Service
public class ManagerUserRoleServiceImpl extends ServiceImpl<ManagerUserRoleMapper, ManagerUserRole> implements IManagerUserRoleService {

    /**
     * 通过条件查询用户和角色关联关系
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 用户和角色关联关系列表
     */
    @Override
    public List<ManagerUserRole> queryUserRoleByParams(Long roleId, Long userId) {
        return baseMapper.queryUserRoleByParams(roleId,userId);
    }

    /**
     * 查询出不等于当前角色的用户与角色关系数量
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 数量
     */
    @Override
    public Integer queryUserRoleNoCurrentRole(Long userId, Long roleId) {
        return baseMapper.queryUserRoleNoCurrentRole(userId,roleId);
    }

    /**
     * 查询出等于当前角色的用户与角色关系数量
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 数量
     */
    @Override
    public Integer queryCurrentUserRole(Long userId, Long roleId) {
        return baseMapper.queryCurrentUserRole(userId,roleId);
    }
}
