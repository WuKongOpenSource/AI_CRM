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

import com.kakarote.ai_crm.common.Const;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.service.IManagerRoleService;

import com.kakarote.ai_crm.service.FileStorageService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 系统用户表 实现类
 *
 * @author zhangzhiwei
 */
@Service("manageUserService")
public class ManageUserServiceImpl extends ServiceImpl<ManageUserMapper, ManagerUser> implements ManageUserService {

    @Autowired
    private IManagerUserRoleService userRoleService;

    @Autowired
    private ManagerDeptMapper deptMapper;

    @Autowired
    private IManagerRoleService roleService;

    @Autowired
    private FileStorageService fileStorageService;


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
    @Transactional(rollbackFor = Exception.class)
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
            if (userAddBO.getStatus() != null) {
                manageUser.setStatus(userAddBO.getStatus());
            } else {
                manageUser.setStatus(1);
            }
            manageUser.setCreateTime(new Date());
            save(manageUser);
            // 关联角色
            if (CollUtil.isNotEmpty(userAddBO.getRoleIds())) {
                Long userId = manageUser.getUserId();
                List<ManagerUserRole> roles = userAddBO.getRoleIds().stream().map(roleId -> {
                    ManagerUserRole ur = new ManagerUserRole();
                    ur.setUserId(userId);
                    ur.setRoleId(roleId);
                    ur.setCreateUserId(UserUtil.getUserId());
                    ur.setCreateTime(LocalDateTime.now());
                    return ur;
                }).collect(Collectors.toList());
                userRoleService.saveBatch(roles, Const.BATCH_SAVE_SIZE);
            }
        }
    }

    /**
     * 通过条件查询员工
     *
     * @param userQueryBO queryBo
     * @return userList 员工列表
     */
    @Override
    public BasePage<ManageUserVO> queryPageList(UserQueryBO userQueryBO) {
        BasePage<ManageUserVO> page = baseMapper.queryPageList(userQueryBO.parse(), userQueryBO);
        fillRoleInfo(page.getRecords());
        fillImgUrl(page.getRecords());
        return page;
    }

    private void fillRoleInfo(List<ManageUserVO> users) {
        if (CollUtil.isEmpty(users)) return;
        List<Long> userIds = users.stream().map(ManageUserVO::getUserId).collect(Collectors.toList());
        // 查询所有用户的角色关联
        List<ManagerUserRole> userRoles = userRoleService.lambdaQuery()
                .in(ManagerUserRole::getUserId, userIds).list();
        if (CollUtil.isEmpty(userRoles)) {
            users.forEach(u -> { u.setRoleIds(Collections.emptyList()); u.setRoleNames(Collections.emptyList()); });
            return;
        }
        // 查询涉及的角色名称
        Set<Long> roleIdSet = userRoles.stream().map(ManagerUserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, String> roleNameMap = roleService.lambdaQuery()
                .in(ManagerRole::getRoleId, roleIdSet).list().stream()
                .collect(Collectors.toMap(ManagerRole::getRoleId, ManagerRole::getRoleName, (a, b) -> a));
        // 按用户分组
        Map<Long, List<ManagerUserRole>> userRoleMap = userRoles.stream()
                .collect(Collectors.groupingBy(ManagerUserRole::getUserId));
        for (ManageUserVO user : users) {
            List<ManagerUserRole> urs = userRoleMap.getOrDefault(user.getUserId(), Collections.emptyList());
            user.setRoleIds(urs.stream().map(ManagerUserRole::getRoleId).collect(Collectors.toList()));
            user.setRoleNames(urs.stream().map(ur -> roleNameMap.getOrDefault(ur.getRoleId(), "")).filter(StrUtil::isNotEmpty).collect(Collectors.toList()));
        }
    }

    /**
     * 修改用户
     *
     * @param updateBO data
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateBO updateBO) {
        ManagerUser userEntity = baseMapper.getUserId(updateBO.getUserId());
        if (ObjectUtil.isNotNull(userEntity)){
            if (ObjectUtil.isNotNull(updateBO.getImg())){
                userEntity.setImg(updateBO.getImg());
            }
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
            if (ObjectUtil.isNotNull(updateBO.getEmail())){
                userEntity.setEmail(updateBO.getEmail());
            }
            if (ObjectUtil.isNotNull(updateBO.getDeptId())){
                userEntity.setDeptId(updateBO.getDeptId());
            }
            if (ObjectUtil.isNotNull(updateBO.getPost())){
                userEntity.setPost(updateBO.getPost());
            }
            if (ObjectUtil.isNotNull(updateBO.getSex())){
                userEntity.setSex(updateBO.getSex());
            }
            if (ObjectUtil.isNotNull(updateBO.getStatus())){
                userEntity.setStatus(updateBO.getStatus());
            }
            if (updateBO.getParentId() != null){
                userEntity.setParentId(updateBO.getParentId() == 0 ? null : updateBO.getParentId());
            }
            updateById(userEntity);
            // 同步角色
            if (updateBO.getRoleIds() != null) {
                // 删除旧角色
                userRoleService.lambdaUpdate().eq(ManagerUserRole::getUserId, updateBO.getUserId()).remove();
                // 插入新角色
                if (CollUtil.isNotEmpty(updateBO.getRoleIds())) {
                    List<ManagerUserRole> newRoles = updateBO.getRoleIds().stream().map(roleId -> {
                        ManagerUserRole ur = new ManagerUserRole();
                        ur.setUserId(updateBO.getUserId());
                        ur.setRoleId(roleId);
                        ur.setCreateUserId(UserUtil.getUserId());
                        ur.setCreateTime(LocalDateTime.now());
                        return ur;
                    }).collect(Collectors.toList());
                    userRoleService.saveBatch(newRoles, Const.BATCH_SAVE_SIZE);
                }
            }
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
            ManageUserVO vo = BeanUtil.copyProperties(userEntity, ManageUserVO.class);
            // 填充部门名称
            if (ObjectUtil.isNotNull(userEntity.getDeptId())) {
                ManagerDept dept = deptMapper.selectById(userEntity.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getDeptName());
                }
            }
            // 填充角色信息
            fillRoleInfo(Collections.singletonList(vo));
            fillImgUrl(Collections.singletonList(vo));
            return vo;
        }
        return null;
    }

    /**
     * 填充头像访问URL
     */
    private void fillImgUrl(List<ManageUserVO> voList) {
        for (ManageUserVO vo : voList) {
            if (StrUtil.isNotBlank(vo.getImg())) {
                try {
                    vo.setImgUrl(fileStorageService.getUrl(vo.getImg()));
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 修改密码
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    @Override
    public void updatePassword(String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_PASSWORD_TOO_SHORT);
        }
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
