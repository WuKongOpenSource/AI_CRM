package com.kakarote.ai_crm.ai.state;

import cn.hutool.core.bean.BeanUtil;
import com.kakarote.ai_crm.entity.BO.CustomerAddBO;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 AI 会话暂存待确认的客户创建草稿。
 */
@Component
public class PendingCustomerCreationStore {

    private static final Duration PENDING_TTL = Duration.ofMinutes(30);

    private final Map<Long, PendingCustomerCreation> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 保存Pending客户CreationStore。
     */
    public void save(Long sessionId, CustomerAddBO customerAddBO) {
        if (sessionId == null || customerAddBO == null) {
            return;
        }
        pendingRequests.put(
            sessionId,
            new PendingCustomerCreation(copyCustomerAddBO(customerAddBO), Instant.now().plus(PENDING_TTL))
        );
    }

    /**
     * 获取Pending客户CreationStore。
     */
    public PendingCustomerCreation get(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        PendingCustomerCreation pending = pendingRequests.get(sessionId);
        if (pending == null) {
            return null;
        }
        if (pending.isExpired()) {
            pendingRequests.remove(sessionId);
            return null;
        }
        return pending;
    }

    /**
     * 移除Pending客户CreationStore。
     */
    public PendingCustomerCreation remove(Long sessionId) {
        PendingCustomerCreation pending = get(sessionId);
        if (pending != null) {
            pendingRequests.remove(sessionId);
        }
        return pending;
    }

    /**
     * 清理Pending客户CreationStore。
     */
    public void clear(Long sessionId) {
        if (sessionId != null) {
            pendingRequests.remove(sessionId);
        }
    }

    /**
     * 复制客户ADDBO。
     */
    private CustomerAddBO copyCustomerAddBO(CustomerAddBO source) {
        CustomerAddBO copy = BeanUtil.copyProperties(source, CustomerAddBO.class);
        if (source.getTags() != null) {
            copy.setTags(new ArrayList<>(source.getTags()));
        }
        if (source.getCustomFields() != null) {
            copy.setCustomFields(new HashMap<>(source.getCustomFields()));
        }
        return copy;
    }

    public record PendingCustomerCreation(CustomerAddBO customerAddBO, Instant expiresAt) {

        /**
         * 判断是否Expired。
         */
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
