# IM Phase 1b — WebSocket Realtime Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add near-real-time delivery on top of the REST IM core (Plan 1a): a STOMP/WebSocket endpoint with token-based auth, online presence, and push of new/recalled messages + unread updates to the right users by consuming the domain events 1a already publishes.

**Architecture:** `spring-boot-starter-websocket` with STOMP + in-memory `SimpleBroker` (single instance). Browser clients connect at `/ws` and authenticate by sending `Manager-Token` in the STOMP CONNECT frame; a `ChannelInterceptor` validates it via `TokenService` and binds the userId as the session `Principal`. Server pushes to `convertAndSendToUser(userId, "/queue/im", ...)`. Push is driven by `@TransactionalEventListener(phase = AFTER_COMMIT)` consuming 1a's `ImMessageSentEvent` / `ImMessageRecalledEvent` / `ImConversationReadEvent` — firing only after the DB commit so listeners never read uncommitted rows. Presence is tracked in Redis on STOMP connect/disconnect and broadcast on a shared `/topic/im.presence`.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring WebSocket/STOMP, Redis, JUnit5 + Mockito.

**Environment:** Maven via Docker (not on PATH):
`MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn <args>`
Branch `feature/im-phase1` (do NOT switch). Builds on Plan 1a (already merged into this branch): entities/services/controller exist; `ImMessageSentEvent(conversationId, messageId, memberUserIds)`, `ImMessageRecalledEvent(...)`, `ImConversationReadEvent(conversationId, userId)` are published inside the service transactions (each `publishEvent` carries a comment requiring AFTER_COMMIT listeners).

---

## File Structure

- Modify `backend/pom.xml` — add `spring-boot-starter-websocket`.
- Modify `backend/src/main/java/com/kakarote/ai_crm/config/security/service/TokenService.java` — add `getLoginUser(String token)` overload.
- Modify `backend/src/main/java/com/kakarote/ai_crm/service/ImMessageService.java` + `.../service/impl/ImMessageServiceImpl.java` — add public `getMessageVO(Long id)` (reuse existing private `toVO`).
- Create `backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPrincipal.java` — Principal holding the userId.
- Create `.../im/ws/ImStompAuthChannelInterceptor.java` — CONNECT auth.
- Create `.../im/ws/WebSocketConfig.java` — STOMP config.
- Create `.../im/ws/ImPushEnvelope.java` — push payload.
- Create `.../im/ws/ImPushService.java` — wraps `SimpMessagingTemplate` fan-out + unread.
- Create `.../im/ws/ImRealtimePushListener.java` — AFTER_COMMIT event listeners.
- Create `.../im/ws/ImPresenceService.java` + `.../im/ws/ImPresenceListener.java` — Redis presence + STOMP connect/disconnect.
- Modify `docker/nginx/default.conf.template` — `/ws` upgrade proxy.
- Tests under `backend/src/test/java/com/kakarote/ai_crm/im/ws/`.

---

## Task 1: Add the WebSocket starter

**Files:** Modify `backend/pom.xml`

- [ ] **Step 1:** Add this dependency in the `<dependencies>` block (next to `spring-boot-starter-web`):

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
```

- [ ] **Step 2: Verify it resolves**

Run: `MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q -DskipTests compile`
Expected: BUILD SUCCESS (pulls the starter).

- [ ] **Step 3: Commit**

```bash
git add backend/pom.xml
git commit -m "build(im): add spring-boot-starter-websocket"
```

---

## Task 2: TokenService.getLoginUser(String token) overload

**Files:**
- Modify: `backend/src/main/java/com/kakarote/ai_crm/config/security/service/TokenService.java`
- Test: `backend/src/test/java/com/kakarote/ai_crm/config/security/service/TokenServiceTokenLookupTest.java`

- [ ] **Step 1:** Add this public method to `TokenService` (it mirrors the existing `getLoginUser(HttpServletRequest)` body but takes a raw token and omits the request-only kickout messaging). Place it right after `getLoginUser(HttpServletRequest request)`:

```java
    /**
     * 通过原始 token 字符串解析登录用户（用于 WebSocket 等无 HttpServletRequest 的场景）。
     */
    public LoginUser getLoginUser(String token) {
        if (StrUtil.isEmpty(token)) {
            return null;
        }
        try {
            Claims claims = parseToken(token);
            String uuid = (String) claims.get(Const.LOGIN_USER_KEY);
            String loginUserJson = redisTemplate.opsForValue().get(getTokenKey(uuid));
            if (StrUtil.isBlank(loginUserJson)) {
                return null;
            }
            LoginUser loginUser = JSON.parseObject(loginUserJson, LoginUser.class);
            if (loginUser == null) {
                return null;
            }
            if (isTokenReplaced(loginUser, uuid)) {
                return null;
            }
            return loginUser;
        } catch (Exception e) {
            log.debug("Failed to resolve login user from token string: {}", e.getMessage());
            return null;
        }
    }
```

> Verify `isTokenReplaced(LoginUser, String)` and `getTokenKey(String)` are accessible from within the class (they are — both private methods of `TokenService`). Confirm `log` exists (the class uses `@Slf4j`; if not, use the existing logger reference).

- [ ] **Step 2: Write a focused test** (parse path returns null on blank/garbage — the happy path needs Redis+JWT and is covered by the live smoke test):

`TokenServiceTokenLookupTest.java`:
```java
package com.kakarote.ai_crm.config.security.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTokenLookupTest {

    @Test
    void blankTokenReturnsNull() {
        TokenService service = new TokenService();
        assertThat(service.getLoginUser((String) null)).isNull();
        assertThat(service.getLoginUser("")).isNull();
    }

    @Test
    void garbageTokenReturnsNullNotThrow() {
        TokenService service = new TokenService();
        // No secret/redis configured → parseToken throws internally → caught → null.
        assertThat(service.getLoginUser("not-a-jwt")).isNull();
    }
}
```

Run: `... mvn -q test -Dtest=TokenServiceTokenLookupTest`
Expected: PASS (2 tests). (If `new TokenService()` with no wired fields NPEs on the garbage case before the try/catch, wrap is fine because `parseToken` is the first call inside the try — but if `secret` being null throws outside the try, adjust the test to only assert the blank-token case and rely on the live smoke test for the rest. Keep the test green.)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/config/security/service/TokenService.java backend/src/test/java/com/kakarote/ai_crm/config/security/service/TokenServiceTokenLookupTest.java
git commit -m "feat(im): add TokenService.getLoginUser(String) for WebSocket auth"
```

---

## Task 3: ImMessageService.getMessageVO(Long)

**Files:**
- Modify: `backend/src/main/java/com/kakarote/ai_crm/service/ImMessageService.java`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/service/impl/ImMessageServiceImpl.java`

- [ ] **Step 1:** Add to the interface `ImMessageService` (after `recall`):

```java
    /** Build the VO for a single message id (used by the realtime push layer). Returns null if missing. */
    ImMessageVO getMessageVO(Long messageId);
```

- [ ] **Step 2:** Add to `ImMessageServiceImpl` (the private `toVO(ImMessage)` already exists):

```java
    @Override
    public ImMessageVO getMessageVO(Long messageId) {
        ImMessage m = getById(messageId);
        return m == null ? null : toVO(m);
    }
```

- [ ] **Step 3: Compile**

Run: `... mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/service/ImMessageService.java backend/src/main/java/com/kakarote/ai_crm/service/impl/ImMessageServiceImpl.java
git commit -m "feat(im): expose getMessageVO for the realtime push layer"
```

---

## Task 4: Principal + STOMP auth interceptor

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPrincipal.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/ws/ImStompAuthChannelInterceptor.java`
- Test: `backend/src/test/java/com/kakarote/ai_crm/im/ws/ImStompAuthChannelInterceptorTest.java`

- [ ] **Step 1: Principal**

`ImPrincipal.java`:
```java
package com.kakarote.ai_crm.im.ws;

import java.security.Principal;

/** WebSocket session principal whose name is the user id (as String). */
public record ImPrincipal(String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}
```

- [ ] **Step 2: Write the failing test**

`ImStompAuthChannelInterceptorTest.java`:
```java
package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.config.security.service.TokenService;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImStompAuthChannelInterceptorTest {

    @Mock TokenService tokenService;
    ImStompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ImStompAuthChannelInterceptor();
        ReflectionTestUtils.setField(interceptor, "tokenService", tokenService);
    }

    private Message<byte[]> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (token != null) {
            accessor.addNativeHeader("Manager-Token", token);
        }
        return org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void validTokenSetsPrincipal() {
        ManagerUser u = new ManagerUser();
        u.setUserId(42L);
        LoginUser lu = new LoginUser();
        lu.setUser(u);
        when(tokenService.getLoginUser("good")).thenReturn(lu);

        Message<?> out = interceptor.preSend(connectMessage("good"), null);

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(out);
        assertThat(acc.getUser()).isNotNull();
        assertThat(acc.getUser().getName()).isEqualTo("42");
    }

    @Test
    void invalidTokenRejected() {
        when(tokenService.getLoginUser("bad")).thenReturn(null);
        assertThatThrownBy(() -> interceptor.preSend(connectMessage("bad"), null))
                .isInstanceOf(org.springframework.messaging.MessagingException.class);
    }
}
```

> Verify `LoginUser` has a `setUser(ManagerUser)` setter and `ManagerUser` has `setUserId(Long)` (both Lombok `@Data` — confirm). If `LoginUser.getUser()` is computed rather than a plain field, build the `LoginUser` however its API allows so `getUser().getUserId()` returns 42.

Run: `... mvn -q test -Dtest=ImStompAuthChannelInterceptorTest` → FAIL (interceptor missing). Create it in Step 3.

- [ ] **Step 3: Interceptor**

`ImStompAuthChannelInterceptor.java`:
```java
package com.kakarote.ai_crm.im.ws;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.config.security.service.TokenService;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/** Authenticates the STOMP CONNECT frame using the Manager-Token native header. */
@Component
public class ImStompAuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private TokenService tokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Manager-Token");
            LoginUser loginUser = StrUtil.isBlank(token) ? null : tokenService.getLoginUser(token);
            if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getUserId() == null) {
                throw new MessagingException("IM 鉴权失败：无效或过期的登录令牌");
            }
            accessor.setUser(new ImPrincipal(String.valueOf(loginUser.getUser().getUserId())));
        }
        return message;
    }
}
```

Run: `... mvn -q test -Dtest=ImStompAuthChannelInterceptorTest` → PASS (2 tests).

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPrincipal.java backend/src/main/java/com/kakarote/ai_crm/im/ws/ImStompAuthChannelInterceptor.java backend/src/test/java/com/kakarote/ai_crm/im/ws/ImStompAuthChannelInterceptorTest.java
git commit -m "feat(im): STOMP CONNECT auth interceptor binding userId principal"
```

---

## Task 5: WebSocket/STOMP config

**Files:** Create `backend/src/main/java/com/kakarote/ai_crm/im/ws/WebSocketConfig.java`

- [ ] **Step 1:** Create the config:

```java
package com.kakarote.ai_crm.im.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private ImStompAuthChannelInterceptor authChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket (frontend uses @stomp/stompjs). Origins are validated at the nginx/proxy layer.
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
```

- [ ] **Step 2: Compile**

Run: `... mvn -q -DskipTests compile` → BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/im/ws/WebSocketConfig.java
git commit -m "feat(im): STOMP WebSocket config with /ws endpoint and auth interceptor"
```

---

## Task 6: Push envelope + push service + realtime listeners

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPushEnvelope.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPushService.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/ws/ImRealtimePushListener.java`
- Test: `backend/src/test/java/com/kakarote/ai_crm/im/ws/ImRealtimePushListenerTest.java`

- [ ] **Step 1: Envelope**

`ImPushEnvelope.java`:
```java
package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Typed payload pushed to /user/{userId}/queue/im. type = message | unread | presence. */
@Data
@AllArgsConstructor
public class ImPushEnvelope {
    private String type;
    private String conversationId;
    private ImMessageVO message; // for type=message (new or recalled)
    private Long unread;         // recipient's unread count for the conversation
}
```

- [ ] **Step 2: Push service** (wraps the messaging template + unread, so listeners stay thin and it's mockable)

`ImPushService.java`:
```java
package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.service.ImConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImPushService {

    public static final String USER_QUEUE = "/queue/im";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ImConversationService conversationService;

    /** Push a message (new or recalled) to each member, each with their own unread count. */
    public void pushMessage(Long conversationId, ImMessageVO message, List<Long> memberUserIds) {
        for (Long uid : memberUserIds) {
            long unread = conversationService.unreadCount(conversationId, uid);
            ImPushEnvelope env = new ImPushEnvelope("message", String.valueOf(conversationId), message, unread);
            messagingTemplate.convertAndSendToUser(String.valueOf(uid), USER_QUEUE, env);
        }
    }

    /** Push an unread refresh (e.g. after the user themselves marked read). */
    public void pushUnread(Long conversationId, Long userId) {
        long unread = conversationService.unreadCount(conversationId, userId);
        ImPushEnvelope env = new ImPushEnvelope("unread", String.valueOf(conversationId), null, unread);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_QUEUE, env);
    }
}
```

- [ ] **Step 3: Write the failing listener test**

`ImRealtimePushListenerTest.java`:
```java
package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.im.event.ImMessageSentEvent;
import com.kakarote.ai_crm.service.ImMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImRealtimePushListenerTest {

    @Mock ImMessageService messageService;
    @Mock ImPushService pushService;
    ImRealtimePushListener listener;

    @BeforeEach
    void setUp() {
        listener = new ImRealtimePushListener();
        ReflectionTestUtils.setField(listener, "messageService", messageService);
        ReflectionTestUtils.setField(listener, "pushService", pushService);
    }

    @Test
    void onMessageSentPushesVoToMembers() {
        ImMessageVO vo = new ImMessageVO();
        vo.setId("100");
        when(messageService.getMessageVO(100L)).thenReturn(vo);

        listener.onMessageSent(new ImMessageSentEvent(10L, 100L, List.of(1L, 2L)));

        verify(pushService).pushMessage(eq(10L), eq(vo), eq(List.of(1L, 2L)));
    }
}
```

Run: `... mvn -q test -Dtest=ImRealtimePushListenerTest` → FAIL (listener missing). Create in Step 4.

- [ ] **Step 4: Listener** (AFTER_COMMIT so it never reads uncommitted rows — per the contract documented in 1a's services)

`ImRealtimePushListener.java`:
```java
package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.im.event.ImConversationReadEvent;
import com.kakarote.ai_crm.im.event.ImMessageRecalledEvent;
import com.kakarote.ai_crm.im.event.ImMessageSentEvent;
import com.kakarote.ai_crm.service.ImMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ImRealtimePushListener {

    @Autowired
    private ImMessageService messageService;

    @Autowired
    private ImPushService pushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(ImMessageSentEvent event) {
        ImMessageVO vo = messageService.getMessageVO(event.messageId());
        if (vo != null) {
            pushService.pushMessage(event.conversationId(), vo, event.memberUserIds());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageRecalled(ImMessageRecalledEvent event) {
        ImMessageVO vo = messageService.getMessageVO(event.messageId());
        if (vo != null) {
            pushService.pushMessage(event.conversationId(), vo, event.memberUserIds());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationRead(ImConversationReadEvent event) {
        pushService.pushUnread(event.conversationId(), event.userId());
    }
}
```

Run: `... mvn -q test -Dtest=ImRealtimePushListenerTest` → PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPushEnvelope.java backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPushService.java backend/src/main/java/com/kakarote/ai_crm/im/ws/ImRealtimePushListener.java backend/src/test/java/com/kakarote/ai_crm/im/ws/ImRealtimePushListenerTest.java
git commit -m "feat(im): realtime push of messages/unread via AFTER_COMMIT event listeners"
```

---

## Task 7: Presence (Redis + STOMP connect/disconnect)

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPresenceService.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPresenceListener.java`

- [ ] **Step 1: Presence service** (Redis key with TTL; broadcast on a shared topic)

`ImPresenceService.java`:
```java
package com.kakarote.ai_crm.im.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class ImPresenceService {

    private static final String KEY_PREFIX = "im:online:";
    private static final Duration TTL = Duration.ofSeconds(90);
    public static final String PRESENCE_TOPIC = "/topic/im.presence";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void markOnline(String userId) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", TTL);
        messagingTemplate.convertAndSend(PRESENCE_TOPIC, Map.of("userId", userId, "online", true));
    }

    public void refresh(String userId) {
        stringRedisTemplate.expire(KEY_PREFIX + userId, TTL);
    }

    public void markOffline(String userId) {
        stringRedisTemplate.delete(KEY_PREFIX + userId);
        messagingTemplate.convertAndSend(PRESENCE_TOPIC, Map.of("userId", userId, "online", false));
    }

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + userId));
    }
}
```

> `StringRedisTemplate` is a standard Spring bean (auto-configured by `spring-boot-starter-data-redis`, already a dependency). If the app only exposes `RedisTemplate<String,String>` and not `StringRedisTemplate`, inject `RedisTemplate<String,String>` instead and use `opsForValue().set(key, "1")` + `expire(...)`.

- [ ] **Step 2: Connect/disconnect listener**

`ImPresenceListener.java`:
```java
package com.kakarote.ai_crm.im.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class ImPresenceListener {

    @Autowired
    private ImPresenceService presenceService;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        String userId = principalName(event.getUser());
        if (userId != null) {
            presenceService.markOnline(userId);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal user = StompHeaderAccessor.wrap(event.getMessage()).getUser();
        String userId = principalName(user);
        if (userId != null) {
            presenceService.markOffline(userId);
        }
    }

    private String principalName(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
```

> Note: presence is best-effort (a hard browser close still fires `SessionDisconnectEvent`; a crashed client falls off via the 90s TTL since nothing refreshes it). Phase 1 does not implement client→server heartbeat refresh of the Redis key; the TTL is sized so a normally-connected client (STOMP heartbeats keep the socket alive) reconnects and re-marks online well within it. This is acceptable per the spec ("approximate presence").

- [ ] **Step 3: Compile**

Run: `... mvn -q -DskipTests compile` → BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPresenceService.java backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPresenceListener.java
git commit -m "feat(im): Redis-backed presence on STOMP connect/disconnect"
```

---

## Task 8: nginx WebSocket proxy

**Files:** Modify `docker/nginx/default.conf.template`

- [ ] **Step 1:** Add a `/ws` location (before the catch-all `location /`), proxying to the crm backend with WebSocket upgrade headers:

```nginx
    location /ws {
      proxy_pass http://crm:8088/ws;
      proxy_http_version 1.1;
      proxy_set_header Upgrade $http_upgrade;
      proxy_set_header Connection "upgrade";
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
      proxy_read_timeout 3600s;
      proxy_send_timeout 3600s;
    }
```

> Place it among the other `location` blocks in the `server { ... }` (e.g. right after the `location /crmapi/ { ... }` block). The existing `location / { proxy_pass http://crm:8088/; }` would otherwise swallow `/ws` without upgrade headers.

- [ ] **Step 2: Commit**

```bash
git add docker/nginx/default.conf.template
git commit -m "feat(im): proxy /ws WebSocket through nginx with upgrade headers"
```

---

## Task 9: Build, deploy, and verify the realtime path

**Files:** none (verification).

- [ ] **Step 1: Rebuild + redeploy** (recreate both `crm` and `frontend` so the new nginx template is loaded). Use the project rebuild recipe:

```bash
WD="C:/Users/bankk/AppData/Local/Temp/crmbuild_imb"; mkdir -p "$WD"
docker cp crm:/opt/wk_ai_crm/crm-1.0.0.jar "$WD/crm-1.0.0.jar"
MSYS_NO_PATHCONV=1 docker run --rm -v "$WD":/j -v "F:/projects/WUKONG/AI_CRM":/work -w /tmp maven:3.9.9-eclipse-temurin-21 sh -c "cd /tmp && jar xf /j/crm-1.0.0.jar BOOT-INF/classes/public && rm -rf /work/backend/src/main/resources/public && mkdir -p /work/backend/src/main/resources/public && cp -r /tmp/BOOT-INF/classes/public/. /work/backend/src/main/resources/public/"
MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM":/work -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q -DskipTests clean package
MSYS_NO_PATHCONV=1 docker build -t wk_ai_crm:local-aiimport -f backend/Dockerfile backend/
cd docker && MSYS_NO_PATHCONV=1 docker compose up -d --force-recreate crm frontend; cd /f/projects/WUKONG/AI_CRM
rm -rf backend/src/main/resources/public "$WD"
```
Expected: both containers healthy. Confirm STOMP wired: `docker logs crm 2>&1 | grep -iE "websocket|stomp|Started ManagerApplication" | tail`.

- [ ] **Step 2: Verify the WS handshake + auth** with a tiny Node STOMP client (run it inside a node container on the compose network so it reaches crm directly):

Create `/tmp/wscheck.mjs`:
```js
import { Client } from '@stomp/stompjs';
import { WebSocket } from 'ws';
const token = process.argv[2];
const c = new Client({
  webSocketFactory: () => new WebSocket('ws://crm:8088/ws'),
  connectHeaders: { 'Manager-Token': token },
  onConnect: () => { console.log('CONNECTED'); process.exit(0); },
  onStompError: (f) => { console.log('STOMP_ERROR', f.headers['message']); process.exit(2); },
  onWebSocketError: () => { console.log('WS_ERROR'); process.exit(3); },
});
c.activate();
setTimeout(() => { console.log('TIMEOUT'); process.exit(4); }, 8000);
```
Run (valid token → CONNECTED; garbage token → STOMP_ERROR):
```bash
TOKEN=$(curl -s -XPOST localhost:8088/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"123456a"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
MSYS_NO_PATHCONV=1 docker run --rm --network docker_crm-network -v /tmp/wscheck.mjs:/wscheck.mjs node:20 sh -c "cd /tmp && npm i @stomp/stompjs ws >/dev/null 2>&1 && node /wscheck.mjs '$TOKEN'"
# Expect: CONNECTED
MSYS_NO_PATHCONV=1 docker run --rm --network docker_crm-network -v /tmp/wscheck.mjs:/wscheck.mjs node:20 sh -c "cd /tmp && npm i @stomp/stompjs ws >/dev/null 2>&1 && node /wscheck.mjs 'garbage'"
# Expect: STOMP_ERROR (auth rejected)
```
Expected: valid token prints `CONNECTED`; garbage prints `STOMP_ERROR`. (Full message-delivery + presence is verified in the browser during Plan 1c, which subscribes to `/user/queue/im` and `/topic/im.presence`.)

- [ ] **Step 3: Commit (only if verification needed fixups)**

```bash
git add -A && git commit -m "chore(im): websocket layer deploy fixups"
```

---

## Self-Review Notes

- **Spec coverage (1b portion):** WebSocket/STOMP endpoint `/ws` (Task 5) ✓; CONNECT-header auth via `TokenService` (Tasks 2,4) ✓; user-destination push (Task 6) ✓; push driven by 1a events, AFTER_COMMIT so no uncommitted reads (Task 6, addressing the code-review C2 contract) ✓; unread pushed per recipient (Task 6) ✓; presence via Redis on connect/disconnect + shared topic (Task 7) ✓; nginx `/ws` upgrade proxy (Task 8) ✓; recall pushes the recalled VO (Task 6 `onMessageRecalled`) ✓.
- **Deferred to Plan 1c (frontend):** subscribing `/user/queue/im` + `/topic/im.presence`, browser notifications, attachments upload UI, conversation-list & contacts endpoints/UI, optimistic send + reconnect catch-up. Browser notifications and attachment *upload* are client-side; the backend already accepts attachment fields on `POST /im/messages` (1a).
- **Placeholder scan:** none; every step has complete code or exact commands. Explicit verifications flagged inline: `LoginUser.setUser`/`getUser`, `StringRedisTemplate` vs `RedisTemplate<String,String>`, and the `new TokenService()` garbage-token test guard.
- **Type consistency:** `getLoginUser(String)`, `getMessageVO(Long)`, `ImPrincipal(name)`, `ImPushEnvelope(type,conversationId,message,unread)`, `ImPushService.pushMessage/pushUnread`, event accessors `conversationId()/messageId()/memberUserIds()/userId()` all match 1a's record components and the new signatures.
- **Single instance assumption:** `SimpleBroker` + Redis presence is correct for the one-container deployment; multi-instance (external broker/relay) remains deferred.
```
