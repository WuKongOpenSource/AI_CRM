package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.service.IManagerRoleService;
import com.kakarote.ai_crm.service.PermissionService;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private IManagerRoleService managerRoleService;

    /**
     * 判断是否存在权限。
     */
    @Override
    public boolean hasPermission(String permission) {
        JSONObject auth = managerRoleService.auth(UserUtil.getUserId());
        if (auth == null || auth.isEmpty()) {
            return false;
        }
        if (!permission.contains(":")) {
            return auth.containsKey(permission);
        }

        String module = permission.substring(0, permission.indexOf(':'));
        Object moduleNode = auth.get(module);
        if (!(moduleNode instanceof JSONObject moduleAuth)) {
            return false;
        }
        return Boolean.TRUE.equals(moduleAuth.get(permission));
    }
}
