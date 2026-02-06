package com.kakarote.ai_crm.config.security.service;

import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.ManageUserService;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


/**
 * 用户认证类
 * @author zhangzhiwei
 */
@Service
public class UserDetailsUserServiceImpl implements UserDetailsService {

    @Resource
    private ManageUserService manageUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LoginUser loginUser = new LoginUser();
        ManagerUser managerUser = manageUserService.queryUserByUsername(username);
        loginUser.setUser(managerUser);
        return loginUser;
    }

}
