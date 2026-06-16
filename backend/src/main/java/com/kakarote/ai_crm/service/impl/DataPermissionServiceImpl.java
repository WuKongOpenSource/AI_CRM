package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.auth.DataPermissionHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.mapper.ManagerRoleMapper;
import com.kakarote.ai_crm.mapper.ManagerRoleMenuMapper;
import com.kakarote.ai_crm.mapper.ManagerUserRoleMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataPermissionServiceImpl implements DataPermissionService {

    private static final int SCOPE_SELF = 1;
    private static final int SCOPE_SELF_AND_SUBORDINATES = 2;
    private static final int SCOPE_DEPT = 3;
    private static final int SCOPE_DEPT_AND_SUB = 4;
    private static final int SCOPE_ALL = 5;

    @Autowired
    private ManageUserMapper manageUserMapper;

    @Autowired
    private ManagerDeptMapper managerDeptMapper;

    @Autowired
    private ManagerUserRoleMapper managerUserRoleMapper;

    @Autowired
    private ManagerRoleMapper managerRoleMapper;

    @Autowired
    private ManagerRoleMenuMapper managerRoleMenuMapper;

    @Override
    public DataPermissionContext createContext(String module) {
        DataPermissionContext cached = DataPermissionHolder.get(module);
        if (cached != null) {
            return cached;
        }

        Long currentUserId = UserUtil.getUserId();
        if (currentUserId == null) {
            DataPermissionContext context = DataPermissionContext.none();
            DataPermissionHolder.put(module, context);
            return context;
        }
        if (isSuperAdmin(currentUserId)) {
            DataPermissionContext context = DataPermissionContext.all();
            DataPermissionHolder.put(module, context);
            return context;
        }

        List<Integer> dataScopes = managerRoleMenuMapper.queryDataScopesByUserIdAndModule(currentUserId, module);
        DataPermissionContext context = buildContextByScopes(currentUserId, dataScopes);
        DataPermissionHolder.put(module, context);
        return context;
    }

    @Override
    public boolean hasUserDataAccess(String module, Long targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        DataPermissionContext context = createContext(module);
        return context.isAllData() || (context.getUserIds() != null && context.getUserIds().contains(targetUserId));
    }

    @Override
    public DataPermissionContext createContextByPermission(String permission) {
        String key = "permission:" + (permission == null ? "" : permission);
        DataPermissionContext cached = DataPermissionHolder.get(key);
        if (cached != null) {
            return cached;
        }

        Long currentUserId = UserUtil.getUserId();
        if (currentUserId == null) {
            DataPermissionContext context = DataPermissionContext.none();
            DataPermissionHolder.put(key, context);
            return context;
        }
        if (isSuperAdmin(currentUserId)) {
            DataPermissionContext context = DataPermissionContext.all();
            DataPermissionHolder.put(key, context);
            return context;
        }

        List<Integer> dataScopes = managerRoleMenuMapper.queryDataScopesByUserIdAndPermission(currentUserId, permission);
        DataPermissionContext context = buildContextByScopes(currentUserId, dataScopes);
        DataPermissionHolder.put(key, context);
        return context;
    }

    @Override
    public boolean hasUserDataAccessByPermission(String permission, Long targetUserId) {
        if (targetUserId == null) {
            return false;
        }
        DataPermissionContext context = createContextByPermission(permission);
        return context.isAllData() || (context.getUserIds() != null && context.getUserIds().contains(targetUserId));
    }

    @Override
    public void assertUserDataAccess(String module, Long targetUserId) {
        if (!hasUserDataAccess(module, targetUserId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH);
        }
    }

    @Override
    public void assertUserDataAccessByPermission(String permission, Long targetUserId) {
        if (!hasUserDataAccessByPermission(permission, targetUserId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH);
        }
    }

    private String resolveModule(String permission) {
        if (permission == null || permission.isBlank()) {
            return "";
        }
        int separator = permission.indexOf(':');
        return separator > 0 ? permission.substring(0, separator) : permission;
    }

    private DataPermissionContext buildContextByScopes(Long currentUserId, List<Integer> dataScopes) {
        if (dataScopes == null || dataScopes.isEmpty()) {
            return DataPermissionContext.none();
        }
        if (dataScopes.contains(SCOPE_ALL)) {
            return DataPermissionContext.all();
        }

        ManagerUser currentUser = manageUserMapper.getUserId(currentUserId);
        if (currentUser == null) {
            return DataPermissionContext.none();
        }

        List<ManagerUser> allUsers = manageUserMapper.selectList(
                new QueryWrapper<ManagerUser>()
                        .select("user_id", "parent_id", "dept_id")
        );

        Set<Long> allowedUserIds = new LinkedHashSet<>();
        if (dataScopes.contains(SCOPE_SELF)) {
            allowedUserIds.add(currentUserId);
        }
        if (dataScopes.contains(SCOPE_SELF_AND_SUBORDINATES)) {
            allowedUserIds.add(currentUserId);
            allowedUserIds.addAll(collectSubordinateUserIds(currentUserId, allUsers));
        }
        if (dataScopes.contains(SCOPE_DEPT) && currentUser.getDeptId() != null) {
            allowedUserIds.addAll(collectUsersByDeptIds(Set.of(currentUser.getDeptId()), allUsers));
        }
        if (dataScopes.contains(SCOPE_DEPT_AND_SUB) && currentUser.getDeptId() != null) {
            allowedUserIds.addAll(collectUsersByDeptIds(collectDeptIds(currentUser.getDeptId()), allUsers));
        }

        return allowedUserIds.isEmpty()
                ? DataPermissionContext.none()
                : DataPermissionContext.users(allowedUserIds);
    }

    private boolean isSuperAdmin(Long userId) {
        if (userId.equals(UserUtil.getSuperUserId())) {
            return true;
        }
        List<ManagerUserRole> userRoles = managerUserRoleMapper.selectList(
                new LambdaQueryWrapper<ManagerUserRole>()
                        .eq(ManagerUserRole::getUserId, userId)
        );
        if (userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        List<Long> roleIds = userRoles.stream()
                .map(ManagerUserRole::getRoleId)
                .toList();
        return managerRoleMapper.selectCount(
                new LambdaQueryWrapper<ManagerRole>()
                        .in(ManagerRole::getRoleId, roleIds)
                        .eq(ManagerRole::getRealm, "super_admin")
        ) > 0;
    }

    private Set<Long> collectSubordinateUserIds(Long currentUserId, List<ManagerUser> allUsers) {
        Map<Long, List<Long>> childrenByParent = allUsers.stream()
                .filter(user -> user.getParentId() != null)
                .collect(Collectors.groupingBy(
                        ManagerUser::getParentId,
                        Collectors.mapping(ManagerUser::getUserId, Collectors.toList())
                ));
        return traverseTree(currentUserId, childrenByParent);
    }

    private Set<Long> collectDeptIds(Long deptId) {
        List<ManagerDept> allDepts = managerDeptMapper.selectList(
                new QueryWrapper<ManagerDept>()
                        .select("dept_id", "parent_id")
        );
        Map<Long, List<Long>> childrenByParent = allDepts.stream()
                .filter(dept -> dept.getParentId() != null)
                .collect(Collectors.groupingBy(
                        ManagerDept::getParentId,
                        Collectors.mapping(ManagerDept::getDeptId, Collectors.toList())
                ));
        Set<Long> deptIds = new LinkedHashSet<>();
        deptIds.add(deptId);
        deptIds.addAll(traverseTree(deptId, childrenByParent));
        return deptIds;
    }

    private Set<Long> collectUsersByDeptIds(Set<Long> deptIds, List<ManagerUser> allUsers) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptySet();
        }
        return allUsers.stream()
                .filter(user -> user.getDeptId() != null && deptIds.contains(user.getDeptId()))
                .map(ManagerUser::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> traverseTree(Long rootId, Map<Long, List<Long>> childrenByParent) {
        List<Long> initialChildren = childrenByParent.get(rootId);
        if (initialChildren == null || initialChildren.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> result = new LinkedHashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>(initialChildren);
        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            if (!result.add(currentId)) {
                continue;
            }
            List<Long> children = childrenByParent.getOrDefault(currentId, new ArrayList<>());
            queue.addAll(children);
        }
        return result;
    }
}
