package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.entity.BO.RegisterBO;
import com.kakarote.ai_crm.entity.BO.ResetPasswordBO;
import com.kakarote.ai_crm.entity.BO.ExternalTenantMemberRegisterBO;
import com.kakarote.ai_crm.entity.BO.ExternalTenantRegisterBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;

public interface RegistrationService {

    /**
     * 注册新租户及超级管理员用户
     * @param registerBO 注册信息
     */
    void register(RegisterBO registerBO);

    ManagerUser registerExternalTenant(ExternalTenantRegisterBO registerBO);

    ManagerUser registerExternalTenantMember(ExternalTenantMemberRegisterBO registerBO);


    /**
     * 发送邮件验证码
     *
     * @param email 邮件
     * @param type  类型 1为注册 2为找回密码
     */
    public void sendEmail(String email, Integer type);

    /**
     * 通过邮箱验证码重置密码
     *
     * @param resetPasswordBO 重置密码信息
     */
    void resetPassword(ResetPasswordBO resetPasswordBO);
}
