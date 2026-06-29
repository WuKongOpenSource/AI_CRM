package com.kakarote.ai_crm.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.enums.EmployeeStatusEnum;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ResetUsernameBO;
import com.kakarote.ai_crm.entity.BO.UserPreferenceUpdateBO;
import com.kakarote.ai_crm.entity.BO.UserAddBO;
import com.kakarote.ai_crm.entity.BO.UserQueryBO;
import com.kakarote.ai_crm.entity.BO.UserStatusBO;
import com.kakarote.ai_crm.entity.BO.UserUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import com.kakarote.ai_crm.entity.VO.ManageUserVO;
import com.kakarote.ai_crm.entity.VO.UserPreferenceVO;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.service.IManagerUserRoleService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.support.UserPreferenceSupport;
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

    @Override
    public List<ManagerUser> queryUsersByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return Collections.emptyList();
        }
        return baseMapper.queryUsersByUsername(StrUtil.trim(username));
    }

    @Override
    public List<ManagerUser> findUsersByUsernames(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyList();
        }
        return baseMapper.selectList(
                new LambdaQueryWrapper<ManagerUser>()
                        .in(ManagerUser::getUsername, usernames));
    }

    /**
     * 添加用户
     *
     * @param userAddBO data
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(UserAddBO userAddBO) {
        if (userAddBO == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "用户信息不能为空");
        }
        if (StrUtil.isNotEmpty(userAddBO.getUsername())) {
            String username = StrUtil.trim(userAddBO.getUsername());
            long duplicateCount = baseMapper.selectCount(new QueryWrapper<ManagerUser>().eq("username", username));
            if (duplicateCount > 0) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "用户名已存在");
            }
            userAddBO.setUsername(username);
        }
        Long parentId = normalizeParentUserId(userAddBO.getParentId());
        validateParentUser(null, parentId);
        userAddBO.setParentId(parentId);

        if (StrUtil.isNotEmpty(userAddBO.getPassword())) {
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            String password = bCryptPasswordEncoder.encode(userAddBO.getPassword());
            userAddBO.setPassword(password);
        }
        ManagerUser manageUser = BeanUtil.copyProperties(userAddBO, ManagerUser.class);
        if (userAddBO.getStatus() != null) {
            manageUser.setStatus(userAddBO.getStatus());
        } else {
            manageUser.setStatus(1);
        }
        manageUser.setEmployeeStatus(EmployeeStatusEnum.normalize(userAddBO.getEmployeeStatus()));
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

    /**
     * 通过条件查询员工
     *
     * @param userQueryBO queryBo
     * @return userList 员工列表
     */
    @Override
    public BasePage<ManageUserVO> queryPageList(UserQueryBO userQueryBO) {
        if (StrUtil.isNotBlank(userQueryBO.getEmployeeStatus())) {
            userQueryBO.setEmployeeStatus(EmployeeStatusEnum.normalize(userQueryBO.getEmployeeStatus()));
        }
        // 如果指定了部门ID，收集该部门及所有下级部门ID
        if (userQueryBO.getDeptId() != null) {
            List<ManagerDept> allDepts = deptMapper.selectList(null);
            Set<Long> deptIds = new LinkedHashSet<>();
            collectChildDeptIds(allDepts, userQueryBO.getDeptId(), deptIds, Const.AUTH_DATA_RECURSION_NUM);
            userQueryBO.setDeptIds(new ArrayList<>(deptIds));
        }
        BasePage<ManageUserVO> page = baseMapper.queryPageList(userQueryBO.parse(), userQueryBO);
        fillRoleInfo(page.getRecords());
        fillEmployeeStatusName(page.getRecords());
        fillImgUrl(page.getRecords());
        return page;
    }

    /**
     * 递归收集指定部门及其所有下级部门ID
     */
    private void collectChildDeptIds(List<ManagerDept> allDepts, Long parentId, List<Long> result) {
        Set<Long> deptIds = new LinkedHashSet<>(result);
        collectChildDeptIds(allDepts, parentId, deptIds, Const.AUTH_DATA_RECURSION_NUM);
        result.clear();
        result.addAll(deptIds);
    }

    private void collectChildDeptIds(List<ManagerDept> allDepts, Long parentId, Set<Long> result, int depth) {
        if (parentId == null || depth < 0 || !result.add(parentId)) {
            return;
        }
        for (ManagerDept dept : allDepts) {
            if (parentId.equals(dept.getParentId())) {
                collectChildDeptIds(allDepts, dept.getDeptId(), result, depth - 1);
            }
        }
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

    private void fillEmployeeStatusName(List<ManageUserVO> users) {
        if (CollUtil.isEmpty(users)) return;
        users.forEach(user -> {
            String status = EmployeeStatusEnum.normalize(user.getEmployeeStatus());
            user.setEmployeeStatus(status);
            user.setEmployeeStatusName(EmployeeStatusEnum.getName(status));
        });
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
            if (ObjectUtil.isNotNull(updateBO.getEmployeeStatus())) {
                userEntity.setEmployeeStatus(EmployeeStatusEnum.normalize(updateBO.getEmployeeStatus()));
            }
            if (updateBO.getParentId() != null){
                Long parentId = normalizeParentUserId(updateBO.getParentId());
                validateParentUser(updateBO.getUserId(), parentId);
                userEntity.setParentId(parentId);
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
     * 重置用户名。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUsername(ResetUsernameBO resetUsernameBO) {
        if (resetUsernameBO == null || resetUsernameBO.getUserId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
        }

        ManagerUser userEntity = baseMapper.getUserId(resetUsernameBO.getUserId());
        if (ObjectUtil.isNull(userEntity)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
        }

        String oldUsername = StrUtil.trim(userEntity.getUsername());
        String newUsername = StrUtil.trim(resetUsernameBO.getUsername());
        if (StrUtil.isBlank(newUsername)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "请输入新用户名");
        }
        if (StrUtil.equals(oldUsername, newUsername)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "新用户名不能与当前用户名相同");
        }

        Long currentUserId = UserUtil.getUserId();
        if (Objects.equals(currentUserId, userEntity.getUserId())) {
            validateCurrentPassword(resetUsernameBO.getCurrentPassword());
        }

        long duplicateCount = lambdaQuery()
                .eq(ManagerUser::getUsername, newUsername)
                .ne(ManagerUser::getUserId, userEntity.getUserId())
                .count();
        if (duplicateCount > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "用户名已存在");
        }

        userEntity.setUsername(newUsername);
        updateById(userEntity);
    }

    /**
     * 验证当前登录密码。
     */
    private void validateCurrentPassword(String currentPassword) {
        if (StrUtil.isBlank(currentPassword)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "请输入当前登录密码");
        }
        ManagerUser currentUser = baseMapper.getUserId(UserUtil.getUserId());
        if (currentUser == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
        }
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        if (!bCryptPasswordEncoder.matches(currentPassword, currentUser.getPassword())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_OLD_PASSWORD_ERROR);
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
            vo.setPreferences(UserPreferenceSupport.parsePreferences(userEntity.getUiPreferences()));
            return vo;
        }
        return null;
    }

    @Override
    public UserPreferenceVO updateCurrentUserPreferences(UserPreferenceUpdateBO preferenceUpdateBO) {
        ManagerUser userEntity = baseMapper.getUserId(UserUtil.getUserId());
        if (ObjectUtil.isNull(userEntity)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
        }

        UserPreferenceVO preferences = UserPreferenceSupport.toPreferenceVO(
                preferenceUpdateBO == null ? null : preferenceUpdateBO.getSidebarModuleOrder()
        );
        userEntity.setUiPreferences(UserPreferenceSupport.serializePreferences(preferences));
        updateById(userEntity);
        return preferences;
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

    private Long normalizeParentUserId(Long parentId) {
        return parentId == null || Objects.equals(parentId, 0L) ? null : parentId;
    }

    private void validateParentUser(Long userId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (Objects.equals(userId, parentId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "直属上级不能选择当前员工");
        }

        Map<Long, Long> parentByUserId = new HashMap<>();
        baseMapper.selectList(new QueryWrapper<ManagerUser>().select("user_id", "parent_id"))
                .forEach(user -> {
                    if (user.getUserId() != null) {
                        parentByUserId.put(user.getUserId(), normalizeParentUserId(user.getParentId()));
                    }
                });

        if (!parentByUserId.containsKey(parentId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "直属上级不存在");
        }

        if (userId != null && wouldCreateParentCycle(userId, parentId, parentByUserId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "直属上级不能选择当前员工或其下级员工");
        }
    }

    private boolean wouldCreateParentCycle(Long userId, Long parentId, Map<Long, Long> parentByUserId) {
        Set<Long> visited = new HashSet<>();
        Long currentParentId = parentId;

        while (currentParentId != null && visited.add(currentParentId)) {
            if (Objects.equals(userId, currentParentId)) {
                return true;
            }
            currentParentId = parentByUserId.get(currentParentId);
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            return;
        }
        List<Long> userIdList = Arrays.asList(userIds);
        List<Long> protectedRoleIds = roleService.lambdaQuery()
                .select(ManagerRole::getRoleId)
                .eq(ManagerRole::getRealm, "super_admin")
                .list()
                .stream()
                .map(ManagerRole::getRoleId)
                .filter(Objects::nonNull)
                .toList();
        if (CollUtil.isNotEmpty(protectedRoleIds)) {
            long protectedUserCount = userRoleService.lambdaQuery()
                    .in(ManagerUserRole::getUserId, userIdList)
                    .in(ManagerUserRole::getRoleId, protectedRoleIds)
                    .count();
            if (protectedUserCount > 0) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "主账号不允许删除");
            }
        }
        removeByIds(userIdList);
        List<ManagerUserRole> list = userRoleService.lambdaQuery().in(ManagerUserRole::getUserId, userIdList).list();
        if(CollUtil.isNotEmpty(list)){
            userRoleService.removeByIds(list);
        }
    }
}
