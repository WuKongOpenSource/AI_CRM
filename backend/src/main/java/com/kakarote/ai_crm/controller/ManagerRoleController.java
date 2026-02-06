package com.kakarote.ai_crm.controller;

import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.RelatedMenuBO;
import com.kakarote.ai_crm.entity.BO.RoleBO;
import com.kakarote.ai_crm.entity.BO.RoleQueryBO;
import com.kakarote.ai_crm.entity.BO.SetRoleBO;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.service.IManagerRoleService;
import com.kakarote.ai_crm.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 角色表 前端控制器
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
@RestController
@RequestMapping("/managerRole")
@Tag(name = "角色相关接口")
public class ManagerRoleController {

    @Autowired
    private IManagerRoleService managerRoleService;

    @PostMapping("/queryById/{id}")
    @Operation(summary = "根据ID查询")
    public Result<ManagerRole> queryById(@PathVariable("id") @Parameter(name = "id", description = "id") Serializable id) {
        ManagerRole entity = managerRoleService.queryById(id);
        return Result.ok(entity);
    }

    @PostMapping("/add")
    @Operation(summary = "保存数据")
    public Result add(@RequestBody SetRoleBO setRoleBO) {
        managerRoleService.addOrUpdate(setRoleBO);
        return Result.ok();
    }

    @PostMapping("/update")
    @Operation(summary = "修改数据")
    public Result update(@RequestBody  SetRoleBO setRoleBO) {
        managerRoleService.addOrUpdate(setRoleBO);
        return Result.ok();
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "查询列表页数据")
    public Result<BasePage<ManagerRole>> queryPageList(@RequestBody RoleQueryBO roleQueryBO) {
        return Result.ok(managerRoleService.queryPageList(roleQueryBO));
    }
    @PostMapping("/relatedUser")
    @Operation(summary = "角色关联员工")
    public Result relatedUser(@RequestBody RoleBO idsRoleBO) {
        managerRoleService.relatedUser(idsRoleBO.getUserIds(), idsRoleBO.getRoleIds());
        return Result.ok();
    }

    @PostMapping("/unbindingUser")
    @Operation(summary = "取消角色关联员工")
    public Result unbindingUser(@RequestParam("userId") Long userId, @RequestParam("roleId") Long roleId) {
        managerRoleService.unbindingUser(userId, roleId);
        return Result.ok();
    }

    @PostMapping("/getAllRoleList")
    @Operation(summary = "全局角色查询")
    public Result<List<ManagerRole>> getAllRoleList() {
        List<ManagerRole> allRoleList = managerRoleService.getAllRoleList();
        return Result.ok(allRoleList);
    }

    @PostMapping("/auth")
    @Operation(summary = "角色权限")
    public Result<JSONObject> auth() {
        JSONObject object = managerRoleService.auth(UserUtil.getUserId());
        return Result.ok(object);
    }
    @PostMapping("/relatedMenu")
    @Operation(summary = "角色关联菜单")
    public Result relatedMenu(@RequestBody RelatedMenuBO relatedMenuBO) {
        managerRoleService.relatedMenu(relatedMenuBO.getRoleId(), relatedMenuBO.getMenuIds());
        return Result.ok();
    }

    @PostMapping("/queryMenuIdList/{id}")
    @Operation(summary = "通过角色ID查询菜单")
    public Result<List<Long>> queryMenuIdList(@PathVariable("id") @Parameter(name = "id", description = "id") Long id){
        return Result.ok(managerRoleService.queryMenuIdList(id));
    }

}
