# IM Phase 2 — Group Channels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add public/private multi-member channels to the IM feature, reusing the Phase 1 conversation/member/message tables and the WebSocket fan-out (already N-member ready).

**Architecture:** Channels are `crm_im_conversation` rows with `type='channel'` + new columns (name/description/visibility/owner_id); members and messages reuse the Phase 1 tables; messaging/recall/unread/realtime-push are unchanged (membership-based). New service methods + REST endpoints handle create/browse/join/leave/add-members; the frontend adds a 频道 filter, create/browse dialogs, and a channel chat header.

**Tech Stack:** Java 21, Spring Boot 3.3, MyBatis-Plus, PostgreSQL/Flyway, JUnit5+Mockito; Vue 3 + TS + Pinia + Element Plus.

**Environment:** Branch `feature/im-phase2-channels` (do NOT switch). Maven via Docker:
`MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn <args>`.
Frontend build via Docker:
`MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM/frontend:/app" -w /app node:20 sh -c "npm install && npm run build"` (vite chunk warnings are fine; only vue-tsc errors fail).

## File Structure

- Create `backend/.../db/migration/V42__im_channels.sql` — alter conversation table.
- Modify `backend/.../entity/PO/ImConversation.java` — add fields.
- Modify `backend/.../entity/VO/ImConversationVO.java` — add type/name/memberCount/visibility.
- Create `backend/.../entity/BO/ImCreateChannelBO.java`, `ImChannelMembersBO.java`.
- Modify `backend/.../service/ImConversationService.java` + `impl/ImConversationServiceImpl.java` — channel methods + channel-aware listMyConversations.
- Modify `backend/.../controller/ImController.java` — channel endpoints.
- Test `backend/.../service/impl/ImChannelServiceTest.java`.
- Modify `frontend/src/api/im.ts`, `frontend/src/stores/im.ts`, `frontend/src/views/im/ImView.vue`.

---

## Task 1: Migration + entity fields

**Files:**
- Create: `backend/src/main/resources/db/migration/V42__im_channels.sql`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/entity/PO/ImConversation.java`

- [ ] **Step 1: Migration** (`V42` is the next free version after V41 — confirm with `ls backend/src/main/resources/db/migration | grep -oE 'V[0-9]+' | sed 's/V//' | sort -n | tail -1` → `41`):

```sql
-- ============================================
-- V42: IM group channels. Extends crm_im_conversation (type now direct|channel).
-- ============================================
ALTER TABLE crm_im_conversation ADD COLUMN IF NOT EXISTS name VARCHAR(100);
ALTER TABLE crm_im_conversation ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE crm_im_conversation ADD COLUMN IF NOT EXISTS visibility VARCHAR(16);
ALTER TABLE crm_im_conversation ADD COLUMN IF NOT EXISTS owner_id BIGINT;

COMMENT ON COLUMN crm_im_conversation.name IS '频道名称（direct 为空）';
COMMENT ON COLUMN crm_im_conversation.visibility IS 'public/private（direct 为空）';
COMMENT ON COLUMN crm_im_conversation.owner_id IS '频道创建者用户ID';

CREATE INDEX IF NOT EXISTS idx_im_conv_channel ON crm_im_conversation (type, visibility);
```

- [ ] **Step 2: Entity** — add fields to `ImConversation.java` after `private String memberKey;`:

```java
    private String name;
    private String description;
    private String visibility;
    private Long ownerId;
```

- [ ] **Step 3: Compile + commit**

Run: `... mvn -q -DskipTests compile` → BUILD SUCCESS.
```bash
git add backend/src/main/resources/db/migration/V42__im_channels.sql backend/src/main/java/com/kakarote/ai_crm/entity/PO/ImConversation.java
git commit -m "feat(im): channel columns on crm_im_conversation (V42)"
```

---

## Task 2: VO + BOs

**Files:**
- Modify: `backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImConversationVO.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/BO/ImCreateChannelBO.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/BO/ImChannelMembersBO.java`

- [ ] **Step 1:** Add fields to `ImConversationVO` (after `private Date updateTime;`):

```java
    private String type;          // direct | channel
    private String name;          // channel name (null for direct)
    private String visibility;    // public | private (channels)
    private Integer memberCount;  // channels
```

- [ ] **Step 2:** `ImCreateChannelBO.java`:

```java
package com.kakarote.ai_crm.entity.BO;

import lombok.Data;
import java.util.List;

@Data
public class ImCreateChannelBO {
    private String name;
    private String description;
    private String visibility;       // public | private; defaults to public if blank
    private List<Long> memberIds;    // optional initial members (besides creator)
}
```

- [ ] **Step 3:** `ImChannelMembersBO.java`:

```java
package com.kakarote.ai_crm.entity.BO;

import lombok.Data;
import java.util.List;

@Data
public class ImChannelMembersBO {
    private List<Long> userIds;
}
```

- [ ] **Step 4: Compile + commit**

Run: `... mvn -q -DskipTests compile` → BUILD SUCCESS.
```bash
git add backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImConversationVO.java backend/src/main/java/com/kakarote/ai_crm/entity/BO/ImCreateChannelBO.java backend/src/main/java/com/kakarote/ai_crm/entity/BO/ImChannelMembersBO.java
git commit -m "feat(im): channel VO fields + create/members BOs"
```

---

## Task 3: Channel service methods + channel-aware list + tests

**Files:**
- Modify: `backend/src/main/java/com/kakarote/ai_crm/service/ImConversationService.java`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/service/impl/ImConversationServiceImpl.java`
- Test: `backend/src/test/java/com/kakarote/ai_crm/service/impl/ImChannelServiceTest.java`

Context — the impl already injects `ImConversationMemberMapper memberMapper`, `ImMessageMapper messageMapper`, `ImMessageService imMessageService`, `ManageUserService manageUserService`, `FileStorageService fileStorageService` (required=false). `ImConversationMember` fields: `id, conversationId, userId, lastReadMessageId, createTime, updateTime`. `unreadCount(convId,userId)` and `memberUserIds(convId)` and `assertMember(convId,userId)` exist. The service extends `ServiceImpl<ImConversationMapper, ImConversation>` (so `save`, `getById`, `list`, `lambdaQuery` available).

- [ ] **Step 1:** Add to `ImConversationService` interface:

```java
    ImConversation createChannel(Long creatorId, String name, String description, String visibility, java.util.List<Long> memberIds);
    java.util.List<com.kakarote.ai_crm.entity.VO.ImConversationVO> browsePublicChannels(Long userId, String keyword);
    void joinChannel(Long userId, Long channelId);
    void leaveChannel(Long userId, Long channelId);
    void addMembers(Long actorId, Long channelId, java.util.List<Long> userIds);
```

- [ ] **Step 2:** Add to `ImConversationServiceImpl` these methods + a private helper. Imports needed: `com.kakarote.ai_crm.entity.PO.ImConversationMember`, `cn.hutool.core.util.StrUtil`, `com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper`, `com.kakarote.ai_crm.common.exception.BusinessException`, `com.kakarote.ai_crm.common.result.SystemCodeEnum`, `java.util.*`.

```java
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public ImConversation createChannel(Long creatorId, String name, String description, String visibility, List<Long> memberIds) {
        String trimmed = StrUtil.trim(name);
        if (StrUtil.isBlank(trimmed)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "频道名称不能为空");
        }
        String vis = "private".equalsIgnoreCase(StrUtil.trim(visibility)) ? "private" : "public";
        ImConversation conv = new ImConversation();
        conv.setType("channel");
        conv.setName(trimmed);
        conv.setDescription(StrUtil.trim(description));
        conv.setVisibility(vis);
        conv.setOwnerId(creatorId);
        Date now = new Date();
        conv.setCreateTime(now);
        conv.setUpdateTime(now);
        save(conv);
        // creator + distinct members
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ids.add(creatorId);
        if (memberIds != null) {
            for (Long id : memberIds) {
                if (id != null) ids.add(id);
            }
        }
        for (Long uid : ids) {
            addMemberRow(conv.getId(), uid);
        }
        return conv;
    }

    @Override
    public List<com.kakarote.ai_crm.entity.VO.ImConversationVO> browsePublicChannels(Long userId, String keyword) {
        Set<Long> myConvIds = memberMapper.selectList(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getUserId, userId)).stream()
                .map(ImConversationMember::getConversationId).collect(java.util.stream.Collectors.toSet());
        LambdaQueryWrapper<ImConversation> w = new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getType, "channel")
                .eq(ImConversation::getVisibility, "public")
                .orderByDesc(ImConversation::getUpdateTime);
        if (StrUtil.isNotBlank(keyword)) {
            w.like(ImConversation::getName, StrUtil.trim(keyword));
        }
        List<com.kakarote.ai_crm.entity.VO.ImConversationVO> out = new ArrayList<>();
        for (ImConversation c : list(w)) {
            if (myConvIds.contains(c.getId())) continue;
            out.add(toChannelVO(c));
        }
        return out;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void joinChannel(Long userId, Long channelId) {
        ImConversation conv = getById(channelId);
        if (conv == null || !"channel".equals(conv.getType())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "频道不存在");
        }
        if (!"public".equals(conv.getVisibility())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "私有频道需被邀请加入");
        }
        addMemberRow(channelId, userId);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void leaveChannel(Long userId, Long channelId) {
        memberMapper.delete(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getConversationId, channelId)
                .eq(ImConversationMember::getUserId, userId));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void addMembers(Long actorId, Long channelId, List<Long> userIds) {
        ImConversation conv = getById(channelId);
        if (conv == null || !"channel".equals(conv.getType())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "频道不存在");
        }
        assertMember(channelId, actorId); // any member may add (per Phase 2 decision)
        if (userIds == null) return;
        for (Long uid : userIds) {
            if (uid != null) addMemberRow(channelId, uid);
        }
    }

    /** Add a membership row if the user is not already a member (idempotent). */
    private void addMemberRow(Long conversationId, Long userId) {
        Long existing = memberMapper.selectCount(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getConversationId, conversationId)
                .eq(ImConversationMember::getUserId, userId));
        if (existing != null && existing > 0) return;
        ImConversationMember m = new ImConversationMember();
        m.setConversationId(conversationId);
        m.setUserId(userId);
        m.setLastReadMessageId(0L);
        Date now = new Date();
        m.setCreateTime(now);
        m.setUpdateTime(now);
        memberMapper.insert(m);
    }

    /** Build a lightweight VO for a channel (no peer fields). */
    private com.kakarote.ai_crm.entity.VO.ImConversationVO toChannelVO(ImConversation c) {
        com.kakarote.ai_crm.entity.VO.ImConversationVO vo = new com.kakarote.ai_crm.entity.VO.ImConversationVO();
        vo.setId(String.valueOf(c.getId()));
        vo.setType("channel");
        vo.setName(c.getName());
        vo.setVisibility(c.getVisibility());
        vo.setUpdateTime(c.getUpdateTime());
        int count = Math.toIntExact(memberMapper.selectCount(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getConversationId, c.getId())));
        vo.setMemberCount(count);
        return vo;
    }
```

- [ ] **Step 3:** Make `listMyConversations` channel-aware. Find the existing per-conversation loop body in `listMyConversations` (it currently assumes direct and resolves a peer). Replace its body so it branches on type — for `channel` build via the channel path, for `direct` keep the existing peer logic. Concretely, inside the `for` loop over my memberships, after loading `ImConversation conv = getById(...)` and skipping null, insert at the very top of the loop body:

```java
            if ("channel".equals(conv.getType())) {
                com.kakarote.ai_crm.entity.VO.ImConversationVO cv = toChannelVO(conv);
                cv.setUnreadCount(unreadCount(conv.getId(), userId));
                if (conv.getLastMessageId() != null) {
                    cv.setLastMessage(imMessageService.getMessageVO(conv.getLastMessageId()));
                }
                out.add(cv);
                continue;
            }
```

Also set `vo.setType("direct")` on the direct VO that the existing code builds (so the frontend can branch). Add `vo.setType("direct");` right after the direct `vo.setId(...)` line.

- [ ] **Step 4: Write the failing tests** — `ImChannelServiceTest.java`. These mock the mappers (`ImConversationMemberMapper`, `ImMessageMapper`) and `ManageUserService`, and inject them + set the MyBatis-Plus `baseMapper` (the `ImConversationMapper`) so `save`/`getById`/`list` route to a mock. Use the same `ReflectionTestUtils` pattern as `ImConversationServiceImplTest`.

```java
package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.PO.ImConversation;
import com.kakarote.ai_crm.entity.PO.ImConversationMember;
import com.kakarote.ai_crm.mapper.ImConversationMapper;
import com.kakarote.ai_crm.mapper.ImConversationMemberMapper;
import com.kakarote.ai_crm.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImChannelServiceTest {

    @Mock ImConversationMapper conversationMapper;
    @Mock ImConversationMemberMapper memberMapper;
    ImConversationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImConversationServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", conversationMapper);
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);
        // no existing members by default
        when(memberMapper.selectCount(any())).thenReturn(0L);
    }

    @Test
    void createChannelSetsTypeOwnerVisibilityAndAddsCreatorPlusMembers() {
        when(conversationMapper.insert(any(ImConversation.class))).thenAnswer(inv -> {
            ((ImConversation) inv.getArgument(0)).setId(500L);
            return 1;
        });

        ImConversation conv = service.createChannel(1L, "  研发组  ", "desc", "private", List.of(2L, 3L, 2L));

        assertThat(conv.getType()).isEqualTo("channel");
        assertThat(conv.getName()).isEqualTo("研发组");
        assertThat(conv.getVisibility()).isEqualTo("private");
        assertThat(conv.getOwnerId()).isEqualTo(1L);
        // creator(1) + 2 + 3 distinct = 3 member rows
        verify(memberMapper, times(3)).insert(any(ImConversationMember.class));
    }

    @Test
    void createChannelRejectsBlankName() {
        assertThatThrownBy(() -> service.createChannel(1L, "  ", null, "public", null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void joinRejectsPrivateChannel() {
        ImConversation c = new ImConversation();
        c.setId(7L); c.setType("channel"); c.setVisibility("private");
        when(conversationMapper.selectById(7L)).thenReturn(c);
        assertThatThrownBy(() -> service.joinChannel(9L, 7L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void joinAddsMemberForPublicChannel() {
        ImConversation c = new ImConversation();
        c.setId(7L); c.setType("channel"); c.setVisibility("public");
        when(conversationMapper.selectById(7L)).thenReturn(c);
        service.joinChannel(9L, 7L);
        verify(memberMapper).insert(any(ImConversationMember.class));
    }

    @Test
    void addMembersRequiresActorToBeMember() {
        ImConversation c = new ImConversation();
        c.setId(7L); c.setType("channel"); c.setVisibility("public");
        when(conversationMapper.selectById(7L)).thenReturn(c);
        // assertMember uses memberMapper.selectCount on (conv,user). Make actor NOT a member:
        when(memberMapper.selectCount(any())).thenReturn(0L);
        assertThatThrownBy(() -> service.addMembers(2L, 7L, List.of(3L)))
                .isInstanceOf(BusinessException.class);
    }
}
```

> Verify `assertMember` throws `BusinessException` when `selectCount==0` (it does in Phase 1). Since `addMemberRow` and `assertMember` both call `memberMapper.selectCount`, the `addMembersRequiresActorToBeMember` test relies on `assertMember` running first and throwing before any insert — confirm ordering in your impl (assertMember is called before the add loop). If the shared `selectCount` stub makes a test ambiguous, use `when(memberMapper.selectCount(any())).thenReturn(0L)` (actor not a member → assertMember throws) as written.

- [ ] **Step 5: Run tests**

Run: `... mvn -q test -Dtest=ImChannelServiceTest` → PASS (5 tests). Iterate if the `baseMapper` field name or `assertMember`/`selectCount` interaction needs adjustment (mirror `ImConversationServiceImplTest`).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/kakarote/ai_crm/service/ImConversationService.java backend/src/main/java/com/kakarote/ai_crm/service/impl/ImConversationServiceImpl.java backend/src/test/java/com/kakarote/ai_crm/service/impl/ImChannelServiceTest.java
git commit -m "feat(im): channel create/browse/join/leave/addMembers + channel-aware list"
```

---

## Task 4: Controller endpoints

**Files:** Modify `backend/src/main/java/com/kakarote/ai_crm/controller/ImController.java`

- [ ] **Step 1:** Add imports: `com.kakarote.ai_crm.entity.BO.ImCreateChannelBO`, `com.kakarote.ai_crm.entity.BO.ImChannelMembersBO`. (`ImConversationVO`, `ImContactVO`, `ManageUserVO`, `Result`, `UserUtil`, `StrUtil`, `List`, `ArrayList` are already imported from Phase 1c.) Add these methods (the class already has `conversationService`, `manageUserService`, `presenceService`):

```java
    @PostMapping("/channels")
    @Operation(summary = "创建频道")
    @RequirePermission("im")
    public Result<java.util.Map<String, String>> createChannel(@RequestBody ImCreateChannelBO bo) {
        var conv = conversationService.createChannel(
                UserUtil.getUserId(), bo.getName(), bo.getDescription(), bo.getVisibility(), bo.getMemberIds());
        return Result.ok(java.util.Map.of("conversationId", String.valueOf(conv.getId())));
    }

    @GetMapping("/channels/public")
    @Operation(summary = "浏览可加入的公开频道")
    @RequirePermission("im")
    public Result<List<ImConversationVO>> publicChannels(@RequestParam(value = "keyword", required = false) String keyword) {
        return Result.ok(conversationService.browsePublicChannels(UserUtil.getUserId(), keyword));
    }

    @PostMapping("/channels/{id}/join")
    @Operation(summary = "加入公开频道")
    @RequirePermission("im")
    public Result<String> joinChannel(@PathVariable("id") Long id) {
        conversationService.joinChannel(UserUtil.getUserId(), id);
        return Result.ok("ok");
    }

    @PostMapping("/channels/{id}/leave")
    @Operation(summary = "退出频道")
    @RequirePermission("im")
    public Result<String> leaveChannel(@PathVariable("id") Long id) {
        conversationService.leaveChannel(UserUtil.getUserId(), id);
        return Result.ok("ok");
    }

    @PostMapping("/channels/{id}/members")
    @Operation(summary = "添加频道成员")
    @RequirePermission("im")
    public Result<String> addChannelMembers(@PathVariable("id") Long id, @RequestBody ImChannelMembersBO bo) {
        conversationService.addMembers(UserUtil.getUserId(), id, bo.getUserIds());
        return Result.ok("ok");
    }

    @GetMapping("/channels/{id}/members")
    @Operation(summary = "频道成员列表")
    @RequirePermission("im")
    public Result<List<ImContactVO>> channelMembers(@PathVariable("id") Long id) {
        Long me = UserUtil.getUserId();
        conversationService.assertMember(id, me);
        List<ImContactVO> out = new ArrayList<>();
        for (Long uid : conversationService.memberUserIds(id)) {
            ManageUserVO u = manageUserService.getById(uid) == null ? null
                    : org.springframework.beans.BeanUtils.instantiateClass(ManageUserVO.class);
            var pu = manageUserService.getById(uid);
            if (pu == null) continue;
            ImContactVO vo = new ImContactVO();
            vo.setUserId(String.valueOf(uid));
            vo.setName(StrUtil.blankToDefault(pu.getRealname(), pu.getUsername()));
            vo.setOnline(presenceService.isOnline(String.valueOf(uid)));
            out.add(vo);
        }
        return Result.ok(out);
    }
```

> Simplify `channelMembers`: `manageUserService.getById(uid)` returns a `ManagerUser` (PO) — use its `getRealname()/getUsername()/getImg()`. Drop the stray `ManageUserVO` line; build `ImContactVO` directly from the `ManagerUser`. Resolve avatar via `fileStorageService.getUrl(img)` only if you inject it; otherwise leave avatarUrl null. Final loop body:
```java
        for (Long uid : conversationService.memberUserIds(id)) {
            com.kakarote.ai_crm.entity.PO.ManagerUser pu = manageUserService.getById(uid);
            if (pu == null) continue;
            ImContactVO vo = new ImContactVO();
            vo.setUserId(String.valueOf(uid));
            vo.setName(StrUtil.blankToDefault(pu.getRealname(), pu.getUsername()));
            vo.setOnline(presenceService.isOnline(String.valueOf(uid)));
            out.add(vo);
        }
```

- [ ] **Step 2: Compile + commit**

Run: `... mvn -q -DskipTests compile` → BUILD SUCCESS.
```bash
git add backend/src/main/java/com/kakarote/ai_crm/controller/ImController.java
git commit -m "feat(im): channel REST endpoints (create/browse/join/leave/members)"
```

---

## Task 5: Frontend API client

**Files:** Modify `frontend/src/api/im.ts`

- [ ] **Step 1:** Extend the `ImConversation` interface with the new fields and add channel endpoints. Change `ImConversation` to:

```ts
export interface ImConversation {
  id: string
  type?: 'direct' | 'channel'
  name?: string | null
  visibility?: 'public' | 'private' | null
  memberCount?: number | null
  peerUserId: string
  peerName: string
  peerAvatarUrl?: string | null
  lastMessage?: ImMessage | null
  unreadCount: number
  updateTime: string
}
```

Add at the end of the file:
```ts
export interface ImCreateChannelPayload {
  name: string
  description?: string
  visibility: 'public' | 'private'
  memberIds?: string[]
}

export const createChannel = (payload: ImCreateChannelPayload) =>
  post<{ conversationId: string }>('/im/channels', payload)
export const browsePublicChannels = (keyword?: string) =>
  get<ImConversation[]>('/im/channels/public', { params: keyword ? { keyword } : {} })
export const joinChannel = (id: string) => post<string>(`/im/channels/${id}/join`)
export const leaveChannel = (id: string) => post<string>(`/im/channels/${id}/leave`)
export const addChannelMembers = (id: string, userIds: string[]) =>
  post<string>(`/im/channels/${id}/members`, { userIds })
export const listChannelMembers = (id: string) => get<ImContact[]>(`/im/channels/${id}/members`)
```

> `memberIds`/`userIds` are sent as strings; backend `Long` binding parses numeric strings fine (Snowflake-safe).

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/im.ts
git commit -m "feat(im): frontend channel API + ImConversation channel fields"
```

---

## Task 6: Store channel actions

**Files:** Modify `frontend/src/stores/im.ts`

- [ ] **Step 1:** Import the new api functions (extend the existing import from `@/api/im`):

```ts
import {
  listConversations, listContacts, openDirect, fetchHistory,
  sendMessage, recallMessage, markRead,
  createChannel, browsePublicChannels, joinChannel, leaveChannel, addChannelMembers, listChannelMembers,
  type ImConversation, type ImMessage, type ImContact, type ImSendPayload, type ImCreateChannelPayload,
} from '@/api/im'
```

- [ ] **Step 2:** Add these actions inside the store (before the `return { ... }`), and add them to the returned object:

```ts
  async function createChannelAction(payload: ImCreateChannelPayload): Promise<string> {
    const { conversationId } = await createChannel(payload)
    await refreshConversations()
    await selectConversation(conversationId)
    return conversationId
  }
  async function browseChannels(keyword?: string): Promise<ImConversation[]> {
    return browsePublicChannels(keyword)
  }
  async function joinChannelAction(channelId: string) {
    await joinChannel(channelId)
    await refreshConversations()
    await selectConversation(channelId)
  }
  async function leaveChannelAction(channelId: string) {
    await leaveChannel(channelId)
    if (activeConversationId.value === channelId) activeConversationId.value = null
    await refreshConversations()
  }
  async function addMembersAction(channelId: string, userIds: string[]) {
    await addChannelMembers(channelId, userIds)
  }
  async function fetchChannelMembers(channelId: string): Promise<ImContact[]> {
    return listChannelMembers(channelId)
  }
```

Add to the `return { ... }` list: `createChannelAction, browseChannels, joinChannelAction, leaveChannelAction, addMembersAction, fetchChannelMembers,`.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/stores/im.ts
git commit -m "feat(im): store actions for channel create/browse/join/leave/members"
```

---

## Task 7: ImView channel UI

**Files:** Modify `frontend/src/views/im/ImView.vue`

The view currently renders conversations assuming a peer (avatar from `peerUserId`, online dot, name from `peerName`). Make it channel-aware and add create/browse/members dialogs.

- [ ] **Step 1: Channel-aware list + header rendering.**
  - Add a helper `isChannel(c)` → `c.type === 'channel'`.
  - Add a display-name helper `convName(c)` → `c.type === 'channel' ? (c.name || '频道') : c.peerName`.
  - Add a display-avatar-text helper `convAvatarText(c)` → `c.type === 'channel' ? '#' : avatarText(c.peerName)`.
  - Conversation row: use `convName(c)` and `convAvatarText(c)`; show the online dot **only** when `!isChannel(c) && im.presence[c.peerUserId]`; avatar bg via `avatarBg(c.id, convName(c))`.
  - Add the **频道** filter chip: extend `filters` to `[{key:'all',label:'全部'},{key:'unread',label:'未读'},{key:'channel',label:'频道'}]`, change `listFilter` type to `'all' | 'unread' | 'channel'`, and in `filteredConversations` add: `if (listFilter.value === 'channel' && c.type !== 'channel') return false`.
  - Chat header: use `convName(activeConv)` and `convAvatarText(activeConv)`; show online text only for direct, and for channel show `${activeConv.memberCount ?? ''} 人` + a 成员 button that opens the members dialog.

- [ ] **Step 2: Header "+" menu** — replace the single 发起聊天 button with a small dropdown (use `el-dropdown`) offering **发起私聊** (opens contacts dialog, existing), **创建频道** (opens create dialog), **浏览频道** (opens browse dialog). Minimal markup:

```vue
        <el-dropdown trigger="click" @command="onNewMenu">
          <button class="wk-im-newchat"><span class="material-symbols-outlined" style="font-size:16px;">add</span>新建</button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="dm">发起私聊</el-dropdown-item>
              <el-dropdown-item command="channel">创建频道</el-dropdown-item>
              <el-dropdown-item command="browse">浏览频道</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
```
with `function onNewMenu(cmd: string){ if(cmd==='dm') openContacts.value=true; else if(cmd==='channel') openCreate.value=true; else { openBrowse.value=true; void loadBrowse() } }`.

- [ ] **Step 3: Create-channel dialog:**

```vue
    <el-dialog v-model="openCreate" title="创建频道" width="460px">
      <el-input v-model="channelForm.name" placeholder="频道名称" class="mb-2" maxlength="100" />
      <el-input v-model="channelForm.description" placeholder="频道简介（可选）" class="mb-2" />
      <el-radio-group v-model="channelForm.visibility" class="mb-3">
        <el-radio value="public">公开（全员可浏览加入）</el-radio>
        <el-radio value="private">私有（仅邀请）</el-radio>
      </el-radio-group>
      <el-input v-model="memberKeyword" placeholder="搜索同事添加为成员" class="mb-2" @input="im.refreshContacts(memberKeyword)" />
      <div style="max-height:240px;overflow-y:auto;">
        <label v-for="ct in im.contacts" :key="ct.userId" class="wk-im-contact" style="cursor:pointer;">
          <input type="checkbox" :value="ct.userId" v-model="channelForm.memberIds" />
          <span style="font-size:14px;">{{ ct.name }}</span>
          <span style="margin-left:auto;font-size:12px;color:#a7a7ab;">{{ ct.deptName }}</span>
        </label>
      </div>
      <template #footer>
        <el-button @click="openCreate = false">取消</el-button>
        <el-button type="primary" :disabled="!channelForm.name.trim()" @click="submitCreateChannel">创建</el-button>
      </template>
    </el-dialog>
```

- [ ] **Step 4: Browse-channels dialog:**

```vue
    <el-dialog v-model="openBrowse" title="浏览公开频道" width="440px">
      <el-input v-model="browseKeyword" placeholder="搜索频道" class="mb-2" @input="loadBrowse" />
      <div style="max-height:320px;overflow-y:auto;">
        <div v-for="ch in browseList" :key="ch.id" class="wk-im-contact">
          <div class="wk-im-avatar" :style="{ width:'32px',height:'32px',borderRadius:'8px',fontSize:'14px',background: avatarBg(ch.id, ch.name) }">#</div>
          <div style="min-width:0;">
            <div style="font-size:14px;">{{ ch.name }}</div>
            <div style="font-size:12px;color:#a7a7ab;">{{ ch.memberCount }} 人</div>
          </div>
          <el-button size="small" type="primary" style="margin-left:auto;" @click="joinAndOpen(ch.id)">加入</el-button>
        </div>
        <div v-if="!browseList.length" style="padding:20px;text-align:center;color:#a7a7ab;font-size:13px;">没有可加入的公开频道</div>
      </div>
    </el-dialog>
```

- [ ] **Step 5: Members dialog** (channel header 成员 button → opens; lists members, add-member via contacts multi-select):

```vue
    <el-dialog v-model="openMembers" title="频道成员" width="420px" @open="loadMembers">
      <div style="max-height:240px;overflow-y:auto;margin-bottom:10px;">
        <div v-for="mb in memberList" :key="mb.userId" class="wk-im-contact">
          <div class="wk-im-avatar" :style="{ width:'30px',height:'30px',borderRadius:'8px',fontSize:'12px',background: avatarBg(mb.userId, mb.name) }">{{ avatarText(mb.name) }}</div>
          <span style="font-size:14px;">{{ mb.name }}</span>
          <span v-if="mb.online" class="wk-im-dot" style="position:static;border:none;width:8px;height:8px;" />
        </div>
      </div>
      <el-input v-model="memberKeyword" placeholder="搜索同事添加" class="mb-2" @input="im.refreshContacts(memberKeyword)" />
      <div style="max-height:200px;overflow-y:auto;">
        <div v-for="ct in im.contacts" :key="ct.userId" class="wk-im-contact" @click="addOneMember(ct.userId)">
          <span style="font-size:14px;">{{ ct.name }}</span>
          <span style="margin-left:auto;font-size:12px;color:#6d4aff;">添加</span>
        </div>
      </div>
    </el-dialog>
```

- [ ] **Step 6: Script additions** (refs + handlers):

```ts
const openCreate = ref(false)
const openBrowse = ref(false)
const openMembers = ref(false)
const memberKeyword = ref('')
const browseKeyword = ref('')
const browseList = ref<import('@/api/im').ImConversation[]>([])
const memberList = ref<import('@/api/im').ImContact[]>([])
const channelForm = ref<{ name: string; description: string; visibility: 'public' | 'private'; memberIds: string[] }>(
  { name: '', description: '', visibility: 'public', memberIds: [] }
)

function isChannel(c: import('@/api/im').ImConversation) { return c.type === 'channel' }
function convName(c: import('@/api/im').ImConversation) { return c.type === 'channel' ? (c.name || '频道') : c.peerName }
function convAvatarText(c: import('@/api/im').ImConversation) { return c.type === 'channel' ? '#' : avatarText(c.peerName) }

function onNewMenu(cmd: string) {
  if (cmd === 'dm') { openContacts.value = true; im.refreshContacts() }
  else if (cmd === 'channel') { openCreate.value = true; channelForm.value = { name: '', description: '', visibility: 'public', memberIds: [] }; im.refreshContacts() }
  else { openBrowse.value = true; void loadBrowse() }
}
async function submitCreateChannel() {
  if (!channelForm.value.name.trim()) return
  openCreate.value = false
  await im.createChannelAction({
    name: channelForm.value.name.trim(),
    description: channelForm.value.description.trim(),
    visibility: channelForm.value.visibility,
    memberIds: channelForm.value.memberIds,
  })
}
async function loadBrowse() { browseList.value = await im.browseChannels(browseKeyword.value) }
async function joinAndOpen(id: string) { openBrowse.value = false; await im.joinChannelAction(id) }
async function loadMembers() { if (im.activeConversationId) memberList.value = await im.fetchChannelMembers(im.activeConversationId) }
async function addOneMember(userId: string) {
  if (!im.activeConversationId) return
  await im.addMembersAction(im.activeConversationId, [userId])
  await loadMembers()
}
```

- [ ] **Step 7: Type-check.**

Run: `MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM/frontend:/app" -w /app node:20 sh -c "npm install && npm run build"`
Expected: `vue-tsc` clean (chunk warnings fine). Fix TS errors (most likely: `el-dropdown` import is global via Element Plus auto-registration — confirm it renders; `el-radio` `value` prop vs `label` for EP 2.8 — use whichever the codebase uses, check a neighbor `el-radio`). Iterate until clean.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/views/im/ImView.vue
git commit -m "feat(im): channel UI — 频道 filter, create/browse/members dialogs, channel header"
```

---

## Task 8: Build, deploy, smoke test

**Files:** none (verification).

- [ ] **Step 1:** Backend tests + full build:
`... mvn -q test -Dtest=ImChannelServiceTest,ImConversationServiceImplTest,ImMessageServiceImplTest` → pass; `... mvn -q -DskipTests package` → SUCCESS.

- [ ] **Step 2:** Build frontend, bake dist into the image, deploy (reuse the project recipe):
```bash
cd /f/projects/WUKONG/AI_CRM
MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM/frontend:/app" -w /app node:20 sh -c "npm install && npm run build"
rm -rf backend/src/main/resources/public && mkdir -p backend/src/main/resources/public && cp -r frontend/dist/. backend/src/main/resources/public/
MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q -DskipTests clean package
MSYS_NO_PATHCONV=1 docker build -t wk_ai_crm:local-aiimport -f backend/Dockerfile backend/
cd docker && MSYS_NO_PATHCONV=1 docker compose up -d --force-recreate crm; cd /f/projects/WUKONG/AI_CRM
rm -rf backend/src/main/resources/public
```
Confirm crm healthy + Flyway V42 applied (`docker logs crm 2>&1 | grep -i "version .42"`).

- [ ] **Step 3: API smoke test** (admin token; create a 2nd user as in Phase 1a's test if needed):
```bash
TOKEN=$(curl -s -XPOST localhost:8088/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"123456a"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
# create a public channel
CH=$(curl -s -XPOST localhost:8088/im/channels -H "Manager-Token: $TOKEN" -H 'Content-Type: application/json' -d '{"name":"研发组","visibility":"public"}')
echo "$CH"
CID=$(echo "$CH" | sed -n 's/.*"conversationId":"\([0-9]*\)".*/\1/p')
# send a message to the channel (reuses /im/messages)
curl -s -XPOST localhost:8088/im/messages -H "Manager-Token: $TOKEN" -H 'Content-Type: application/json' -d "{\"conversationId\":$CID,\"content\":\"hello channel\"}" -w " HTTP %{http_code}\n"
# it shows in my conversation list as a channel
curl -s localhost:8088/im/conversations -H "Manager-Token: $TOKEN" | grep -o '"type":"channel"[^}]*' | head -c 200; echo
# browse public channels as another user would (here admin is already a member, so expect it absent)
curl -s "localhost:8088/im/channels/public" -H "Manager-Token: $TOKEN" -w " HTTP %{http_code}\n" | head -c 120; echo
```
Expected: channel created, message sent (code 0), the conversation list contains a `"type":"channel"` entry. Clean up the test channel afterward (delete its rows) if desired.

- [ ] **Step 4: Browser hand-off:** verify in the UI — 新建 → 创建频道 (public, pick members) → send messages → a second logged-in member receives them in real time → 浏览频道 + 加入 as another user → 成员 panel + 添加成员 → 退出. Report issues.

---

## Self-Review Notes

- **Spec coverage:** create channel (Task 3 `createChannel`, Task 4 POST, Task 7 dialog) ✓; public/private visibility (Task 1 column, Task 3 logic, Task 7 toggle) ✓; channel messaging reuses Phase 1 (no new code — verified Task 8 smoke) ✓; conversation list shows channels (Task 3 channel-aware list + `type`, Task 7 rendering) ✓; browse+join public (Task 3/4/7) ✓; any-member add (Task 3 `addMembers` asserts actor membership, Task 4/7) ✓; leave (Task 3/4/7) ✓; member list + add (Task 4 `/members`, Task 7 dialog) ✓; realtime fan-out reuses Phase 1 push (no change) ✓.
- **Placeholder scan:** complete code throughout; the only flagged verifications are the `baseMapper` field name (mirror `ImConversationServiceImplTest`), the `channelMembers` loop simplification (use `ManagerUser` PO directly), and EP 2.8 `el-radio`/`el-dropdown` prop names — all resolved by the existing tests / `vue-tsc` build.
- **Type consistency:** `createChannel(creatorId,name,description,visibility,memberIds)`, `browsePublicChannels(userId,keyword)`, `joinChannel/leaveChannel(userId,channelId)`, `addMembers(actorId,channelId,userIds)`, `toChannelVO`, `addMemberRow`; VO fields `type/name/visibility/memberCount`; store actions `createChannelAction/browseChannels/joinChannelAction/leaveChannelAction/addMembersAction/fetchChannelMembers`; api `createChannel/browsePublicChannels/joinChannel/leaveChannel/addChannelMembers/listChannelMembers` — consistent across backend, api, store, and view.
- **Reuse:** messaging/recall/unread/presence/push are unchanged from Phase 1; only the conversation row gains channel branches. No new tables.
