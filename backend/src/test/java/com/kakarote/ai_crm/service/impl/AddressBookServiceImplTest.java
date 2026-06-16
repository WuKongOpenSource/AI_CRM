package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.entity.BO.AddressBookQueryBO;
import com.kakarote.ai_crm.entity.VO.AddressBookEmployeeVO;
import com.kakarote.ai_crm.mapper.AddressBookMapper;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.IScheduleService;
import com.kakarote.ai_crm.service.ITaskService;
import com.kakarote.ai_crm.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressBookServiceImplTest {

    @Mock
    private AddressBookMapper addressBookMapper;

    @Mock
    private ManagerDeptMapper deptMapper;

    @Mock
    private DataPermissionService dataPermissionService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ITaskService taskService;

    @Mock
    private IScheduleService scheduleService;

    @Mock
    private IKnowledgeService knowledgeService;

    private AddressBookServiceImpl addressBookService;

    @BeforeEach
    void setUp() {
        addressBookService = new AddressBookServiceImpl();
        ReflectionTestUtils.setField(addressBookService, "addressBookMapper", addressBookMapper);
        ReflectionTestUtils.setField(addressBookService, "deptMapper", deptMapper);
        ReflectionTestUtils.setField(addressBookService, "dataPermissionService", dataPermissionService);
        ReflectionTestUtils.setField(addressBookService, "fileStorageService", fileStorageService);
        ReflectionTestUtils.setField(addressBookService, "permissionService", permissionService);
        ReflectionTestUtils.setField(addressBookService, "taskService", taskService);
        ReflectionTestUtils.setField(addressBookService, "scheduleService", scheduleService);
        ReflectionTestUtils.setField(addressBookService, "knowledgeService", knowledgeService);
        lenient().when(dataPermissionService.createContextByPermission(any())).thenReturn(DataPermissionContext.all());
    }

    @Test
    void queryPageListNormalizesStatusAndFillsImageUrl() {
        AddressBookQueryBO queryBO = new AddressBookQueryBO();
        queryBO.setEmployeeStatus("unknown");

        AddressBookEmployeeVO employee = new AddressBookEmployeeVO();
        employee.setUserId(101L);
        employee.setRealname("Alice");
        employee.setEmployeeStatus("disabled");
        employee.setImg("avatar/a.png");

        BasePage<AddressBookEmployeeVO> page = new BasePage<>(1, 15);
        page.setRecords(java.util.List.of(employee));
        page.setTotal(1);

        when(addressBookMapper.queryPageList(any(), any())).thenReturn(page);
        when(fileStorageService.getUrl("avatar/a.png")).thenReturn("https://static.example/avatar/a.png");

        BasePage<AddressBookEmployeeVO> result = addressBookService.queryPageList(queryBO);

        ArgumentCaptor<AddressBookQueryBO> queryCaptor = ArgumentCaptor.forClass(AddressBookQueryBO.class);
        verify(addressBookMapper).queryPageList(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().getEmployeeStatus()).isEqualTo("active");
        assertThat(result.getRecords()).containsExactly(employee);
        assertThat(employee.getEmployeeStatusName()).isEqualTo("停用");
        assertThat(employee.getImgUrl()).isEqualTo("https://static.example/avatar/a.png");
    }

    @Test
    void getDetailThrowsBusinessExceptionWhenEmployeeMissing() {
        when(addressBookMapper.getDetail(101L)).thenReturn(null);

        assertThatThrownBy(() -> addressBookService.getDetail(101L))
                .isInstanceOf(BusinessException.class);
    }
}
