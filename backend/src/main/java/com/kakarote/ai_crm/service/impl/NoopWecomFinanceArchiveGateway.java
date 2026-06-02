package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NoopWecomFinanceArchiveGateway implements WecomFinanceArchiveGateway {

    @Override
    public List<JSONObject> fetchMessages(WecomCorpConfig config, long startSeq, int limit) {
        throw new IllegalStateException("WeCom Finance SDK gateway is not configured");
    }
}
