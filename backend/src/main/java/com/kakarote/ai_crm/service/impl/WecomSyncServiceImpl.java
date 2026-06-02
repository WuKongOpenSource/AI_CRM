package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.entity.BO.WecomSyncRunBO;
import com.kakarote.ai_crm.entity.PO.WecomConversation;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.entity.PO.WecomEmployee;
import com.kakarote.ai_crm.entity.PO.WecomExternalCustomer;
import com.kakarote.ai_crm.entity.PO.WecomMessage;
import com.kakarote.ai_crm.entity.PO.WecomSyncCursor;
import com.kakarote.ai_crm.entity.PO.WecomSyncLog;
import com.kakarote.ai_crm.entity.VO.WecomSyncStatusVO;
import com.kakarote.ai_crm.mapper.WecomConversationMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.mapper.WecomExternalCustomerMapper;
import com.kakarote.ai_crm.mapper.WecomMessageMapper;
import com.kakarote.ai_crm.mapper.WecomSyncCursorMapper;
import com.kakarote.ai_crm.mapper.WecomSyncLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class WecomSyncServiceImpl {

    private static final String SYNC_STATUS_SUCCESS = "success";
    private static final String SYNC_STATUS_FAILED = "failed";
    private static final String CURSOR_ARCHIVE = "archive";
    private static final String CURSOR_KEY_MAIN = "main";

    @Autowired
    private WecomTokenService tokenService;

    @Autowired
    private WecomApiClient apiClient;

    @Autowired
    private WecomFinanceArchiveGateway archiveGateway;

    @Autowired
    private WecomMessageNormalizeService messageNormalizeService;

    @Autowired
    private WecomEmployeeMapper employeeMapper;

    @Autowired
    private WecomExternalCustomerMapper externalCustomerMapper;

    @Autowired
    private WecomConversationMapper conversationMapper;

    @Autowired
    private WecomMessageMapper messageMapper;

    @Autowired
    private WecomSyncCursorMapper cursorMapper;

    @Autowired
    private WecomSyncLogMapper syncLogMapper;

    @Transactional(rollbackFor = Exception.class)
    public WecomSyncStatusVO runSync(WecomCorpConfig config, WecomSyncRunBO runBO) {
        WecomSyncLog log = new WecomSyncLog();
        log.setCorpId(config.getCorpId());
        log.setSyncType("manual");
        log.setStatus("running");
        log.setFetchedCount(0);
        log.setSavedCount(0);
        log.setFailedCount(0);
        log.setStartedAt(new Date());
        syncLogMapper.insert(log);

        int fetched = 0;
        int saved = 0;
        int failed = 0;
        try {
            if (Boolean.TRUE.equals(runBO.getSyncEmployees())) {
                String token = tokenService.fetchAppAccessToken(config);
                List<JSONObject> employees = apiClient.listEmployees(token);
                fetched += employees.size();
                saved += saveEmployees(config, employees);
            }
            if (Boolean.TRUE.equals(runBO.getSyncCustomers())) {
                String token = tokenService.fetchContactAccessToken(config);
                List<String> followUsers = apiClient.listFollowUsers(token);
                for (String followUser : followUsers) {
                    List<String> externalUserIds = apiClient.listExternalUserIds(token, followUser);
                    fetched += externalUserIds.size();
                    for (String externalUserId : externalUserIds) {
                        saved += saveExternalCustomer(config, apiClient.getExternalCustomer(token, externalUserId));
                    }
                }
            }
            if (Boolean.TRUE.equals(runBO.getSyncConversations())) {
                saved += syncArchiveMessages(config, runBO.getArchiveLimit());
            }
            log.setStatus(SYNC_STATUS_SUCCESS);
        } catch (Exception e) {
            failed++;
            log.setStatus(SYNC_STATUS_FAILED);
            log.setErrorMessage(e.getMessage());
        } finally {
            log.setFetchedCount(fetched);
            log.setSavedCount(saved);
            log.setFailedCount(failed);
            log.setFinishedAt(new Date());
            syncLogMapper.updateById(log);
        }

        WecomSyncStatusVO status = new WecomSyncStatusVO();
        status.setCorpId(config.getCorpId());
        status.setLastSyncTime(log.getFinishedAt());
        status.setLastSyncStatus(log.getStatus());
        status.setLastSyncError(log.getErrorMessage());
        status.setFetchedCount(fetched);
        status.setSavedCount(saved);
        status.setFailedCount(failed);
        return status;
    }

    private int saveEmployees(WecomCorpConfig config, List<JSONObject> employees) {
        int saved = 0;
        for (JSONObject user : employees) {
            String userId = user.getString("userid");
            if (StrUtil.isBlank(userId)) {
                continue;
            }
            WecomEmployee employee = employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                    .eq(WecomEmployee::getCorpId, config.getCorpId())
                    .eq(WecomEmployee::getUserId, userId)
                    .last("LIMIT 1"));
            if (employee == null) {
                employee = new WecomEmployee();
                employee.setCorpId(config.getCorpId());
                employee.setUserId(userId);
            }
            employee.setName(StrUtil.blankToDefault(user.getString("name"), userId));
            employee.setAvatar(user.getString("avatar"));
            employee.setDepartmentList(jsonArrayToString(user.getJSONArray("department")));
            employee.setStatus(user.getInteger("status"));
            employee.setSyncedAt(new Date());
            if (employee.getId() == null) {
                employeeMapper.insert(employee);
            } else {
                employeeMapper.updateById(employee);
            }
            saved++;
        }
        return saved;
    }

    private int saveExternalCustomer(WecomCorpConfig config, JSONObject response) {
        JSONObject customerJson = response.getJSONObject("external_contact");
        if (customerJson == null) {
            return 0;
        }
        String externalUserId = customerJson.getString("external_userid");
        if (StrUtil.isBlank(externalUserId)) {
            return 0;
        }
        WecomExternalCustomer customer = externalCustomerMapper.selectOne(Wrappers.<WecomExternalCustomer>lambdaQuery()
                .eq(WecomExternalCustomer::getCorpId, config.getCorpId())
                .eq(WecomExternalCustomer::getExternalUserId, externalUserId)
                .last("LIMIT 1"));
        if (customer == null) {
            customer = new WecomExternalCustomer();
            customer.setCorpId(config.getCorpId());
            customer.setExternalUserId(externalUserId);
            customer.setBindStatus("UNBOUND");
        }
        customer.setName(StrUtil.blankToDefault(customerJson.getString("name"), externalUserId));
        customer.setAvatar(customerJson.getString("avatar"));
        customer.setType(customerJson.getInteger("type"));
        customer.setGender(customerJson.getInteger("gender"));
        customer.setUnionId(customerJson.getString("unionid"));
        customer.setPosition(customerJson.getString("position"));
        customer.setCorpName(customerJson.getString("corp_name"));
        customer.setCorpFullName(customerJson.getString("corp_full_name"));
        customer.setExternalProfile(response.toJSONString());
        customer.setSyncedAt(new Date());
        if (customer.getId() == null) {
            externalCustomerMapper.insert(customer);
        } else {
            externalCustomerMapper.updateById(customer);
        }
        return 1;
    }

    private int syncArchiveMessages(WecomCorpConfig config, Integer archiveLimit) {
        if (!Boolean.TRUE.equals(config.getArchiveEnabled())) {
            return 0;
        }
        WecomSyncCursor cursor = cursorMapper.selectOne(Wrappers.<WecomSyncCursor>lambdaQuery()
                .eq(WecomSyncCursor::getCorpId, config.getCorpId())
                .eq(WecomSyncCursor::getCursorType, CURSOR_ARCHIVE)
                .eq(WecomSyncCursor::getCursorKey, CURSOR_KEY_MAIN)
                .last("LIMIT 1"));
        long startSeq = cursor == null || cursor.getSeq() == null ? 0L : cursor.getSeq();
        List<JSONObject> messages = archiveGateway.fetchMessages(config, startSeq, resolveArchiveLimit(archiveLimit));
        long maxSeq = startSeq;
        int saved = 0;
        for (JSONObject raw : messages) {
            Long conversationId = ensureConversation(config, raw);
            WecomMessage message = messageNormalizeService.normalize(conversationId, raw);
            message.setCorpId(config.getCorpId());
            Long seq = raw.getLong("seq");
            if (seq != null) {
                message.setSeq(seq);
                maxSeq = Math.max(maxSeq, seq);
            }
            if (StrUtil.isNotBlank(message.getMsgId()) && messageMapper.selectCount(Wrappers.<WecomMessage>lambdaQuery()
                    .eq(WecomMessage::getMsgId, message.getMsgId())) == 0) {
                messageMapper.insert(message);
                saved++;
            }
        }
        if (!Objects.equals(maxSeq, startSeq)) {
            if (cursor == null) {
                cursor = new WecomSyncCursor();
                cursor.setCorpId(config.getCorpId());
                cursor.setCursorType(CURSOR_ARCHIVE);
                cursor.setCursorKey(CURSOR_KEY_MAIN);
                cursor.setSeq(maxSeq);
                cursorMapper.insert(cursor);
            } else {
                cursor.setSeq(maxSeq);
                cursorMapper.updateById(cursor);
            }
        }
        return saved;
    }

    private Long ensureConversation(WecomCorpConfig config, JSONObject raw) {
        String from = raw.getString("from");
        JSONArray toList = raw.getJSONArray("tolist");
        String firstTo = toList == null || toList.isEmpty() ? "" : toList.getString(0);
        String conversationKey = from + ":" + firstTo;
        WecomConversation conversation = conversationMapper.selectOne(Wrappers.<WecomConversation>lambdaQuery()
                .eq(WecomConversation::getCorpId, config.getCorpId())
                .eq(WecomConversation::getChatId, conversationKey)
                .last("LIMIT 1"));
        if (conversation == null) {
            conversation = new WecomConversation();
            conversation.setCorpId(config.getCorpId());
            conversation.setConversationType(firstTo.startsWith("wm") ? "customer" : "employee");
            conversation.setEmployeeUserId(from);
            conversation.setExternalUserId(firstTo.startsWith("wm") ? firstTo : null);
            conversation.setChatId(conversationKey);
            conversation.setTitle(conversationKey);
            conversation.setOwnerUserId(null);
            conversation.setMessageCount(0);
            conversationMapper.insert(conversation);
        }
        conversation.setLastMsgId(raw.getString("msgid"));
        conversation.setLastMsgTime(raw.getLong("msgtime") == null ? null : new Date(raw.getLong("msgtime")));
        conversation.setLastMsgPreview(messageNormalizeService.normalize(conversation.getId(), raw).getContentText());
        conversation.setMessageCount(conversation.getMessageCount() == null ? 1 : conversation.getMessageCount() + 1);
        conversationMapper.updateById(conversation);
        return conversation.getId();
    }

    private String jsonArrayToString(JSONArray array) {
        return array == null ? "[]" : array.toJSONString();
    }

    private int resolveArchiveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }
        return Math.min(limit, 1000);
    }
}
