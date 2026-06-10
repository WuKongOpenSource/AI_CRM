package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.entity.PO.ManagerMenu;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import com.kakarote.ai_crm.entity.VO.MenuVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.mapper.ManagerMenuMapper;
import com.kakarote.ai_crm.mapper.ManagerRoleMapper;
import com.kakarote.ai_crm.service.IManagerMenuService;
import com.kakarote.ai_crm.service.IManagerUserRoleService;
import com.kakarote.ai_crm.utils.RecursionUtil;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
@Service
public class ManagerMenuServiceImpl extends ServiceImpl<ManagerMenuMapper, ManagerMenu> implements IManagerMenuService {

    private static final Set<String> LEGACY_SUPER_ADMIN_ROLE_NAMES = Set.of("超级管理员", "super admin");
    private static final String LEGACY_IMPORTED_ROLE_REALM_PREFIX = "wk_role_";

    @Autowired
    private IManagerUserRoleService userRoleService;

    @Autowired
    private ManagerRoleMapper roleMapper;

    /**
     * 查询菜单列表
     *
     * @return 菜单列表
     */
    @Override
    public List<MenuVO> queryAllMenuList() {
        List<MenuVO> menuVOList = getBaseMapper().queryAllMenuList();
        return RecursionUtil.getChildListTree(menuVOList, "parentId", 0L, "menuId", "children", MenuVO.class);
    }

    /**
     * 查询用户所拥有的菜单权限
     *
     * @param userId
     * @return list 菜单列表
     */
    @Override
    public List<ManagerMenu> queryMenuList(Long userId) {
        // 超级管理员（userId=1 或拥有 super_admin 角色）拥有全部菜单
        if (ObjectUtil.equals(userId, UserUtil.getSuperUserId()) || isSuperAdmin(userId)) {
            return lambdaQuery().list();
        }
        return getBaseMapper().queryMenuList(userId);
    }

    /**
     * 判断用户是否拥有超级管理员角色。
     */
    private boolean isSuperAdmin(Long userId) {
        // 查询用户的所有角色ID
        List<ManagerUserRole> userRoles = userRoleService.lambdaQuery()
                .eq(ManagerUserRole::getUserId, userId)
                .list();
        if (userRoles.isEmpty()) {
            return false;
        }
        List<Long> roleIds = userRoles.stream()
                .map(ManagerUserRole::getRoleId)
                .collect(Collectors.toList());
        // 检查是否有超级管理员角色（使用 Mapper 避免与 RoleService 循环依赖）。
        LambdaQueryWrapper<ManagerRole> wrapper = new LambdaQueryWrapper<ManagerRole>()
                .in(ManagerRole::getRoleId, roleIds)
                .and(role -> role.eq(ManagerRole::getRealm, RegistrationServiceImpl.SUPER_ADMIN_REALM)
                        .or(legacy -> legacy.in(ManagerRole::getRoleName, LEGACY_SUPER_ADMIN_ROLE_NAMES)
                                .likeRight(ManagerRole::getRealm, LEGACY_IMPORTED_ROLE_REALM_PREFIX)));
        return roleMapper.selectCount(wrapper) > 0;
    }
}
