package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ResetUsernameBO;
import com.kakarote.ai_crm.entity.BO.UserPreferenceUpdateBO;
import com.kakarote.ai_crm.entity.BO.UserAddBO;
import com.kakarote.ai_crm.entity.BO.UserQueryBO;
import com.kakarote.ai_crm.entity.BO.UserStatusBO;
import com.kakarote.ai_crm.entity.BO.UserUpdateBO;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.entity.VO.UserPreferenceVO;
import com.kakarote.ai_crm.service.ManageUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/managerUser")
@RestController
@Tag(name = "用户管理")
public class ManagerUserController {

    @Autowired
    private ManageUserService manageUserService;

    @PostMapping("/addUser")
    @Operation(summary = "新增用户")
    @RequirePermission("user:create")
    public Result<String> addUser(@RequestBody UserAddBO userAddBO) {
        manageUserService.addUser(userAddBO);
        return Result.ok();
    }

    @PostMapping("/queryLoginUser")
    @Operation(summary = "查询当前登录用户")
    public Result<ManageUserVO> queryLoginUser() {
        return Result.ok(manageUserService.queryLoginUser());
    }

    @PostMapping("/preferences")
    @Operation(summary = "更新当前用户偏好")
    public Result<UserPreferenceVO> updateCurrentUserPreferences(@RequestBody UserPreferenceUpdateBO preferenceUpdateBO) {
        return Result.ok(manageUserService.updateCurrentUserPreferences(preferenceUpdateBO));
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "分页查询用户")
    public Result<BasePage<ManageUserVO>> queryPageList(@RequestBody UserQueryBO userQueryBO) {
        return Result.ok(manageUserService.queryPageList(userQueryBO));
    }

    @PostMapping("/updateUser")
    @Operation(summary = "更新用户基本信息")
    public Result<String> updateUser(@RequestBody UserUpdateBO updateBO) {
        manageUserService.updateUser(updateBO);
        return Result.ok();
    }

    @PostMapping("/resetUsername")
    @Operation(summary = "重置用户名")
    @RequirePermission("user:edit")
    public Result<String> resetUsername(@RequestBody ResetUsernameBO resetUsernameBO) {
        manageUserService.resetUsername(resetUsernameBO);
        return Result.ok();
    }

    @PostMapping("/updatePassword")
    @Operation(summary = "更新密码")
    public Result<String> updatePassword(@Parameter(name = "oldPassword", description = "旧密码") @RequestParam("oldPassword") String oldPassword,
                                         @Parameter(name = "newPassword", description = "新密码") @RequestParam("newPassword") String newPassword) {
        manageUserService.updatePassword(oldPassword, newPassword);
        return Result.ok();
    }

    @PostMapping("/setUserStatus")
    @Operation(summary = "设置用户状态")
    @RequirePermission("user:status")
    public Result setUserStatus(@RequestBody UserStatusBO userStatusBO) {
        manageUserService.setUserStatus(userStatusBO);
        return Result.ok();
    }

    @PostMapping("/deleteByIds")
    @Operation(summary = "删除用户")
    @RequirePermission("user:delete")
    public Result deleteByIds(@RequestBody Long[] userIds) {
        manageUserService.deleteByIds(userIds);
        return Result.ok();
    }
}
