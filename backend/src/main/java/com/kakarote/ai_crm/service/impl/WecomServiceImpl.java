package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.WecomConfigSaveBO;
import com.kakarote.ai_crm.entity.BO.WecomConversationQueryBO;
import com.kakarote.ai_crm.entity.BO.WecomCustomerQueryBO;
import com.kakarote.ai_crm.entity.BO.WecomEmployeeSessionQueryBO;
import com.kakarote.ai_crm.entity.BO.WecomSyncRunBO;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.ExternalAuthIdentity;
import com.kakarote.ai_crm.entity.PO.WecomConversation;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.entity.PO.WecomEmployee;
import com.kakarote.ai_crm.entity.PO.WecomExternalCustomer;
import com.kakarote.ai_crm.entity.PO.WecomMessage;
import com.kakarote.ai_crm.entity.PO.WecomSyncLog;
import com.kakarote.ai_crm.entity.VO.WecomConfigVO;
import com.kakarote.ai_crm.entity.VO.WecomConversationTabVO;
import com.kakarote.ai_crm.entity.VO.WecomConversationVO;
import com.kakarote.ai_crm.entity.VO.WecomCustomerBindingVO;
import com.kakarote.ai_crm.entity.VO.WecomEmployeeSessionVO;
import com.kakarote.ai_crm.entity.VO.WecomExternalCustomerVO;
import com.kakarote.ai_crm.entity.VO.WecomMessageVO;
import com.kakarote.ai_crm.entity.VO.WecomSyncStatusVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.ExternalAuthIdentityMapper;
import com.kakarote.ai_crm.mapper.WecomConversationMapper;
import com.kakarote.ai_crm.mapper.WecomCorpConfigMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.mapper.WecomExternalCustomerMapper;
import com.kakarote.ai_crm.mapper.WecomMessageMapper;
import com.kakarote.ai_crm.mapper.WecomSyncLogMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WecomServiceImpl {

    private static final String TYPE_EMPLOYEE = "employee";
    private static final String TYPE_CUSTOMER = "customer";
    private static final String TYPE_GROUP = "group";
    private static final String AUTH_REQUIRED_MESSAGE = "Please authorize WeCom third-party app first";

    @Autowired
    private WecomCorpConfigMapper configMapper;

    @Autowired
    private WecomEmployeeMapper employeeMapper;

    @Autowired
    private WecomExternalCustomerMapper externalCustomerMapper;

    @Autowired
    private WecomConversationMapper conversationMapper;

    @Autowired
    private WecomMessageMapper messageMapper;

    @Autowired
    private WecomSyncLogMapper syncLogMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ExternalAuthIdentityMapper identityMapper;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Autowired
    private WecomSyncServiceImpl syncService;

    @Autowired
    private WecomOpenPlatformService openPlatformService;

    @Autowired
    private WecomCustomerBindingServiceImpl bindingService;

    @Autowired
    private DataPermissionService dataPermissionService;

    public WecomConfigVO getConfig() {
        return toConfigVO(findConfig());
    }

    @Transactional(rollbackFor = Exception.class)
    public WecomConfigVO saveConfig(WecomConfigSaveBO saveBO) {
        WecomCorpConfig config = requireConfig();
        config.setArchivePublicKeyVersion(StrUtil.trim(saveBO.getArchivePublicKeyVersion()));
        config.setArchiveCorpId(StrUtil.trimToNull(saveBO.getArchiveCorpId()));
        config.setArchiveEnabled(Boolean.TRUE.equals(saveBO.getArchiveEnabled()));
        config.setCustomerContactEnabled(Boolean.TRUE.equals(saveBO.getCustomerContactEnabled()));
        config.setSyncEnabled(Boolean.TRUE.equals(saveBO.getSyncEnabled()));
        setEncryptedIfPresent(saveBO.getArchiveSecret(), config::setArchiveSecretEncrypted);
        setEncryptedIfPresent(saveBO.getArchivePrivateKey(), config::setArchivePrivateKeyEncrypted);
        configMapper.updateById(config);
        return toConfigVO(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public WecomSyncStatusVO runSync(WecomSyncRunBO runBO) {
        WecomCorpConfig config = requireConfig();
        WecomSyncStatusVO status = syncService.runSync(config, runBO == null ? new WecomSyncRunBO() : runBO);
        config.setLastSyncTime(status.getLastSyncTime());
        config.setLastSyncStatus(status.getLastSyncStatus());
        config.setLastSyncError(status.getLastSyncError());
        configMapper.updateById(config);
        return status;
    }

    @Transactional(rollbackFor = Exception.class)
    public WecomSyncStatusVO syncOrganization() {
        WecomCorpConfig config = requireConfig();
        int saved = syncService.syncOrganization(config);
        WecomSyncStatusVO status = buildSyncStatus(config, saved, saved);
        config.setLastSyncTime(status.getLastSyncTime());
        config.setLastSyncStatus(status.getLastSyncStatus());
        config.setLastSyncError(status.getLastSyncError());
        configMapper.updateById(config);
        return status;
    }

    @Transactional(rollbackFor = Exception.class)
    public WecomSyncStatusVO syncMyCustomers() {
        LoginUser loginUser = UserUtil.getLoginUser();
        Long currentUserId = loginUser.getUser().getUserId();
        ExternalAuthIdentity identity = identityMapper.selectOne(Wrappers.<ExternalAuthIdentity>lambdaQuery()
                .eq(ExternalAuthIdentity::getProvider, "wecom")
                .eq(ExternalAuthIdentity::getTenantId, loginUser.getUser().getTenantId())
                .eq(ExternalAuthIdentity::getUserId, currentUserId)
                .eq(ExternalAuthIdentity::getStatus, 1)
                .last("LIMIT 1"));
        if (identity == null || StrUtil.isBlank(identity.getSubject())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Please bind WeCom in personal center first");
        }
        String corpId = resolveSubjectCorpId(identity.getSubject());
        String wecomUserId = resolveSubjectUserId(identity.getSubject());
        if (StrUtil.isBlank(corpId) || StrUtil.isBlank(wecomUserId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom binding is incomplete");
        }
        WecomCorpConfig config = requireConfig(corpId);
        WecomSyncServiceImpl.CustomerSyncResult syncResult = syncService.syncVisibleCustomersWithResult(config, wecomUserId, currentUserId);
        WecomSyncStatusVO status = buildSyncStatus(config, syncResult.fetched(), syncResult.saved());
        status.setFailedCount(syncResult.failed());
        status.setLastSyncError(syncResult.errorMessage());
        config.setLastSyncTime(status.getLastSyncTime());
        config.setLastSyncStatus(status.getLastSyncStatus());
        config.setLastSyncError(status.getLastSyncError());
        configMapper.updateById(config);
        return status;
    }

    public WecomSyncStatusVO getSyncStatus() {
        WecomCorpConfig config = findConfig();
        WecomSyncLog latest = syncLogMapper.selectOne(Wrappers.<WecomSyncLog>lambdaQuery()
                .orderByDesc(WecomSyncLog::getStartedAt)
                .last("LIMIT 1"));
        WecomSyncStatusVO status = new WecomSyncStatusVO();
        if (config != null) {
            status.setCorpId(config.getCorpId());
            status.setLastSyncTime(config.getLastSyncTime());
            status.setLastSyncStatus(config.getLastSyncStatus());
            status.setLastSyncError(config.getLastSyncError());
        }
        if (latest != null) {
            status.setLastSyncTime(latest.getFinishedAt());
            status.setLastSyncStatus(latest.getStatus());
            status.setLastSyncError(latest.getErrorMessage());
            status.setFetchedCount(latest.getFetchedCount());
            status.setSavedCount(latest.getSavedCount());
            status.setFailedCount(latest.getFailedCount());
        }
        return status;
    }

    public BasePage<WecomEmployeeSessionVO> queryEmployeeSessions(WecomEmployeeSessionQueryBO queryBO) {
        LambdaQueryWrapper<WecomEmployee> wrapper = Wrappers.<WecomEmployee>lambdaQuery()
                .like(StrUtil.isNotBlank(queryBO.getKeyword()), WecomEmployee::getName, queryBO.getKeyword())
                .eq(StrUtil.isNotBlank(queryBO.getUserId()), WecomEmployee::getUserId, queryBO.getUserId())
                .orderByAsc(WecomEmployee::getName);
        DataPermissionContext context = dataPermissionService.createContext("wecomCustomerSession");
        if (!context.isAllData()) {
            if (context.isEmpty()) {
                wrapper.eq(WecomEmployee::getId, -1L);
            } else {
                wrapper.in(WecomEmployee::getCrmUserId, context.getUserIds());
            }
        }
        IPage<WecomEmployee> page = employeeMapper.selectPage(queryBO.parse(), wrapper);
        BasePage<WecomEmployeeSessionVO> result = new BasePage<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toEmployeeSessionVO).toList());
        return result;
    }

    public BasePage<WecomConversationVO> queryConversations(WecomConversationQueryBO queryBO) {
        String type = normalizeConversationType(queryBO.getConversationType());
        LambdaQueryWrapper<WecomConversation> wrapper = Wrappers.<WecomConversation>lambdaQuery()
                .eq(StrUtil.isNotBlank(type), WecomConversation::getConversationType, type)
                .eq(queryBO.getEmployeeId() != null, WecomConversation::getEmployeeId, queryBO.getEmployeeId())
                .eq(StrUtil.isNotBlank(queryBO.getEmployeeUserId()), WecomConversation::getEmployeeUserId, queryBO.getEmployeeUserId())
                .eq(queryBO.getExternalCustomerId() != null, WecomConversation::getExternalCustomerId, queryBO.getExternalCustomerId())
                .eq(queryBO.getGroupChatId() != null, WecomConversation::getGroupChatId, queryBO.getGroupChatId())
                .eq(queryBO.getCustomerId() != null, WecomConversation::getCustomerId, queryBO.getCustomerId())
                .and(StrUtil.isNotBlank(queryBO.getKeyword()), nested -> nested
                        .like(WecomConversation::getTitle, queryBO.getKeyword())
                        .or()
                        .like(WecomConversation::getPeerName, queryBO.getKeyword()))
                .orderByDesc(WecomConversation::getLastMsgTime)
                .orderByDesc(WecomConversation::getUpdateTime);
        applyConversationDataPermission(wrapper, type);
        IPage<WecomConversation> page = conversationMapper.selectPage(queryBO.parse(), wrapper);
        BasePage<WecomConversationVO> result = new BasePage<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(this::toConversationVO).toList());
        return result;
    }

    public BasePage<WecomMessageVO> queryConversationMessages(Long conversationId, int page, int limit) {
        WecomConversation conversation = requireConversationAccess(conversationId);
        BasePage<WecomMessage> messagePage = new BasePage<>(Math.max(page, 1), limit <= 0 ? 50 : Math.min(limit, 200));
        IPage<WecomMessage> data = messageMapper.selectPage(messagePage, Wrappers.<WecomMessage>lambdaQuery()
                .eq(WecomMessage::getConversationId, conversation.getId())
                .orderByAsc(WecomMessage::getMsgTime)
                .orderByAsc(WecomMessage::getSeq)
                .orderByAsc(WecomMessage::getCreateTime));
        BasePage<WecomMessageVO> result = new BasePage<>(data.getCurrent(), data.getSize(), data.getTotal());
        result.setRecords(data.getRecords().stream().map(this::toMessageVO).toList());
        return result;
    }

    public BasePage<WecomExternalCustomerVO> queryExternalCustomers(WecomCustomerQueryBO queryBO) {
        LambdaQueryWrapper<WecomExternalCustomer> wrapper = Wrappers.<WecomExternalCustomer>lambdaQuery()
                .eq(StrUtil.isNotBlank(queryBO.getBindStatus()), WecomExternalCustomer::getBindStatus, queryBO.getBindStatus())
                .eq(queryBO.getCustomerId() != null, WecomExternalCustomer::getCustomerId, queryBO.getCustomerId())
                .and(StrUtil.isNotBlank(queryBO.getKeyword()), nested -> nested
                        .like(WecomExternalCustomer::getName, queryBO.getKeyword())
                        .or()
                        .like(WecomExternalCustomer::getCorpName, queryBO.getKeyword())
                        .or()
                        .like(WecomExternalCustomer::getExternalUserId, queryBO.getKeyword()))
                .orderByDesc(WecomExternalCustomer::getSyncedAt)
                .orderByDesc(WecomExternalCustomer::getUpdateTime);
        IPage<WecomExternalCustomer> page = externalCustomerMapper.selectPage(queryBO.parse(), wrapper);
        Map<Long, Customer> customers = loadCustomers(page.getRecords().stream()
                .map(WecomExternalCustomer::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        BasePage<WecomExternalCustomerVO> result = new BasePage<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(item -> toExternalCustomerVO(item, customers)).toList());
        return result;
    }

    public WecomExternalCustomerVO getExternalCustomer(Long id) {
        WecomExternalCustomer externalCustomer = externalCustomerMapper.selectById(id);
        if (externalCustomer == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST);
        }
        Map<Long, Customer> customers = loadCustomers(externalCustomer.getCustomerId() == null
                ? Set.of()
                : Set.of(externalCustomer.getCustomerId()));
        return toExternalCustomerVO(externalCustomer, customers);
    }

    public List<WecomCustomerBindingVO> getCustomerBindings(Long customerId) {
        return bindingService.queryByCustomerId(customerId);
    }

    public List<WecomConversationTabVO> getCustomerConversationTabs(Long customerId) {
        List<WecomCustomerBindingVO> bindings = bindingService.queryByCustomerId(customerId);
        List<Long> externalCustomerIds = bindings.stream()
                .map(WecomCustomerBindingVO::getExternalCustomerId)
                .filter(Objects::nonNull)
                .toList();
        LambdaQueryWrapper<WecomConversation> wrapper = Wrappers.<WecomConversation>lambdaQuery()
                .and(nested -> {
                    nested.eq(WecomConversation::getCustomerId, customerId);
                    if (!externalCustomerIds.isEmpty()) {
                        nested.or().in(WecomConversation::getExternalCustomerId, externalCustomerIds);
                    }
                });
        applyConversationDataPermission(wrapper, TYPE_CUSTOMER);
        return conversationMapper.selectList(wrapper)
                .stream()
                .sorted(Comparator.comparing(WecomConversation::getLastMsgTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toConversationTabVO)
                .toList();
    }

    public BasePage<WecomMessageVO> queryCustomerConversationMessages(Long customerId, Long conversationId, int page, int limit) {
        WecomConversation conversation = requireConversationAccess(conversationId);
        if (!Objects.equals(conversation.getCustomerId(), customerId)) {
            List<Long> externalCustomerIds = bindingService.queryByCustomerId(customerId).stream()
                    .map(WecomCustomerBindingVO::getExternalCustomerId)
                    .filter(Objects::nonNull)
                    .toList();
            if (!externalCustomerIds.contains(conversation.getExternalCustomerId())) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH);
            }
        }
        return queryConversationMessages(conversationId, page, limit);
    }

    private WecomCorpConfig findConfig() {
        return configMapper.selectOne(Wrappers.<WecomCorpConfig>lambdaQuery()
                .last("""
                        ORDER BY CASE
                            WHEN auth_status = 'AUTHORIZED' THEN 0
                            ELSE 1
                        END, update_time DESC
                        LIMIT 1
                        """));
    }

    private WecomCorpConfig requireConfig() {
        WecomCorpConfig config = findConfig();
        if (config == null || StrUtil.isBlank(config.getCorpId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    openPlatformService.isUsable() ? AUTH_REQUIRED_MESSAGE : "WeCom config is not complete");
        }
        if (!openPlatformService.isAuthorized(config)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, AUTH_REQUIRED_MESSAGE);
        }
        return config;
    }

    private WecomCorpConfig requireConfig(String corpId) {
        WecomCorpConfig config = configMapper.selectOne(Wrappers.<WecomCorpConfig>lambdaQuery()
                .eq(WecomCorpConfig::getCorpId, corpId)
                .last("""
                        ORDER BY CASE
                            WHEN auth_status = 'AUTHORIZED' THEN 0
                            ELSE 1
                        END, update_time DESC
                        LIMIT 1
                        """));
        if (config == null || StrUtil.isBlank(config.getCorpId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    openPlatformService.isUsable() ? AUTH_REQUIRED_MESSAGE : "WeCom config is not complete");
        }
        if (!openPlatformService.isAuthorized(config)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, AUTH_REQUIRED_MESSAGE);
        }
        return config;
    }

    private WecomSyncStatusVO buildSyncStatus(WecomCorpConfig config, int fetched, int saved) {
        WecomSyncStatusVO status = new WecomSyncStatusVO();
        status.setCorpId(config.getCorpId());
        status.setLastSyncTime(new Date());
        status.setLastSyncStatus("success");
        status.setFetchedCount(fetched);
        status.setSavedCount(saved);
        status.setFailedCount(0);
        return status;
    }

    private String resolveSubjectCorpId(String subject) {
        int index = subject == null ? -1 : subject.indexOf(':');
        return index <= 0 ? null : subject.substring(0, index);
    }

    private String resolveSubjectUserId(String subject) {
        int index = subject == null ? -1 : subject.indexOf(':');
        if (index < 0 || index + 1 >= subject.length()) {
            return null;
        }
        return subject.substring(index + 1);
    }

    private WecomConfigVO toConfigVO(WecomCorpConfig config) {
        WecomConfigVO vo = new WecomConfigVO();
        vo.setThirdPartyEnabled(openPlatformService.isUsable());
        vo.setThirdPartyAuthorized(config != null && openPlatformService.isAuthorized(config));
        if (config == null) {
            vo.setAuthStatus("UNAUTHORIZED");
            return vo;
        }
        vo.setId(config.getId());
        vo.setCorpId(config.getCorpId());
        vo.setCorpName(config.getCorpName());
        vo.setAgentId(config.getAgentId());
        vo.setAuthStatus(config.getAuthStatus());
        vo.setAuthorizedAt(config.getAuthorizedAt());
        vo.setUnauthorizedAt(config.getUnauthorizedAt());
        vo.setArchiveSecretConfigured(StrUtil.isNotBlank(config.getArchiveSecretEncrypted()));
        vo.setArchivePrivateKeyConfigured(StrUtil.isNotBlank(config.getArchivePrivateKeyEncrypted()));
        vo.setArchivePublicKeyVersion(config.getArchivePublicKeyVersion());
        vo.setArchiveCorpId(config.getArchiveCorpId());
        vo.setArchiveEnabled(config.getArchiveEnabled());
        vo.setCustomerContactEnabled(config.getCustomerContactEnabled());
        vo.setSyncEnabled(config.getSyncEnabled());
        vo.setLastSyncTime(config.getLastSyncTime());
        vo.setLastSyncStatus(config.getLastSyncStatus());
        vo.setLastSyncError(config.getLastSyncError());
        return vo;
    }

    private WecomEmployeeSessionVO toEmployeeSessionVO(WecomEmployee employee) {
        WecomEmployeeSessionVO vo = BeanUtil.copyProperties(employee, WecomEmployeeSessionVO.class);
        vo.setConversationCount(conversationMapper.selectCount(Wrappers.<WecomConversation>lambdaQuery()
                .eq(WecomConversation::getEmployeeUserId, employee.getUserId())));
        WecomConversation latest = conversationMapper.selectOne(Wrappers.<WecomConversation>lambdaQuery()
                .eq(WecomConversation::getEmployeeUserId, employee.getUserId())
                .orderByDesc(WecomConversation::getLastMsgTime)
                .last("LIMIT 1"));
        vo.setLastMsgTime(latest == null ? null : latest.getLastMsgTime());
        return vo;
    }

    private WecomConversationVO toConversationVO(WecomConversation conversation) {
        return BeanUtil.copyProperties(conversation, WecomConversationVO.class);
    }

    private WecomMessageVO toMessageVO(WecomMessage message) {
        return BeanUtil.copyProperties(message, WecomMessageVO.class);
    }

    private WecomExternalCustomerVO toExternalCustomerVO(WecomExternalCustomer customer, Map<Long, Customer> customers) {
        WecomExternalCustomerVO vo = BeanUtil.copyProperties(customer, WecomExternalCustomerVO.class);
        Customer boundCustomer = customers.get(customer.getCustomerId());
        if (boundCustomer != null) {
            vo.setCustomerName(boundCustomer.getCompanyName());
        }
        return vo;
    }

    private WecomConversationTabVO toConversationTabVO(WecomConversation conversation) {
        WecomConversationTabVO vo = new WecomConversationTabVO();
        vo.setConversationId(conversation.getId());
        vo.setTabKey("wecom-" + conversation.getId());
        vo.setTitle(StrUtil.blankToDefault(conversation.getTitle(),
                StrUtil.blankToDefault(conversation.getEmployeeUserId(), "WeCom")));
        vo.setEmployeeUserId(conversation.getEmployeeUserId());
        vo.setEmployeeName(conversation.getEmployeeUserId());
        vo.setConversationType(conversation.getConversationType());
        vo.setLastMsgTime(conversation.getLastMsgTime());
        vo.setMessageCount(conversation.getMessageCount());
        return vo;
    }

    private Map<Long, Customer> loadCustomers(Set<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Map.of();
        }
        return customerMapper.selectBatchIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private WecomConversation requireConversationAccess(Long conversationId) {
        WecomConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST);
        }
        DataPermissionContext context = dataPermissionService.createContext(resolveConversationModule(conversation.getConversationType()));
        if (!context.isAllData() && (context.isEmpty()
                || conversation.getOwnerUserId() == null
                || !context.getUserIds().contains(conversation.getOwnerUserId()))) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_AUTH);
        }
        return conversation;
    }

    private void applyConversationDataPermission(LambdaQueryWrapper<WecomConversation> wrapper, String conversationType) {
        String module = resolveConversationModule(conversationType);
        DataPermissionContext context = dataPermissionService.createContext(module);
        if (context.isAllData()) {
            return;
        }
        if (context.isEmpty()) {
            wrapper.eq(WecomConversation::getId, -1L);
        } else {
            wrapper.in(WecomConversation::getOwnerUserId, context.getUserIds());
        }
    }

    private String resolveConversationModule(String conversationType) {
        return switch (normalizeConversationType(conversationType)) {
            case TYPE_EMPLOYEE -> "wecomEmployeeSession";
            case TYPE_GROUP -> "wecomGroupSession";
            default -> "wecomCustomerSession";
        };
    }

    private String normalizeConversationType(String type) {
        if (StrUtil.equalsAnyIgnoreCase(type, TYPE_EMPLOYEE, TYPE_CUSTOMER, TYPE_GROUP)) {
            return type.toLowerCase();
        }
        return StrUtil.isBlank(type) ? TYPE_CUSTOMER : type.toLowerCase();
    }

    private void setEncryptedIfPresent(String plainText, java.util.function.Consumer<String> setter) {
        if (StrUtil.isNotBlank(plainText)) {
            setter.accept(secretTextCipher.encrypt(StrUtil.trim(plainText)));
        }
    }
}
