package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.entity.PO.ManagerMenu;
import com.kakarote.ai_crm.entity.VO.MenuVO;
import com.kakarote.ai_crm.mapper.ManagerMenuMapper;
import com.kakarote.ai_crm.service.IManagerMenuService;
import com.kakarote.ai_crm.utils.RecursionUtil;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
@Service
public class ManagerMenuServiceImpl extends ServiceImpl<ManagerMenuMapper, ManagerMenu> implements IManagerMenuService {

    /**
     * 查询菜单列表
     *
     * @return 菜单列表
     */
    @Override
    public List<MenuVO> queryAllMenuList() {
        List<MenuVO> menuVOList = getBaseMapper().queryAllMenuList();
        return RecursionUtil.getChildListTree(menuVOList, "parentId", 0L, "menuId", "children", MenuVO.class);
    }

    /**
     * 查询用户所拥有的菜单权限
     *
     * @param userId
     * @return list 菜单列表
     */
    @Override
    public List<ManagerMenu> queryMenuList(Long userId) {
        if(ObjectUtil.equals(userId, UserUtil.getSuperUserId())){
            return lambdaQuery().list();
        }
        return getBaseMapper().queryMenuList(userId);
    }
}
