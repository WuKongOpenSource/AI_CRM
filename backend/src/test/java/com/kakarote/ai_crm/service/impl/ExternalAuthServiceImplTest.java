package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.config.ExternalAuthProperties;
import com.kakarote.ai_crm.entity.BO.ExternalTenantRegisterBO;
import com.kakarote.ai_crm.entity.PO.ExternalAuthIdentity;
import com.kakarote.ai_crm.entity.PO.ExternalTenantBinding;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.mapper.ExternalAuthIdentityMapper;
import com.kakarote.ai_crm.mapper.ExternalTenantBindingMapper;
import com.kakarote.ai_crm.service.AuthSessionService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalAuthServiceImplTest {

    @Test
    void autoProvisionWecomIdentityShouldCreateTenantBindingAndIdentityForUnknownCorp() {
        ExternalAuthServiceImpl service = newService();
        RegistrationService registrationService = mapper(service, "registrationService");
        ExternalAuthIdentityMapper identityMapper = mapper(service, "identityMapper");
        ExternalTenantBindingMapper tenantBindingMapper = mapper(service, "tenantBindingMapper");

        ManagerUser createdUser = new ManagerUser();
        createdUser.setTenantId(66L);
        createdUser.setUserId(77L);
        when(tenantBindingMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(registrationService.registerExternalTenant(any(ExternalTenantRegisterBO.class))).thenReturn(createdUser);

        AtomicReference<ExternalAuthIdentity> insertedIdentity = new AtomicReference<>();
        doAnswer(invocation -> {
            ExternalAuthIdentity identity = invocation.getArgument(0);
            identity.setId(99L);
            insertedIdentity.set(identity);
            return 1;
        }).when(identityMapper).insert(any(ExternalAuthIdentity.class));
        when(identityMapper.selectOne(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> insertedIdentity.get());

        ExternalAuthServiceImpl.ExternalProfile profile = new ExternalAuthServiceImpl.ExternalProfile();
        profile.setProvider("wecom");
        profile.setSubject("corp_1:user_a");
        profile.setDisplayName("User A");
        profile.setExternalTenantKey("corp_1");
        profile.setExternalTenantName("Corp One");
        profile.setRawJson("{}");

        ExternalAuthIdentity identity = ReflectionTestUtils.invokeMethod(service, "autoProvisionWecomIdentity", profile);

        assertThat(identity).isNotNull();
        assertThat(identity.getId()).isEqualTo(99L);
        assertThat(identity.getProvider()).isEqualTo("wecom");
        assertThat(identity.getSubject()).isEqualTo("corp_1:user_a");
        assertThat(identity.getTenantId()).isEqualTo(66L);
        assertThat(identity.getUserId()).isEqualTo(77L);
        assertThat(identity.getEmail()).isEqualTo("wecom.corp_1_user_a.corp_1@external.wecom.local");

        ArgumentCaptor<ExternalTenantRegisterBO> registerCaptor = ArgumentCaptor.forClass(ExternalTenantRegisterBO.class);
        verify(registrationService).registerExternalTenant(registerCaptor.capture());
        assertThat(registerCaptor.getValue().getCompanyName()).isEqualTo("Corp One");
        assertThat(registerCaptor.getValue().getRealname()).isEqualTo("User A");
        assertThat(registerCaptor.getValue().getEmailVerificationRequired()).isFalse();

        ArgumentCaptor<ExternalTenantBinding> bindingCaptor = ArgumentCaptor.forClass(ExternalTenantBinding.class);
        verify(tenantBindingMapper).insert(bindingCaptor.capture());
        assertThat(bindingCaptor.getValue().getProvider()).isEqualTo("wecom");
        assertThat(bindingCaptor.getValue().getExternalTenantKey()).isEqualTo("corp_1");
        assertThat(bindingCaptor.getValue().getTenantId()).isEqualTo(66L);
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(ExternalAuthServiceImpl service, String fieldName) {
        return (T) ReflectionTestUtils.getField(service, fieldName);
    }

    private static ExternalAuthServiceImpl newService() {
        ExternalAuthServiceImpl service = new ExternalAuthServiceImpl();
        ReflectionTestUtils.setField(service, "properties", new ExternalAuthProperties());
        ReflectionTestUtils.setField(service, "redis", mock(Redis.class));
        ReflectionTestUtils.setField(service, "identityMapper", mock(ExternalAuthIdentityMapper.class));
        ReflectionTestUtils.setField(service, "tenantBindingMapper", mock(ExternalTenantBindingMapper.class));
        ReflectionTestUtils.setField(service, "manageUserService", mock(ManageUserService.class));
        ReflectionTestUtils.setField(service, "registrationService", mock(RegistrationService.class));
        ReflectionTestUtils.setField(service, "authSessionService", mock(AuthSessionService.class));
        return service;
    }
}
