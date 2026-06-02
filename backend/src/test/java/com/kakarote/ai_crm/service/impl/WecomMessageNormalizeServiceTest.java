package com.kakarote.ai_crm.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.entity.PO.WecomMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WecomMessageNormalizeServiceTest {

    @Test
    void normalizeShouldConvertArchiveTextMessageToStoredMessage() {
        WecomMessageNormalizeService service = new WecomMessageNormalizeService();
        JSONObject raw = JSON.parseObject("""
                {
                  "msgid": "msg-1",
                  "action": "send",
                  "from": "zhangsan",
                  "tolist": ["wm-external-1"],
                  "msgtime": 1717200000000,
                  "msgtype": "text",
                  "text": {"content": "报价方案已发出"}
                }
                """);

        WecomMessage message = service.normalize(9L, raw);

        assertThat(message.getConversationId()).isEqualTo(9L);
        assertThat(message.getMsgId()).isEqualTo("msg-1");
        assertThat(message.getSenderId()).isEqualTo("zhangsan");
        assertThat(message.getReceiverList()).isEqualTo("[\"wm-external-1\"]");
        assertThat(message.getMsgType()).isEqualTo("text");
        assertThat(message.getContentText()).isEqualTo("报价方案已发出");
        assertThat(message.getRecalled()).isFalse();
    }

    @Test
    void normalizeShouldMarkRecallMessages() {
        WecomMessageNormalizeService service = new WecomMessageNormalizeService();
        JSONObject raw = JSON.parseObject("""
                {
                  "msgid": "msg-2",
                  "action": "recall",
                  "from": "zhangsan",
                  "tolist": ["lisi"],
                  "msgtime": 1717200001000,
                  "msgtype": "revoke",
                  "revoke": {"pre_msgid": "msg-1"}
                }
                """);

        WecomMessage message = service.normalize(9L, raw);

        assertThat(message.getRecalled()).isTrue();
        assertThat(message.getContentText()).contains("msg-1");
    }

    @Test
    void receiverListShouldBeEmptyJsonArrayWhenMissing() {
        WecomMessageNormalizeService service = new WecomMessageNormalizeService();
        JSONObject raw = new JSONObject();
        raw.put("msgid", "msg-3");
        raw.put("msgtype", "text");

        WecomMessage message = service.normalize(9L, raw);

        assertThat(JSON.parseArray(message.getReceiverList(), String.class)).isEqualTo(List.of());
    }
}
