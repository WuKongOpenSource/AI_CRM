package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.UserQueryBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户表
 *
 * @author hmb
 * @date 2020-12-16 13:51:10
 */
@Mapper
public interface ManageUserMapper extends BaseMapper<ManagerUser> {
    /**
     * 通过条件查询员工
     *
     * @param userQueryBO queryBo
     * @return userList 员工列表
     */
    BasePage<ManagerUser> queryPageList(BasePage<ManagerUser> parse, @Param("data") UserQueryBO userQueryBO);

    /**
     * 通过用户ID查询员工
     * @param userId 用户ID
     * @return
     */
    ManagerUser getUserId(@Param("userId") Long userId);

    /**
     * 通过用户名查询员工
     * @param username 用户名
     * @return ManageUserEntity 员工
     */
    ManagerUser queryUserByUsername(String username);
}
