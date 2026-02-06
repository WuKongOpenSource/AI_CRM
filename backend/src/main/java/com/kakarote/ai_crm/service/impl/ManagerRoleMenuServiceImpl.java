package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.Const;
import com.kakarote.ai_crm.entity.PO.ManagerRoleMenu;
import com.kakarote.ai_crm.mapper.ManagerRoleMenuMapper;
import com.kakarote.ai_crm.service.IManagerRoleMenuService;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 角色菜单对应关系表 服务实现类
 * </p>
 *
 * @author guomenghao
 * @since 2023-02-15
 */
@Service
public class ManagerRoleMenuServiceImpl extends ServiceImpl<ManagerRoleMenuMapper, ManagerRoleMenu> implements IManagerRoleMenuService {

    /**
     * 保存角色枚举
     *
     * @param roleId     :角色id
     * @param menuIdList :menuIdList
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleMenu(Long roleId, List<Long> menuIdList) {
        List<ManagerRoleMenu> roleMenuList = new ArrayList<>();
        menuIdList.forEach(menuId -> {
            ManagerRoleMenu roleMenu = new ManagerRoleMenu();
            roleMenu.setMenuId(menuId);
            roleMenu.setRoleId(roleId);
            roleMenu.setCreateUserId(UserUtil.getUserId());
            roleMenu.setCreateTime(LocalDateTime.now());
            roleMenuList.add(roleMenu);
        });
        saveBatch(roleMenuList, Const.BATCH_SAVE_SIZE);
    }

    /**
     * 通过角色查询角色菜单对应关系
     *
     * @param roleId 角色ID
     * @return
     */
    @Override
    public List<ManagerRoleMenu> queryRoleMenuByRoleId(Long roleId) {
        return baseMapper.queryRoleMenuByRoleId(roleId);
    }

    /**
     * 通过角色ID查询菜单列表
     *
     * @param id
     * @return
     */
    @Override
    public List<Long> queryMenuIdListByRoleId(Long id) {
        return baseMapper.queryMenuIdListByRoleId(id);
    }

}
