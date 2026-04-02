package com.kakarote.ai_crm.controller;

import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.RelatedMenuBO;
import com.kakarote.ai_crm.entity.BO.RoleBO;
import com.kakarote.ai_crm.entity.BO.RolePermissionSaveBO;
import com.kakarote.ai_crm.entity.BO.RoleQueryBO;
import com.kakarote.ai_crm.entity.BO.SetRoleBO;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.entity.VO.RolePermissionVO;
import com.kakarote.ai_crm.entity.VO.RoleVO;
import com.kakarote.ai_crm.service.IManagerRoleService;
import com.kakarote.ai_crm.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;

@RestController
@RequestMapping("/managerRole")
@Tag(name = "Role APIs")
public class ManagerRoleController {

    @Autowired
    private IManagerRoleService managerRoleService;

    @PostMapping("/queryById/{id}")
    @Operation(summary = "Query role by id")
    @RequirePermission("role")
    public Result<ManagerRole> queryById(@PathVariable("id") @Parameter(name = "id", description = "Role ID") Serializable id) {
        ManagerRole entity = managerRoleService.queryById(id);
        return Result.ok(entity);
    }

    @PostMapping("/add")
    @Operation(summary = "Create role")
    @RequirePermission("role:create")
    public Result add(@RequestBody SetRoleBO setRoleBO) {
        managerRoleService.addOrUpdate(setRoleBO);
        return Result.ok();
    }

    @PostMapping("/update")
    @Operation(summary = "Update role")
    @RequirePermission("role:edit")
    public Result update(@RequestBody SetRoleBO setRoleBO) {
        managerRoleService.addOrUpdate(setRoleBO);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "Query role page")
    @RequirePermission("role")
    public Result<BasePage<ManagerRole>> queryPageList(@RequestBody RoleQueryBO roleQueryBO) {
        return Result.ok(managerRoleService.queryPageList(roleQueryBO));
    }

    @PostMapping("/relatedUser")
    @Operation(summary = "Bind users to role")
    @RequirePermission("role:user")
    public Result relatedUser(@RequestBody RoleBO idsRoleBO) {
        managerRoleService.relatedUser(idsRoleBO.getUserIds(), idsRoleBO.getRoleIds());
        return Result.ok();
    }

    @PostMapping("/unbindingUser")
    @Operation(summary = "Unbind user from role")
    @RequirePermission("role:user")
    public Result unbindingUser(@RequestParam("userId") Long userId, @RequestParam("roleId") Long roleId) {
        managerRoleService.unbindingUser(userId, roleId);
        return Result.ok();
    }

    @PostMapping("/getAllRoleList")
    @Operation(summary = "Query all roles")
    @RequirePermission("role")
    public Result<List<ManagerRole>> getAllRoleList() {
        List<ManagerRole> allRoleList = managerRoleService.getAllRoleList();
        return Result.ok(allRoleList);
    }

    @PostMapping("/auth")
    @Operation(summary = "Query current user role auth tree")
    public Result<JSONObject> auth() {
        JSONObject object = managerRoleService.auth(UserUtil.getUserId());
        return Result.ok(object);
    }

    @PostMapping("/relatedMenu")
    @Operation(summary = "Bind menus to role")
    @RequirePermission("role:permission")
    public Result relatedMenu(@RequestBody RelatedMenuBO relatedMenuBO) {
        managerRoleService.relatedMenu(relatedMenuBO.getRoleId(), relatedMenuBO.getMenuIds());
        return Result.ok();
    }

    @PostMapping("/queryMenuIdList/{id}")
    @Operation(summary = "Query role menu ids")
    @RequirePermission("role:permission")
    public Result<List<Long>> queryMenuIdList(@PathVariable("id") @Parameter(name = "id", description = "Role ID") Long id) {
        return Result.ok(managerRoleService.queryMenuIdList(id));
    }

    @PostMapping("/delete")
    @Operation(summary = "Delete role")
    @RequirePermission("role:delete")
    public Result delete(@RequestBody List<Long> ids) {
        managerRoleService.deleteByIds(ids.stream().map(id -> (Serializable) id).collect(java.util.stream.Collectors.toList()));
        return Result.ok();
    }

    @PostMapping("/queryRoleListWithUserCount")
    @Operation(summary = "Query role list with user count")
    @RequirePermission("role")
    public Result<List<RoleVO>> queryRoleListWithUserCount(@RequestBody(required = false) RoleQueryBO query) {
        String search = query != null ? query.getSearch() : null;
        return Result.ok(managerRoleService.queryRoleListWithUserCount(search));
    }

    @PostMapping("/queryPermissions/{roleId}")
    @Operation(summary = "Query role permissions")
    @RequirePermission("role:permission")
    public Result<List<RolePermissionVO>> queryPermissions(@PathVariable("roleId") Long roleId) {
        return Result.ok(managerRoleService.queryRolePermissions(roleId));
    }

    @PostMapping("/savePermissions")
    @Operation(summary = "Save role permissions")
    @RequirePermission("role:permission")
    public Result savePermissions(@RequestBody RolePermissionSaveBO bo) {
        managerRoleService.saveRolePermissions(bo);
        return Result.ok();
    }
}
