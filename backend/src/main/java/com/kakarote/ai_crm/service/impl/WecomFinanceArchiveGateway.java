package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;

import java.util.List;

public interface WecomFinanceArchiveGateway {

    List<JSONObject> fetchMessages(WecomCorpConfig config, long startSeq, int limit);
}
