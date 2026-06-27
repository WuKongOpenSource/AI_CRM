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
import com.kakarote.ai_crm.entity.BO.UserUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.IManagerDeptService;
import com.kakarote.ai_crm.service.ManageUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    @Tool(description = "为通讯录新增/批量导入员工生成预览。这是预览待导入员工的唯一方式：当用户以任意形式提供员工名单（上传文件、粘贴文本，或直接口述一个或多个员工）并想加入通讯录时，都必须调用本工具，禁止自己整理或直接输出名单当作预览。"
            + "参数 employeesJson 是识别出的员工 JSON 数组字符串，每个元素字段："
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

        try {
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
                Map<String, Long> existing = manageUserService.findUsersByUsernames(usernames).stream()
                        .collect(Collectors.toMap(
                                ManagerUser::getUsername,
                                ManagerUser::getUserId,
                                (a, b) -> a));
                for (EmployeeImportRow row : rows) {
                    Long id = existing.get(row.getUser().getUsername());
                    if (id != null && !row.hasError()) {
                        row.setDuplicate(true);
                        row.setExistingUserId(id);
                    }
                }
            }

            pendingEmployeeImportStore.save(sessionId, rows);
            return buildPreview(rows);
        } catch (Exception e) {
            log.error("【Tool调用】previewEmployeeImport 失败: {}", e.getMessage(), e);
            return "员工名单预览失败：" + e.getMessage();
        }
    }

    @Tool(description = "确认执行已预览的员工批量导入。仅当之前调用过 previewEmployeeImport 且用户明确表示\"确认导入/继续/可以\"时调用。"
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
                // 该行已通过校验（用户名非空），异常来自建号/更新调用，用用户名标识更有意义
                skipReasons.add(StrUtil.blankToDefault(u.getUsername(), "(无用户名)") + "：" + e.getMessage());
                log.warn("批量导入员工失败 username={}", u.getUsername(), e);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("导入完成：新建 ").append(created).append(" 人，更新 ").append(updated)
          .append(" 人，跳过 ").append(skipped).append(" 行。");
        if (created > 0) {
            sb.append("\n新建员工的默认登录密码为 123456，请提醒他们首次登录后修改。");
        }
        if (!skipReasons.isEmpty()) {
            sb.append("\n跳过明细：\n").append(String.join("\n", skipReasons));
        }
        return sb.toString();
    }

    @Tool(description = "取消尚未确认的员工批量导入。当用户在预览后表示\"不导入了/取消/算了\"时调用。")
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
