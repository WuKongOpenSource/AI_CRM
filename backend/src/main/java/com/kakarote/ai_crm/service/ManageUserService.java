package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.ResetUsernameBO;
import com.kakarote.ai_crm.entity.BO.UserAddBO;
import com.kakarote.ai_crm.entity.BO.UserPreferenceUpdateBO;
import com.kakarote.ai_crm.entity.BO.UserQueryBO;
import com.kakarote.ai_crm.entity.BO.UserStatusBO;
import com.kakarote.ai_crm.entity.BO.UserUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.entity.VO.UserPreferenceVO;

import java.util.Collection;
import java.util.List;


/**
 * 用户表
 *
 * @author hmb
 * @date 2020-12-16 13:51:10
 */
public interface ManageUserService extends IService<ManagerUser> {

    /**
     * 通过用户名查询用户
     * @param username 用户名
     * @return user
     */
    ManagerUser queryUserByUsername(String username);

    /**
     * 通过用户名查询所有员工。
     */
    List<ManagerUser> queryUsersByUsername(String username);

    /**
     * 批量按用户名查询员工（供 AI 批量导入查重使用）。
     * @param usernames 用户名集合
     * @return 已存在的用户列表
     */
    List<ManagerUser> findUsersByUsernames(Collection<String> usernames);

    /**
     * 添加用户
     *
     * @param userAddBO data
     */
    void addUser(UserAddBO userAddBO);

    /**
     * 通过条件查询员工
     *
     * @param userQueryBO queryBo
     * @return userList 员工列表
     */
    BasePage<ManageUserVO> queryPageList(UserQueryBO userQueryBO);

    /**
     * 修改用户
     * @param updateBO data
     */
    void updateUser(UserUpdateBO updateBO);

    /**
     * 重置用户名
     * @param resetUsernameBO data
     */
    void resetUsername(ResetUsernameBO resetUsernameBO);

    /**
     * 设置用户状态
     * @param userStatusBO data
     */
    void setUserStatus(UserStatusBO userStatusBO);

    /**
     * 删除用户
     */
    void deleteByIds(Long[] userIds);

    /**
     * 查询登录用户
     * @return
     */
    ManageUserVO queryLoginUser();

    /**
     * 修改密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void updatePassword(String oldPassword, String newPassword);

    /**
     * 更新当前登录用户 UI 偏好。
     * @param preferenceUpdateBO 偏好数据
     * @return 规范化后的偏好
     */
    UserPreferenceVO updateCurrentUserPreferences(UserPreferenceUpdateBO preferenceUpdateBO);
}
