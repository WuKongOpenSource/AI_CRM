package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.PO.ManagerMenu;
import com.kakarote.ai_crm.entity.VO.MenuVO;

import java.util.List;

/**
 * <p>
 * 菜单表 服务类
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
public interface IManagerMenuService extends IService<ManagerMenu> {

    /**
     * 查询菜单列表
     * @return 菜单列表
     */
    List<MenuVO> queryAllMenuList();
    /**
     *查询用户所拥有的菜单权限
     * @param userId
     * @return list 菜单列表
     */
    List<ManagerMenu> queryMenuList(Long userId);
}
