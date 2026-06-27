# IM Phase 3 — Reactions, @Mentions, Threads Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add emoji reactions, @mentions (individual + @all), and threads to the IM, reusing the Phase 1 member fan-out.

**Architecture:** A new `crm_im_message_reaction` table + new columns on `crm_im_message` (`parent_id`, `reply_count`, `last_reply_time`, `mentioned_user_ids`, `mention_all`). Reactions get their own toggle endpoint + a `reaction` push envelope; mentions/threads ride the existing `POST /im/messages` send path and message push. Main-timeline history excludes thread replies; a thread fetch returns root+replies. Frontend adds reaction chips, an `@` autocomplete + highlight, and a thread drawer.

**Tech Stack:** Java 21, Spring Boot 3.3, MyBatis-Plus, PostgreSQL/Flyway, JUnit5+Mockito; Vue 3 + TS + Pinia + Element Plus.

**Environment:** Branch `feature/im-phase3-threads-reactions-mentions` (do NOT switch). Maven via Docker:
`MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn <args>`.
Frontend build (~3 min): `MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM/frontend:/app" -w /app node:20 sh -c "npm install && npm run build"` (vite chunk warnings OK; only vue-tsc type errors fail).

## Existing code this builds on (read before editing)

- `service/impl/ImMessageServiceImpl.java`: injects `conversationService`, `conversationMapper`, `memberMapper`, `eventPublisher` (ApplicationEventPublisher), `fileStorageService`(required=false), `manageUserService`. Methods: `send(senderId, ImSendMessageBO)` (asserts membership, saves message, updates conv lastMessage, bumps own read, publishes `ImMessageSentEvent`, returns `toVO(m)`); `history(conversationId, beforeId, limit)`; `recall`; `markRead`; `getMessageVO(messageId)`; private `toVO(ImMessage)`.
- `entity/PO/ImMessage.java`: id/conversationId/senderId/contentType/content/attachment*/status/createTime/updateTime.
- `entity/VO/ImMessageVO.java`: id/conversationId/senderId/contentType/content/attachmentName/attachmentUrl/attachmentSize/attachmentMime/status/createTime/senderName.
- `entity/BO/ImSendMessageBO.java`: conversationId/contentType/content/attachment*.
- `im/event/ImMessageSentEvent.java`: `record(Long conversationId, Long messageId, List<Long> memberUserIds)`.
- `im/ws/ImPushEnvelope.java`: `{String type; String conversationId; ImMessageVO message; Long unread}` (`@Data @AllArgsConstructor`).
- `im/ws/ImPushService.java`: `pushMessage(convId, vo, memberUserIds)` (loops members, per-user unread), `pushUnread(convId, userId)`. `USER_QUEUE="/queue/im"`, uses `SimpMessagingTemplate.convertAndSendToUser`.
- `im/ws/ImRealtimePushListener.java`: `@TransactionalEventListener(AFTER_COMMIT)` handlers for sent/recalled/read.
- `controller/ImController.java`: `POST /im/messages` (send), `GET /im/conversations/{id}/messages` (history, asserts member), `POST /im/messages/{id}/recall`, etc. All `@RequirePermission("im")`.
- `service/ImConversationService.java`: `assertMember(convId, userId)`, `memberUserIds(convId)`, `unreadCount(convId, userId)`.
- Frontend `frontend/src/api/im.ts` (`ImMessage`, `ImSendPayload`, endpoints), `frontend/src/stores/im.ts` (`messagesByConv`, `onPush`, `upsertMessage`, `notifyIfHidden`, `send`, `selectConversation`), `frontend/src/views/im/ImView.vue` (Slack-style list + chat pane; hover action bar with recall; `doUpload`; helpers `avatarBg/avatarText/clockTime/msgSenderName/msgAvatarText/msgAvatarBg`; grouped messages).

## Curated emoji set (server-validated + client palette)
`👍 ❤️ 😂 🎉 👀 ✅ 🙏 😄` — define once on the backend as `ImReactionService.ALLOWED_EMOJI` (a `Set<String>`) and mirror as a constant array in the frontend.

---

## Task 1: Migration V43 + entities

**Files:**
- Create `backend/src/main/resources/db/migration/V43__im_phase3.sql`
- Modify `backend/src/main/java/com/kakarote/ai_crm/entity/PO/ImMessage.java`
- Create `backend/src/main/java/com/kakarote/ai_crm/entity/PO/ImMessageReaction.java`
- Create `backend/src/main/java/com/kakarote/ai_crm/mapper/ImMessageReactionMapper.java`

- [ ] **Step 1: Migration** (confirm V43 is next free: `ls .../db/migration | grep -oE '^V[0-9]+' | sed 's/V//' | sort -n | tail -1` → `42`):

```sql
-- ============================================
-- V43: IM Phase 3 — reactions, @mentions, threads
-- ============================================
CREATE TABLE IF NOT EXISTS crm_im_message_reaction (
    id              BIGINT PRIMARY KEY,
    message_id      BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    emoji           VARCHAR(32) NOT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_im_reaction ON crm_im_message_reaction (message_id, user_id, emoji);
CREATE INDEX IF NOT EXISTS idx_im_reaction_msg ON crm_im_message_reaction (message_id);

ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS reply_count INT DEFAULT 0;
ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS last_reply_time TIMESTAMP;
ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS mentioned_user_ids VARCHAR(500);
ALTER TABLE crm_im_message ADD COLUMN IF NOT EXISTS mention_all BOOLEAN DEFAULT false;

COMMENT ON COLUMN crm_im_message.parent_id IS '话题根消息ID（普通/根消息为空）';
COMMENT ON COLUMN crm_im_message.reply_count IS '根消息的话题回复数';
COMMENT ON COLUMN crm_im_message.mentioned_user_ids IS '@提及的用户ID(csv)';

CREATE INDEX IF NOT EXISTS idx_im_msg_conv_parent ON crm_im_message (conversation_id, parent_id);
```

- [ ] **Step 2: Entity fields** — add to `ImMessage.java` after `private String status;`:

```java
    private Long parentId;
    private Integer replyCount;
    private Date lastReplyTime;
    private String mentionedUserIds;
    private Boolean mentionAll;
```

- [ ] **Step 3: Reaction entity** `ImMessageReaction.java`:

```java
package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_im_message_reaction")
public class ImMessageReaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long messageId;
    private Long conversationId;
    private Long userId;
    private String emoji;
    private Date createTime;
}
```

- [ ] **Step 4: Mapper** `ImMessageReactionMapper.java`:

```java
package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ImMessageReaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImMessageReactionMapper extends BaseMapper<ImMessageReaction> {
}
```

- [ ] **Step 5: Compile + commit**

Run `... mvn -q -DskipTests compile` → BUILD SUCCESS.
```bash
git add backend/src/main/resources/db/migration/V43__im_phase3.sql backend/src/main/java/com/kakarote/ai_crm/entity/PO/ImMessage.java backend/src/main/java/com/kakarote/ai_crm/entity/PO/ImMessageReaction.java backend/src/main/java/com/kakarote/ai_crm/mapper/ImMessageReactionMapper.java
git commit -m "feat(im): V43 reactions table + message thread/mention columns + entities"
```

---

## Task 2: VO + BO changes

**Files:**
- Modify `entity/VO/ImMessageVO.java`
- Create `entity/VO/ImReactionVO.java`
- Modify `entity/BO/ImSendMessageBO.java`
- Create `entity/BO/ImReactionBO.java`

- [ ] **Step 1:** `ImReactionVO.java`:

```java
package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class ImReactionVO {
    private String emoji;
    private int count;
    private boolean mine;
}
```

- [ ] **Step 2:** Add to `ImMessageVO.java` (after `senderName`):

```java
    private java.util.List<ImReactionVO> reactions;
    private java.util.List<String> mentionedUserIds;
    private Boolean mentionAll;
    private String parentId;     // root message id if this is a thread reply
    private Integer replyCount;  // for root messages
```

- [ ] **Step 3:** Add to `ImSendMessageBO.java`:

```java
    private java.util.List<Long> mentionedUserIds;
    private Boolean mentionAll;
    private Long parentId;       // set when sending a thread reply
```

- [ ] **Step 4:** `ImReactionBO.java`:

```java
package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class ImReactionBO {
    private String emoji;
}
```

- [ ] **Step 5: Compile + commit**

Run `... mvn -q -DskipTests compile` → BUILD SUCCESS.
```bash
git add backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImMessageVO.java backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImReactionVO.java backend/src/main/java/com/kakarote/ai_crm/entity/BO/ImSendMessageBO.java backend/src/main/java/com/kakarote/ai_crm/entity/BO/ImReactionBO.java
git commit -m "feat(im): message VO/BO fields for reactions, mentions, threads"
```

---

## Task 3: Reaction service + tests

**Files:**
- Create `service/ImReactionService.java`
- Create `service/impl/ImReactionServiceImpl.java`
- Create `im/event/ImReactionChangedEvent.java`
- Test `backend/src/test/java/com/kakarote/ai_crm/service/impl/ImReactionServiceTest.java`

- [ ] **Step 1:** Event record `ImReactionChangedEvent.java`:

```java
package com.kakarote.ai_crm.im.event;

import java.util.List;

/** Published after a reaction is toggled. */
public record ImReactionChangedEvent(Long conversationId, Long messageId, List<Long> memberUserIds) {
}
```

- [ ] **Step 2:** Interface `ImReactionService.java`:

```java
package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.PO.ImMessageReaction;
import com.kakarote.ai_crm.entity.VO.ImReactionVO;
import java.util.List;
import java.util.Set;

public interface ImReactionService extends IService<ImMessageReaction> {

    Set<String> ALLOWED_EMOJI = Set.of("👍", "❤️", "😂", "🎉", "👀", "✅", "🙏", "😄");

    /** Toggle the emoji for (message, caller). Returns the message's aggregated reactions for the caller. */
    List<ImReactionVO> toggle(Long userId, Long messageId, String emoji);

    /** Aggregate a message's reactions from the given viewer's perspective. */
    List<ImReactionVO> aggregate(Long messageId, Long viewerId);
}
```

- [ ] **Step 3:** Impl `ImReactionServiceImpl.java`:

```java
package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.entity.PO.ImMessageReaction;
import com.kakarote.ai_crm.entity.VO.ImReactionVO;
import com.kakarote.ai_crm.im.event.ImReactionChangedEvent;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.mapper.ImMessageReactionMapper;
import com.kakarote.ai_crm.service.ImConversationService;
import com.kakarote.ai_crm.service.ImReactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ImReactionServiceImpl extends ServiceImpl<ImMessageReactionMapper, ImMessageReaction> implements ImReactionService {

    @Autowired
    private ImMessageMapper messageMapper;

    @Autowired
    private ImConversationService conversationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ImReactionVO> toggle(Long userId, Long messageId, String emoji) {
        if (emoji == null || !ALLOWED_EMOJI.contains(emoji)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "不支持的表情");
        }
        ImMessage msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "消息不存在");
        }
        conversationService.assertMember(msg.getConversationId(), userId);

        ImMessageReaction existing = getOne(new LambdaQueryWrapper<ImMessageReaction>()
                .eq(ImMessageReaction::getMessageId, messageId)
                .eq(ImMessageReaction::getUserId, userId)
                .eq(ImMessageReaction::getEmoji, emoji), false);
        if (existing != null) {
            removeById(existing.getId());
        } else {
            ImMessageReaction r = new ImMessageReaction();
            r.setMessageId(messageId);
            r.setConversationId(msg.getConversationId());
            r.setUserId(userId);
            r.setEmoji(emoji);
            r.setCreateTime(new Date());
            try {
                save(r);
            } catch (DuplicateKeyException ignored) {
                // concurrent add; unique constraint makes it idempotent
            }
        }
        // notify members (each gets their own `mine`) after commit
        eventPublisher.publishEvent(new ImReactionChangedEvent(
                msg.getConversationId(), messageId, conversationService.memberUserIds(msg.getConversationId())));
        return aggregate(messageId, userId);
    }

    @Override
    public List<ImReactionVO> aggregate(Long messageId, Long viewerId) {
        List<ImMessageReaction> rows = list(new LambdaQueryWrapper<ImMessageReaction>()
                .eq(ImMessageReaction::getMessageId, messageId)
                .orderByAsc(ImMessageReaction::getId));
        // preserve first-seen emoji order
        LinkedHashMap<String, ImReactionVO> byEmoji = new LinkedHashMap<>();
        for (ImMessageReaction r : rows) {
            ImReactionVO vo = byEmoji.computeIfAbsent(r.getEmoji(), e -> {
                ImReactionVO v = new ImReactionVO();
                v.setEmoji(e);
                v.setCount(0);
                v.setMine(false);
                return v;
            });
            vo.setCount(vo.getCount() + 1);
            if (viewerId != null && viewerId.equals(r.getUserId())) {
                vo.setMine(true);
            }
        }
        return new ArrayList<>(byEmoji.values());
    }
}
```

- [ ] **Step 4: Tests** `ImReactionServiceTest.java`. Mirror `ImChannelServiceTest`'s pattern: inject the `ImMessageReactionMapper` as `baseMapper`, mock `ImMessageMapper` + `ImConversationService` + `ApplicationEventPublisher`. Because `getOne`/`list`/`save`/`removeById` route through `baseMapper`, stub the mapper calls.

```java
package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.entity.PO.ImMessageReaction;
import com.kakarote.ai_crm.entity.VO.ImReactionVO;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.mapper.ImMessageReactionMapper;
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

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImReactionServiceTest {

    @Mock ImMessageReactionMapper reactionMapper;
    @Mock ImMessageMapper messageMapper;
    @Mock ImConversationService conversationService;
    @Mock ApplicationEventPublisher eventPublisher;
    ImReactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImReactionServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", reactionMapper);
        ReflectionTestUtils.setField(service, "messageMapper", messageMapper);
        ReflectionTestUtils.setField(service, "conversationService", conversationService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        ImMessage m = new ImMessage();
        m.setId(10L); m.setConversationId(99L);
        when(messageMapper.selectById(10L)).thenReturn(m);
        when(conversationService.memberUserIds(99L)).thenReturn(List.of(1L, 2L));
    }

    @Test
    void toggleRejectsUnknownEmoji() {
        assertThatThrownBy(() -> service.toggle(1L, 10L, "🚀"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void toggleAddsWhenAbsentThenAggregates() {
        // not yet reacted
        when(reactionMapper.selectOne(any(), eq(false))).thenReturn(null); // getOne(...,false)
        // aggregate() list() returns the just-added row
        ImMessageReaction added = new ImMessageReaction();
        added.setMessageId(10L); added.setUserId(1L); added.setEmoji("👍");
        when(reactionMapper.selectList(any())).thenReturn(List.of(added));

        List<ImReactionVO> out = service.toggle(1L, 10L, "👍");

        verify(reactionMapper).insert(any(ImMessageReaction.class));
        assertThat(out).hasSize(1);
        assertThat(out.get(0).getEmoji()).isEqualTo("👍");
        assertThat(out.get(0).getCount()).isEqualTo(1);
        assertThat(out.get(0).isMine()).isTrue();
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void toggleRemovesWhenPresent() {
        ImMessageReaction existing = new ImMessageReaction();
        existing.setId(500L); existing.setMessageId(10L); existing.setUserId(1L); existing.setEmoji("👍");
        when(reactionMapper.selectOne(any(), eq(false))).thenReturn(existing);
        when(reactionMapper.selectList(any())).thenReturn(List.of()); // none left

        List<ImReactionVO> out = service.toggle(1L, 10L, "👍");

        verify(reactionMapper).deleteById(500L);
        verify(reactionMapper, never()).insert(any());
        assertThat(out).isEmpty();
    }

    @Test
    void aggregateGroupsByEmojiWithMineFlag() {
        ImMessageReaction r1 = new ImMessageReaction(); r1.setEmoji("👍"); r1.setUserId(1L);
        ImMessageReaction r2 = new ImMessageReaction(); r2.setEmoji("👍"); r2.setUserId(2L);
        ImMessageReaction r3 = new ImMessageReaction(); r3.setEmoji("❤️"); r3.setUserId(2L);
        when(reactionMapper.selectList(any())).thenReturn(List.of(r1, r2, r3));

        List<ImReactionVO> out = service.aggregate(10L, 1L);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).getEmoji()).isEqualTo("👍");
        assertThat(out.get(0).getCount()).isEqualTo(2);
        assertThat(out.get(0).isMine()).isTrue();   // viewer 1 reacted 👍
        assertThat(out.get(1).getEmoji()).isEqualTo("❤️");
        assertThat(out.get(1).getCount()).isEqualTo(1);
        assertThat(out.get(1).isMine()).isFalse();
    }
}
```

> If MyBatis-Plus's `getOne(wrapper, false)` / `list(wrapper)` / `removeById` / `save` route to different mapper methods than `selectOne(w,boolean)` / `selectList(w)` / `deleteById` / `insert`, adjust the stubs to match what `ServiceImpl` actually calls (check by reading how `ImChannelServiceTest` stubs `selectCount`/`insert`). The intent of each test is what matters.

- [ ] **Step 5: Run tests**

`... mvn -q test -Dtest=ImReactionServiceTest` → 4 pass. Iterate stubs if needed.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/service/ImReactionService.java backend/src/main/java/com/kakarote/ai_crm/service/impl/ImReactionServiceImpl.java backend/src/main/java/com/kakarote/ai_crm/im/event/ImReactionChangedEvent.java backend/src/test/java/com/kakarote/ai_crm/service/impl/ImReactionServiceTest.java
git commit -m "feat(im): reaction toggle + aggregate service with tests"
```

---

## Task 4: Mentions + threads in the message service + tests

**Files:**
- Modify `service/ImMessageService.java` (history signature + new `getThread`)
- Modify `service/impl/ImMessageServiceImpl.java`
- Test: extend `backend/src/test/java/com/kakarote/ai_crm/service/impl/ImMessageServiceImplTest.java`

- [ ] **Step 1: Interface** — change `history` to take a viewer and add `getThread`:

```java
    List<ImMessageVO> history(Long conversationId, Long viewerId, Long beforeId, int limit);
    List<ImMessageVO> getThread(Long rootId, Long viewerId);
```

- [ ] **Step 2: Impl wiring** — inject the reaction service. Add field:

```java
    @Autowired
    private com.kakarote.ai_crm.service.ImReactionService reactionService;
```

- [ ] **Step 3: `send` — store mentions + thread parent + bump root reply count.** In `send(...)`, after `m.setStatus("normal");` and before `save(m)`, add:

```java
        m.setParentId(bo.getParentId());
        if (bo.getMentionedUserIds() != null && !bo.getMentionedUserIds().isEmpty()) {
            m.setMentionedUserIds(cn.hutool.core.util.StrUtil.join(",", bo.getMentionedUserIds()));
        }
        m.setMentionAll(Boolean.TRUE.equals(bo.getMentionAll()));
```

Then after `save(m);` and the existing `conversationMapper.updateById(conv)` block, add a root-reply-count bump when this is a thread reply:

```java
        if (bo.getParentId() != null) {
            ImMessage root = getById(bo.getParentId());
            if (root != null) {
                ImMessage rootUpdate = new ImMessage();
                rootUpdate.setId(root.getId());
                rootUpdate.setReplyCount((root.getReplyCount() == null ? 0 : root.getReplyCount()) + 1);
                rootUpdate.setLastReplyTime(now);
                updateById(rootUpdate);
            }
        }
```

(The existing `ImMessageSentEvent` publish stays; thread replies push to all members like any message — the client routes by `parentId`.)

- [ ] **Step 4: `history` — exclude thread replies + pass viewer.** Change the signature to `history(Long conversationId, Long viewerId, Long beforeId, int limit)`; add `.isNull(ImMessage::getParentId)` to the wrapper so replies are excluded; change `toVO(m)` calls to `toVO(m, viewerId)` (see Step 6).

```java
    public List<ImMessageVO> history(Long conversationId, Long viewerId, Long beforeId, int limit) {
        int size = Math.min(Math.max(limit, 1), 50);
        LambdaQueryWrapper<ImMessage> w = new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getConversationId, conversationId)
                .isNull(ImMessage::getParentId)
                .orderByDesc(ImMessage::getId)
                .last("LIMIT " + size);
        if (beforeId != null && beforeId > 0) {
            w.lt(ImMessage::getId, beforeId);
        }
        List<ImMessage> rows = list(w);
        rows.sort(Comparator.comparing(ImMessage::getId));
        List<ImMessageVO> out = new ArrayList<>();
        for (ImMessage m : rows) {
            out.add(toVO(m, viewerId));
        }
        return out;
    }
```

- [ ] **Step 5: `getThread`** — root + replies, ascending:

```java
    @Override
    public List<ImMessageVO> getThread(Long rootId, Long viewerId) {
        ImMessage root = getById(rootId);
        if (root == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "话题不存在");
        }
        conversationService.assertMember(root.getConversationId(), viewerId);
        List<ImMessageVO> out = new ArrayList<>();
        out.add(toVO(root, viewerId));
        List<ImMessage> replies = list(new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getParentId, rootId)
                .orderByAsc(ImMessage::getId));
        for (ImMessage r : replies) {
            out.add(toVO(r, viewerId));
        }
        return out;
    }
```

- [ ] **Step 6: `toVO` — add viewer overload populating reactions/mentions/thread fields.** Replace the existing `private ImMessageVO toVO(ImMessage m)` with two methods:

```java
    private ImMessageVO toVO(ImMessage m) {
        return toVO(m, null);
    }

    private ImMessageVO toVO(ImMessage m, Long viewerId) {
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
        vo.setParentId(m.getParentId() == null ? null : String.valueOf(m.getParentId()));
        vo.setReplyCount(m.getReplyCount() == null ? 0 : m.getReplyCount());
        vo.setMentionAll(Boolean.TRUE.equals(m.getMentionAll()));
        if (cn.hutool.core.util.StrUtil.isNotBlank(m.getMentionedUserIds())) {
            vo.setMentionedUserIds(java.util.Arrays.asList(m.getMentionedUserIds().split(",")));
        }
        vo.setReactions(reactionService.aggregate(m.getId(), viewerId));
        if (m.getSenderId() != null) {
            try {
                ManagerUser u = manageUserService.getById(m.getSenderId());
                if (u != null) {
                    vo.setSenderName(StrUtil.blankToDefault(u.getRealname(), u.getUsername()));
                }
            } catch (Exception ignored) { }
        }
        if (StrUtil.isNotBlank(m.getAttachmentPath()) && fileStorageService != null) {
            try {
                vo.setAttachmentUrl(fileStorageService.getUrl(m.getAttachmentPath()));
            } catch (Exception ignored) { }
        }
        return vo;
    }
```

(`getMessageVO(messageId)` keeps calling `toVO(m)` → viewer-agnostic; reactions there have `mine=false`, which is correct for the message-push path where each member's `mine` doesn't matter for a brand-new message, and the dedicated reaction push (Task 5) recomputes per-member.)

- [ ] **Step 7: Tests** — extend `ImMessageServiceImplTest.java`. It already mocks the message mapper as `baseMapper`. Add `reactionService` mock (returns empty list) and tests:

```java
    // in setUp / fields: @Mock ImReactionService reactionService;
    //   ReflectionTestUtils.setField(service, "reactionService", reactionService);
    //   when(reactionService.aggregate(anyLong(), any())).thenReturn(java.util.List.of());

    @Test
    void sendThreadReplyStoresParentAndBumpsRootReplyCount() {
        // arrange: conversationService.assertMember no-op; save sets id; root exists with replyCount 2
        ImMessage root = new ImMessage(); root.setId(10L); root.setConversationId(99L); root.setReplyCount(2);
        when(messageMapper.selectById(10L)).thenReturn(root);
        when(messageMapper.insert(any(ImMessage.class))).thenAnswer(inv -> { ((ImMessage) inv.getArgument(0)).setId(11L); return 1; });
        when(conversationService.memberUserIds(99L)).thenReturn(java.util.List.of(1L));

        ImSendMessageBO bo = new ImSendMessageBO();
        bo.setConversationId(99L); bo.setContent("re"); bo.setParentId(10L);
        service.send(1L, bo);

        // root update carries replyCount=3
        verify(messageMapper, atLeastOnce()).updateById(argThat(u ->
            u instanceof ImMessage && Integer.valueOf(3).equals(((ImMessage) u).getReplyCount())));
    }

    @Test
    void sendStoresMentions() {
        when(messageMapper.insert(any(ImMessage.class))).thenAnswer(inv -> { ((ImMessage) inv.getArgument(0)).setId(12L); return 1; });
        when(conversationService.memberUserIds(anyLong())).thenReturn(java.util.List.of(1L));
        ImSendMessageBO bo = new ImSendMessageBO();
        bo.setConversationId(99L); bo.setContent("hi @a"); bo.setMentionAll(true);
        bo.setMentionedUserIds(java.util.List.of(2L, 3L));
        service.send(1L, bo);
        verify(messageMapper).insert(argThat(o -> {
            ImMessage m = (ImMessage) o;
            return Boolean.TRUE.equals(m.getMentionAll()) && "2,3".equals(m.getMentionedUserIds());
        }));
    }
```

> Match the existing test's mock field names (`messageMapper`/`conversationService`) and `assertMember` stubbing. If the existing test mocks `conversationService.assertMember` to do nothing, reuse that. Confirm `updateById` vs the MyBatis-Plus call the impl emits for the root bump.

- [ ] **Step 8: Run tests**

`... mvn -q test -Dtest=ImMessageServiceImplTest` → existing + 2 new pass. (Also re-run `ImReactionServiceTest`.)

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/service/ImMessageService.java backend/src/main/java/com/kakarote/ai_crm/service/impl/ImMessageServiceImpl.java backend/src/test/java/com/kakarote/ai_crm/service/impl/ImMessageServiceImplTest.java
git commit -m "feat(im): mentions + thread reply in send, thread fetch, reply-excluded history"
```

---

## Task 5: Reaction push + controller endpoints

**Files:**
- Modify `im/ws/ImPushEnvelope.java`, `im/ws/ImPushService.java`, `im/ws/ImRealtimePushListener.java`
- Modify `controller/ImController.java`

- [ ] **Step 1: Envelope** — add reaction fields to `ImPushEnvelope.java`. Because it's `@AllArgsConstructor`, add the fields AND keep the existing 4-arg usages compiling by adding a no-arg/builder OR add a second constructor. Simplest: add fields + a static factory and switch `ImPushService` to use factories. Replace the class with:

```java
package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.entity.VO.ImReactionVO;
import lombok.Data;
import java.util.List;

/** Typed payload pushed to /user/{userId}/queue/im. type = message | unread | reaction. */
@Data
public class ImPushEnvelope {
    private String type;
    private String conversationId;
    private ImMessageVO message;      // type=message
    private Long unread;              // type=message|unread
    private String messageId;         // type=reaction
    private List<ImReactionVO> reactions; // type=reaction

    public static ImPushEnvelope message(String conversationId, ImMessageVO message, Long unread) {
        ImPushEnvelope e = new ImPushEnvelope();
        e.type = "message"; e.conversationId = conversationId; e.message = message; e.unread = unread;
        return e;
    }
    public static ImPushEnvelope unread(String conversationId, Long unread) {
        ImPushEnvelope e = new ImPushEnvelope();
        e.type = "unread"; e.conversationId = conversationId; e.unread = unread;
        return e;
    }
    public static ImPushEnvelope reaction(String conversationId, String messageId, List<ImReactionVO> reactions) {
        ImPushEnvelope e = new ImPushEnvelope();
        e.type = "reaction"; e.conversationId = conversationId; e.messageId = messageId; e.reactions = reactions;
        return e;
    }
}
```

- [ ] **Step 2: `ImPushService`** — switch to the factories and add `pushReaction`. Inject `ImReactionService`. Replace the `pushMessage`/`pushUnread` bodies to use `ImPushEnvelope.message(...)`/`unread(...)`, and add:

```java
    @Autowired
    private com.kakarote.ai_crm.service.ImReactionService reactionService;

    /** Push fresh aggregated reactions for a message to each member (per-member `mine`). */
    public void pushReaction(Long conversationId, Long messageId, List<Long> memberUserIds) {
        for (Long uid : memberUserIds) {
            ImPushEnvelope env = ImPushEnvelope.reaction(
                    String.valueOf(conversationId), String.valueOf(messageId), reactionService.aggregate(messageId, uid));
            messagingTemplate.convertAndSendToUser(String.valueOf(uid), USER_QUEUE, env);
        }
    }
```

Update the two existing methods, e.g.:
```java
    public void pushMessage(Long conversationId, ImMessageVO message, List<Long> memberUserIds) {
        for (Long uid : memberUserIds) {
            long unread = conversationService.unreadCount(conversationId, uid);
            messagingTemplate.convertAndSendToUser(String.valueOf(uid), USER_QUEUE,
                    ImPushEnvelope.message(String.valueOf(conversationId), message, unread));
        }
    }
    public void pushUnread(Long conversationId, Long userId) {
        long unread = conversationService.unreadCount(conversationId, userId);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_QUEUE,
                ImPushEnvelope.unread(String.valueOf(conversationId), unread));
    }
```

- [ ] **Step 3: Listener** — add a reaction handler to `ImRealtimePushListener.java`:

```java
    @org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void onReactionChanged(com.kakarote.ai_crm.im.event.ImReactionChangedEvent event) {
        try {
            pushService.pushReaction(event.conversationId(), event.messageId(), event.memberUserIds());
        } catch (Exception e) {
            log.error("IM 实时推送失败(onReactionChanged) conversationId={}, messageId={}: {}",
                    event.conversationId(), event.messageId(), e.getMessage(), e);
        }
    }
```

- [ ] **Step 4: Controller** — inject `ImReactionService reactionService`; update the existing `history` call to pass the viewer; add the two endpoints. In `ImController.java`:
  - Change history handler body to `return Result.ok(messageService.history(conversationId, UserUtil.getUserId(), beforeId, limit));`
  - Add:

```java
    @Autowired
    private com.kakarote.ai_crm.service.ImReactionService reactionService;

    @PostMapping("/messages/{id}/reactions")
    @Operation(summary = "切换消息表情回应")
    @RequirePermission("im")
    public Result<java.util.List<com.kakarote.ai_crm.entity.VO.ImReactionVO>> toggleReaction(
            @PathVariable("id") Long messageId, @RequestBody com.kakarote.ai_crm.entity.BO.ImReactionBO bo) {
        return Result.ok(reactionService.toggle(UserUtil.getUserId(), messageId, bo.getEmoji()));
    }

    @GetMapping("/messages/{id}/thread")
    @Operation(summary = "获取话题（根消息+回复）")
    @RequirePermission("im")
    public Result<List<ImMessageVO>> thread(@PathVariable("id") Long rootId) {
        return Result.ok(messageService.getThread(rootId, UserUtil.getUserId()));
    }
```

- [ ] **Step 5: Compile + full IM tests + commit**

`... mvn -q test -Dtest=ImReactionServiceTest,ImMessageServiceImplTest,ImConversationServiceImplTest,ImChannelServiceTest` → pass; `... mvn -q -DskipTests compile` → SUCCESS.
```bash
git add backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPushEnvelope.java backend/src/main/java/com/kakarote/ai_crm/im/ws/ImPushService.java backend/src/main/java/com/kakarote/ai_crm/im/ws/ImRealtimePushListener.java backend/src/main/java/com/kakarote/ai_crm/controller/ImController.java
git commit -m "feat(im): reaction push envelope + endpoints (toggle reaction, fetch thread)"
```

---

## Task 6: Frontend API client

**Files:** Modify `frontend/src/api/im.ts`

- [ ] **Step 1:** Add a `ImReaction` interface and extend `ImMessage` + `ImSendPayload`:

```ts
export interface ImReaction {
  emoji: string
  count: number
  mine: boolean
}
```
Add to `ImMessage`: `reactions?: ImReaction[]`, `mentionedUserIds?: string[]`, `mentionAll?: boolean`, `parentId?: string | null`, `replyCount?: number`.
Add to `ImSendPayload`: `mentionedUserIds?: string[]`, `mentionAll?: boolean`, `parentId?: string`.

Add endpoints:
```ts
export const toggleReaction = (messageId: string, emoji: string) =>
  post<ImReaction[]>(`/im/messages/${messageId}/reactions`, { emoji })
export const fetchThread = (rootId: string) => get<ImMessage[]>(`/im/messages/${rootId}/thread`)
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/im.ts
git commit -m "feat(im): frontend API for reactions + thread + mention/parent send fields"
```

---

## Task 7: Store — reactions, threads, mention notifications

**Files:** Modify `frontend/src/stores/im.ts`

Read the store first to match the exact shape of `onPush`, `messagesByConv`, `upsertMessage`, `notifyIfHidden`, `send`, `activeConversationId`, `myId`.

- [ ] **Step 1:** Import `toggleReaction`, `fetchThread`, `type ImReaction` from `@/api/im`.

- [ ] **Step 2:** Add thread state near the other refs:
```ts
const threadRoot = ref<ImMessage | null>(null)
const threadMessages = ref<ImMessage[]>([])
```

- [ ] **Step 3:** Extend `onPush` to handle `type === 'reaction'` and route thread replies. Inside the push handler:
```ts
    if (env.type === 'reaction') {
      const list = messagesByConv.value[env.conversationId]
      const target = list?.find(m => m.id === env.messageId)
      if (target) target.reactions = env.reactions
      if (threadRoot.value && (threadRoot.value.id === env.messageId)) threadRoot.value.reactions = env.reactions
      const tm = threadMessages.value.find(m => m.id === env.messageId)
      if (tm) tm.reactions = env.reactions
      return
    }
```
For incoming `message` envelopes that are thread replies (`env.message.parentId`), do NOT append to the main list; instead bump the root's `replyCount` and append to the open thread if it matches:
```ts
    // inside the type==='message' branch, before upsertMessage:
    const msg = env.message
    if (msg.parentId) {
      const list = messagesByConv.value[env.conversationId]
      const root = list?.find(m => m.id === msg.parentId)
      if (root) root.replyCount = (root.replyCount ?? 0) + 1
      if (threadRoot.value && threadRoot.value.id === msg.parentId) threadMessages.value.push(msg)
      // still surface mention notification + unread, then return (don't add to main timeline)
      maybeNotifyMention(msg)
      return
    }
```
Add a mention-notification helper and call it for normal incoming messages too:
```ts
  function maybeNotifyMention(msg: ImMessage) {
    const mentionsMe = msg.mentionAll === true || (msg.mentionedUserIds || []).includes(myId.value)
    if (mentionsMe && msg.senderId !== myId.value) {
      // reuse the existing notification path; show even if conversation is active
      try { if (document.hidden && Notification.permission === 'granted') new Notification(`${msg.senderName || ''} 提及了你`, { body: msg.content || '' }) } catch {}
    }
  }
```
(Integrate with the existing `notifyIfHidden` rather than duplicating if that fits more cleanly — the key behavior: a mention raises a notification.)

- [ ] **Step 4:** Add actions and export them:
```ts
  async function toggleReactionAction(messageId: string, emoji: string) {
    const reactions = await toggleReaction(messageId, emoji)
    // optimistic local update for the actor; push will also arrive
    for (const cid in messagesByConv.value) {
      const m = messagesByConv.value[cid].find(x => x.id === messageId)
      if (m) m.reactions = reactions
    }
    if (threadRoot.value?.id === messageId) threadRoot.value.reactions = reactions
    const tm = threadMessages.value.find(x => x.id === messageId)
    if (tm) tm.reactions = reactions
  }
  async function openThread(rootId: string) {
    const rows = await fetchThread(rootId)
    threadRoot.value = rows[0] ?? null
    threadMessages.value = rows.slice(1)
  }
  function closeThread() { threadRoot.value = null; threadMessages.value = [] }
  async function sendThreadReply(payload: ImSendPayload) {
    await sendMessage({ ...payload, parentId: threadRoot.value?.id })
    // the WS push appends the reply to the open thread; no local append needed
  }
```
Return `threadRoot, threadMessages, toggleReactionAction, openThread, closeThread, sendThreadReply`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/im.ts
git commit -m "feat(im): store reaction/thread state + mention notification"
```

---

## Task 8: ImView — reactions UI

**Files:** Modify `frontend/src/views/im/ImView.vue`

- [ ] **Step 1:** Add the curated palette + a popover-open state:
```ts
const REACTION_EMOJIS = ['👍','❤️','😂','🎉','👀','✅','🙏','😄']
const reactionPickerFor = ref<string | null>(null) // messageId whose picker is open
```

- [ ] **Step 2:** In the message hover action bar (where 撤回 lives), add an emoji button that toggles `reactionPickerFor` for that message; render a small popover with `REACTION_EMOJIS` that calls `im.toggleReactionAction(msg.id, emoji)` then closes the picker. Use the existing `.wk-im-hoverbar` styling; the popover can be an absolutely-positioned `div` with the 8 emojis.

- [ ] **Step 3:** Below each message body, render reaction chips when `row.msg.reactions?.length`:
```vue
<div v-if="row.msg.reactions && row.msg.reactions.length" style="display:flex;gap:6px;margin-top:4px;flex-wrap:wrap;">
  <button v-for="r in row.msg.reactions" :key="r.emoji" class="wk-im-reaction" :class="{ mine: r.mine }" @click="im.toggleReactionAction(row.msg.id, r.emoji)">
    <span>{{ r.emoji }}</span><span style="font-size:11px;">{{ r.count }}</span>
  </button>
</div>
```
Add `.wk-im-reaction` CSS (rounded pill, border; `.mine` → purple-tinted border/bg like `#6d4aff`).

- [ ] **Step 4: Type-check + commit**

Build → vue-tsc clean.
```bash
git add frontend/src/views/im/ImView.vue
git commit -m "feat(im): message reaction picker + chips UI"
```

---

## Task 9: ImView — @mention composer + rendering

**Files:** Modify `frontend/src/views/im/ImView.vue`

- [ ] **Step 1: Mention state + candidates:**
```ts
const mentionOpen = ref(false)
const mentionQuery = ref('')
const pendingMentions = ref<Record<string, string>>({}) // name -> userId, accumulated for the draft
const channelMembersCache = ref<ImContact[]>([])
const mentionCandidates = computed(() => {
  const base = isChannel(activeConv.value!) ? channelMembersCache.value : (activeConv.value ? [{ userId: activeConv.value.peerUserId, name: activeConv.value.peerName } as ImContact] : [])
  const q = mentionQuery.value.toLowerCase()
  const list = q ? base.filter(c => c.name.toLowerCase().includes(q)) : base
  return isChannel(activeConv.value!) ? [{ userId: '__all__', name: '所有人' } as ImContact, ...list] : list
})
```
When a channel conversation is selected, load its members into `channelMembersCache` (call `im.fetchChannelMembers(convId)` in the existing `selectConversation` watch or on demand when `@` is typed).

- [ ] **Step 2: Composer `@` detection.** In the composer `@keyup`/`input` handler (or a `watch(draft)`), detect a trailing `@<query>` token at the caret; set `mentionOpen=true` + `mentionQuery`. Render an autocomplete list above the composer iterating `mentionCandidates`; selecting an item:
  - replaces the `@<query>` in `draft` with `@<name> `,
  - if `userId === '__all__'` set a `draftMentionAll = true` flag; else `pendingMentions[name] = userId`,
  - closes the picker.

- [ ] **Step 3: On send**, compute the payload mention fields from the draft. Before sending, derive which pending mentions actually remain in the text:
```ts
function buildMentionFields(text: string) {
  const ids: string[] = []
  let all = false
  for (const [name, uid] of Object.entries(pendingMentions.value)) {
    if (text.includes('@' + name)) ids.push(uid)
  }
  if (draftMentionAll.value && text.includes('@所有人')) all = true
  return { mentionedUserIds: ids, mentionAll: all }
}
```
Pass `{ ...mentionFields }` into `im.send(...)`. Reset `pendingMentions`/`draftMentionAll` after send.

- [ ] **Step 4: Render mentions highlighted.** Replace the plain `{{ row.msg.content }}` text rendering with a small render that splits on `@`-tokens and wraps any `@Name` (matching a mentioned user or `@所有人`) in a highlight span. A pragmatic approach: a computed/function `renderContent(msg)` returning HTML-safe segments, rendered with `<template v-for>` spans (avoid `v-html` for XSS safety) — split `content` by the regex `/(@[^\s@]+)/g`, and style a segment purple if it starts with `@` and (`msg.mentionAll` && seg==='@所有人') or the name maps to an id in `msg.mentionedUserIds` resolvable via the member list. If exact id resolution is hard at render time, highlight any `@token` that is `@所有人` or whose name is in the current member set — acceptable for v1.
- Add an "@你" badge on a message when `row.msg.mentionAll || (row.msg.mentionedUserIds||[]).includes(myId)`.

- [ ] **Step 5: Type-check + commit**

Build → vue-tsc clean.
```bash
git add frontend/src/views/im/ImView.vue
git commit -m "feat(im): @mention composer autocomplete + highlight + @you badge"
```

---

## Task 10: ImView — thread drawer

**Files:** Modify `frontend/src/views/im/ImView.vue`

- [ ] **Step 1:** On each main-timeline message, add a "回复" hover action (next to reaction/recall) calling `openThreadDrawer(row.msg.id)`, and on messages with `row.msg.replyCount > 0` render a "💬 {{ replyCount }} 条回复" affordance below the body that also calls `openThreadDrawer`.

```ts
const threadDrawerOpen = ref(false)
const threadDraft = ref('')
async function openThreadDrawer(rootId: string) { await im.openThread(rootId); threadDrawerOpen.value = true }
function closeThreadDrawer() { threadDrawerOpen.value = false; im.closeThread() }
async function sendThreadMessage() {
  const text = threadDraft.value.trim(); if (!text) return
  threadDraft.value = ''
  await im.sendThreadReply({ conversationId: im.activeConversationId!, contentType: 'text', content: text })
}
```

- [ ] **Step 2:** Add a right-side thread drawer (use `el-drawer` with `direction="rtl"` and a width ~420px, or an absolutely-positioned panel consistent with the app). Header: "话题". Body: render `im.threadRoot` then `im.threadMessages` reusing the same message-row layout (sender avatar/name/time, content, reactions). Footer: a composer (textarea + send) bound to `threadDraft` calling `sendThreadMessage` (Enter to send, Shift+Enter newline — reuse the main composer's key handler pattern). Reactions inside the thread reuse `im.toggleReactionAction`.

- [ ] **Step 3: Type-check + commit**

Build → vue-tsc clean.
```bash
git add frontend/src/views/im/ImView.vue
git commit -m "feat(im): thread drawer (root + replies + composer) with reply-count affordance"
```

---

## Task 11: Build, deploy, smoke test

**Files:** none (verification).

- [ ] **Step 1:** Backend tests + package:
`... mvn -q test -Dtest=ImReactionServiceTest,ImMessageServiceImplTest,ImConversationServiceImplTest,ImChannelServiceTest` → pass; `... mvn -q -DskipTests package` → SUCCESS.

- [ ] **Step 2:** Build FE, bake, deploy (project recipe):
```bash
cd /f/projects/WUKONG/AI_CRM
MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM/frontend:/app" -w /app node:20 sh -c "npm install && npm run build"
rm -rf backend/src/main/resources/public && mkdir -p backend/src/main/resources/public && cp -r frontend/dist/. backend/src/main/resources/public/
MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q -DskipTests clean package
MSYS_NO_PATHCONV=1 docker build -t wk_ai_crm:local-aiimport -f backend/Dockerfile backend/
cd docker && MSYS_NO_PATHCONV=1 docker compose up -d --force-recreate crm; cd /f/projects/WUKONG/AI_CRM
rm -rf backend/src/main/resources/public
```
Confirm crm healthy + Flyway V43 applied (`docker exec WeKnora-postgres psql -U postgres -d wk_ai_crm -t -A -c "SELECT version,success FROM flyway_schema_history WHERE version='43';"`).

- [ ] **Step 3: API smoke test** (admin):
```bash
TOKEN=$(curl -s -XPOST localhost:8088/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"123456a"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
# create a channel, send a root message
CID=$(curl -s -XPOST localhost:8088/im/channels -H "Manager-Token: $TOKEN" -H 'Content-Type: application/json' --data-binary '{"name":"p3-test","visibility":"public"}' | sed -n 's/.*"conversationId":"\([0-9]*\)".*/\1/p')
MID=$(curl -s -XPOST localhost:8088/im/messages -H "Manager-Token: $TOKEN" -H 'Content-Type: application/json' --data-binary "{\"conversationId\":$CID,\"content\":\"root\",\"mentionAll\":true}" | sed -n 's/.*"id":"\([0-9]*\)".*/\1/p')
# react
curl -s -XPOST "localhost:8088/im/messages/$MID/reactions" -H "Manager-Token: $TOKEN" -H 'Content-Type: application/json' --data-binary '{"emoji":"👍"}'; echo
# thread reply
curl -s -XPOST localhost:8088/im/messages -H "Manager-Token: $TOKEN" -H 'Content-Type: application/json' --data-binary "{\"conversationId\":$CID,\"content\":\"reply\",\"parentId\":$MID}" -w " HTTP %{http_code}\n" -o /dev/null
# main timeline excludes the reply; root shows replyCount=1
curl -s "localhost:8088/im/conversations/$CID/messages" -H "Manager-Token: $TOKEN" | grep -o '"replyCount":[0-9]*' | head; echo
# thread fetch returns root + reply
curl -s "localhost:8088/im/messages/$MID/thread" -H "Manager-Token: $TOKEN" | grep -o '"content":"[^"]*"'
# clean up
docker exec WeKnora-postgres psql -U postgres -d wk_ai_crm -c "DELETE FROM crm_im_message_reaction WHERE conversation_id=$CID; DELETE FROM crm_im_message WHERE conversation_id=$CID; DELETE FROM crm_im_conversation_member WHERE conversation_id=$CID; DELETE FROM crm_im_conversation WHERE id=$CID;" >/dev/null
```
Expected: reaction returns `[{emoji:👍,count:1,mine:true}]`; reply send 200; main timeline root has `"replyCount":1` and does NOT contain `"content":"reply"`; thread fetch returns both `root` and `reply`.

- [ ] **Step 4: Browser hand-off:** verify reactions (hover → emoji → chip toggles, live across two sessions), @mention (`@` autocomplete + `@所有人`, highlight, notification to a second user), threads (回复 → drawer, reply count on root, replies stay out of main timeline).

---

## Self-Review Notes

- **Spec coverage:** reactions table + toggle + aggregate + push + chips (T1/T3/T5/T8) ✓; curated emoji server-validated (`ALLOWED_EMOJI`, T3) ✓; mentions store+VO+notify+highlight+@all (T2/T4/T7/T9) ✓; threads parent_id/reply_count + reply-excluded history + thread fetch + drawer + count affordance (T1/T4/T5/T10) ✓; reactions/mentions inside threads (thread messages are ordinary messages, rendered with the same row → T10 reuses chips/highlight) ✓; realtime reuse + reaction envelope (T5/T7) ✓; membership asserted on toggle/thread (T3/T4) ✓.
- **Type consistency:** backend `toggle(userId,messageId,emoji)→List<ImReactionVO>`, `aggregate(messageId,viewerId)`, `history(convId,viewerId,beforeId,limit)`, `getThread(rootId,viewerId)`, `ImSendMessageBO{mentionedUserIds:List<Long>,mentionAll,parentId}`, `ImMessageVO{reactions,mentionedUserIds:List<String>,mentionAll,parentId,replyCount}`, envelope factories `message/unread/reaction`; api `toggleReaction/fetchThread` + `ImSendPayload{mentionedUserIds,mentionAll,parentId}`; store `toggleReactionAction/openThread/closeThread/sendThreadReply` + `threadRoot/threadMessages`. Consistent across stack.
- **Placeholder scan:** complete code for backend + concrete code for frontend; the frontend `@`-token highlight is intentionally pragmatic (highlight `@所有人` + member-name tokens) and noted as v1-acceptable; emoji palette mirrored both sides.
- **Reuse:** no change to recall/markRead/presence; history gains a viewer arg + reply exclusion; `getMessageVO` stays viewer-agnostic (reactions `mine=false`) and the reaction push recomputes per-member.
