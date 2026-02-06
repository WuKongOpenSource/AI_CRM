package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ManagerMenu;
import com.kakarote.ai_crm.entity.VO.MenuVO;

import java.util.List;

/**
 * <p>
 * 菜单表 Mapper 接口
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
public interface ManagerMenuMapper extends BaseMapper<ManagerMenu> {
    /**
     * 查询全部菜单
     * @return 菜单列表
     */
    List<MenuVO> queryAllMenuList();
    /**
     * 查询用户所拥有的菜单权限
     * @param userId
     * @return list 菜单列表
     */
    List<ManagerMenu> queryMenuList(Long userId);
}
