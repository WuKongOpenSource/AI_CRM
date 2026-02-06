package com.kakarote.ai_crm.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.RoleQueryBO;
import com.kakarote.ai_crm.entity.BO.SetRoleBO;
import com.kakarote.ai_crm.entity.PO.ManagerRole;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 角色表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
public interface IManagerRoleService extends IService<ManagerRole> {

    /**
     * 查询角色信息
     * @param id 主键ID
     * @return data  角色信息
     */
    public ManagerRole queryById(Serializable id);

    /**
     * 保存或新增信息
     * @author guomenghao
     * @param setRoleBO data
     */
    public void addOrUpdate(SetRoleBO setRoleBO);


    /**
     * 查询所有数据
     * @author guomenghao
     * @param roleQueryBO 搜索条件
     * @return list 角色列表
     */
    public BasePage<ManagerRole> queryPageList(RoleQueryBO roleQueryBO);

    /**
     * 根据ID列表删除数据
     * @author guomenghao
     * @param ids ids
     */
    public void deleteByIds(List<Serializable> ids);

    /**
     * 查询权限
     * @param userId 用户ID
     * @return data 权限
     */
    JSONObject auth(Long userId);

    /**
     * 查询所有角色
     * @return list 角色列表
     */
    List<ManagerRole> getAllRoleList();

    /**
     * 角色关联菜单
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     */
    void relatedMenu(Long roleId, List<Long> menuIds);

    /**
     * 取消角色关联员工
     * @param userId 用户ID
     * @param roleId 角色ID
     */
    void unbindingUser(Long userId, Long roleId);

    /**
     * 角色关联员工
     * @param userIds 用户ID列表
     * @param roleIds 角色ID列表
     */
    void relatedUser(List<Long> userIds, List<Long> roleIds);

    /**
     * 通过角色ID查询菜单
     * @param id 角色ID
     * @return list 菜单ID列表
     */
    List<Long> queryMenuIdList(Long id);
}
