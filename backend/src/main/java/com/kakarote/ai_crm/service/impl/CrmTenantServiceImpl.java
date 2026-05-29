package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.CrmTenant;
import com.kakarote.ai_crm.mapper.CrmTenantMapper;
import com.kakarote.ai_crm.service.ICrmTenantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrmTenantServiceImpl extends ServiceImpl<CrmTenantMapper, CrmTenant> implements ICrmTenantService {

    /**
     * 获取赠送CreditTotal。
     */
    @Override
    public long getGiftCreditTotal(Long tenantId) {
        CrmTenant tenant = tenantId != null ? getById(tenantId) : null;
        if (tenant == null) {
            return 0L;
        }
        Long total = tenant.getGiftCreditTotal();
        return total != null && total > 0 ? total : DEFAULT_GIFT_CREDIT_TOTAL;
    }

    /**
     * 获取赠送CreditUsed。
     */
    @Override
    public long getGiftCreditUsed(Long tenantId) {
        CrmTenant tenant = tenantId != null ? getById(tenantId) : null;
        if (tenant == null || tenant.getGiftCreditUsed() == null) {
            return 0L;
        }
        return Math.max(tenant.getGiftCreditUsed(), 0L);
    }

    /**
     * 获取赠送Credit剩余。
     */
    @Override
    public long getGiftCreditRemaining(Long tenantId) {
        long total = getGiftCreditTotal(tenantId);
        long used = getGiftCreditUsed(tenantId);
        return Math.max(total - used, 0L);
    }

    /**
     * 判断是否存在可用赠送Credits。
     */
    @Override
    public boolean hasAvailableGiftCredits(Long tenantId) {
        return getGiftCreditRemaining(tenantId) > 0;
    }

    /**
     * 获取PurchasedCreditTotal。
     */
    @Override
    public long getPurchasedCreditTotal(Long tenantId) {
        CrmTenant tenant = tenantId != null ? getById(tenantId) : null;
        if (tenant == null || tenant.getPurchasedCreditTotal() == null) {
            return 0L;
        }
        return Math.max(tenant.getPurchasedCreditTotal(), 0L);
    }

    /**
     * 获取PurchasedCreditUsed。
     */
    @Override
    public long getPurchasedCreditUsed(Long tenantId) {
        CrmTenant tenant = tenantId != null ? getById(tenantId) : null;
        if (tenant == null || tenant.getPurchasedCreditUsed() == null) {
            return 0L;
        }
        return Math.max(tenant.getPurchasedCreditUsed(), 0L);
    }

    /**
     * 获取PurchasedCredit剩余。
     */
    @Override
    public long getPurchasedCreditRemaining(Long tenantId) {
        long total = getPurchasedCreditTotal(tenantId);
        long used = getPurchasedCreditUsed(tenantId);
        return Math.max(total - used, 0L);
    }

    /**
     * 获取TotalCredit剩余。
     */
    @Override
    public long getTotalCreditRemaining(Long tenantId) {
        return getGiftCreditRemaining(tenantId) + getPurchasedCreditRemaining(tenantId);
    }

    /**
     * 判断是否存在可用Credits。
     */
    @Override
    public boolean hasAvailableCredits(Long tenantId) {
        return getTotalCreditRemaining(tenantId) > 0;
    }

    /**
     * 消耗赠送Credits。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumeGiftCredits(Long tenantId, long consumeCredits) {
        if (tenantId == null || consumeCredits <= 0) {
            return;
        }
        baseMapper.consumeGiftCredits(tenantId, consumeCredits);
    }

    /**
     * 消耗Credits。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditConsumeResult consumeCredits(Long tenantId, long consumeCredits) {
        if (tenantId == null || consumeCredits <= 0) {
            return CreditConsumeResult.zero(getTotalCreditRemaining(tenantId));
        }

        CrmTenant tenant = baseMapper.selectByIdForUpdate(tenantId);
        if (tenant == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tenant does not exist");
        }

        long giftTotal = ObjectUtil.defaultIfNull(tenant.getGiftCreditTotal(), DEFAULT_GIFT_CREDIT_TOTAL);
        long giftUsed = Math.max(ObjectUtil.defaultIfNull(tenant.getGiftCreditUsed(), 0L), 0L);
        long giftRemaining = Math.max(giftTotal - giftUsed, 0L);

        long purchasedTotal = Math.max(ObjectUtil.defaultIfNull(tenant.getPurchasedCreditTotal(), 0L), 0L);
        long purchasedUsed = Math.max(ObjectUtil.defaultIfNull(tenant.getPurchasedCreditUsed(), 0L), 0L);
        long purchasedRemaining = Math.max(purchasedTotal - purchasedUsed, 0L);

        long consumeGift = Math.min(giftRemaining, consumeCredits);
        long consumePurchased = Math.min(purchasedRemaining, Math.max(consumeCredits - consumeGift, 0L));
        long balanceBefore = giftRemaining + purchasedRemaining;
        long actualCreditsUsed = consumeGift + consumePurchased;

        if (actualCreditsUsed < consumeCredits) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "AI credits are insufficient");
        }

        tenant.setGiftCreditUsed(giftUsed + consumeGift);
        tenant.setPurchasedCreditUsed(purchasedUsed + consumePurchased);
        updateById(tenant);
        return new CreditConsumeResult(
            actualCreditsUsed,
            consumeGift,
            consumePurchased,
            balanceBefore,
            Math.max(balanceBefore - actualCreditsUsed, 0L)
        );
    }

    /**
     * 新增PurchasedCredits。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPurchasedCredits(Long tenantId, long creditAmount) {
        if (tenantId == null || creditAmount <= 0) {
            return;
        }
        baseMapper.addPurchasedCredits(tenantId, creditAmount);
    }
}
