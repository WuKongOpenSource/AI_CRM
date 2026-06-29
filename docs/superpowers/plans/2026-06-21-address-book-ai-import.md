# Address Book AI Bulk Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an AI-driven bulk employee import to the "通讯录" chat application: the user uploads any roster file, the AI recognizes the employees, previews them for confirmation, then creates the accounts.

**Architecture:** Three new Spring AI `@Tool` methods on a new `EmployeeTools` bean (`previewEmployeeImport` / `confirmEmployeeImport` / `cancelEmployeeImport`), backed by a per-session draft store (`PendingEmployeeImportStore`), mirroring the existing `CustomerTools` + `PendingCustomerCreationStore` pending→confirm pattern. The tools are wired into the `ADDRESS_BOOK` chat application via `ChatApplicationRegistry` and `DynamicChatClientProvider`. File content already reaches the model through the existing attachment pipeline in `ChatServiceImpl`, so no upload/parsing code is needed — extraction is the model's job; the tools only validate and persist.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring AI tool-calling, MyBatis-Plus, Hutool (`JSONUtil`), JUnit 5 + Mockito.

---

## File Structure

- **Create** `backend/src/main/java/com/kakarote/ai_crm/ai/state/EmployeeImportRow.java` — one parsed/validated employee row plus preview metadata (duplicate flag, existing userId, dept-resolution note, row errors).
- **Create** `backend/src/main/java/com/kakarote/ai_crm/ai/state/PendingEmployeeImportStore.java` — per-session draft store of `List<EmployeeImportRow>` with 30-minute TTL.
- **Create** `backend/src/main/java/com/kakarote/ai_crm/ai/tools/EmployeeTools.java` — the three `@Tool` methods plus parse/validate/preview/summary helpers.
- **Modify** `backend/src/main/java/com/kakarote/ai_crm/ai/app/ChatApplicationRegistry.java` — add `TOOL_GROUP_EMPLOYEE`, attach it to `ADDRESS_BOOK`, extend the prompt and recommended questions.
- **Modify** `backend/src/main/java/com/kakarote/ai_crm/ai/DynamicChatClientProvider.java` — inject `EmployeeTools`, add the switch case.
- **Create** `backend/src/test/java/com/kakarote/ai_crm/ai/state/PendingEmployeeImportStoreTest.java`
- **Create** `backend/src/test/java/com/kakarote/ai_crm/ai/tools/EmployeeToolsTest.java`

All `mvn` commands run from the `backend/` directory. The tests are pure JUnit5 + Mockito (no `@SpringBootTest`), so they do not load `application.yml` or touch any database.

---

## Task 1: EmployeeImportRow + PendingEmployeeImportStore

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/ai/state/EmployeeImportRow.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/ai/state/PendingEmployeeImportStore.java`
- Test: `backend/src/test/java/com/kakarote/ai_crm/ai/state/PendingEmployeeImportStoreTest.java`

- [ ] **Step 1: Create the row holder**

`EmployeeImportRow.java`:

```java
package com.kakarote.ai_crm.ai.state;

import com.kakarote.ai_crm.entity.BO.UserAddBO;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入员工时，单行解析校验后的数据 + 预览元信息。
 */
public class EmployeeImportRow {

    /** 已解析的员工字段（realname/username/password/post/deptId/mobile/email/employeeStatus）。 */
    private UserAddBO user = new UserAddBO();

    /** 文件中原始填写的部门名称（用于预览展示与未匹配提示）。 */
    private String deptName;

    /** deptName 是否成功匹配到现有部门；填了名称但没匹配上时为 false。 */
    private boolean deptResolved = true;

    /** username 是否已存在（命中则走覆盖更新）。 */
    private boolean duplicate;

    /** 命中重复时，已存在用户的 ID。 */
    private Long existingUserId;

    /** 行级校验错误；非空表示该行不会被导入。 */
    private List<String> errors = new ArrayList<>();

    public UserAddBO getUser() { return user; }
    public void setUser(UserAddBO user) { this.user = user; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public boolean isDeptResolved() { return deptResolved; }
    public void setDeptResolved(boolean deptResolved) { this.deptResolved = deptResolved; }

    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }

    public Long getExistingUserId() { return existingUserId; }
    public void setExistingUserId(Long existingUserId) { this.existingUserId = existingUserId; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public boolean hasError() { return errors != null && !errors.isEmpty(); }
}
```

- [ ] **Step 2: Create the store**

`PendingEmployeeImportStore.java` (mirrors `PendingCustomerCreationStore`):

```java
package com.kakarote.ai_crm.ai.state;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 AI 会话暂存待确认的员工批量导入草稿。
 */
@Component
public class PendingEmployeeImportStore {

    private static final Duration PENDING_TTL = Duration.ofMinutes(30);

    private final Map<Long, PendingEmployeeImport> pendingRequests = new ConcurrentHashMap<>();

    public void save(Long sessionId, List<EmployeeImportRow> rows) {
        if (sessionId == null || rows == null) {
            return;
        }
        pendingRequests.put(sessionId, new PendingEmployeeImport(rows, Instant.now().plus(PENDING_TTL)));
    }

    public PendingEmployeeImport get(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        PendingEmployeeImport pending = pendingRequests.get(sessionId);
        if (pending == null) {
            return null;
        }
        if (pending.isExpired()) {
            pendingRequests.remove(sessionId);
            return null;
        }
        return pending;
    }

    public PendingEmployeeImport remove(Long sessionId) {
        PendingEmployeeImport pending = get(sessionId);
        if (pending != null) {
            pendingRequests.remove(sessionId);
        }
        return pending;
    }

    public void clear(Long sessionId) {
        if (sessionId != null) {
            pendingRequests.remove(sessionId);
        }
    }

    public record PendingEmployeeImport(List<EmployeeImportRow> rows, Instant expiresAt) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
```

- [ ] **Step 3: Write the failing store test**

`PendingEmployeeImportStoreTest.java`:

```java
package com.kakarote.ai_crm.ai.state;

import com.kakarote.ai_crm.entity.BO.UserAddBO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PendingEmployeeImportStoreTest {

    private EmployeeImportRow row(String username) {
        EmployeeImportRow r = new EmployeeImportRow();
        UserAddBO u = new UserAddBO();
        u.setUsername(username);
        u.setRealname(username);
        r.setUser(u);
        return r;
    }

    @Test
    void saveAndGetReturnsRowsForSession() {
        PendingEmployeeImportStore store = new PendingEmployeeImportStore();
        store.save(100L, List.of(row("alice"), row("bob")));

        PendingEmployeeImportStore.PendingEmployeeImport pending = store.get(100L);

        assertThat(pending).isNotNull();
        assertThat(pending.rows()).hasSize(2);
    }

    @Test
    void sessionsAreIsolated() {
        PendingEmployeeImportStore store = new PendingEmployeeImportStore();
        store.save(100L, List.of(row("alice")));

        assertThat(store.get(200L)).isNull();
    }

    @Test
    void expiredDraftIsRemovedOnGet() {
        PendingEmployeeImportStore store = new PendingEmployeeImportStore();
        store.save(100L, List.of(row("alice")));
        @SuppressWarnings("unchecked")
        Map<Long, PendingEmployeeImportStore.PendingEmployeeImport> map =
            (Map<Long, PendingEmployeeImportStore.PendingEmployeeImport>)
                ReflectionTestUtils.getField(store, "pendingRequests");
        map.put(100L, new PendingEmployeeImportStore.PendingEmployeeImport(
            List.of(row("alice")), Instant.now().minusSeconds(1)));

        assertThat(store.get(100L)).isNull();
    }

    @Test
    void removeReturnsAndClearsDraft() {
        PendingEmployeeImportStore store = new PendingEmployeeImportStore();
        store.save(100L, List.of(row("alice")));

        assertThat(store.remove(100L)).isNotNull();
        assertThat(store.get(100L)).isNull();
    }
}
```

- [ ] **Step 4: Run the store test**

Run: `mvn test -Dtest=PendingEmployeeImportStoreTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/ai/state/EmployeeImportRow.java \
        backend/src/main/java/com/kakarote/ai_crm/ai/state/PendingEmployeeImportStore.java \
        backend/src/test/java/com/kakarote/ai_crm/ai/state/PendingEmployeeImportStoreTest.java
git commit -m "feat: add per-session pending store for employee bulk import"
```

---

## Task 2: EmployeeTools.previewEmployeeImport

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/ai/tools/EmployeeTools.java`
- Test: `backend/src/test/java/com/kakarote/ai_crm/ai/tools/EmployeeToolsTest.java`

- [ ] **Step 1: Create EmployeeTools with the preview tool + helpers**

`EmployeeTools.java`:

```java
package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.state.EmployeeImportRow;
import com.kakarote.ai_crm.ai.state.PendingEmployeeImportStore;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.common.enums.EmployeeStatusEnum;
import com.kakarote.ai_crm.entity.BO.UserAddBO;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.IManagerDeptService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.mybatisplus.extension.toolkit.Db; // placeholder import removed in Step note
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通讯录员工批量导入 AI Tool。
 * 用户在通讯录对话上传名单 → 模型识别后调用 previewEmployeeImport 预览，
 * 用户确认后调用 confirmEmployeeImport 落库；放弃则 cancelEmployeeImport。
 */
@Slf4j
@Component
public class EmployeeTools {

    private static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private IManagerDeptService deptService;

    @Autowired
    private PendingEmployeeImportStore pendingEmployeeImportStore;

    @Tool(description = "预览批量导入的员工名单。当用户上传了包含多名员工信息的文件（表格、文档、图片等）并希望批量添加到通讯录时调用。"
            + "参数 employeesJson 是从文件中识别出的员工 JSON 数组字符串，每个元素字段："
            + "realname(姓名,必填)、username(登录用户名,必填)、password(可选,留空默认123456)、"
            + "post(职位,可选)、deptName(部门名称,可选)、mobile(手机,可选)、email(邮箱,可选)、"
            + "employeeStatus(员工状态:在职/离职/停用,可选,默认在职)。"
            + "本工具只做解析、校验、查重并暂存草稿，不会真正建号；返回预览结果后请向用户展示并询问是否确认导入。")
    @AiToolPermission(value = "user:create", action = "预览批量导入员工")
    public String previewEmployeeImport(
            @ToolParam(description = "员工信息 JSON 数组字符串") String employeesJson) {

        Long sessionId = AiContextHolder.getCurrentSessionId();
        if (sessionId == null) {
            return "预览失败：当前会话不存在，无法暂存导入草稿。";
        }
        if (StrUtil.isBlank(employeesJson) || !JSONUtil.isTypeJSONArray(employeesJson)) {
            return "没有识别到有效的员工名单。请确认上传的文件中包含员工信息，或用文字补充。";
        }

        JSONArray array;
        try {
            array = JSONUtil.parseArray(employeesJson);
        } catch (Exception e) {
            return "员工名单解析失败：" + e.getMessage();
        }
        if (array.isEmpty()) {
            return "没有识别到任何员工记录。";
        }

        // 部门名称 → ID 映射（一次性加载全部部门，与 ManageUserServiceImpl 一致）
        Map<String, Long> deptNameToId = deptService.list().stream()
                .filter(d -> StrUtil.isNotBlank(d.getDeptName()))
                .collect(Collectors.toMap(ManagerDept::getDeptName, ManagerDept::getDeptId, (a, b) -> a));

        List<EmployeeImportRow> rows = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            EmployeeImportRow row = new EmployeeImportRow();
            UserAddBO user = row.getUser();

            String realname = StrUtil.trim(obj.getStr("realname"));
            String username = StrUtil.trim(obj.getStr("username"));
            user.setRealname(realname);
            user.setUsername(username);
            user.setPassword(StrUtil.trim(obj.getStr("password")));
            user.setPost(StrUtil.trim(obj.getStr("post")));
            user.setMobile(StrUtil.trim(obj.getStr("mobile")));
            user.setEmail(StrUtil.trim(obj.getStr("email")));
            user.setEmployeeStatus(resolveEmployeeStatus(obj.getStr("employeeStatus")));
            user.setStatus(1);

            String deptName = StrUtil.trim(obj.getStr("deptName"));
            row.setDeptName(deptName);
            if (StrUtil.isNotBlank(deptName)) {
                Long deptId = deptNameToId.get(deptName);
                if (deptId != null) {
                    user.setDeptId(deptId);
                } else {
                    row.setDeptResolved(false);
                }
            }

            if (StrUtil.isBlank(realname)) {
                row.getErrors().add("姓名不能为空");
            }
            if (StrUtil.isBlank(username)) {
                row.getErrors().add("用户名不能为空");
            }
            rows.add(row);
        }

        // 批量查重（username 已存在 → 覆盖更新）
        Set<String> usernames = rows.stream()
                .map(r -> r.getUser().getUsername())
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!usernames.isEmpty()) {
            Map<String, Long> existing = manageUserService.lambdaQuery()
                    .in(ManagerUser::getUsername, usernames)
                    .list().stream()
                    .collect(Collectors.toMap(ManagerUser::getUsername, ManagerUser::getUserId, (a, b) -> a));
            for (EmployeeImportRow row : rows) {
                Long id = existing.get(row.getUser().getUsername());
                if (id != null) {
                    row.setDuplicate(true);
                    row.setExistingUserId(id);
                }
            }
        }

        pendingEmployeeImportStore.save(sessionId, rows);
        return buildPreview(rows);
    }

    /** 中文/英文员工状态 → 标准 value。 */
    private String resolveEmployeeStatus(String raw) {
        String v = StrUtil.trim(raw);
        if (StrUtil.isBlank(v)) {
            return EmployeeStatusEnum.ACTIVE.getValue();
        }
        switch (v) {
            case "在职": return EmployeeStatusEnum.ACTIVE.getValue();
            case "离职": return EmployeeStatusEnum.RESIGNED.getValue();
            case "停用": return EmployeeStatusEnum.DISABLED.getValue();
            default: return EmployeeStatusEnum.normalize(v);
        }
    }

    /** 生成给模型转述的预览文本。 */
    private String buildPreview(List<EmployeeImportRow> rows) {
        int total = rows.size();
        int errorCount = (int) rows.stream().filter(EmployeeImportRow::hasError).count();
        int dupCount = (int) rows.stream().filter(r -> !r.hasError() && r.isDuplicate()).count();
        int newCount = total - errorCount - dupCount;

        StringBuilder sb = new StringBuilder();
        sb.append("识别到 ").append(total).append(" 条员工记录：新建 ").append(newCount)
          .append(" 人，覆盖更新 ").append(dupCount).append(" 人，无法导入 ").append(errorCount).append(" 行。\n");
        int idx = 1;
        for (EmployeeImportRow row : rows) {
            UserAddBO u = row.getUser();
            String marker = row.hasError() ? "❌" : (row.isDuplicate() ? "♻️覆盖更新" : "🆕新建");
            sb.append(idx++).append(". ").append(marker)
              .append(" 姓名=").append(StrUtil.blankToDefault(u.getRealname(), "-"))
              .append(" 用户名=").append(StrUtil.blankToDefault(u.getUsername(), "-"));
            if (StrUtil.isNotBlank(row.getDeptName())) {
                sb.append(" 部门=").append(row.getDeptName());
                if (!row.isDeptResolved()) {
                    sb.append("(未匹配到，将留空)");
                }
            }
            sb.append(" 状态=").append(EmployeeStatusEnum.getName(u.getEmployeeStatus()));
            if (row.hasError()) {
                sb.append(" 错误：").append(String.join("、", row.getErrors()));
            }
            sb.append("\n");
        }
        sb.append("\n请向用户展示以上名单并询问是否确认导入。确认后调用 confirmEmployeeImport，放弃则调用 cancelEmployeeImport。"
                + "未填密码的员工将使用默认密码 123456。");
        return sb.toString();
    }
}
```

**Note on imports:** delete the bogus `import com.mybatisplus.extension.toolkit.Db;` line shown above — it is not used and will not compile. It is included here only to flag that you must not add stray imports; the real imports are the ones listed at the top of the file.

- [ ] **Step 2: Write the failing preview test**

`EmployeeToolsTest.java`:

```java
package com.kakarote.ai_crm.ai.tools;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.state.PendingEmployeeImportStore;
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
import static org.mockito.Mockito.mock;
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

    /** Stub the MyBatis-Plus lambdaQuery() chain to return the given existing users. */
    @SuppressWarnings("unchecked")
    private void stubExistingUsers(List<ManagerUser> existing) {
        LambdaQueryChainWrapper<ManagerUser> chain = mock(LambdaQueryChainWrapper.class);
        when(manageUserService.lambdaQuery()).thenReturn(chain);
        when(chain.in(any(), (Object) any())).thenReturn(chain);
        when(chain.list()).thenReturn(existing);
    }

    @Test
    void previewMarksNewDuplicateAndErrorRows() {
        stubExistingUsers(List.of(user(99L, "bob")));

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
        stubExistingUsers(List.of());

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
}
```

- [ ] **Step 3: Run the preview tests**

Run: `mvn test -Dtest=EmployeeToolsTest`
Expected: PASS (3 tests). If compilation fails on a stray import, remove the bogus `Db` import noted in Step 1.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/ai/tools/EmployeeTools.java \
        backend/src/test/java/com/kakarote/ai_crm/ai/tools/EmployeeToolsTest.java
git commit -m "feat: add previewEmployeeImport AI tool for address book bulk import"
```

---

## Task 3: confirmEmployeeImport + cancelEmployeeImport

**Files:**
- Modify: `backend/src/main/java/com/kakarote/ai_crm/ai/tools/EmployeeTools.java`
- Test: `backend/src/test/java/com/kakarote/ai_crm/ai/tools/EmployeeToolsTest.java`

- [ ] **Step 1: Add the confirm/cancel tools**

Add these methods to `EmployeeTools` (and add the imports `com.kakarote.ai_crm.entity.BO.UserUpdateBO` and `com.kakarote.ai_crm.ai.state.PendingEmployeeImportStore.PendingEmployeeImport`):

```java
    @Tool(description = "确认执行已预览的员工批量导入。仅当之前调用过 previewEmployeeImport 且用户明确表示“确认导入/继续/可以”时调用。"
            + "会根据暂存草稿真正建号：用户名不存在则新建（未填密码默认123456），用户名已存在则更新该员工资料（不改密码）；有错误的行会被跳过。")
    @AiToolPermission(value = "user:create", action = "确认批量导入员工")
    public String confirmEmployeeImport() {
        Long sessionId = AiContextHolder.getCurrentSessionId();
        if (sessionId == null) {
            return "确认导入失败：当前会话不存在。";
        }
        PendingEmployeeImportStore.PendingEmployeeImport pending = pendingEmployeeImportStore.remove(sessionId);
        if (pending == null) {
            return "当前没有待确认的员工导入草稿，请先上传名单并预览。";
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> skipReasons = new ArrayList<>();

        for (EmployeeImportRow row : pending.rows()) {
            UserAddBO u = row.getUser();
            if (row.hasError()) {
                skipped++;
                skipReasons.add(StrUtil.blankToDefault(u.getRealname(), "(无姓名)")
                        + "：" + String.join("、", row.getErrors()));
                continue;
            }
            try {
                if (row.isDuplicate()) {
                    UserUpdateBO update = new UserUpdateBO();
                    update.setUserId(row.getExistingUserId());
                    update.setRealname(u.getRealname());
                    update.setPost(u.getPost());
                    update.setMobile(u.getMobile());
                    update.setEmail(u.getEmail());
                    update.setDeptId(u.getDeptId());
                    update.setEmployeeStatus(u.getEmployeeStatus());
                    manageUserService.updateUser(update);
                    updated++;
                } else {
                    if (StrUtil.isBlank(u.getPassword())) {
                        u.setPassword(DEFAULT_PASSWORD);
                    }
                    manageUserService.addUser(u);
                    created++;
                }
            } catch (Exception e) {
                skipped++;
                skipReasons.add(StrUtil.blankToDefault(u.getUsername(), "(无用户名)") + "：" + e.getMessage());
                log.warn("批量导入员工失败 username={}", u.getUsername(), e);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("导入完成：新建 ").append(created).append(" 人，更新 ").append(updated)
          .append(" 人，跳过 ").append(skipped).append(" 行。");
        if (created > 0) {
            sb.append(" 新建员工的默认登录密码为 123456，请提醒他们首次登录后修改。");
        }
        if (!skipReasons.isEmpty()) {
            sb.append("\n跳过明细：\n").append(String.join("\n", skipReasons));
        }
        return sb.toString();
    }

    @Tool(description = "取消尚未确认的员工批量导入。当用户在预览后表示“不导入了/取消/算了”时调用。")
    @AiToolPermission(value = "user:create", action = "取消批量导入员工")
    public String cancelEmployeeImport() {
        Long sessionId = AiContextHolder.getCurrentSessionId();
        if (sessionId == null) {
            return "取消失败：当前会话不存在。";
        }
        PendingEmployeeImportStore.PendingEmployeeImport pending = pendingEmployeeImportStore.remove(sessionId);
        if (pending == null) {
            return "当前没有待确认的员工导入草稿。";
        }
        return "已取消本次员工批量导入，未做任何改动。";
    }
```

- [ ] **Step 2: Write the failing confirm/cancel tests**

Add to `EmployeeToolsTest` (imports: `com.kakarote.ai_crm.entity.BO.UserAddBO`, `com.kakarote.ai_crm.entity.BO.UserUpdateBO`, `static org.mockito.Mockito.verify`, `static org.mockito.Mockito.never`, `static org.mockito.ArgumentMatchers.argThat`):

```java
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
        // draft consumed
        assertThat(store.get(SESSION_ID)).isNull();
    }

    @Test
    void confirmSkipsErrorRows() {
        stubExistingUsers(List.of());
        tools.previewEmployeeImport("[{\"realname\":\"\",\"username\":\"\"}]");

        String result = tools.confirmEmployeeImport();

        assertThat(result).contains("新建 0 人", "跳过 1 行");
        verify(manageUserService, never()).addUser(any());
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
```

- [ ] **Step 3: Run all EmployeeTools tests**

Run: `mvn test -Dtest=EmployeeToolsTest`
Expected: PASS (7 tests total).

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/ai/tools/EmployeeTools.java \
        backend/src/test/java/com/kakarote/ai_crm/ai/tools/EmployeeToolsTest.java
git commit -m "feat: add confirm/cancel employee import AI tools"
```

---

## Task 4: Wire the tool group into the address book chat application

**Files:**
- Modify: `backend/src/main/java/com/kakarote/ai_crm/ai/app/ChatApplicationRegistry.java`

- [ ] **Step 1: Add the tool-group constant**

In `ChatApplicationRegistry`, after the existing `TOOL_GROUP_CRM_NOOP` constant (around line 25), add:

```java
    public static final String TOOL_GROUP_EMPLOYEE = "employee";
```

- [ ] **Step 2: Attach the group to ADDRESS_BOOK and extend the prompt**

Replace the `ADDRESS_BOOK` registration block (currently lines ~105-120) with:

```java
        register(new ChatApplicationDefinition(
                ChatApplicationCodes.ADDRESS_BOOK,
                "通讯录",
                "customer",
                "围绕企业员工做任务安排、日程记录、附件归档和知识库检索，并支持上传名单批量导入员工。",
                """
                当前应用是通讯录员工对象助手。你是在围绕当前员工做工作安排、任务记录、日程记录和附件归档，不是在给员工发送即时消息。
                当用户说“他/她/这个员工/该员工”且当前会话绑定了员工时，默认指当前员工。
                没有具体执行时间点、只有截止或待办语义时创建任务；出现具体执行时间点时创建日程。
                创建任务时默认负责人是当前员工；创建日程时默认把当前员工加入参与人。
                当用户上传包含多名员工信息的文件（表格、文档、图片等）并希望批量添加员工到通讯录时：
                先从文件内容中识别出员工名单，调用 previewEmployeeImport（传入识别到的员工 JSON 数组）生成预览，
                把解析出的名单展示给用户并询问是否确认导入；用户确认后调用 confirmEmployeeImport，用户放弃则调用 cancelEmployeeImport。
                如果文件中没有可识别的员工信息，请如实说明，不要调用导入工具。未填密码的员工将使用默认密码 123456。
                只有在工具结果确认成功后，才能说数据已创建、更新或关联成功。
                """,
                false,
                List.of(TOOL_GROUP_TASK_SCHEDULE, TOOL_GROUP_KNOWLEDGE, TOOL_GROUP_EMPLOYEE),
                List.of("上传名单帮我批量添加员工", "明天下午让他完成客户资料整理", "下周一上午和他开项目复盘会", "总结这个员工最近的任务和附件")
        ));
```

- [ ] **Step 3: Verify it compiles**

Run: `mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/ai/app/ChatApplicationRegistry.java
git commit -m "feat: expose employee import tools in address book chat app"
```

---

## Task 5: Register EmployeeTools in DynamicChatClientProvider

**Files:**
- Modify: `backend/src/main/java/com/kakarote/ai_crm/ai/DynamicChatClientProvider.java`

- [ ] **Step 1: Add the import and the injected field**

Add the import alongside the other tool imports (near `import com.kakarote.ai_crm.ai.tools.TaskTools;`):

```java
import com.kakarote.ai_crm.ai.tools.EmployeeTools;
```

Add the field alongside the other `@Autowired` tool fields (e.g. after the `relationTools` field around line 104):

```java
    @Autowired
    private EmployeeTools employeeTools;
```

- [ ] **Step 2: Add the switch case**

In `addToolsForGroup`, add a case alongside the others (e.g. after the `TOOL_GROUP_CRM_NOOP` case):

```java
            case ChatApplicationRegistry.TOOL_GROUP_EMPLOYEE -> addTool(tools, employeeTools);
```

- [ ] **Step 3: Verify it compiles**

Run: `mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/ai/DynamicChatClientProvider.java
git commit -m "feat: register EmployeeTools for the employee tool group"
```

---

## Task 6: Full build + manual smoke test

**Files:** none (verification only)

- [ ] **Step 1: Run the new unit tests together**

Run: `mvn test -Dtest=EmployeeToolsTest,PendingEmployeeImportStoreTest`
Expected: PASS (11 tests).

- [ ] **Step 2: Compile the whole backend**

Run: `mvn -q -DskipTests package`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Manual smoke test against the running deployment**

The local stack is already running via `docker/docker-compose.yaml` (the `crm` container runs a published image, so to test new backend code either run `mvn spring-boot:run` from `backend/` against the same Postgres/Redis, or rebuild the crm image — for a quick check, run the backend locally on 8088 with the docker Postgres/Redis reachable).

In the UI (http://localhost), open a chat, switch the application (the `+` / 悟空技能 selector) to **通讯录**, upload a small file containing 2-3 fake employees (a `.xlsx`, `.csv`, or even a screenshot), and send "把这些人加到通讯录". Verify:
- The assistant lists the parsed employees with 新建/覆盖更新/错误 markers and asks for confirmation.
- Replying "确认导入" creates the users; check 设置 → 团队管理 for the new rows (default password 123456).
- Re-running the same file shows them as 覆盖更新.

- [ ] **Step 4: Final commit (if any manual-test fixups were needed)**

```bash
git add -A
git commit -m "chore: address book AI import manual-test fixups"
```

---

## Self-Review Notes

- **Spec coverage:** file-type-agnostic recognition (existing attachment pipeline, Task 6 smoke test) ✓; preview→confirm→cancel (Tasks 2-3) ✓; overwrite-update on duplicate username (Task 3 confirm) ✓; default password 123456 (Task 3) ✓; department by name with unresolved note (Task 2) ✓; roles/parent excluded (not in `UserAddBO` mapping) ✓; no frontend changes ✓; tests for parsing/validation/dedupe/overwrite/default-password/dept/summary + store TTL & isolation (Tasks 1-3) ✓.
- **Type consistency:** `previewEmployeeImport(String)`, `confirmEmployeeImport()`, `cancelEmployeeImport()`, `PendingEmployeeImportStore.save/get/remove/clear`, `EmployeeImportRow` getters, `EmployeeStatusEnum.getValue()/normalize()/getName()`, `ManageUserService.addUser(UserAddBO)/updateUser(UserUpdateBO)/lambdaQuery()`, `IManagerDeptService.list()`, `ManagerDept.getDeptId()/getDeptName()`, `ManagerUser.getUserId()/getUsername()` — all verified against the codebase.
- **Known stub fragility:** `EmployeeToolsTest.stubExistingUsers` mocks the MyBatis-Plus `lambdaQuery()` chain. If `in(...)` overload resolution makes the stub brittle, switch the production dedupe lookup and the test to a dedicated service method `List<ManagerUser> findByUsernames(Collection<String>)` on `ManageUserService` (mock that single method instead). The behavior is unchanged.
