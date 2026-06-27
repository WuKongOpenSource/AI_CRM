package com.kakarote.ai_crm.im.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * 在线状态：连接时写 Redis 键（长 TTL）并广播，断开时删除并广播。
 * 下线由 STOMP 断开事件驱动；TTL 仅作为异常断网（无正常断开事件）客户端的兜底，
 * 期间该用户可能仍显示在线（Phase 1 可接受的近似在线状态）。
 */
@Component
public class ImPresenceService {

    private static final String KEY_PREFIX = "im:online:";
    private static final Duration TTL = Duration.ofHours(12);
    public static final String PRESENCE_TOPIC = "/topic/im.presence";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void markOnline(String userId) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", TTL);
        messagingTemplate.convertAndSend(PRESENCE_TOPIC, Map.of("userId", userId, "online", true));
    }

    public void markOffline(String userId) {
        stringRedisTemplate.delete(KEY_PREFIX + userId);
        messagingTemplate.convertAndSend(PRESENCE_TOPIC, Map.of("userId", userId, "online", false));
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + userId));
    }
}
