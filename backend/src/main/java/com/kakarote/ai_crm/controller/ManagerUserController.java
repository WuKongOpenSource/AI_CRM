package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.UserAddBO;
import com.kakarote.ai_crm.entity.BO.UserQueryBO;
import com.kakarote.ai_crm.entity.BO.UserStatusBO;
import com.kakarote.ai_crm.entity.BO.UserUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.service.ManageUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/managerUser")
@RestController
@Tag(name = "用户信息控制器")
public class ManagerUserController {

    @Autowired
    private ManageUserService manageUserService;

    @PostMapping("/addUser")
    @Operation(summary = "添加用户")
    public Result<String> addUser(@RequestBody UserAddBO userAddBO) {
        manageUserService.addUser(userAddBO);
        return Result.ok();
    }

    @PostMapping("/queryLoginUser")
    @Operation(summary = "查询当前登录用户")
    public Result<ManageUserVO> queryLoginUser() {
        return Result.ok(manageUserService.queryLoginUser());
    }

    @PostMapping("/queryPageList")
    @Operation(summary = "查询列表页数据")
    public Result<BasePage<ManagerUser>> queryPageList(@RequestBody UserQueryBO userQueryBO) {
        return Result.ok(manageUserService.queryPageList(userQueryBO));
    }

    @PostMapping("/updateUser")
    @Operation(summary = "修改用户基本信息")
    public Result<String> updateUser(@RequestBody UserUpdateBO updateBO) {
        manageUserService.updateUser(updateBO);
        return Result.ok();
    }

    @PostMapping("/updatePassword")
    @Operation(summary = "修改密码")
    public Result<String> updatePassword(@Parameter(name = "oldPassword", description = "旧密码") @RequestParam("oldPassword") String oldPassword,
                                         @Parameter(name = "newPassword", description = "新密码") @RequestParam("newPassword") String newPassword) {
        manageUserService.updatePassword(oldPassword, newPassword);
        return Result.ok();
    }

    @PostMapping("/setUserStatus")
    @Operation(summary = "设置用户状态")
    public Result setUserStatus(@RequestBody UserStatusBO userStatusBO) {
        manageUserService.setUserStatus(userStatusBO);
        return Result.ok();
    }

    @PostMapping("/deleteByIds")
    @Operation(summary = "删除用户")
    public Result deleteByIds(@RequestBody Long[] userIds) {
        manageUserService.deleteByIds(userIds);
        return Result.ok();
    }
}
