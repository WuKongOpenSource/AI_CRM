package com.kakarote.ai_crm.plugin;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.kakarote.ai_crm.entity.PO.CrmPluginSubscription;
import com.kakarote.ai_crm.service.PluginAuthService;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 异步、带 HMAC 签名与重试的 webhook 投递（独立 bean,确保 @Async 生效）。
 */
@Slf4j
@Component
public class PluginWebhookDeliverer {

    private static final int MAX_ATTEMPTS = 2;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Autowired
    private PluginAuthService pluginAuthService;

    @Async
    public void deliver(CrmPluginSubscription sub, String eventType, Map<String, Object> data) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventType", eventType);
        envelope.put("timestamp", System.currentTimeMillis());
        envelope.put("data", data);
        String payload = JSON.toJSONString(envelope);

        String secret = secretTextCipher.decrypt(sub.getSigningSecretEncrypted());
        String sig = StrUtil.isBlank(secret) ? "" : hmacSha256Hex(secret, payload);

        String result = "error";
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(sub.getEndpointUrl()))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json")
                        .header("X-Plugin-Event", eventType)
                        .header("X-Plugin-Signature", "sha256=" + sig)
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                int sc = resp.statusCode();
                result = "delivered:" + sc;
                log.info("插件 webhook 投递 plugin={} event={} url={} status={} sigPrefix={}",
                        sub.getPluginId(), eventType, sub.getEndpointUrl(), sc,
                        sig.substring(0, Math.min(12, sig.length())));
                if (sc >= 200 && sc < 300) {
                    break;
                }
            } catch (Exception e) {
                result = "error:" + e.getClass().getSimpleName();
                log.warn("插件 webhook 第{}次投递失败 plugin={} url={} err={}",
                        attempt, sub.getPluginId(), sub.getEndpointUrl(), e.getMessage());
            }
            if (attempt < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(300L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        pluginAuthService.audit(sub.getPluginId(), "subscription:" + sub.getSubscriptionId(),
                "webhook_delivery", eventType, result, sub.getEndpointUrl());
    }

    private static String hmacSha256Hex(String secret, String msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }
}
