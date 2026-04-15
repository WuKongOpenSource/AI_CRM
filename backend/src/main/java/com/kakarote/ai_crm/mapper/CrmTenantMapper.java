package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.CrmTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CrmTenantMapper extends BaseMapper<CrmTenant> {

    @Update("""
            UPDATE crm_tenant
            SET gift_token_used = LEAST(COALESCE(gift_token_total, 0), COALESCE(gift_token_used, 0) + #{consumeTokens}),
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
              AND COALESCE(gift_token_total, 0) > COALESCE(gift_token_used, 0)
            """)
    int consumeGiftTokens(@Param("tenantId") Long tenantId, @Param("consumeTokens") Long consumeTokens);

    @Select("""
            SELECT *
            FROM crm_tenant
            WHERE tenant_id = #{tenantId}
            FOR UPDATE
            """)
    CrmTenant selectByIdForUpdate(@Param("tenantId") Long tenantId);

    @Update("""
            UPDATE crm_tenant
            SET purchased_token_total = COALESCE(purchased_token_total, 0) + #{tokenAmount},
                update_time = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId}
            """)
    int addPurchasedTokens(@Param("tenantId") Long tenantId, @Param("tokenAmount") Long tokenAmount);
}
