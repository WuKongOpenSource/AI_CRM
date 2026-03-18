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
import com.kakarote.ai_crm.entity.BO.RolePermissionSaveBO;
import com.kakarote.ai_crm.entity.BO.RoleQueryBO;
import com.kakarote.ai_crm.entity.BO.SetRoleBO;
import com.kakarote.ai_crm.entity.PO.ManagerMenu;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.entity.PO.ManagerRoleMenu;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import com.kakarote.ai_crm.entity.VO.RolePermissionVO;
import com.kakarote.ai_crm.entity.VO.RoleVO;
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
        // 收集用户有权限的菜单的 parentId，只添加对应的父模块
        Set<Long> parentIds = managerMenus.stream()
                .map(ManagerMenu::getParentId)
                .collect(Collectors.toSet());
        List<ManagerMenu> allMenus = menuService.list();
        for (ManagerMenu menu : allMenus) {
            if (Objects.equals(0L, menu.getParentId()) && parentIds.contains(menu.getMenuId())) {
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

    @Override
    public List<RoleVO> queryRoleListWithUserCount(String search) {
        return baseMapper.queryRoleListWithUserCount(search);
    }

    @Override
    public List<RolePermissionVO> queryRolePermissions(Long roleId) {
        // 1. 获取所有菜单
        List<ManagerMenu> allMenus = menuService.list();
        // 2. 过滤模块菜单(type=3, parentId=0) 和操作菜单(type=5)
        List<ManagerMenu> moduleMenus = allMenus.stream()
                .filter(m -> Objects.equals(3, m.getType()) && Objects.equals(0L, m.getParentId()))
                .collect(Collectors.toList());
        Map<Long, List<ManagerMenu>> actionMenusByParent = allMenus.stream()
                .filter(m -> Objects.equals(5, m.getType()))
                .collect(Collectors.groupingBy(ManagerMenu::getParentId));
        // 3. 获取该角色已关联菜单及 data_scope
        List<ManagerRoleMenu> roleMenus = roleMenuService.queryRoleMenuWithScopeByRoleId(roleId);
        Map<Long, ManagerRoleMenu> roleMenuMap = roleMenus.stream()
                .collect(Collectors.toMap(ManagerRoleMenu::getMenuId, rm -> rm, (a, b) -> a));
        // 4. 组装返回
        List<RolePermissionVO> result = new ArrayList<>();
        for (ManagerMenu module : moduleMenus) {
            RolePermissionVO vo = new RolePermissionVO();
            vo.setModule(module.getRealm());
            vo.setModuleName(module.getRealmName());
            List<ManagerMenu> actions = actionMenusByParent.getOrDefault(module.getMenuId(), Collections.emptyList());
            List<RolePermissionVO.ActionPerm> actionPerms = new ArrayList<>();
            for (ManagerMenu action : actions) {
                RolePermissionVO.ActionPerm ap = new RolePermissionVO.ActionPerm();
                ap.setMenuId(String.valueOf(action.getMenuId()));
                String realm = action.getRealm();
                String actionName = realm.contains(":") ? realm.substring(realm.lastIndexOf(':') + 1) : realm;
                ap.setAction(actionName);
                ap.setActionName(action.getRealmName());
                Set<String> dataScopeModules = Set.of("customer", "contact", "task", "followup", "knowledge");
                ap.setHasScopeOption(dataScopeModules.contains(module.getRealm()) && !realm.endsWith(":create"));
                ManagerRoleMenu rm = roleMenuMap.get(action.getMenuId());
                if (rm != null) {
                    ap.setEnabled(true);
                    ap.setDataScope(rm.getDataScope());
                } else {
                    ap.setEnabled(false);
                    ap.setDataScope(1);
                }
                actionPerms.add(ap);
            }
            vo.setActions(actionPerms);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissions(RolePermissionSaveBO bo) {
        Long roleId = bo.getRoleId();
        // 1. 获取所有 type=3/5 的菜单 ID
        List<ManagerMenu> allMenus = menuService.list();
        Set<Long> permMenuIds = allMenus.stream()
                .filter(m -> Objects.equals(3, m.getType()) || Objects.equals(5, m.getType()))
                .map(ManagerMenu::getMenuId)
                .collect(Collectors.toSet());
        // 2. 删除该角色下属于这些菜单的 role_menu 记录
        List<ManagerRoleMenu> existingRoleMenus = roleMenuService.queryRoleMenuWithScopeByRoleId(roleId);
        List<Long> toDelete = existingRoleMenus.stream()
                .filter(rm -> permMenuIds.contains(rm.getMenuId()))
                .map(ManagerRoleMenu::getId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(toDelete)) {
            roleMenuService.removeByIds(toDelete);
        }
        // 3. 批量插入新的
        if (CollUtil.isNotEmpty(bo.getPermissions())) {
            roleMenuService.saveRoleMenuWithScope(roleId, bo.getPermissions());
        }
        // 4. 清除该角色所有用户的 Redis 权限缓存
        List<ManagerUserRole> userRoles = userRoleService.lambdaQuery()
                .eq(ManagerUserRole::getRoleId, roleId).list();
        for (ManagerUserRole ur : userRoles) {
            redis.del(Const.USER_AUTH_CACHE_KET + ur.getUserId());
        }
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
