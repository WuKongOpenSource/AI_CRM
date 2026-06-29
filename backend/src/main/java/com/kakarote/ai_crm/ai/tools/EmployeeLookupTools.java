package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.UserQueryBO;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.service.IManagerDeptService;
import com.kakarote.ai_crm.service.ManageUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通讯录员工只读查询 AI Tool。
 * 用于把人名解析成用户ID（例如给项目/任务指派负责人前先查到对应的用户ID），不做任何写操作。
 */
@Slf4j
@Component
public class EmployeeLookupTools {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private IManagerDeptService deptService;

    @Tool(description = "按姓名或用户名模糊搜索通讯录中的员工，用于把人名解析成用户ID（例如给项目/任务指派负责人前先查到对应的用户ID）。"
            + "返回匹配到的员工列表，含用户ID、姓名、用户名、部门、职位。"
            + "当你需要根据人名确定负责人/成员的用户ID时必须调用本工具，不要凭空编造用户ID；"
            + "若关键词较短可能命中多人，请把候选列表给用户确认后再使用对应用户ID。")
    @AiToolPermission(value = "addressBook:list", action = "搜索通讯录员工")
    public String searchEmployees(
            @ToolParam(description = "搜索关键词，匹配员工姓名或用户名（模糊匹配）") String keyword,
            @ToolParam(description = "返回数量，默认10，最多20", required = false) String limit) {
        String kw = StrUtil.trim(keyword);
        if (StrUtil.isBlank(kw)) {
            return "请提供要搜索的员工姓名或关键词。";
        }

        int resolvedLimit = DEFAULT_LIMIT;
        if (StrUtil.isNotBlank(limit)) {
            try {
                resolvedLimit = Math.min(MAX_LIMIT, Math.max(1, Integer.parseInt(limit.trim())));
            } catch (NumberFormatException ignored) {
                // 解析失败时沿用默认值
            }
        }

        UserQueryBO query = new UserQueryBO();
        query.setSearch(kw);
        query.setPage(1);
        query.setLimit(resolvedLimit);

        BasePage<ManageUserVO> page;
        try {
            page = manageUserService.queryPageList(query);
        } catch (Exception e) {
            log.error("【Tool调用】searchEmployees 失败: {}", e.getMessage(), e);
            return "搜索员工失败：" + e.getMessage();
        }

        List<ManageUserVO> records = page == null ? List.of() : page.getRecords();
        if (records == null || records.isEmpty()) {
            return "通讯录中没有匹配「" + kw + "」的员工。请确认姓名，或换一个关键词再试。";
        }

        Map<Long, String> deptNames = deptService.list().stream()
                .filter(d -> d.getDeptId() != null && StrUtil.isNotBlank(d.getDeptName()))
                .collect(Collectors.toMap(ManagerDept::getDeptId, ManagerDept::getDeptName, (a, b) -> a));

        StringBuilder sb = new StringBuilder();
        sb.append("匹配「").append(kw).append("」的员工共 ").append(records.size()).append(" 人：\n");
        int idx = 1;
        for (ManageUserVO u : records) {
            String dept = StrUtil.isNotBlank(u.getDeptName())
                    ? u.getDeptName()
                    : (u.getDeptId() != null ? deptNames.getOrDefault(u.getDeptId(), "") : "");
            sb.append(idx++).append(". 用户ID=").append(u.getUserId())
              .append(" 姓名=").append(StrUtil.blankToDefault(u.getRealname(), "-"))
              .append(" 用户名=").append(StrUtil.blankToDefault(u.getUsername(), "-"));
            if (StrUtil.isNotBlank(dept)) {
                sb.append(" 部门=").append(dept);
            }
            if (StrUtil.isNotBlank(u.getPost())) {
                sb.append(" 职位=").append(u.getPost());
            }
            sb.append("\n");
        }
        sb.append("\n请用上面的「用户ID」作为负责人ID（ownerIdStr）来指派；如有多个相近候选且无法确定，请先与用户确认。");
        return sb.toString();
    }
}
