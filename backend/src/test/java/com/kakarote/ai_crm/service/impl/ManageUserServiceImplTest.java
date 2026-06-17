package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.UserAddBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManageUserServiceImplTest {

    @Test
    void addUserRejectsDuplicateUsername() {
        ManageUserServiceImpl service = new ManageUserServiceImpl();
        ManageUserMapper mapper = mock(ManageUserMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.selectCount(any())).thenReturn(1L);

        UserAddBO userAddBO = new UserAddBO();
        userAddBO.setUsername("alice");

        assertThatThrownBy(() -> service.addUser(userAddBO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");

        verify(mapper, never()).insert(any(ManagerUser.class));
    }

    @Test
    void addUserRejectsMissingParentUser() {
        ManageUserServiceImpl service = new ManageUserServiceImpl();
        ManageUserMapper mapper = mock(ManageUserMapper.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        UserAddBO userAddBO = new UserAddBO();
        userAddBO.setUsername("bob");
        userAddBO.setParentId(9001L);

        assertThatThrownBy(() -> service.addUser(userAddBO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("直属上级不存在");

        verify(mapper, never()).insert(any(ManagerUser.class));
    }
}
