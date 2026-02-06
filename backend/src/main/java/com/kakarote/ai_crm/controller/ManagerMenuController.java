package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.VO.MenuVO;
import com.kakarote.ai_crm.service.IManagerMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 菜单表 前端控制器
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
@RestController
@RequestMapping("/managerMenu")
@Tag(name = "菜单相关接口")
public class ManagerMenuController {

    @Autowired
    private IManagerMenuService managerMenuService;

    @PostMapping("/queryAllMenuList")
    @Operation(summary = "查询菜单列表")
    public Result<List<MenuVO>> queryAllMenuList(){
        return Result.ok(managerMenuService.queryAllMenuList());

    }
}
