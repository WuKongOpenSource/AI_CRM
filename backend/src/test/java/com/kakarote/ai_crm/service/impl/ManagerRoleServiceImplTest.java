package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.kakarote.ai_crm.common.Const;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.entity.PO.ManagerMenu;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import com.kakarote.ai_crm.entity.VO.RolePermissionVO;
import com.kakarote.ai_crm.mapper.ManagerRoleMapper;
import com.kakarote.ai_crm.service.IManagerMenuService;
import com.kakarote.ai_crm.service.IManagerRoleMenuService;
import com.kakarote.ai_crm.service.IManagerUserRoleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagerRoleServiceImplTest {

    @Test
    void authShouldHandleSharedMenuListWithoutConcurrentModification() {
        ManagerRoleServiceImpl service = new ManagerRoleServiceImpl();
        IManagerMenuService menuService = mock(IManagerMenuService.class);
        IManagerUserRoleService userRoleService = mock(IManagerUserRoleService.class);
        IManagerRoleMenuService roleMenuService = mock(IManagerRoleMenuService.class);
        Redis redis = mock(Redis.class);

        ReflectionTestUtils.setField(service, "menuService", menuService);
        ReflectionTestUtils.setField(service, "userRoleService", userRoleService);
        ReflectionTestUtils.setField(service, "roleMenuService", roleMenuService);
        ReflectionTestUtils.setField(service, "redis", redis);

        Long userId = 99L;
        ManagerMenu module = menu(100L, 0L, "knowledge", 3);
        ManagerMenu action = menu(101L, 100L, "knowledge:view", 5);
        List<ManagerMenu> sharedMenus = new ArrayList<>(List.of(module, action));

        when(redis.exists(Const.USER_AUTH_CACHE_KET + userId)).thenReturn(false);
        // Simulate both service methods exposing the same backing list instance.
        when(menuService.queryMenuList(userId)).thenReturn(sharedMenus);
        when(menuService.list()).thenReturn(sharedMenus);

        JSONObject auth = service.auth(userId);

        assertThat(auth).isNotNull();
        assertThat(auth.getJSONObject("knowledge")).isNotNull();
        assertThat(auth.getJSONObject("knowledge").getBoolean("knowledge:view")).isTrue();

        ArgumentCaptor<JSONObject> cachedAuth = ArgumentCaptor.forClass(JSONObject.class);
        verify(redis).setex(eq(Const.USER_AUTH_CACHE_KET + userId), eq(300), cachedAuth.capture());
        assertThat(cachedAuth.getValue().getJSONObject("knowledge").getBoolean("knowledge:view")).isTrue();
    }

    @Test
    void queryRolePermissionsShouldExposeScheduleScopeOptionsForNonCreateActions() {
        ManagerRoleServiceImpl service = new ManagerRoleServiceImpl();
        IManagerMenuService menuService = mock(IManagerMenuService.class);
        IManagerRoleMenuService roleMenuService = mock(IManagerRoleMenuService.class);

        ReflectionTestUtils.setField(service, "menuService", menuService);
        ReflectionTestUtils.setField(service, "roleMenuService", roleMenuService);

        Long roleId = 7L;
        ManagerMenu scheduleModule = menu(200L, 0L, "schedule", 3);
        ManagerMenu createAction = menu(201L, 200L, "schedule:create", 5);
        ManagerMenu viewAction = menu(202L, 200L, "schedule:view", 5);
        when(menuService.list()).thenReturn(List.of(scheduleModule, createAction, viewAction));
        when(roleMenuService.queryRoleMenuWithScopeByRoleId(roleId)).thenReturn(List.of());

        List<RolePermissionVO> permissions = service.queryRolePermissions(roleId);

        RolePermissionVO schedule = permissions.stream()
                .filter(vo -> "schedule".equals(vo.getModule()))
                .findFirst()
                .orElseThrow();

        RolePermissionVO.ActionPerm create = schedule.getActions().stream()
                .filter(action -> "create".equals(action.getAction()))
                .findFirst()
                .orElseThrow();
        RolePermissionVO.ActionPerm view = schedule.getActions().stream()
                .filter(action -> "view".equals(action.getAction()))
                .findFirst()
                .orElseThrow();

        assertThat(create.isHasScopeOption()).isFalse();
        assertThat(view.isHasScopeOption()).isTrue();
    }

    @Test
    void queryRolePermissionsShouldExposeWecomScopeOptionsForViewAndBindActions() {
        ManagerRoleServiceImpl service = new ManagerRoleServiceImpl();
        IManagerMenuService menuService = mock(IManagerMenuService.class);
        IManagerRoleMenuService roleMenuService = mock(IManagerRoleMenuService.class);

        ReflectionTestUtils.setField(service, "menuService", menuService);
        ReflectionTestUtils.setField(service, "roleMenuService", roleMenuService);

        Long roleId = 7L;
        ManagerMenu sessionModule = menu(300L, 0L, "wecomCustomerSession", 3);
        ManagerMenu sessionView = menu(301L, 300L, "wecomCustomerSession:view", 5);
        ManagerMenu customerModule = menu(310L, 0L, "wecomCustomer", 3);
        ManagerMenu customerBind = menu(311L, 310L, "wecomCustomer:bind", 5);
        when(menuService.list()).thenReturn(List.of(sessionModule, sessionView, customerModule, customerBind));
        when(roleMenuService.queryRoleMenuWithScopeByRoleId(roleId)).thenReturn(List.of());

        List<RolePermissionVO> permissions = service.queryRolePermissions(roleId);

        RolePermissionVO customerSession = permissions.stream()
                .filter(vo -> "wecomCustomerSession".equals(vo.getModule()))
                .findFirst()
                .orElseThrow();
        RolePermissionVO wecomCustomer = permissions.stream()
                .filter(vo -> "wecomCustomer".equals(vo.getModule()))
                .findFirst()
                .orElseThrow();

        assertThat(customerSession.getActions().getFirst().isHasScopeOption()).isTrue();
        assertThat(wecomCustomer.getActions().getFirst().isHasScopeOption()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void managerMenuShouldTreatLegacyImportedSuperAdminRoleAsSuperAdmin() {
        ManagerMenuServiceImpl service = new ManagerMenuServiceImpl();
        IManagerUserRoleService userRoleService = mock(IManagerUserRoleService.class);
        ManagerRoleMapper roleMapper = mock(ManagerRoleMapper.class);
        LambdaQueryChainWrapper<ManagerUserRole> userRoleQuery = mock(LambdaQueryChainWrapper.class);

        ReflectionTestUtils.setField(service, "userRoleService", userRoleService);
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);

        Long userId = 310967000560087040L;
        ManagerUserRole legacySuperAdminUserRole = new ManagerUserRole();
        legacySuperAdminUserRole.setRoleId(310966977172647936L);

        when(userRoleService.lambdaQuery()).thenReturn(userRoleQuery);
        when(userRoleQuery.eq(org.mockito.Mockito.<SFunction<ManagerUserRole, ?>>any(), eq(userId)))
                .thenReturn(userRoleQuery);
        when(userRoleQuery.list()).thenReturn(List.of(legacySuperAdminUserRole));
        when(roleMapper.selectCount(org.mockito.Mockito.<LambdaQueryWrapper<ManagerRole>>any()))
                .thenAnswer(invocation -> {
                    LambdaQueryWrapper<ManagerRole> wrapper = invocation.getArgument(0);
                    return wrapper.getSqlSegment().contains("role_name") ? 1L : 0L;
                });

        Boolean isSuperAdmin = ReflectionTestUtils.invokeMethod(service, "isSuperAdmin", userId);

        assertThat(isSuperAdmin).isTrue();
    }

    private ManagerMenu menu(Long menuId, Long parentId, String realm, Integer type) {
        ManagerMenu menu = new ManagerMenu();
        menu.setMenuId(menuId);
        menu.setParentId(parentId);
        menu.setRealm(realm);
        menu.setType(type);
        return menu;
    }
}
