package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.DeptAddBO;
import com.kakarote.ai_crm.entity.BO.DeptUpdateBO;
import com.kakarote.ai_crm.entity.VO.DeptVO;
import com.kakarote.ai_crm.service.IManagerDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/managerDept")
@RestController
@Tag(name = "Department APIs")
public class ManagerDeptController {

    @Autowired
    private IManagerDeptService managerDeptService;

    @PostMapping("/queryDeptTree")
    @Operation(summary = "Query department tree")
    @RequirePermission("dept")
    public Result<List<DeptVO>> queryDeptTree() {
        return Result.ok(managerDeptService.queryDeptTree());
    }

    @PostMapping("/add")
    @Operation(summary = "Add department")
    @RequirePermission("dept:create")
    public Result<String> add(@RequestBody DeptAddBO deptAddBO) {
        managerDeptService.addDept(deptAddBO);
        return Result.ok();
    }

    @PostMapping("/update")
    @Operation(summary = "Update department")
    @RequirePermission("dept:edit")
    public Result<String> update(@RequestBody DeptUpdateBO deptUpdateBO) {
        managerDeptService.updateDept(deptUpdateBO);
        return Result.ok();
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete department")
    @RequirePermission("dept:delete")
    public Result<String> delete(@Parameter(description = "Department ID") @RequestParam("deptId") Long deptId) {
        managerDeptService.deleteDept(deptId);
        return Result.ok();
    }
}
