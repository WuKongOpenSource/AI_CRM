package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.TokenPurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TokenPurchaseOrderMapper extends BaseMapper<TokenPurchaseOrder> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM crm_token_purchase_order
            WHERE order_no = #{orderNo}
            LIMIT 1
            """)
    TokenPurchaseOrder selectGlobalByOrderNo(@Param("orderNo") String orderNo);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT *
            FROM crm_token_purchase_order
            WHERE order_no = #{orderNo}
            LIMIT 1
            FOR UPDATE
            """)
    TokenPurchaseOrder selectGlobalByOrderNoForUpdate(@Param("orderNo") String orderNo);
}
