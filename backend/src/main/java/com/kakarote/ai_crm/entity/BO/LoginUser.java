package com.kakarote.ai_crm.entity.BO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kakarote.ai_crm.common.enums.LoginTypeEnum;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 登录用户身份权限
 */
public class LoginUser implements UserDetails {
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识
     */
    private String token;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 过期时间
     */
    private Long expireTime;


    /**
     * 用户信息
     */
    private ManagerUser user;

    /**
     * Login client type used for same-type single-login isolation.
     */
    private LoginTypeEnum loginType;

    /**
     * 获取Token。
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置Token。
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 初始化Login用户实例。
     */
    public LoginUser() {
    }

    /**
     * 获取密码。
     */
    @JsonIgnore
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * 获取Username。
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * 账户是否未过期,过期无法验证
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 指定用户是否解锁,锁定的用户无法进行身份验证
     *
     * @return
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 指示是否已过期的用户的凭据(密码),过期的凭据防止认证
     *
     * @return
     */
    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 是否可用 ,禁用的用户不能身份验证
     *
     * @return
     */
    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return user != null && Integer.valueOf(1).equals(user.getStatus());
    }

    /**
     * 获取Login时间。
     */
    public Long getLoginTime() {
        return loginTime;
    }

    /**
     * 设置Login时间。
     */
    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    /**
     * 获取Expire时间。
     */
    public Long getExpireTime() {
        return expireTime;
    }

    /**
     * 设置Expire时间。
     */
    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * 获取用户。
     */
    public ManagerUser getUser() {
        return user;
    }

    /**
     * 设置用户。
     */
    public void setUser(ManagerUser user) {
        this.user = user;
    }

    public LoginTypeEnum getLoginType() {
        return loginType;
    }

    public void setLoginType(LoginTypeEnum loginType) {
        this.loginType = loginType;
    }

    /**
     * 获取Authorities。
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }
}
