package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.PO.CrmTenant;

public interface ICrmTenantService extends IService<CrmTenant> {

    long DEFAULT_GIFT_CREDIT_TOTAL = 300L;

    /**
     * 获取租户赠送积分总量。
     */
    long getGiftCreditTotal(Long tenantId);

    /**
     * 获取租户赠送积分已使用量。
     */
    long getGiftCreditUsed(Long tenantId);

    /**
     * 获取租户赠送积分剩余额度。
     */
    long getGiftCreditRemaining(Long tenantId);

    /**
     * 当前租户是否仍可使用赠送额度。
     */
    boolean hasAvailableGiftCredits(Long tenantId);

    /**
     * 获取PurchasedCreditTotal。
     */
    long getPurchasedCreditTotal(Long tenantId);

    /**
     * 获取PurchasedCreditUsed。
     */
    long getPurchasedCreditUsed(Long tenantId);

    /**
     * 获取PurchasedCredit剩余。
     */
    long getPurchasedCreditRemaining(Long tenantId);

    /**
     * 获取TotalCredit剩余。
     */
    long getTotalCreditRemaining(Long tenantId);

    /**
     * 判断是否存在可用积分。
     */
    boolean hasAvailableCredits(Long tenantId);

    /**
     * 消耗赠送额度。额度不足时会自动扣到 0，不会出现负数。
     */
    void consumeGiftCredits(Long tenantId, long consumeCredits);

    /**
     * 消耗积分。
     */
    void consumeCredits(Long tenantId, long consumeCredits);

    /**
     * 新增PurchasedCredits。
     */
    void addPurchasedCredits(Long tenantId, long creditAmount);
}
