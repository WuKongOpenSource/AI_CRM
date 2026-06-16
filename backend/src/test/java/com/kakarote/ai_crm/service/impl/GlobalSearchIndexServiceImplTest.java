package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class GlobalSearchIndexServiceImplTest {

    @Test
    void relationSearchShouldBeEnabledWithoutRoleMenuPermission() {
        GlobalSearchIndexServiceImpl service = new GlobalSearchIndexServiceImpl();
        PermissionService permissionService = mock(PermissionService.class);
        ReflectionTestUtils.setField(service, "permissionService", permissionService);

        GlobalSearchQueryBO queryBO = new GlobalSearchQueryBO();

        ReflectionTestUtils.invokeMethod(service, "applyRelationScope", queryBO);

        assertThat(queryBO.getRelationEnabled()).isTrue();
        verifyNoInteractions(permissionService);
    }
}
