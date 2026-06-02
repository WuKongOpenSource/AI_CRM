package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.ai_crm.entity.PO.WecomMessage;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class WecomMessageNormalizeService {

    public WecomMessage normalize(Long conversationId, JSONObject raw) {
        WecomMessage message = new WecomMessage();
        message.setConversationId(conversationId);
        message.setMsgId(raw.getString("msgid"));
        message.setSeq(raw.getLong("seq"));
        message.setAction(StrUtil.blankToDefault(raw.getString("action"), "send"));
        message.setMsgType(StrUtil.blankToDefault(raw.getString("msgtype"), "unknown"));
        message.setSenderId(raw.getString("from"));
        message.setSenderType(resolveSenderType(raw.getString("from")));
        message.setReceiverList(normalizeReceiverList(raw.getJSONArray("tolist")));
        message.setMsgTime(resolveMessageTime(raw));
        message.setContentText(extractContentText(raw));
        message.setContentJson(extractContentJson(raw));
        message.setSdkFileId(extractSdkFileId(raw));
        message.setFileName(extractFileName(raw));
        message.setFileSize(extractFileSize(raw));
        message.setRecalled(isRecall(raw));
        message.setRawJson(raw.toJSONString());
        return message;
    }

    private String normalizeReceiverList(JSONArray receivers) {
        if (receivers == null || receivers.isEmpty()) {
            return "[]";
        }
        return JSON.toJSONString(receivers.toJavaList(String.class));
    }

    private Date resolveMessageTime(JSONObject raw) {
        Long msgTime = raw.getLong("msgtime");
        return msgTime == null ? null : new Date(msgTime);
    }

    private boolean isRecall(JSONObject raw) {
        return "recall".equalsIgnoreCase(raw.getString("action"))
                || "revoke".equalsIgnoreCase(raw.getString("msgtype"));
    }

    private String resolveSenderType(String senderId) {
        if (StrUtil.isBlank(senderId)) {
            return "unknown";
        }
        return senderId.startsWith("wm") ? "external" : "employee";
    }

    private String extractContentText(JSONObject raw) {
        String msgType = StrUtil.blankToDefault(raw.getString("msgtype"), "unknown");
        if (isRecall(raw)) {
            JSONObject revoke = raw.getJSONObject("revoke");
            return "Message recalled: " + (revoke == null ? raw.getString("msgid") : revoke.getString("pre_msgid"));
        }
        return switch (msgType) {
            case "text" -> nestedString(raw, "text", "content");
            case "image" -> "[image]";
            case "file" -> StrUtil.blankToDefault(nestedString(raw, "file", "filename"), "[file]");
            case "voice" -> "[voice]";
            case "link" -> firstNotBlank(nestedString(raw, "link", "title"), nestedString(raw, "link", "description"), "[link]");
            case "weapp" -> firstNotBlank(nestedString(raw, "weapp", "title"), nestedString(raw, "weapp", "displayname"), "[mini program]");
            default -> StrUtil.blankToDefault(raw.getString("content"), "[" + msgType + "]");
        };
    }

    private String extractContentJson(JSONObject raw) {
        String msgType = StrUtil.blankToDefault(raw.getString("msgtype"), "unknown");
        JSONObject content = raw.getJSONObject(msgType);
        if (content != null) {
            return content.toJSONString();
        }
        return raw.toJSONString();
    }

    private String extractSdkFileId(JSONObject raw) {
        for (String key : List.of("image", "file", "voice", "video")) {
            String sdkFileId = nestedString(raw, key, "sdkfileid");
            if (StrUtil.isNotBlank(sdkFileId)) {
                return sdkFileId;
            }
        }
        return null;
    }

    private String extractFileName(JSONObject raw) {
        return firstNotBlank(nestedString(raw, "file", "filename"), nestedString(raw, "image", "filename"));
    }

    private Long extractFileSize(JSONObject raw) {
        for (String key : List.of("file", "image", "voice", "video")) {
            JSONObject content = raw.getJSONObject(key);
            if (content != null && content.getLong("filesize") != null) {
                return content.getLong("filesize");
            }
        }
        return null;
    }

    private String nestedString(JSONObject raw, String objectKey, String valueKey) {
        JSONObject object = raw.getJSONObject(objectKey);
        return object == null ? null : object.getString(valueKey);
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
