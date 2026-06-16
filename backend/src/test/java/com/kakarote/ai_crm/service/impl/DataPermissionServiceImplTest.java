package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.auth.DataPermissionHolder;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.mapper.ManagerRoleMapper;
import com.kakarote.ai_crm.mapper.ManagerRoleMenuMapper;
import com.kakarote.ai_crm.mapper.ManagerUserRoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPermissionServiceImplTest {

    @AfterEach
    void tearDown() {
        DataPermissionHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createContextByPermissionShouldUseExactPermissionScopeInsteadOfModuleUnion() {
        DataPermissionServiceImpl service = new DataPermissionServiceImpl();
        ManageUserMapper manageUserMapper = mock(ManageUserMapper.class);
        ManagerDeptMapper managerDeptMapper = mock(ManagerDeptMapper.class);
        ManagerUserRoleMapper managerUserRoleMapper = mock(ManagerUserRoleMapper.class);
        ManagerRoleMapper managerRoleMapper = mock(ManagerRoleMapper.class);
        ManagerRoleMenuMapper managerRoleMenuMapper = mock(ManagerRoleMenuMapper.class);

        ReflectionTestUtils.setField(service, "manageUserMapper", manageUserMapper);
        ReflectionTestUtils.setField(service, "managerDeptMapper", managerDeptMapper);
        ReflectionTestUtils.setField(service, "managerUserRoleMapper", managerUserRoleMapper);
        ReflectionTestUtils.setField(service, "managerRoleMapper", managerRoleMapper);
        ReflectionTestUtils.setField(service, "managerRoleMenuMapper", managerRoleMenuMapper);

        Long userId = 99L;
        mockLoginUser(userId);

        ManagerUser currentUser = new ManagerUser();
        currentUser.setUserId(userId);
        currentUser.setStatus(1);

        when(managerUserRoleMapper.selectList(any())).thenReturn(List.of());
        when(manageUserMapper.getUserId(userId)).thenReturn(currentUser);
        when(manageUserMapper.selectList(any())).thenReturn(List.of(currentUser));
        when(managerRoleMenuMapper.queryDataScopesByUserIdAndPermission(userId, "schedule:view")).thenReturn(List.of(1));
        when(managerRoleMenuMapper.queryDataScopesByUserIdAndPermission(userId, "schedule:delete")).thenReturn(List.of(5));

        DataPermissionContext viewContext = service.createContextByPermission("schedule:view");
        DataPermissionContext deleteContext = service.createContextByPermission("schedule:delete");

        assertThat(viewContext.isAllData()).isFalse();
        assertThat(viewContext.getUserIds()).containsExactly(userId);
        assertThat(deleteContext.isAllData()).isTrue();

        verify(managerRoleMenuMapper).queryDataScopesByUserIdAndPermission(userId, "schedule:view");
        verify(managerRoleMenuMapper).queryDataScopesByUserIdAndPermission(userId, "schedule:delete");
        verify(managerRoleMenuMapper, never()).queryDataScopesByUserIdAndModule(eq(userId), eq("schedule"));
    }

    private void mockLoginUser(Long userId) {
        ManagerUser user = new ManagerUser();
        user.setUserId(userId);
        user.setUsername("tester");
        user.setPassword("secret");
        user.setStatus(1);

        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of())
        );
    }
}
