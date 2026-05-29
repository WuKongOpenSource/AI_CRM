package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.EnterpriseConfigUpdateBO;
import com.kakarote.ai_crm.entity.BO.ExternalTenantMemberRegisterBO;
import com.kakarote.ai_crm.entity.BO.ExternalTenantRegisterBO;
import com.kakarote.ai_crm.entity.BO.RegisterBO;
import com.kakarote.ai_crm.entity.BO.ResetPasswordBO;
import com.kakarote.ai_crm.entity.PO.CrmTenant;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerMenu;
import com.kakarote.ai_crm.entity.PO.ManagerRole;
import com.kakarote.ai_crm.entity.PO.ManagerRoleMenu;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.ManagerUserRole;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.service.ICrmTenantService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IManagerMenuService;
import com.kakarote.ai_crm.service.IManagerRoleMenuService;
import com.kakarote.ai_crm.service.IManagerRoleService;
import com.kakarote.ai_crm.service.IManagerUserRoleService;
import com.kakarote.ai_crm.service.ISystemConfigService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.service.RegistrationService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import com.kakarote.ai_crm.utils.CloudUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class RegistrationServiceImpl implements RegistrationService {

    public static final String SUPER_ADMIN_REALM = "super_admin";
    public static final String DEFAULT_ROLE_REALM = "default_user";

    private static final String EMAIL_CODE_CACHE_PREFIX = "register:email:code:";
    private static final int EMAIL_CODE_EXPIRE_SECONDS = 600;
    private static final int EMAIL_CODE_COOLDOWN_SECONDS = 60;
    private static final int REGISTER_EMAIL_TYPE = 1;
    private static final int RESET_PASSWORD_EMAIL_TYPE = 2;
    private static final int MENU_TYPE_ACTION = 5;
    private static final int DATA_SCOPE_SELF = 1;
    private static final Set<String> SYSTEM_SETTING_MODULES = Set.of(
            "user", "dept", "role", "config", "customField", "agent"
    );
    private static final Set<String> DATA_SCOPE_MODULES = Set.of(
            "customer", "contact", "task", "followup", "schedule", "knowledge"
    );

    @Autowired
    private ICrmTenantService tenantService;

    @Autowired
    private ManagerDeptMapper deptMapper;

    @Autowired
    private IManagerRoleService roleService;

    @Autowired
    private IManagerMenuService menuService;

    @Autowired
    private IManagerRoleMenuService roleMenuService;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private IManagerUserRoleService userRoleService;

    @Autowired
    private ICustomFieldService customFieldService;

    @Autowired
    private ISystemConfigService systemConfigService;

    @Lazy
    @Autowired
    private WeKnoraClient weKnoraClient;

    @Autowired
    private CloudUtil cloudUtil;

    @Autowired
    private Redis redis;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 完成用户注册。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterBO registerBO) {
        String email = normalizeEmail(registerBO.getEmail());
        String realname = normalizeText(registerBO.getRealname());
        String companyName = StrUtil.trim(registerBO.getCompanyName());
        verifyEmailCode(email, registerBO.getVerificationCode());
        ensureEmailNotRegistered(email);

        CrmTenant tenant = new CrmTenant();
        tenant.setTenantName(companyName);
        tenant.setContactEmail(email);
        tenant.setContactName(StrUtil.isNotBlank(realname) ? realname : email);
        tenant.setStatus(1);
        tenant.setMaxUsers(50);
        tenant.setGiftCreditTotal(ICrmTenantService.DEFAULT_GIFT_CREDIT_TOTAL);
        tenant.setGiftCreditUsed(0L);
        tenant.setCreateTime(new Date());
        tenant.setUpdateTime(new Date());
        tenantService.save(tenant);

        Long newTenantId = tenant.getTenantId();
        // 注册阶段还没有 JWT，后续部门/角色/用户初始化又依赖租户拦截器，因此必须先手动写入租户上下文。
        TenantContextHolder.setTenantId(newTenantId);

        try {
            customFieldService.initializeSystemFields("customer");
            customFieldService.initializeSystemFields("contact");
            initializeEnterpriseConfig(companyName);

            ManagerDept dept = new ManagerDept();
            dept.setDeptName(companyName);
            dept.setParentId(0L);
            dept.setSortOrder(1);
            dept.setCreateTime(new Date());
            deptMapper.insert(dept);

            ManagerRole adminRole = ManagerRole.builder()
                    .roleName("超级管理员")
                    .realm(SUPER_ADMIN_REALM)
                    .description("系统超级管理员（注册时自动创建）")
                    .dataType(5)
                    .createTime(LocalDateTime.now())
                    .build();
            roleService.save(adminRole);
            initializeDefaultRole();

            ManagerUser user = new ManagerUser();
            user.setUsername(email);
            user.setPassword(passwordEncoder.encode(registerBO.getPassword()));
            user.setEmail(email);
            user.setRealname(StrUtil.isNotBlank(realname) ? realname : email);
            user.setDeptId(dept.getDeptId());
            user.setStatus(1);
            user.setCreateTime(new Date());
            manageUserService.save(user);

            ManagerUserRole userRole = new ManagerUserRole();
            userRole.setUserId(user.getUserId());
            userRole.setRoleId(adminRole.getRoleId());
            userRole.setCreateTime(LocalDateTime.now());
            userRoleService.save(userRole);

            redis.del(getEmailCodeCacheKey(email));
            log.info("租户注册成功: tenantId={}, email={}, companyName={}", newTenantId, email, companyName);

            if (weKnoraClient.isEnabled()) {
                try {
                    // 这里做一次预热，失败不回滚主注册流程，首次真正使用知识库时仍会按懒初始化逻辑重试。
                    weKnoraClient.getOrCreateTenantContext(newTenantId);
                    log.info("WeKnora 租户初始化成功: tenantId={}", newTenantId);
                } catch (Exception e) {
                    log.warn("WeKnora 租户初始化失败，将在首次使用时重试: tenantId={}, error={}", newTenantId, e.getMessage());
                }
            }
        } finally {
            // 注册请求结束必须清理，避免容器线程复用后把新租户上下文带到后续无关请求。
            TenantContextHolder.clear();
        }
    }

    /**
     * 发送邮箱。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagerUser registerExternalTenant(ExternalTenantRegisterBO registerBO) {
        String email = normalizeEmail(registerBO.getEmail());
        String realname = normalizeText(registerBO.getRealname());
        String companyName = StrUtil.trim(registerBO.getCompanyName());
        if (!Validator.isEmail(email, true)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Invalid email");
        }
        if (StrUtil.isBlank(companyName)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Company name is required");
        }
        if (Boolean.TRUE.equals(registerBO.getEmailVerificationRequired())) {
            verifyEmailCode(email, registerBO.getVerificationCode());
        }
        ensureEmailNotRegistered(email);

        CrmTenant tenant = new CrmTenant();
        tenant.setTenantName(companyName);
        tenant.setContactEmail(email);
        tenant.setContactName(StrUtil.isNotBlank(realname) ? realname : email);
        tenant.setStatus(1);
        tenant.setMaxUsers(50);
        tenant.setGiftCreditTotal(ICrmTenantService.DEFAULT_GIFT_CREDIT_TOTAL);
        tenant.setGiftCreditUsed(0L);
        tenant.setCreateTime(new Date());
        tenant.setUpdateTime(new Date());
        tenantService.save(tenant);

        Long newTenantId = tenant.getTenantId();
        TenantContextHolder.setTenantId(newTenantId);

        try {
            customFieldService.initializeSystemFields("customer");
            customFieldService.initializeSystemFields("contact");
            initializeEnterpriseConfig(companyName);

            ManagerDept dept = new ManagerDept();
            dept.setDeptName(companyName);
            dept.setParentId(0L);
            dept.setSortOrder(1);
            dept.setCreateTime(new Date());
            deptMapper.insert(dept);

            ManagerRole adminRole = ManagerRole.builder()
                    .roleName("super admin")
                    .realm(SUPER_ADMIN_REALM)
                    .description("Created by external auth registration")
                    .dataType(5)
                    .createTime(LocalDateTime.now())
                    .build();
            roleService.save(adminRole);
            initializeDefaultRole();

            ManagerUser user = new ManagerUser();
            user.setUsername(email);
            user.setEmail(email);
            user.setRealname(StrUtil.isNotBlank(realname) ? realname : email);
            user.setDeptId(dept.getDeptId());
            user.setStatus(1);
            user.setCreateTime(new Date());
            manageUserService.save(user);

            ManagerUserRole userRole = new ManagerUserRole();
            userRole.setUserId(user.getUserId());
            userRole.setRoleId(adminRole.getRoleId());
            userRole.setCreateTime(LocalDateTime.now());
            userRoleService.save(userRole);

            redis.del(getEmailCodeCacheKey(email));
            log.info("External auth tenant registration succeeded: tenantId={}, email={}", newTenantId, email);

            if (weKnoraClient.isEnabled()) {
                try {
                    weKnoraClient.getOrCreateTenantContext(newTenantId);
                } catch (Exception e) {
                    log.warn("WeKnora tenant initialization failed after external registration: tenantId={}, error={}",
                            newTenantId, e.getMessage());
                }
            }
            return user;
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManagerUser registerExternalTenantMember(ExternalTenantMemberRegisterBO registerBO) {
        if (registerBO.getTenantId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tenant is required");
        }
        String email = normalizeEmail(registerBO.getEmail());
        String realname = normalizeText(registerBO.getRealname());
        if (!Validator.isEmail(email, true)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Invalid email");
        }

        Long previousTenantId = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(registerBO.getTenantId());
        try {
            long duplicateCount = manageUserService.lambdaQuery()
                    .eq(ManagerUser::getUsername, email)
                    .count();
            if (duplicateCount > 0) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                        "This email already exists in the enterprise. Please sign in and bind it manually.");
            }

            CrmTenant tenant = tenantService.getById(registerBO.getTenantId());
            if (tenant == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST);
            }

            ManagerRole defaultRole = roleService.lambdaQuery()
                    .eq(ManagerRole::getRealm, DEFAULT_ROLE_REALM)
                    .last("LIMIT 1")
                    .one();
            if (defaultRole == null) {
                initializeDefaultRole();
                defaultRole = roleService.lambdaQuery()
                        .eq(ManagerRole::getRealm, DEFAULT_ROLE_REALM)
                        .last("LIMIT 1")
                        .one();
            }

            ManagerDept rootDept = deptMapper.selectList(Wrappers.<ManagerDept>lambdaQuery()
                            .eq(ManagerDept::getParentId, 0L)
                            .last("LIMIT 1"))
                    .stream()
                    .findFirst()
                    .orElse(null);

            ManagerUser user = new ManagerUser();
            user.setUsername(email);
            user.setEmail(email);
            user.setRealname(StrUtil.isNotBlank(realname) ? realname : email);
            user.setDeptId(rootDept == null ? null : rootDept.getDeptId());
            user.setStatus(1);
            user.setCreateTime(new Date());
            manageUserService.save(user);

            if (defaultRole != null) {
                ManagerUserRole userRole = new ManagerUserRole();
                userRole.setUserId(user.getUserId());
                userRole.setRoleId(defaultRole.getRoleId());
                userRole.setCreateTime(LocalDateTime.now());
                userRoleService.save(userRole);
            }

            log.info("External auth tenant member registration succeeded: tenantId={}, email={}",
                    registerBO.getTenantId(), email);
            return user;
        } finally {
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            } else {
                TenantContextHolder.clear();
            }
        }
    }

    @Override
    public void sendEmail(String email, Integer type) {
        String normalizedEmail = normalizeEmail(email);
        if (!Validator.isEmail(normalizedEmail, true)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "邮箱格式不正确");
        }
        validateSendScene(normalizedEmail, type);

        String cacheKey = getEmailCodeCacheKey(normalizedEmail);
        Long ttl = redis.ttl(cacheKey);
        if (ttl != null && ttl > EMAIL_CODE_EXPIRE_SECONDS - EMAIL_CODE_COOLDOWN_SECONDS) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_BAD_REQUEST, "验证码发送过于频繁，请稍后再试");
        }

        String code = RandomUtil.randomNumbers(6);
        String sceneName = Objects.equals(type, RESET_PASSWORD_EMAIL_TYPE) ? "找回密码" : "注册";
        boolean sent = cloudUtil.sendVerificationCodeEmail(
                normalizedEmail,
                "AI CRM 邮箱验证码",
                code,
                sceneName
        );
        if (!sent) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "验证码发送失败，请稍后重试");
        }

        redis.setex(cacheKey, EMAIL_CODE_EXPIRE_SECONDS, code);
        log.info("发送邮箱验证码成功: email={}, type={}", normalizedEmail, type);
    }

    /**
     * 重置密码。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordBO resetPasswordBO) {
        String email = normalizeEmail(resetPasswordBO.getEmail());
        verifyEmailCode(email, resetPasswordBO.getVerificationCode());

        String password = resetPasswordBO.getPassword();
        if (StrUtil.length(password) < 6) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_PASSWORD_TOO_SHORT);
        }

        var matchedUsers = manageUserService.queryUsersByUsername(email);
        if (CollUtil.isEmpty(matchedUsers)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST);
        }

        String encodedPassword = passwordEncoder.encode(password);
        Long previousTenantId = TenantContextHolder.getTenantId();
        try {
            for (ManagerUser matchedUser : matchedUsers) {
                // 同一邮箱可能跨租户存在多个账号，逐个切换租户上下文后再更新，确保命中正确的租户行。
                if (matchedUser.getTenantId() != null) {
                    TenantContextHolder.setTenantId(matchedUser.getTenantId());
                } else {
                    TenantContextHolder.clear();
                }
                matchedUser.setPassword(encodedPassword);
                manageUserService.updateById(matchedUser);
            }
        } finally {
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            } else {
                TenantContextHolder.clear();
            }
        }

        redis.del(getEmailCodeCacheKey(email));
        log.info("邮箱找回密码成功: email={}, userCount={}", email, matchedUsers.size());
    }

    /**
     * 校验SendScene。
     */
    private void validateSendScene(String email, Integer type) {
        if (Objects.equals(type, REGISTER_EMAIL_TYPE)) {
            ensureEmailNotRegistered(email);
            return;
        }
        boolean hasExistingUser = CollUtil.isNotEmpty(manageUserService.queryUsersByUsername(email));
        if (Objects.equals(type, RESET_PASSWORD_EMAIL_TYPE)) {
            if (!hasExistingUser) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_USER_DOES_NOT_EXIST, "该邮箱尚未注册");
            }
            return;
        }
        throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "不支持的验证码类型");
    }

    /**
     * 校验邮箱验证码。
     */
    private void verifyEmailCode(String email, String verificationCode) {
        String cachedCode = redis.get(getEmailCodeCacheKey(email));
        if (StrUtil.isBlank(cachedCode)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_VERIFICATION_CODE_ERROR, "验证码已失效，请重新获取");
        }
        if (!StrUtil.equals(StrUtil.trim(verificationCode), cachedCode)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_VERIFICATION_CODE_ERROR);
        }
    }

    /**
     * 确保邮箱编号TRegistered。
     */
    private void ensureEmailNotRegistered(String email) {
        long registeredEnterpriseCount = tenantService.count(
                Wrappers.<CrmTenant>lambdaQuery().eq(CrmTenant::getContactEmail, email)
        );
        if (registeredEnterpriseCount > 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_EMAIL_ALREADY_REGISTERED);
        }
    }

    /**
     * 获取邮箱验证码缓存键。
     */
    private String getEmailCodeCacheKey(String email) {
        return EMAIL_CODE_CACHE_PREFIX + email;
    }

    /**
     * 标准化邮箱。
     */
    private String normalizeEmail(String email) {
        return StrUtil.trim(email).toLowerCase();
    }

    /**
     * 标准化文本。
     */
    private String normalizeText(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) ? null : normalized;
    }

    /**
     * 初始化新企业默认角色。
     */
    private void initializeDefaultRole() {
        ManagerRole defaultRole = ManagerRole.builder()
                .roleName("默认角色")
                .realm(DEFAULT_ROLE_REALM)
                .description("默认角色")
                .dataType(DATA_SCOPE_SELF)
                .createTime(LocalDateTime.now())
                .build();
        roleService.save(defaultRole);
        initializeDefaultRolePermissions(defaultRole.getRoleId());
    }

    /**
     * 默认角色拥有除系统设置外的所有功能权限，业务数据权限固定为本人。
     */
    private void initializeDefaultRolePermissions(Long roleId) {
        List<ManagerMenu> menus = menuService.list();
        if (CollUtil.isEmpty(menus)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ManagerRoleMenu> roleMenus = new ArrayList<>();
        for (ManagerMenu menu : menus) {
            String realm = menu.getRealm();
            if (!Objects.equals(menu.getType(), MENU_TYPE_ACTION) || StrUtil.isBlank(realm)) {
                continue;
            }

            String module = resolvePermissionModule(realm);
            if (SYSTEM_SETTING_MODULES.contains(module)) {
                continue;
            }

            ManagerRoleMenu roleMenu = new ManagerRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menu.getMenuId());
            roleMenu.setDataScope(resolveDefaultDataScope(module, realm));
            roleMenu.setCreateTime(now);
            roleMenus.add(roleMenu);
        }

        if (CollUtil.isNotEmpty(roleMenus)) {
            roleMenuService.saveBatch(roleMenus);
        }
    }

    /**
     * 获取权限所属模块。
     */
    private String resolvePermissionModule(String realm) {
        int separatorIndex = realm.indexOf(':');
        return separatorIndex >= 0 ? realm.substring(0, separatorIndex) : realm;
    }

    /**
     * 只有业务数据模块需要写入数据范围；新建等无范围权限保持为空。
     */
    private Integer resolveDefaultDataScope(String module, String realm) {
        if (DATA_SCOPE_MODULES.contains(module) && !realm.endsWith(":create")) {
            return DATA_SCOPE_SELF;
        }
        return null;
    }

    /**
     * 初始化企业配置。
     */
    private void initializeEnterpriseConfig(String companyName) {
        if (StrUtil.isBlank(companyName)) {
            return;
        }

        EnterpriseConfigUpdateBO enterpriseConfig = new EnterpriseConfigUpdateBO();
        enterpriseConfig.setName(companyName);
        systemConfigService.updateEnterpriseConfig(enterpriseConfig);
    }
}
