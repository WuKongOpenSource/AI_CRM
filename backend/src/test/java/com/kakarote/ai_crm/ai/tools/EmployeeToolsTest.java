package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.state.PendingEmployeeImportStore;
import com.kakarote.ai_crm.entity.BO.UserAddBO;
import com.kakarote.ai_crm.entity.BO.UserUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.IManagerDeptService;
import com.kakarote.ai_crm.service.ManageUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeToolsTest {

    private static final Long SESSION_ID = 7001L;
    private static final Long USER_ID = 1L;

    @Mock
    private ManageUserService manageUserService;

    @Mock
    private IManagerDeptService deptService;

    private PendingEmployeeImportStore store;
    private EmployeeTools tools;

    @BeforeEach
    void setUp() {
        store = new PendingEmployeeImportStore();
        tools = new EmployeeTools();
        ReflectionTestUtils.setField(tools, "manageUserService", manageUserService);
        ReflectionTestUtils.setField(tools, "deptService", deptService);
        ReflectionTestUtils.setField(tools, "pendingEmployeeImportStore", store);
        when(deptService.list()).thenReturn(List.of(dept(10L, "销售部")));
        AiContextHolder.setContext(SESSION_ID, USER_ID);
    }

    @AfterEach
    void tearDown() {
        store.clear(SESSION_ID);
        AiContextHolder.clear();
        AiContextHolder.clearSession(SESSION_ID);
    }

    private ManagerDept dept(Long id, String name) {
        ManagerDept d = new ManagerDept();
        d.setDeptId(id);
        d.setDeptName(name);
        return d;
    }

    private ManagerUser user(Long id, String username) {
        ManagerUser u = new ManagerUser();
        u.setUserId(id);
        u.setUsername(username);
        return u;
    }

    @Test
    void previewMarksNewDuplicateAndErrorRows() {
        when(manageUserService.findUsersByUsernames(any())).thenReturn(List.of(user(99L, "bob")));

        String json = "["
            + "{\"realname\":\"Alice\",\"username\":\"alice\",\"deptName\":\"销售部\"},"
            + "{\"realname\":\"Bob\",\"username\":\"bob\"},"
            + "{\"realname\":\"\",\"username\":\"\"}"
            + "]";

        String result = tools.previewEmployeeImport(json);

        assertThat(result).contains("新建 1 人", "覆盖更新 1 人", "无法导入 1 行");
        assertThat(result).contains("🆕新建", "♻️覆盖更新", "❌");
        assertThat(store.get(SESSION_ID).rows()).hasSize(3);
    }

    @Test
    void previewFlagsUnresolvedDepartment() {
        when(manageUserService.findUsersByUsernames(any())).thenReturn(List.of());

        String json = "[{\"realname\":\"Carol\",\"username\":\"carol\",\"deptName\":\"不存在的部门\"}]";

        String result = tools.previewEmployeeImport(json);

        assertThat(result).contains("未匹配到，将留空");
        assertThat(store.get(SESSION_ID).rows().get(0).getUser().getDeptId()).isNull();
    }

    @Test
    void previewRejectsNonArrayInput() {
        String result = tools.previewEmployeeImport("not json");

        assertThat(result).contains("没有识别到有效的员工名单");
        assertThat(store.get(SESSION_ID)).isNull();
    }

    private void stubExistingUsers(List<ManagerUser> existing) {
        when(manageUserService.findUsersByUsernames(any())).thenReturn(existing);
    }

    @Test
    void confirmCreatesNewAppliesDefaultPasswordAndUpdatesDuplicate() {
        stubExistingUsers(List.of(user(99L, "bob")));
        String json = "["
            + "{\"realname\":\"Alice\",\"username\":\"alice\"},"
            + "{\"realname\":\"Bob\",\"username\":\"bob\",\"mobile\":\"139\"}"
            + "]";
        tools.previewEmployeeImport(json);

        String result = tools.confirmEmployeeImport();

        assertThat(result).contains("新建 1 人", "更新 1 人", "跳过 0 行");
        verify(manageUserService).addUser(argThat((UserAddBO u) ->
            "alice".equals(u.getUsername()) && "123456".equals(u.getPassword())));
        verify(manageUserService).updateUser(argThat((UserUpdateBO u) ->
            u.getUserId().equals(99L) && "139".equals(u.getMobile())));
        assertThat(store.get(SESSION_ID)).isNull();
    }

    @Test
    void confirmSkipsErrorRows() {
        stubExistingUsers(List.of());
        tools.previewEmployeeImport("[{\"realname\":\"\",\"username\":\"\"}]");

        String result = tools.confirmEmployeeImport();

        assertThat(result).contains("新建 0 人", "更新 0 人", "跳过 1 行");
        verify(manageUserService, never()).addUser(any());
    }

    @Test
    void confirmSkipsRowWhenServiceThrows() {
        stubExistingUsers(List.of());
        doThrow(new RuntimeException("用户名已存在")).when(manageUserService).addUser(any());
        tools.previewEmployeeImport("[{\"realname\":\"Dave\",\"username\":\"dave\"}]");

        String result = tools.confirmEmployeeImport();

        assertThat(result).contains("新建 0 人", "跳过 1 行");
        assertThat(result).contains("dave", "用户名已存在");
    }

    @Test
    void confirmWithoutDraftReturnsHint() {
        assertThat(tools.confirmEmployeeImport()).contains("没有待确认的员工导入草稿");
    }

    @Test
    void cancelClearsDraft() {
        stubExistingUsers(List.of());
        tools.previewEmployeeImport("[{\"realname\":\"Alice\",\"username\":\"alice\"}]");

        assertThat(tools.cancelEmployeeImport()).contains("已取消");
        assertThat(store.get(SESSION_ID)).isNull();
    }
}
