package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.entity.BO.TokenPurchaseCreateBO;
import com.kakarote.ai_crm.entity.VO.TokenPurchaseOptionVO;
import com.kakarote.ai_crm.entity.VO.TokenPurchaseOrderVO;

import java.util.List;
import java.util.Map;

public interface ITokenPurchaseService {

    TokenPurchaseOptionVO getOptions();

    TokenPurchaseOrderVO createOrder(TokenPurchaseCreateBO createBO);

    TokenPurchaseOrderVO getOrder(String orderNo);

    List<TokenPurchaseOrderVO> listRecentOrders(int limit);

    String handleWechatNotify(String timestamp, String nonce, String signature, String serial, String body);

    String handleAlipayNotify(Map<String, String> params);
}
