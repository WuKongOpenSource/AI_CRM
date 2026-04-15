package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.PO.CrmTenant;

public interface ICrmTenantService extends IService<CrmTenant> {

    long DEFAULT_GIFT_TOKEN_TOTAL = 200_000L;

    /**
     * 获取租户赠送 token 总量。
     */
    long getGiftTokenTotal(Long tenantId);

    /**
     * 获取租户赠送 token 已使用量。
     */
    long getGiftTokenUsed(Long tenantId);

    /**
     * 获取租户赠送 token 剩余额度。
     */
    long getGiftTokenRemaining(Long tenantId);

    /**
     * 当前租户是否仍可使用赠送额度。
     */
    boolean hasAvailableGiftTokens(Long tenantId);

    long getPurchasedTokenTotal(Long tenantId);

    long getPurchasedTokenUsed(Long tenantId);

    long getPurchasedTokenRemaining(Long tenantId);

    long getTotalTokenRemaining(Long tenantId);

    boolean hasAvailableTokens(Long tenantId);

    /**
     * 消耗赠送额度。额度不足时会自动扣到 0，不会出现负数。
     */
    void consumeGiftTokens(Long tenantId, long consumeTokens);

    void consumeTokens(Long tenantId, long consumeTokens);

    void addPurchasedTokens(Long tenantId, long tokenAmount);
}
