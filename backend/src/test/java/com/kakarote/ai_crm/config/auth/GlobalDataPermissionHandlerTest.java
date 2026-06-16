package com.kakarote.ai_crm.config.auth;

import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.DataPermissionService;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalDataPermissionHandlerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void relationMapperShouldUseOwnerPrivacyWithoutRoleDataPermissionContext() {
        GlobalDataPermissionHandler handler = new GlobalDataPermissionHandler();
        @SuppressWarnings("unchecked")
        ObjectProvider<DataPermissionService> provider = mock(ObjectProvider.class);
        DataPermissionService dataPermissionService = mock(DataPermissionService.class);
        ReflectionTestUtils.setField(handler, "dataPermissionServiceProvider", provider);
        when(provider.getIfAvailable()).thenReturn(dataPermissionService);
        mockLoginUser(100L);

        Expression expression = handler.getSqlSegment(
                new Table("crm_relation"),
                null,
                "com.kakarote.ai_crm.mapper.RelationMapper.queryPageList"
        );

        assertThat(expression).isNotNull();
        assertThat(expression.toString()).isEqualTo("create_user_id = 100");
        verify(dataPermissionService, never()).createContext("relation");
    }

    private static void mockLoginUser(Long userId) {
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
