package com.kakarote.ai_crm.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.UserAddBO;
import com.kakarote.ai_crm.entity.BO.UserQueryBO;
import com.kakarote.ai_crm.entity.BO.UserStatusBO;
import com.kakarote.ai_crm.entity.BO.UserUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.service.IManagerUserRoleService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;


/**
 * 系统用户表 实现类
 *
 * @author zhangzhiwei
 */
@Service("manageUserService")
public class ManageUserServiceImpl extends ServiceImpl<ManageUserMapper, ManagerUser> implements ManageUserService {

    @Autowired
    private IManagerUserRoleService userRoleService;


    @Override
    public ManagerUser queryUserByUsername(String username) {
        ManagerUser managerUser =  baseMapper.queryUserByUsername(username);
        if (ObjectUtil.isNull(managerUser)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
        }
        return managerUser;
    }

    /**
     * 添加用户
     *
     * @param userAddBO data
     */
    @Override
    public void addUser(UserAddBO userAddBO) {
        ManagerUser manageUser = null;
        //判断用户名是否存在
        if (StrUtil.isNotEmpty(userAddBO.getUsername())) {
            Optional<ManagerUser> userOptional = lambdaQuery().eq(ManagerUser::getUsername, userAddBO.getUsername()).oneOpt();
            if (userOptional.isPresent()) {
                manageUser = userOptional.get();
            }
        }
        //用户不存在
        if (manageUser == null) {
            if (StrUtil.isNotEmpty(userAddBO.getPassword())){
                BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                String password =bCryptPasswordEncoder.encode(userAddBO.getPassword());
                userAddBO.setPassword(password);
            }
            manageUser = BeanUtil.copyProperties(userAddBO, ManagerUser.class);
            manageUser.setStatus(1);
            manageUser.setCreateTime(new Date());
            save(manageUser);
        }
    }

    /**
     * 通过条件查询员工
     *
     * @param userQueryBO queryBo
     * @return userList 员工列表
     */
    @Override
    public BasePage<ManagerUser> queryPageList(UserQueryBO userQueryBO) {
      return baseMapper.queryPageList(userQueryBO.parse(),userQueryBO);
    }

    /**
     * 修改用户
     *
     * @param updateBO data
     */
    @Override
    public void updateUser(UserUpdateBO updateBO) {
        ManagerUser userEntity = baseMapper.getUserId(updateBO.getUserId());
        if (ObjectUtil.isNotNull(userEntity)){
            if (ObjectUtil.isNotNull(updateBO.getMobile())){
                userEntity.setMobile(updateBO.getMobile());
            }
            if (ObjectUtil.isNotNull(updateBO.getRealname())){
                userEntity.setRealname(updateBO.getRealname());
            }
            if(StrUtil.isNotBlank(updateBO.getPassword())){
                BCryptPasswordEncoder bCryptPasswordEncoder=new BCryptPasswordEncoder();
                String passWord = bCryptPasswordEncoder.encode(updateBO.getPassword());
                userEntity.setPassword(passWord);
            }
            updateById(userEntity);
        }
    }

    /**
     * 设置用户状态
     *
     * @param userStatusBO data
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserStatus(UserStatusBO userStatusBO) {
        List<ManagerUser> managerUserList = lambdaQuery().in(ManagerUser::getUserId, userStatusBO.getIds()).list();
        managerUserList.forEach(manageUserEntity -> manageUserEntity.setStatus(userStatusBO.getStatus()));
        updateBatchById(managerUserList);
    }

    /**
     * 查询登录用户
     */
    @Override
    public ManageUserVO queryLoginUser() {
        ManagerUser userEntity = baseMapper.getUserId(UserUtil.getUserId());
        if (ObjectUtil.isNotNull(userEntity)) {
            return BeanUtil.copyProperties(userEntity, ManageUserVO.class);
        }
        return null;
    }

    /**
     * 修改密码
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Override
    public void updatePassword(String oldPassword, String newPassword) {
        BCryptPasswordEncoder bCryptPasswordEncoder=new BCryptPasswordEncoder();
        ManagerUser userEntity = baseMapper.getUserId(UserUtil.getUserId());
        boolean matches = bCryptPasswordEncoder.matches(oldPassword, userEntity.getPassword());
        if (!matches){
           throw  new BusinessException(SystemCodeEnum.SYSTEM_OLD_PASSWORD_ERROR);
        }
        String passWord = bCryptPasswordEncoder.encode(newPassword);
        userEntity.setPassword(passWord);
        updateById(userEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(Long[] userIds) {
        List<Long> userIdList = Arrays.asList(userIds);
        removeByIds(userIdList);
        List<ManagerUserRole> list = userRoleService.lambdaQuery().in(ManagerUserRole::getUserId, userIdList).list();
        if(CollUtil.isNotEmpty(list)){
            userRoleService.removeByIds(list);
        }
    }
}
