# IM Phase 1a — Backend Core (REST) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the REST-based backend for 1:1 direct messages (conversations, messages, unread, recall, read, contacts) so two users can exchange messages via API — the foundation the WebSocket layer (Plan 1b) and frontend (Plan 1c) build on.

**Architecture:** Conventional `controller → service(impl) → mapper` with MyBatis-Plus. A `direct` conversation is found-or-created by a canonical `member_key` (`<minId>_<maxId>`). Sending persists a message and bumps the conversation; unread is derived from each member's `last_read_message_id`; recall is a soft status change within a 2-minute window. Services publish Spring `ApplicationEvent`s on send/recall/read so Plan 1b can push over WebSocket without changing 1a. **This codebase is single-tenant — no `tenant_id` columns; scoping is by conversation membership.**

**Tech Stack:** Java 21, Spring Boot 3.3, MyBatis-Plus, PostgreSQL, Flyway, JUnit5 + Mockito.

**Environment:** Maven is not on PATH locally — run via the Docker image with a persistent cache:
`MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn <args>`
You are on branch `feature/im-phase1`. Git identity is configured. Do NOT switch branches.

---

## File Structure

- Create `backend/src/main/resources/db/migration/V40__create_im_tables.sql` — 3 IM tables + `im` permission menu + grant to all roles. (Verify V40 is free: `ls backend/src/main/resources/db/migration | grep -oE 'V[0-9]+' | sed s/V// | sort -n | tail -1` → if ≥40, rename to the next free number.)
- Create entities `entity/PO/ImConversation.java`, `ImConversationMember.java`, `ImMessage.java`.
- Create mappers `mapper/ImConversationMapper.java`, `ImConversationMemberMapper.java`, `ImMessageMapper.java`.
- Create BOs/VOs `entity/BO/ImSendMessageBO.java`; `entity/VO/ImConversationVO.java`, `ImMessageVO.java`.
- Create events `ai`-free package `im/event/ImMessageSentEvent.java`, `ImMessageRecalledEvent.java`, `ImConversationReadEvent.java`.
- Create services `service/ImConversationService.java` + `service/impl/ImConversationServiceImpl.java`; `service/ImMessageService.java` + `service/impl/ImMessageServiceImpl.java`.
- Create controller `controller/ImController.java`.
- Tests under `backend/src/test/java/com/kakarote/ai_crm/service/impl/` and `.../im/`.

Conventions to mirror (verified in repo): entities use `@Data @TableName("...") @TableId(type = IdType.ASSIGN_ID) Long`; `MyMetaObjectHandler` auto-fills `createTime`/`updateTime`/`createUserId` when those fields exist; services `extends ServiceImpl<Mapper, PO> implements XxxService`; controllers return `com.kakarote.ai_crm.common.result.Result` and use `@RequirePermission("...")` and `UserUtil.getUserId()`; paging via `com.kakarote.ai_crm.common.BasePage`.

---

## Task 1: Database migration (tables + permission)

**Files:**
- Create: `backend/src/main/resources/db/migration/V40__create_im_tables.sql`

- [ ] **Step 1: Write the migration**

```sql
-- ============================================
-- V40: Instant Messaging Phase 1 — direct-message tables + permission
-- Single-tenant: no tenant_id; scoping is by conversation membership.
-- ============================================

CREATE TABLE IF NOT EXISTS crm_im_conversation (
    id              BIGINT PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL DEFAULT 'direct',
    member_key      VARCHAR(64),
    last_message_id BIGINT,
    create_time     TIMESTAMP(6),
    update_time     TIMESTAMP(6)
);
COMMENT ON TABLE crm_im_conversation IS 'IM 会话';
COMMENT ON COLUMN crm_im_conversation.type IS '会话类型: direct';
COMMENT ON COLUMN crm_im_conversation.member_key IS '私聊去重键: 两个用户ID升序拼接 minId_maxId';
-- Unique direct conversation per user pair
CREATE UNIQUE INDEX IF NOT EXISTS uk_im_conversation_member_key
    ON crm_im_conversation (member_key) WHERE member_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS crm_im_conversation_member (
    id                   BIGINT PRIMARY KEY,
    conversation_id      BIGINT NOT NULL,
    user_id              BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL DEFAULT 0,
    create_time          TIMESTAMP(6),
    update_time          TIMESTAMP(6)
);
COMMENT ON TABLE crm_im_conversation_member IS 'IM 会话成员';
CREATE UNIQUE INDEX IF NOT EXISTS uk_im_member_conv_user
    ON crm_im_conversation_member (conversation_id, user_id);
CREATE INDEX IF NOT EXISTS idx_im_member_user ON crm_im_conversation_member (user_id);

CREATE TABLE IF NOT EXISTS crm_im_message (
    id              BIGINT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id       BIGINT NOT NULL,
    content_type    VARCHAR(20) NOT NULL DEFAULT 'text',
    content         TEXT,
    attachment_name VARCHAR(255),
    attachment_path VARCHAR(500),
    attachment_size BIGINT,
    attachment_mime VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'normal',
    create_time     TIMESTAMP(6),
    update_time     TIMESTAMP(6)
);
COMMENT ON TABLE crm_im_message IS 'IM 消息';
COMMENT ON COLUMN crm_im_message.content_type IS 'text/image/file';
COMMENT ON COLUMN crm_im_message.status IS 'normal/recalled';
CREATE INDEX IF NOT EXISTS idx_im_message_conv ON crm_im_message (conversation_id, id);

-- Permission menu for IM, granted to all existing roles (removable in role mgmt).
INSERT INTO manager_menu (menu_id, parent_id, realm, realm_name, type)
SELECT 9001, 0, 'im', '即时通讯', 3
WHERE NOT EXISTS (SELECT 1 FROM manager_menu WHERE menu_id = 9001);

INSERT INTO manager_role_menu (id, role_id, menu_id, create_time)
SELECT (r.role_id * 100000 + 9001), r.role_id, 9001, now()
FROM manager_role r
WHERE NOT EXISTS (
    SELECT 1 FROM manager_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = 9001
);
```

> Note: `manager_menu`/`manager_role_menu` columns were confirmed in `crm_init_postgres.sql` (menu: `menu_id, parent_id, realm, realm_name, type`; role_menu: `id, role_id, menu_id, create_user_id, update_user_id, create_time, update_time`). `realm='im'` is the permission string checked by `@RequirePermission("im")`. `type=3` matches the menu/permission convention used by existing seeded menus.

- [ ] **Step 2: Verify the version is free and SQL parses**

Run: `ls backend/src/main/resources/db/migration | grep -oE 'V[0-9]+' | sed 's/V//' | sort -n | tail -1`
Expected: prints `39`. If it prints ≥40, rename the file to `V<max+1>__create_im_tables.sql`.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V40__create_im_tables.sql
git commit -m "feat(im): add direct-message tables and im permission migration"
```

---

## Task 2: Entities + mappers

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/PO/ImConversation.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/PO/ImConversationMember.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/PO/ImMessage.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/mapper/ImConversationMapper.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/mapper/ImConversationMemberMapper.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/mapper/ImMessageMapper.java`

- [ ] **Step 1: Create the three entities**

`ImConversation.java`:
```java
package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_im_conversation")
public class ImConversation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String type;
    private String memberKey;
    private Long lastMessageId;
    private Date createTime;
    private Date updateTime;
}
```

`ImConversationMember.java`:
```java
package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_im_conversation_member")
public class ImConversationMember implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long conversationId;
    private Long userId;
    private Long lastReadMessageId;
    private Date createTime;
    private Date updateTime;
}
```

`ImMessage.java`:
```java
package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_im_message")
public class ImMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String contentType;
    private String content;
    private String attachmentName;
    private String attachmentPath;
    private Long attachmentSize;
    private String attachmentMime;
    private String status;
    private Date createTime;
    private Date updateTime;
}
```

- [ ] **Step 2: Create the three mappers**

`ImConversationMapper.java`:
```java
package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ImConversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImConversationMapper extends BaseMapper<ImConversation> {
}
```

`ImConversationMemberMapper.java`:
```java
package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ImConversationMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImConversationMemberMapper extends BaseMapper<ImConversationMember> {
}
```

`ImMessageMapper.java`:
```java
package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImMessageMapper extends BaseMapper<ImMessage> {
}
```

- [ ] **Step 3: Compile**

Run: `MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/entity/PO/Im*.java backend/src/main/java/com/kakarote/ai_crm/mapper/Im*.java
git commit -m "feat(im): add IM entities and mappers"
```

---

## Task 3: Events (decoupling hooks for the WebSocket layer)

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/event/ImMessageSentEvent.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/event/ImMessageRecalledEvent.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/im/event/ImConversationReadEvent.java`

- [ ] **Step 1: Create the three events**

`ImMessageSentEvent.java`:
```java
package com.kakarote.ai_crm.im.event;

import java.util.List;

/** Published after a message is persisted. memberUserIds = all members of the conversation. */
public record ImMessageSentEvent(Long conversationId, Long messageId, List<Long> memberUserIds) {
}
```

`ImMessageRecalledEvent.java`:
```java
package com.kakarote.ai_crm.im.event;

import java.util.List;

public record ImMessageRecalledEvent(Long conversationId, Long messageId, List<Long> memberUserIds) {
}
```

`ImConversationReadEvent.java`:
```java
package com.kakarote.ai_crm.im.event;

/** Published after a member marks a conversation read; carries the new unread (0) for that user. */
public record ImConversationReadEvent(Long conversationId, Long userId) {
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/im/event/
git commit -m "feat(im): add IM domain events for realtime push hooks"
```

---

## Task 4: ImConversationService (find-or-create DM, membership, unread)

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/service/ImConversationService.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/service/impl/ImConversationServiceImpl.java`
- Test: `backend/src/test/java/com/kakarote/ai_crm/service/impl/ImConversationServiceImplTest.java`

- [ ] **Step 1: Write the interface**

`ImConversationService.java`:
```java
package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.PO.ImConversation;

import java.util.List;

public interface ImConversationService extends IService<ImConversation> {

    /** Find or create the 1:1 direct conversation between the current user and peerUserId. */
    ImConversation getOrCreateDirect(Long currentUserId, Long peerUserId);

    /** All member user IDs of a conversation. */
    List<Long> memberUserIds(Long conversationId);

    /** Throws BusinessException if userId is not a member of conversationId. */
    void assertMember(Long conversationId, Long userId);

    /** Unread count for userId in conversationId (messages with id > lastRead, not own, status=normal). */
    long unreadCount(Long conversationId, Long userId);

    /** Build the canonical member_key for a user pair. */
    static String directMemberKey(Long a, Long b) {
        long lo = Math.min(a, b);
        long hi = Math.max(a, b);
        return lo + "_" + hi;
    }
}
```

- [ ] **Step 2: Write the failing test**

`ImConversationServiceImplTest.java`:
```java
package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.service.ImConversationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImConversationServiceImplTest {

    @Test
    void directMemberKeyIsOrderIndependent() {
        assertThat(ImConversationService.directMemberKey(7L, 3L)).isEqualTo("3_7");
        assertThat(ImConversationService.directMemberKey(3L, 7L)).isEqualTo("3_7");
    }
}
```

Run: `MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q test -Dtest=ImConversationServiceImplTest`
Expected: FAIL to compile (interface/impl not present yet) — create them in Step 3.

- [ ] **Step 3: Write the implementation**

`ImConversationServiceImpl.java`:
```java
package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.ImConversation;
import com.kakarote.ai_crm.entity.PO.ImConversationMember;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.mapper.ImConversationMapper;
import com.kakarote.ai_crm.mapper.ImConversationMemberMapper;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.service.ImConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ImConversationServiceImpl extends ServiceImpl<ImConversationMapper, ImConversation>
        implements ImConversationService {

    @Autowired
    private ImConversationMemberMapper memberMapper;

    @Autowired
    private ImMessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImConversation getOrCreateDirect(Long currentUserId, Long peerUserId) {
        if (currentUserId == null || peerUserId == null || currentUserId.equals(peerUserId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "无效的会话对象");
        }
        String key = ImConversationService.directMemberKey(currentUserId, peerUserId);
        ImConversation existing = getOne(new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getMemberKey, key), false);
        if (existing != null) {
            return existing;
        }
        ImConversation conv = new ImConversation();
        conv.setType("direct");
        conv.setMemberKey(key);
        Date now = new Date();
        conv.setCreateTime(now);
        conv.setUpdateTime(now);
        try {
            save(conv);
        } catch (DuplicateKeyException race) {
            // Concurrent create — return the winner.
            return getOne(new LambdaQueryWrapper<ImConversation>()
                    .eq(ImConversation::getMemberKey, key), false);
        }
        for (Long uid : List.of(currentUserId, peerUserId)) {
            ImConversationMember m = new ImConversationMember();
            m.setConversationId(conv.getId());
            m.setUserId(uid);
            m.setLastReadMessageId(0L);
            m.setCreateTime(now);
            m.setUpdateTime(now);
            memberMapper.insert(m);
        }
        return conv;
    }

    @Override
    public List<Long> memberUserIds(Long conversationId) {
        return memberMapper.selectList(new LambdaQueryWrapper<ImConversationMember>()
                        .eq(ImConversationMember::getConversationId, conversationId))
                .stream().map(ImConversationMember::getUserId).toList();
    }

    @Override
    public void assertMember(Long conversationId, Long userId) {
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getConversationId, conversationId)
                .eq(ImConversationMember::getUserId, userId));
        if (count == null || count == 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "你不在该会话中");
        }
    }

    @Override
    public long unreadCount(Long conversationId, Long userId) {
        ImConversationMember member = memberMapper.selectOne(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getConversationId, conversationId)
                .eq(ImConversationMember::getUserId, userId));
        long lastRead = (member == null || member.getLastReadMessageId() == null) ? 0L : member.getLastReadMessageId();
        Long c = messageMapper.selectCount(new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getConversationId, conversationId)
                .gt(ImMessage::getId, lastRead)
                .ne(ImMessage::getSenderId, userId)
                .eq(ImMessage::getStatus, "normal"));
        return c == null ? 0L : c;
    }
}
```

Run: `MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q test -Dtest=ImConversationServiceImplTest`
Expected: PASS (verify `SystemCodeEnum.SYSTEM_NO_VALID` and `BusinessException(SystemCodeEnum, String)` exist — they are used across the codebase, e.g. `ManageUserServiceImpl`; if the enum constant name differs, grep `common/result/SystemCodeEnum.java` and use the existing "invalid" constant).

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/service/ImConversationService.java backend/src/main/java/com/kakarote/ai_crm/service/impl/ImConversationServiceImpl.java backend/src/test/java/com/kakarote/ai_crm/service/impl/ImConversationServiceImplTest.java
git commit -m "feat(im): conversation service with DM find-or-create, membership, unread"
```

---

## Task 5: BOs/VOs

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/BO/ImSendMessageBO.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImMessageVO.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImConversationVO.java`

- [ ] **Step 1: Create the BO and VOs**

`ImSendMessageBO.java`:
```java
package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class ImSendMessageBO {
    private Long conversationId;
    private String contentType;   // text/image/file; default text
    private String content;
    private String attachmentName;
    private String attachmentPath;
    private Long attachmentSize;
    private String attachmentMime;
}
```

`ImMessageVO.java`:
```java
package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class ImMessageVO {
    private String id;            // serialized as String (Snowflake precision)
    private String conversationId;
    private String senderId;
    private String contentType;
    private String content;
    private String attachmentName;
    private String attachmentUrl; // browser-reachable URL resolved from attachmentPath
    private Long attachmentSize;
    private String attachmentMime;
    private String status;
    private Date createTime;
}
```

`ImConversationVO.java`:
```java
package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class ImConversationVO {
    private String id;
    private String peerUserId;
    private String peerName;
    private String peerAvatarUrl;
    private ImMessageVO lastMessage;
    private long unreadCount;
    private Date updateTime;
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/entity/BO/ImSendMessageBO.java backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImMessageVO.java backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImConversationVO.java
git commit -m "feat(im): add IM request/response DTOs"
```

---

## Task 6: ImMessageService (send, history, recall, read)

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/service/ImMessageService.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/service/impl/ImMessageServiceImpl.java`
- Test: `backend/src/test/java/com/kakarote/ai_crm/service/impl/ImMessageServiceImplTest.java`

- [ ] **Step 1: Write the interface**

`ImMessageService.java`:
```java
package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.ImSendMessageBO;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.entity.VO.ImMessageVO;

import java.util.List;

public interface ImMessageService extends IService<ImMessage> {

    /** Recall window in minutes. */
    int RECALL_WINDOW_MINUTES = 2;

    ImMessageVO send(Long senderId, ImSendMessageBO bo);

    /** History newest-first; beforeId null = latest page. */
    List<ImMessageVO> history(Long conversationId, Long beforeId, int limit);

    /** Recall own message within the window; throws otherwise. */
    ImMessageVO recall(Long messageId, Long userId);

    /** Mark conversation read up to the latest message for userId. */
    void markRead(Long conversationId, Long userId);
}
```

- [ ] **Step 2: Write the failing test** (recall window logic, the core branching)

`ImMessageServiceImplTest.java`:
```java
package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.service.ImConversationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImMessageServiceImplTest {

    @Mock ImMessageMapper messageMapper;
    @Mock ImConversationService conversationService;
    @Mock ApplicationEventPublisher eventPublisher;

    ImMessageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImMessageServiceImpl();
        ReflectionTestUtils.setField(service, "conversationService", conversationService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        // ServiceImpl exposes the mapper via baseMapper; inject it for getById/updateById.
        ReflectionTestUtils.setField(service, "baseMapper", messageMapper);
        when(conversationService.memberUserIds(any())).thenReturn(List.of(1L, 2L));
    }

    private ImMessage msg(long id, long sender, Date created, String status) {
        ImMessage m = new ImMessage();
        m.setId(id); m.setSenderId(sender); m.setConversationId(10L);
        m.setContentType("text"); m.setContent("hi"); m.setStatus(status); m.setCreateTime(created);
        return m;
    }

    @Test
    void recallWithinWindowSucceeds() {
        ImMessage m = msg(100L, 1L, new Date(), "normal");
        when(messageMapper.selectById(100L)).thenReturn(m);
        when(messageMapper.updateById(any())).thenReturn(1);

        var vo = service.recall(100L, 1L);

        assertThat(vo.getStatus()).isEqualTo("recalled");
    }

    @Test
    void recallByNonSenderRejected() {
        ImMessage m = msg(100L, 1L, new Date(), "normal");
        when(messageMapper.selectById(100L)).thenReturn(m);

        assertThatThrownBy(() -> service.recall(100L, 2L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void recallAfterWindowRejected() {
        Date old = new Date(System.currentTimeMillis() - 5 * 60 * 1000L);
        ImMessage m = msg(100L, 1L, old, "normal");
        when(messageMapper.selectById(100L)).thenReturn(m);

        assertThatThrownBy(() -> service.recall(100L, 1L)).isInstanceOf(BusinessException.class);
    }
}
```

Run: `... mvn -q test -Dtest=ImMessageServiceImplTest`
Expected: FAIL to compile (impl missing) — create it in Step 3. (If `ServiceImpl`'s mapper field is not named `baseMapper` in this MyBatis-Plus version, grep `ServiceImpl` usage in repo tests; the field is `baseMapper` in MyBatis-Plus 3.5.x used here.)

- [ ] **Step 3: Write the implementation**

`ImMessageServiceImpl.java`:
```java
package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ImSendMessageBO;
import com.kakarote.ai_crm.entity.PO.ImConversation;
import com.kakarote.ai_crm.entity.PO.ImConversationMember;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.im.event.ImConversationReadEvent;
import com.kakarote.ai_crm.im.event.ImMessageRecalledEvent;
import com.kakarote.ai_crm.im.event.ImMessageSentEvent;
import com.kakarote.ai_crm.mapper.ImConversationMapper;
import com.kakarote.ai_crm.mapper.ImConversationMemberMapper;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ImConversationService;
import com.kakarote.ai_crm.service.ImMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class ImMessageServiceImpl extends ServiceImpl<ImMessageMapper, ImMessage>
        implements ImMessageService {

    @Autowired
    private ImConversationService conversationService;

    @Autowired
    private ImConversationMapper conversationMapper;

    @Autowired
    private ImConversationMemberMapper memberMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private FileStorageService fileStorageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImMessageVO send(Long senderId, ImSendMessageBO bo) {
        if (bo == null || bo.getConversationId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "缺少会话ID");
        }
        conversationService.assertMember(bo.getConversationId(), senderId);
        String type = StrUtil.blankToDefault(bo.getContentType(), "text");
        if ("text".equals(type) && StrUtil.isBlank(bo.getContent())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "消息内容不能为空");
        }

        ImMessage m = new ImMessage();
        m.setConversationId(bo.getConversationId());
        m.setSenderId(senderId);
        m.setContentType(type);
        m.setContent(bo.getContent());
        m.setAttachmentName(bo.getAttachmentName());
        m.setAttachmentPath(bo.getAttachmentPath());
        m.setAttachmentSize(bo.getAttachmentSize());
        m.setAttachmentMime(bo.getAttachmentMime());
        m.setStatus("normal");
        Date now = new Date();
        m.setCreateTime(now);
        m.setUpdateTime(now);
        save(m);

        ImConversation conv = new ImConversation();
        conv.setId(bo.getConversationId());
        conv.setLastMessageId(m.getId());
        conv.setUpdateTime(now);
        conversationMapper.updateById(conv);

        // sender has implicitly read their own message
        bumpReadIfNewer(bo.getConversationId(), senderId, m.getId());

        eventPublisher.publishEvent(new ImMessageSentEvent(
                bo.getConversationId(), m.getId(), conversationService.memberUserIds(bo.getConversationId())));
        return toVO(m);
    }

    @Override
    public List<ImMessageVO> history(Long conversationId, Long beforeId, int limit) {
        int size = Math.min(Math.max(limit, 1), 50);
        LambdaQueryWrapper<ImMessage> w = new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getConversationId, conversationId)
                .orderByDesc(ImMessage::getId)
                .last("LIMIT " + size);
        if (beforeId != null && beforeId > 0) {
            w.lt(ImMessage::getId, beforeId);
        }
        List<ImMessage> rows = list(w);
        rows.sort(Comparator.comparing(ImMessage::getId)); // return ascending for display
        List<ImMessageVO> out = new ArrayList<>();
        for (ImMessage m : rows) {
            out.add(toVO(m));
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImMessageVO recall(Long messageId, Long userId) {
        ImMessage m = getById(messageId);
        if (m == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "消息不存在");
        }
        if (!m.getSenderId().equals(userId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "只能撤回自己发送的消息");
        }
        if ("recalled".equals(m.getStatus())) {
            return toVO(m);
        }
        long ageMs = System.currentTimeMillis() - m.getCreateTime().getTime();
        if (ageMs > RECALL_WINDOW_MINUTES * 60_000L) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "超过 " + RECALL_WINDOW_MINUTES + " 分钟，无法撤回");
        }
        m.setStatus("recalled");
        m.setContent(null);
        m.setAttachmentName(null);
        m.setAttachmentPath(null);
        m.setAttachmentSize(null);
        m.setAttachmentMime(null);
        m.setUpdateTime(new Date());
        updateById(m);
        eventPublisher.publishEvent(new ImMessageRecalledEvent(
                m.getConversationId(), m.getId(), conversationService.memberUserIds(m.getConversationId())));
        return toVO(m);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long conversationId, Long userId) {
        conversationService.assertMember(conversationId, userId);
        ImMessage latest = getOne(new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getConversationId, conversationId)
                .orderByDesc(ImMessage::getId)
                .last("LIMIT 1"), false);
        long latestId = latest == null ? 0L : latest.getId();
        bumpReadIfNewer(conversationId, userId, latestId);
        eventPublisher.publishEvent(new ImConversationReadEvent(conversationId, userId));
    }

    private void bumpReadIfNewer(Long conversationId, Long userId, long messageId) {
        ImConversationMember member = memberMapper.selectOne(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getConversationId, conversationId)
                .eq(ImConversationMember::getUserId, userId));
        if (member == null) {
            return;
        }
        long current = member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId();
        if (messageId > current) {
            member.setLastReadMessageId(messageId);
            member.setUpdateTime(new Date());
            memberMapper.updateById(member);
        }
    }

    private ImMessageVO toVO(ImMessage m) {
        ImMessageVO vo = new ImMessageVO();
        vo.setId(String.valueOf(m.getId()));
        vo.setConversationId(String.valueOf(m.getConversationId()));
        vo.setSenderId(String.valueOf(m.getSenderId()));
        vo.setContentType(m.getContentType());
        vo.setContent(m.getContent());
        vo.setAttachmentName(m.getAttachmentName());
        vo.setAttachmentSize(m.getAttachmentSize());
        vo.setAttachmentMime(m.getAttachmentMime());
        vo.setStatus(m.getStatus());
        vo.setCreateTime(m.getCreateTime());
        if (StrUtil.isNotBlank(m.getAttachmentPath()) && fileStorageService != null) {
            try {
                vo.setAttachmentUrl(fileStorageService.getUrl(m.getAttachmentPath()));
            } catch (Exception ignored) {
                // leave url null if storage lookup fails
            }
        }
        return vo;
    }
}
```

Run: `... mvn -q test -Dtest=ImMessageServiceImplTest`
Expected: PASS (3 tests).

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/service/ImMessageService.java backend/src/main/java/com/kakarote/ai_crm/service/impl/ImMessageServiceImpl.java backend/src/test/java/com/kakarote/ai_crm/service/impl/ImMessageServiceImplTest.java
git commit -m "feat(im): message service with send, history, recall window, mark-read + events"
```

---

## Task 7: ImController (REST API)

**Files:**
- Create: `backend/src/main/java/com/kakarote/ai_crm/controller/ImController.java`
- Test: none new (logic covered by service tests; controller is thin wiring verified by compile + manual smoke).

- [ ] **Step 1: Write the controller**

`ImController.java`:
```java
package com.kakarote.ai_crm.controller;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.ImSendMessageBO;
import com.kakarote.ai_crm.entity.PO.ImConversation;
import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.service.ImConversationService;
import com.kakarote.ai_crm.service.ImMessageService;
import com.kakarote.ai_crm.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "即时通讯")
@RestController
@RequestMapping("/im")
public class ImController {

    @Autowired
    private ImConversationService conversationService;

    @Autowired
    private ImMessageService messageService;

    @PostMapping("/conversations/direct")
    @Operation(summary = "查找或创建与某用户的私聊会话")
    @RequirePermission("im")
    public Result<Map<String, String>> openDirect(@RequestBody Map<String, Object> body) {
        Long peerId = parseLong(body.get("userId"));
        ImConversation conv = conversationService.getOrCreateDirect(UserUtil.getUserId(), peerId);
        return Result.ok(Map.of("conversationId", String.valueOf(conv.getId())));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "会话历史消息")
    @RequirePermission("im")
    public Result<List<ImMessageVO>> history(@PathVariable("id") Long conversationId,
                                             @RequestParam(value = "beforeId", required = false) Long beforeId,
                                             @RequestParam(value = "limit", defaultValue = "30") int limit) {
        conversationService.assertMember(conversationId, UserUtil.getUserId());
        return Result.ok(messageService.history(conversationId, beforeId, limit));
    }

    @PostMapping("/messages")
    @Operation(summary = "发送消息")
    @RequirePermission("im")
    public Result<ImMessageVO> send(@RequestBody ImSendMessageBO bo) {
        return Result.ok(messageService.send(UserUtil.getUserId(), bo));
    }

    @PostMapping("/messages/{id}/recall")
    @Operation(summary = "撤回消息")
    @RequirePermission("im")
    public Result<ImMessageVO> recall(@PathVariable("id") Long messageId) {
        return Result.ok(messageService.recall(messageId, UserUtil.getUserId()));
    }

    @PostMapping("/conversations/{id}/read")
    @Operation(summary = "标记已读")
    @RequirePermission("im")
    public Result<String> read(@PathVariable("id") Long conversationId) {
        messageService.markRead(conversationId, UserUtil.getUserId());
        return Result.ok("ok");
    }

    private Long parseLong(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return StrUtil.isBlank(s) ? null : Long.valueOf(s);
    }
}
```

> Verify `Result.ok(...)`, `@RequirePermission`, and `UserUtil.getUserId()` signatures against the repo (all used widely, e.g. `ManagerUserController`, `CustomerController`). The `GET /im/conversations` list and `GET /im/contacts` endpoints are added in Plan 1c-adjacent work or here if convenient; they are not required for the send/receive smoke test and depend on `ManageUserVO` shaping, so they are scheduled with the frontend plan to keep this plan focused on the core message path.

- [ ] **Step 2: Full compile + run all new tests**

Run: `MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q test -Dtest=ImConversationServiceImplTest,ImMessageServiceImplTest`
Expected: PASS (4 tests). Then `mvn -q -DskipTests package` → BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/controller/ImController.java
git commit -m "feat(im): REST controller for direct messages (send/history/recall/read)"
```

---

## Task 8: Build image, deploy, smoke-test the REST core

**Files:** none (verification). This confirms the migration applies and the REST path works end-to-end against the live stack.

- [ ] **Step 1: Rebuild the crm image with the IM backend** (the published image lacks it). Follow the project's rebuild recipe: extract the bundled frontend, repackage, build, recreate.

```bash
WD="C:/Users/bankk/AppData/Local/Temp/crmbuild_im"; mkdir -p "$WD"
docker cp crm:/opt/wk_ai_crm/crm-1.0.0.jar "$WD/crm-1.0.0.jar"
MSYS_NO_PATHCONV=1 docker run --rm -v "$WD":/j -v "F:/projects/WUKONG/AI_CRM":/work -w /tmp maven:3.9.9-eclipse-temurin-21 sh -c "cd /tmp && jar xf /j/crm-1.0.0.jar BOOT-INF/classes/public && rm -rf /work/backend/src/main/resources/public && mkdir -p /work/backend/src/main/resources/public && cp -r /tmp/BOOT-INF/classes/public/. /work/backend/src/main/resources/public/"
MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM":/work -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q -DskipTests clean package
MSYS_NO_PATHCONV=1 docker build -t wk_ai_crm:local-aiimport -f backend/Dockerfile backend/
cd docker && MSYS_NO_PATHCONV=1 docker compose up -d --force-recreate crm; cd /f/projects/WUKONG/AI_CRM
rm -rf backend/src/main/resources/public "$WD"
```
Expected: crm becomes healthy. Flyway applies V40 on startup (check `docker logs crm 2>&1 | grep -iE "V40|Migrating|flyway"`).

- [ ] **Step 2: Smoke-test the REST path** (admin DMs itself is invalid; create a second user first or use two existing users). With a token from `POST /auth/login` (admin/123456a), pick a peer user id from `manager_user`:

```bash
docker exec WeKnora-postgres psql -U postgres -d wk_ai_crm -t -c "SELECT user_id, realname FROM manager_user LIMIT 5;"
# TOKEN=...; PEER=<a user_id != 1>
# CONV=$(curl -s -XPOST localhost:8088/im/conversations/direct -H "Manager-Token: $TOKEN" -H 'Content-Type: application/json' -d "{\"userId\":$PEER}")
# curl -s -XPOST localhost:8088/im/messages -H "Manager-Token: $TOKEN" -H 'Content-Type: application/json' -d "{\"conversationId\":<id>,\"content\":\"hello\"}"
# curl -s "localhost:8088/im/conversations/<id>/messages" -H "Manager-Token: $TOKEN"
```
Expected: conversation created, message persisted and returned by history. If only `admin` exists, create a throwaway user via 团队管理 or SQL, and delete it after.

- [ ] **Step 3: Commit (only if smoke-test required fixups)**

```bash
git add -A && git commit -m "chore(im): backend core smoke-test fixups"
```

---

## Self-Review Notes

- **Spec coverage (1a portion):** data model (Task 1-2) ✓; DM find-or-create + dedup by member_key (Task 4) ✓; unread derivation (Task 4) ✓; send/history (Task 6) ✓; recall within 2-min window (Task 6) ✓; mark-read (Task 6) ✓; `im` permission granted to all roles, removable (Task 1) ✓; events for the realtime layer (Task 3, published in Task 6) ✓; membership enforcement (Task 4/6/7) ✓. **Deferred to later 1a/1c work (noted):** `GET /im/conversations` list VO assembly and `GET /im/contacts` (peer-name/avatar/presence shaping) — moved next to the frontend plan since they're presentation-shaped and not needed for the core send/receive path. Realtime push (WS), attachments-upload wiring, presence, browser notifications → Plan 1b/1c.
- **Single-tenant correction:** the codebase has no tenant interceptor/`tenant_id` (verified: no `TenantLineInnerInterceptor`, `Customer` has no tenant field, `crm_chat_session` is user-scoped). IM tables omit `tenant_id`; scoping is by membership. This corrects the spec's tenant assumption — the spec doc should be updated to match.
- **Placeholder scan:** no TBD/TODO; every code step has complete code. The one explicit verification is the Flyway version number (rename if V40 taken) and the `SystemCodeEnum` constant / `ServiceImpl` `baseMapper` field name — both flagged inline with how to confirm.
- **Type consistency:** `directMemberKey`, `getOrCreateDirect`, `assertMember`, `memberUserIds`, `unreadCount`, `send`, `history`, `recall`, `markRead`, `RECALL_WINDOW_MINUTES`, `ImMessageVO`/`ImConversationVO`/`ImSendMessageBO` fields, and the three events are consistent across tasks.
- **Scope:** this plan is the REST core only; Plan 1b (WebSocket realtime: ws starter, STOMP config, CONNECT auth interceptor reusing `TokenService` via a new `getLoginUser(String token)` overload, presence, push listeners consuming the Task 3 events, nginx `/ws` proxy) and Plan 1c (frontend: deps, route+sidebar module, `im` Pinia store + STOMP client, conversation list + chat pane, attachments, notifications, recall UI, plus the conversations-list and contacts endpoints) follow after 1a is built and verified.
```
