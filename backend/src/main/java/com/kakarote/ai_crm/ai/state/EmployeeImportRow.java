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
