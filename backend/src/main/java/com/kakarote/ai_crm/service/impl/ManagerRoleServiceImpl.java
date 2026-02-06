package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.enums.AdminCodeEnum;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.Const;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.entity.BO.RoleQueryBO;
import com.kakarote.ai_crm.entity.BO.SetRoleBO;
import com.kakarote.ai_crm.entity.PO.ManagerMenu;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.entity.PO.ManagerRoleMenu;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import com.kakarote.ai_crm.mapper.ManagerRoleMapper;
import com.kakarote.ai_crm.service.IManagerMenuService;
import com.kakarote.ai_crm.service.IManagerRoleMenuService;
import com.kakarote.ai_crm.service.IManagerRoleService;
import com.kakarote.ai_crm.service.IManagerUserRoleService;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
@Service
public class ManagerRoleServiceImpl extends ServiceImpl<ManagerRoleMapper, ManagerRole> implements IManagerRoleService {

    @Autowired
    private IManagerUserRoleService userRoleService;

    @Autowired
    private IManagerRoleMenuService roleMenuService;

    @Autowired
    private IManagerMenuService menuService;

    @Autowired
    private Redis redis;
    /**
     * 查询角色信息
     *
     * @param id 主键ID
     * @return data  角色信息
     */
    @Override
    public ManagerRole queryById(Serializable id) {
        return baseMapper.selectById(id);
    }

    /**
     * 保存或新增信息
     *
     * @param setRoleBO data
     * @author guomenghao
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addOrUpdate(SetRoleBO setRoleBO) {
        //判断角色标识符
        Long count = lambdaQuery().eq(ManagerRole::getRealm, setRoleBO.getRealm()).ne(ObjectUtil.isNotNull(setRoleBO.getRoleId()), ManagerRole::getRoleId, setRoleBO.getRoleId()).count();
        if (count > 0) {
            throw new BusinessException(AdminCodeEnum.ADMIN_ROLE_REALM_EXIST);
        }
        if (ObjectUtil.isNull(setRoleBO.getRoleId())) {
            setRoleBO.setRealm("realm_" + RandomUtil.randomString(RandomUtil.BASE_CHAR, 6));
            ManagerRole managerRole = BeanUtil.copyProperties(setRoleBO, ManagerRole.class);
            managerRole.setCreateTime(LocalDateTime.now());
            managerRole.setCreateUserId(UserUtil.getUserId());
            save(managerRole);
        } else {
            lambdaUpdate().eq(ManagerRole::getRoleId,setRoleBO.getRoleId()).set(ManagerRole::getRoleName, setRoleBO.getRoleName())
                    .set(ManagerRole::getDescription, setRoleBO.getDescription())
                    .set(ManagerRole::getRealm, setRoleBO.getRealm()).update();
        }
    }

    /**
     * 查询所有数据
     *
     * @param roleQueryBO 搜索条件
     * @return list 角色列表
     * @author guomenghao
     */
    @Override
    public BasePage<ManagerRole> queryPageList(RoleQueryBO roleQueryBO) {
        if (StrUtil.isNotEmpty(roleQueryBO.getSearch())){
            return lambdaQuery()
                    .like(ManagerRole::getDescription,roleQueryBO.getSearch())
                    .or().like(ManagerRole::getRoleName,roleQueryBO.getSearch())
                    .or().like(ManagerRole::getRealm,roleQueryBO.getSearch()).page(roleQueryBO.parse());
        }
        return lambdaQuery().page(roleQueryBO.parse());
    }

    /**
     * 根据ID列表删除数据
     *
     * @param ids ids
     * @author guomenghao
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        removeByIds(ids);
        //删除关联的员工
        userRoleService.lambdaUpdate().in(ManagerUserRole::getRoleId,ids).remove();
        //删除关联的菜单
        roleMenuService.lambdaUpdate().in(ManagerRoleMenu::getRoleId,ids).remove();
    }

    /**
     * 查询权限
     *
     * @param userId 用户ID
     * @return data 权限
     */
    @Override
    public JSONObject auth(Long userId) {
        String cacheKey = Const.USER_AUTH_CACHE_KET + UserUtil.getUserId();
        if (redis.exists(cacheKey)) {
            return redis.get(cacheKey);
        }
        List<ManagerMenu> managerMenus = menuService.queryMenuList(userId);
        List<ManagerMenu> menus = menuService.list();
        for (ManagerMenu menu : menus) {
            if (Objects.equals(0L, menu.getParentId())) {
                managerMenus.add(menu);
            }
        }
        JSONObject jsonObject = createMenu(new HashSet<>(managerMenus), 0L);
        redis.setex(cacheKey, 300, jsonObject);
        return jsonObject;
    }

    /**
     * 查询所有角色
     *
     * @return list 角色列表
     */
    @Override
    public List<ManagerRole> getAllRoleList() {
        return lambdaQuery().list();
    }

    /**
     * 角色关联菜单
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void relatedMenu(Long roleId, List<Long> menuIds) {
        List<ManagerRoleMenu> roleMenuList= roleMenuService.queryRoleMenuByRoleId(roleId);
        if (CollUtil.isNotEmpty(roleMenuList)){
            List<Long> ids = roleMenuList.stream().map(ManagerRoleMenu::getId).collect(Collectors.toList());
            roleMenuService.removeByIds(ids);
        }
        roleMenuService.saveRoleMenu(roleId,  menuIds);
    }

    /**
     * 取消角色关联员工
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindingUser(Long userId, Long roleId) {
        List<ManagerUserRole> managerUserRoleList = userRoleService.queryUserRoleByParams(roleId, userId);
        List<Long> ids = managerUserRoleList.stream().map(ManagerUserRole::getId).collect(Collectors.toList());
        userRoleService.removeByIds(ids);
    }

    /**
     * 角色关联员工
     *
     * @param userIds 用户ID列表
     * @param roleIds 角色ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void relatedUser(List<Long> userIds, List<Long> roleIds) {
        roleIds = roleIds.stream().distinct().collect(Collectors.toList());
        List<ManagerUserRole> IdsUserRoleList = new ArrayList<>();
        for (Long userId : userIds) {
            for (Long roleId : roleIds) {
                Integer count = userRoleService.queryCurrentUserRole(userId,roleId);
                if (count == 0) {
                    ManagerUserRole userRole = new ManagerUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    userRole.setCreateUserId(UserUtil.getUserId());
                    userRole.setCreateTime(LocalDateTime.now());
                    IdsUserRoleList.add(userRole);
                }
            }
        }
        userRoleService.saveBatch(IdsUserRoleList, Const.BATCH_SAVE_SIZE);
    }

    /**
     * 通过角色ID查询菜单
     *
     * @param id 角色ID
     * @return list 菜单ID列表
     */
    @Override
    public List<Long> queryMenuIdList(Long id) {
        return roleMenuService.queryMenuIdListByRoleId(id);
    }

    public static JSONObject createMenu(Set<ManagerMenu> adminMenuList, Long parentId) {
        JSONObject jsonObject = new JSONObject();
        adminMenuList.forEach(adminMenu -> {
            if (Objects.equals(parentId, adminMenu.getParentId())) {
                if (Objects.equals(3, adminMenu.getType())) {
                    JSONObject object = createMenu(adminMenuList, adminMenu.getMenuId());
                    if (!object.isEmpty()) {
                        jsonObject.put(adminMenu.getRealm(), object);
                    }
                } else {
                    jsonObject.put(adminMenu.getRealm(), Boolean.TRUE);
                }
            }
        });
        return jsonObject;
    }
}
