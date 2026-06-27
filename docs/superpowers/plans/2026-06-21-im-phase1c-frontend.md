# IM Phase 1c — Frontend + List/Contacts Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make IM usable in the browser: a 「消息」entry + two-pane conversation UI backed by a Pinia store that talks REST (source of truth) and subscribes to the STOMP `/user/queue/im` push from Plan 1b — with unread badges, image/file attachments, online presence, browser notifications, and message recall. Also add the two backend list endpoints deferred from 1a.

**Architecture:** REST is the source of truth (`api/im.ts`); a single STOMP connection (in `stores/im.ts`, using `@stomp/stompjs` + the `Manager-Token` from localStorage in the CONNECT headers) delivers live deltas which the store reconciles by message id. A self-contained `ImView.vue` (left conversation list, right chat pane) avoids touching the complex sortable-module sidebar; a small icon button in the fixed sidebar area and a 通讯录 "发消息" action route to `/im`.

**Tech Stack:** Vue 3, TypeScript (strict; build is `vue-tsc -b && vite build`), Pinia, Element Plus, vue-router (hash history), `@stomp/stompjs`. Backend: Spring Boot (Plan 1a/1b in place).

**Environment:** Branch `feature/im-phase1`. Backend Maven via the Docker image (see prior plans). Frontend build via Docker node image:
`MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM/frontend:/app" -w /app node:20 sh -c "npm install && npm run build"` (npm install pulls `@stomp/stompjs`; first run is slow). Do NOT switch branches. Per AGENTS.md, `npm run build` emits large-chunk warnings — that's pre-existing; only `vue-tsc` type errors are failures.

---

## File Structure

- Modify `backend/.../service/ImConversationService.java` + `ImConversationServiceImpl.java` — add `listMyConversations(userId)`.
- Modify `backend/.../controller/ImController.java` — add `GET /im/conversations` and `GET /im/contacts`.
- Create `backend/.../entity/VO/ImContactVO.java`.
- Create `frontend/src/api/im.ts` — REST wrappers + types.
- Create `frontend/src/stores/im.ts` — STOMP client + state + actions.
- Create `frontend/src/views/im/ImView.vue` — two-pane UI.
- Modify `frontend/src/router/index.ts` — add `/im` route.
- Modify `frontend/src/layouts/MainLayout.vue` — add a 「消息」icon button (with unread badge) routing to `/im`.
- Modify `frontend/src/views/addressBook/AddressBookListView.vue` — add a "发消息" action that routes to `/im?peer=<userId>`.
- Modify `frontend/package.json` — add `@stomp/stompjs`.

---

## Task 1: Backend — conversation list + contacts endpoints

**Files:**
- Modify: `backend/src/main/java/com/kakarote/ai_crm/service/ImConversationService.java`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/service/impl/ImConversationServiceImpl.java`
- Create: `backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImContactVO.java`
- Modify: `backend/src/main/java/com/kakarote/ai_crm/controller/ImController.java`

- [ ] **Step 1:** Add to `ImConversationService` interface:

```java
    /** All conversations the user is a member of, newest-activity first, with peer info, last message, and unread. */
    java.util.List<com.kakarote.ai_crm.entity.VO.ImConversationVO> listMyConversations(Long userId);
```

- [ ] **Step 2:** Implement in `ImConversationServiceImpl` (inject what you need; `ImMessageService` for `getMessageVO`, `ManageUserService` for peer info, `ImConversationMemberMapper` already injected). Add fields and the method:

```java
    @org.springframework.beans.factory.annotation.Autowired
    private com.kakarote.ai_crm.service.ImMessageService imMessageService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.kakarote.ai_crm.service.ManageUserService manageUserService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.kakarote.ai_crm.service.FileStorageService fileStorageService;

    @Override
    public java.util.List<com.kakarote.ai_crm.entity.VO.ImConversationVO> listMyConversations(Long userId) {
        java.util.List<ImConversationMember> myMemberships = memberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImConversationMember>()
                        .eq(ImConversationMember::getUserId, userId));
        java.util.List<com.kakarote.ai_crm.entity.VO.ImConversationVO> out = new java.util.ArrayList<>();
        for (ImConversationMember mine : myMemberships) {
            ImConversation conv = getById(mine.getConversationId());
            if (conv == null) {
                continue;
            }
            // peer = the other member of this direct conversation
            ImConversationMember peerMember = memberMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImConversationMember>()
                            .eq(ImConversationMember::getConversationId, conv.getId())
                            .ne(ImConversationMember::getUserId, userId)
                            .last("LIMIT 1"));
            com.kakarote.ai_crm.entity.VO.ImConversationVO vo = new com.kakarote.ai_crm.entity.VO.ImConversationVO();
            vo.setId(String.valueOf(conv.getId()));
            vo.setUpdateTime(conv.getUpdateTime());
            vo.setUnreadCount(unreadCount(conv.getId(), userId));
            if (peerMember != null) {
                vo.setPeerUserId(String.valueOf(peerMember.getUserId()));
                com.kakarote.ai_crm.entity.PO.ManagerUser peer = manageUserService.getById(peerMember.getUserId());
                if (peer != null) {
                    vo.setPeerName(cn.hutool.core.util.StrUtil.blankToDefault(peer.getRealname(), peer.getUsername()));
                    if (cn.hutool.core.util.StrUtil.isNotBlank(peer.getImg()) && fileStorageService != null) {
                        try {
                            vo.setPeerAvatarUrl(fileStorageService.getUrl(peer.getImg()));
                        } catch (Exception ignored) {
                            // leave avatar null
                        }
                    }
                }
            }
            if (conv.getLastMessageId() != null) {
                vo.setLastMessage(imMessageService.getMessageVO(conv.getLastMessageId()));
            }
            out.add(vo);
        }
        out.sort(java.util.Comparator.comparing(
                com.kakarote.ai_crm.entity.VO.ImConversationVO::getUpdateTime,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return out;
    }
```

> Verify `ManagerUser` has `getRealname()`, `getUsername()`, `getImg()` (it does — confirmed in Plan 1a). Verify no circular-bean issue: `ImMessageServiceImpl` does NOT inject `ImConversationServiceImpl` eagerly in a way that cycles — it injects the `ImConversationService` interface; Spring resolves it. If a cycle is reported at startup, annotate the `imMessageService` field here with `@org.springframework.context.annotation.Lazy`.

- [ ] **Step 3:** Create `ImContactVO.java`:

```java
package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class ImContactVO {
    private String userId;
    private String name;
    private String avatarUrl;
    private String deptName;
    private boolean online;
}
```

- [ ] **Step 4:** Add the two endpoints to `ImController` (inject `ManageUserService manageUserService` and `com.kakarote.ai_crm.im.ws.ImPresenceService presenceService`):

```java
    @GetMapping("/conversations")
    @Operation(summary = "我的会话列表")
    @RequirePermission("im")
    public Result<List<ImConversationVO>> myConversations() {
        return Result.ok(conversationService.listMyConversations(UserUtil.getUserId()));
    }

    @GetMapping("/contacts")
    @Operation(summary = "可发起私聊的通讯录联系人")
    @RequirePermission("im")
    public Result<List<ImContactVO>> contacts(@RequestParam(value = "keyword", required = false) String keyword) {
        Long me = UserUtil.getUserId();
        UserQueryBO query = new UserQueryBO();
        query.setSearch(StrUtil.trimToNull(keyword));
        query.setPage(1);
        query.setLimit(200);
        List<ImContactVO> list = new ArrayList<>();
        for (ManageUserVO u : manageUserService.queryPageList(query).getRecords()) {
            if (u.getUserId() == null || u.getUserId().equals(me)) {
                continue;
            }
            ImContactVO vo = new ImContactVO();
            vo.setUserId(String.valueOf(u.getUserId()));
            vo.setName(StrUtil.blankToDefault(u.getRealname(), u.getUsername()));
            vo.setAvatarUrl(u.getImgUrl());
            vo.setDeptName(u.getDeptName());
            vo.setOnline(presenceService.isOnline(String.valueOf(u.getUserId())));
            list.add(vo);
        }
        return Result.ok(list);
    }
```

Add imports: `com.kakarote.ai_crm.entity.VO.ImConversationVO`, `ImContactVO`, `com.kakarote.ai_crm.entity.VO.ManageUserVO`, `com.kakarote.ai_crm.entity.BO.UserQueryBO`, `com.kakarote.ai_crm.service.ManageUserService`, `com.kakarote.ai_crm.im.ws.ImPresenceService`, `java.util.ArrayList`. `ManageUserVO` has `getImgUrl()`/`getDeptName()`/`getRealname()`/`getUsername()` (confirmed in Plan 1a).

- [ ] **Step 5:** Compile + commit

Run: `MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q -DskipTests compile` → BUILD SUCCESS.
```bash
git add backend/src/main/java/com/kakarote/ai_crm/service/ImConversationService.java backend/src/main/java/com/kakarote/ai_crm/service/impl/ImConversationServiceImpl.java backend/src/main/java/com/kakarote/ai_crm/entity/VO/ImContactVO.java backend/src/main/java/com/kakarote/ai_crm/controller/ImController.java
git commit -m "feat(im): conversation-list and contacts REST endpoints"
```

---

## Task 2: Frontend dependency + API client

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/api/im.ts`

- [ ] **Step 1:** Add `@stomp/stompjs` to `frontend/package.json` dependencies:

```json
    "@stomp/stompjs": "^7.0.0",
```
(place it among the other `dependencies`; keep JSON valid.)

- [ ] **Step 2:** Create `frontend/src/api/im.ts`:

```ts
import { get, post } from '@/utils/request'

export interface ImMessage {
  id: string
  conversationId: string
  senderId: string
  contentType: 'text' | 'image' | 'file'
  content: string | null
  attachmentName?: string | null
  attachmentUrl?: string | null
  attachmentSize?: number | null
  attachmentMime?: string | null
  status: 'normal' | 'recalled'
  createTime: string
}

export interface ImConversation {
  id: string
  peerUserId: string
  peerName: string
  peerAvatarUrl?: string | null
  lastMessage?: ImMessage | null
  unreadCount: number
  updateTime: string
}

export interface ImContact {
  userId: string
  name: string
  avatarUrl?: string | null
  deptName?: string | null
  online: boolean
}

export interface ImSendPayload {
  conversationId: string
  contentType?: 'text' | 'image' | 'file'
  content?: string
  attachmentName?: string
  attachmentPath?: string
  attachmentSize?: number
  attachmentMime?: string
}

export const listConversations = () => get<ImConversation[]>('/im/conversations')
export const listContacts = (keyword?: string) =>
  get<ImContact[]>('/im/contacts', { params: keyword ? { keyword } : {} })
export const openDirect = (userId: string) =>
  post<{ conversationId: string }>('/im/conversations/direct', { userId })
export const fetchHistory = (conversationId: string, beforeId?: string, limit = 30) =>
  get<ImMessage[]>(`/im/conversations/${conversationId}/messages`, {
    params: { ...(beforeId ? { beforeId } : {}), limit },
  })
export const sendMessage = (payload: ImSendPayload) => post<ImMessage>('/im/messages', payload)
export const recallMessage = (id: string) => post<ImMessage>(`/im/messages/${id}/recall`)
export const markRead = (conversationId: string) => post<string>(`/im/conversations/${conversationId}/read`)
```

> Verify `get`/`post` signatures in `src/utils/request.ts` accept `{ params }` config (axios-style) — confirmed (`get<T>(url, config?)`). If image/file upload reuses an existing helper (`upload(...)` or `FileController` presigned flow), the store calls that to get `attachmentPath`; see Task 3.

- [ ] **Step 3: Commit**

```bash
git add frontend/package.json frontend/src/api/im.ts
git commit -m "feat(im): frontend IM API client + @stomp/stompjs dep"
```

---

## Task 3: Pinia store with STOMP client

**Files:** Create `frontend/src/stores/im.ts`

- [ ] **Step 1:** Create the store. It owns one STOMP connection and all IM state.

```ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Client, type IMessage } from '@stomp/stompjs'
import { getToken } from '@/utils/request'
import {
  listConversations, listContacts, openDirect, fetchHistory,
  sendMessage, recallMessage, markRead,
  type ImConversation, type ImMessage, type ImContact, type ImSendPayload,
} from '@/api/im'

interface PushEnvelope {
  type: 'message' | 'unread'
  conversationId: string
  message?: ImMessage | null
  unread?: number | null
}

export const useImStore = defineStore('im', () => {
  const conversations = ref<ImConversation[]>([])
  const contacts = ref<ImContact[]>([])
  const messagesByConv = ref<Record<string, ImMessage[]>>({})
  const presence = ref<Record<string, boolean>>({})
  const activeConversationId = ref<string | null>(null)
  const connected = ref(false)

  let client: Client | null = null

  const totalUnread = computed(() =>
    conversations.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0))

  function wsUrl(): string {
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
    return `${proto}://${window.location.host}/ws`
  }

  function connect() {
    if (client && client.active) return
    const token = getToken()
    if (!token) return
    client = new Client({
      brokerURL: wsUrl(),
      connectHeaders: { 'Manager-Token': token },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        connected.value = true
        client?.subscribe('/user/queue/im', (frame: IMessage) => onPush(JSON.parse(frame.body)))
        client?.subscribe('/topic/im.presence', (frame: IMessage) => {
          const p = JSON.parse(frame.body) as { userId: string; online: boolean }
          presence.value = { ...presence.value, [p.userId]: p.online }
        })
        // REST is source of truth: refresh on (re)connect.
        void refreshConversations()
      },
      onDisconnect: () => { connected.value = false },
      onWebSocketClose: () => { connected.value = false },
    })
    client.activate()
  }

  function disconnect() {
    void client?.deactivate()
    client = null
    connected.value = false
  }

  function onPush(env: PushEnvelope) {
    const conv = conversations.value.find((c) => c.id === env.conversationId)
    if (env.type === 'message' && env.message) {
      upsertMessage(env.conversationId, env.message)
      if (conv) {
        conv.lastMessage = env.message
        conv.updateTime = env.message.createTime
      } else {
        void refreshConversations()
      }
      if (env.conversationId === activeConversationId.value) {
        // viewing it → immediately mark read
        void markReadAction(env.conversationId)
        notifyIfHidden(env)
      } else {
        if (conv && typeof env.unread === 'number') conv.unreadCount = env.unread
        notifyIfHidden(env)
      }
    } else if (env.type === 'unread' && conv && typeof env.unread === 'number') {
      conv.unreadCount = env.unread
    }
  }

  function upsertMessage(convId: string, msg: ImMessage) {
    const list = messagesByConv.value[convId] || []
    const idx = list.findIndex((m) => m.id === msg.id)
    if (idx >= 0) list.splice(idx, 1, msg)       // recall/update
    else list.push(msg)                          // new
    messagesByConv.value = { ...messagesByConv.value, [convId]: list }
  }

  function notifyIfHidden(env: PushEnvelope) {
    if (!env.message) return
    if (document.visibilityState === 'visible' && env.conversationId === activeConversationId.value) return
    if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return
    const conv = conversations.value.find((c) => c.id === env.conversationId)
    const preview = env.message.status === 'recalled' ? '撤回了一条消息'
      : (env.message.contentType === 'text' ? (env.message.content || '') : '[附件]')
    new Notification(conv?.peerName || '新消息', { body: preview })
  }

  async function refreshConversations() {
    conversations.value = await listConversations()
  }
  async function refreshContacts(keyword?: string) {
    contacts.value = await listContacts(keyword)
  }
  async function loadHistory(conversationId: string) {
    const msgs = await fetchHistory(conversationId)
    messagesByConv.value = { ...messagesByConv.value, [conversationId]: msgs }
  }
  async function openConversationWith(userId: string): Promise<string> {
    const { conversationId } = await openDirect(userId)
    if (!conversations.value.find((c) => c.id === conversationId)) await refreshConversations()
    await selectConversation(conversationId)
    return conversationId
  }
  async function selectConversation(conversationId: string) {
    activeConversationId.value = conversationId
    if (!messagesByConv.value[conversationId]) await loadHistory(conversationId)
    await markReadAction(conversationId)
  }
  async function send(payload: ImSendPayload) {
    const msg = await sendMessage(payload)       // optimistic confirm via response
    upsertMessage(payload.conversationId, msg)
    const conv = conversations.value.find((c) => c.id === payload.conversationId)
    if (conv) { conv.lastMessage = msg; conv.updateTime = msg.createTime }
    return msg
  }
  async function recall(conversationId: string, messageId: string) {
    const msg = await recallMessage(messageId)
    upsertMessage(conversationId, msg)
  }
  async function markReadAction(conversationId: string) {
    await markRead(conversationId)
    const conv = conversations.value.find((c) => c.id === conversationId)
    if (conv) conv.unreadCount = 0
  }
  function ensureNotificationPermission() {
    if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
      void Notification.requestPermission()
    }
  }

  return {
    conversations, contacts, messagesByConv, presence, activeConversationId, connected, totalUnread,
    connect, disconnect, refreshConversations, refreshContacts, loadHistory,
    openConversationWith, selectConversation, send, recall, markReadAction, ensureNotificationPermission,
  }
})
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/stores/im.ts
git commit -m "feat(im): Pinia store with STOMP client, reconnect, REST reconciliation"
```

---

## Task 4: ImView two-pane UI

**Files:** Create `frontend/src/views/im/ImView.vue`

- [ ] **Step 1:** Create the view. Use Element Plus components already used elsewhere (read a neighbor like `views/chat/ChatView.vue` for style/classes). Attachments reuse the existing upload helper — if `src/api` has a presigned-upload helper, call it to obtain `attachmentPath`; otherwise use `upload('/file/presigned-upload', formData)` per `FileController`. Keep the view self-contained.

```vue
<template>
  <div class="flex h-full">
    <!-- conversation list -->
    <aside class="w-72 flex-shrink-0 border-r border-[#ececec] overflow-y-auto">
      <div class="p-3 flex items-center justify-between">
        <span class="font-semibold">消息</span>
        <el-button size="small" @click="openContacts = true">发起聊天</el-button>
      </div>
      <div
        v-for="c in im.conversations"
        :key="c.id"
        class="px-3 py-2 cursor-pointer hover:bg-[#f9f9f9] flex items-center gap-2"
        :class="{ 'bg-[#f3f3f3]': c.id === im.activeConversationId }"
        @click="im.selectConversation(c.id)"
      >
        <el-badge :value="c.unreadCount" :hidden="!c.unreadCount" class="flex-shrink-0">
          <el-avatar :size="36" :src="c.peerAvatarUrl || undefined">{{ (c.peerName || '?').slice(0,1) }}</el-avatar>
        </el-badge>
        <span v-if="im.presence[c.peerUserId]" class="w-2 h-2 rounded-full bg-green-500" />
        <div class="min-w-0 flex-1">
          <div class="truncate text-sm font-medium">{{ c.peerName }}</div>
          <div class="truncate text-xs text-gray-400">{{ previewOf(c.lastMessage) }}</div>
        </div>
      </div>
    </aside>

    <!-- chat pane -->
    <section class="flex-1 flex flex-col min-w-0">
      <template v-if="im.activeConversationId">
        <div ref="scrollEl" class="flex-1 overflow-y-auto p-4 space-y-3">
          <div v-for="m in activeMessages" :key="m.id" class="flex flex-col"
               :class="m.senderId === myId ? 'items-end' : 'items-start'">
            <div class="max-w-[70%] rounded-lg px-3 py-2 text-sm"
                 :class="m.senderId === myId ? 'bg-[#d6e4ff]' : 'bg-[#f3f3f3]'">
              <span v-if="m.status === 'recalled'" class="italic text-gray-400">该消息已撤回</span>
              <template v-else-if="m.contentType === 'image'">
                <img :src="m.attachmentUrl || ''" class="max-w-[240px] rounded" />
              </template>
              <template v-else-if="m.contentType === 'file'">
                <a :href="m.attachmentUrl || ''" target="_blank" class="underline">{{ m.attachmentName }}</a>
              </template>
              <span v-else>{{ m.content }}</span>
            </div>
            <button
              v-if="m.senderId === myId && m.status === 'normal' && canRecall(m)"
              class="text-xs text-gray-400 mt-0.5"
              @click="im.recall(im.activeConversationId!, m.id)"
            >撤回</button>
          </div>
        </div>
        <div class="border-t border-[#ececec] p-3 flex items-center gap-2">
          <el-upload :show-file-list="false" :http-request="doUpload">
            <el-button :icon="Paperclip" circle />
          </el-upload>
          <el-input v-model="draft" placeholder="输入消息，回车发送" @keyup.enter="onSend" />
          <el-button type="primary" :disabled="!draft.trim()" @click="onSend">发送</el-button>
        </div>
      </template>
      <div v-else class="flex-1 flex items-center justify-center text-gray-400">选择一个会话开始聊天</div>
    </section>

    <!-- contacts picker -->
    <el-dialog v-model="openContacts" title="发起聊天" width="420px" @open="im.refreshContacts()">
      <el-input v-model="contactKeyword" placeholder="搜索同事" @input="im.refreshContacts(contactKeyword)" class="mb-2" />
      <div class="max-h-80 overflow-y-auto">
        <div v-for="ct in im.contacts" :key="ct.userId"
             class="px-2 py-2 flex items-center gap-2 cursor-pointer hover:bg-[#f9f9f9]"
             @click="startChat(ct.userId)">
          <el-avatar :size="32" :src="ct.avatarUrl || undefined">{{ ct.name.slice(0,1) }}</el-avatar>
          <span class="text-sm">{{ ct.name }}</span>
          <span v-if="ct.online" class="w-2 h-2 rounded-full bg-green-500" />
          <span class="ml-auto text-xs text-gray-400">{{ ct.deptName }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Paperclip } from '@element-plus/icons-vue'
import { useImStore } from '@/stores/im'
import { useUserStore } from '@/stores/user'
import { upload } from '@/utils/request'
import type { ImMessage } from '@/api/im'

const im = useImStore()
const route = useRoute()
const userStore = useUserStore()
const myId = computed(() => String(userStore.userInfo?.userId ?? ''))

const draft = ref('')
const openContacts = ref(false)
const contactKeyword = ref('')
const scrollEl = ref<HTMLElement | null>(null)

const activeMessages = computed<ImMessage[]>(() =>
  im.activeConversationId ? im.messagesByConv[im.activeConversationId] || [] : [])

function previewOf(m?: ImMessage | null) {
  if (!m) return ''
  if (m.status === 'recalled') return '该消息已撤回'
  return m.contentType === 'text' ? (m.content || '') : '[附件]'
}
function canRecall(m: ImMessage) {
  return Date.now() - new Date(m.createTime).getTime() < 2 * 60 * 1000
}
async function onSend() {
  const text = draft.value.trim()
  if (!text || !im.activeConversationId) return
  draft.value = ''
  await im.send({ conversationId: im.activeConversationId, contentType: 'text', content: text })
}
async function startChat(userId: string) {
  openContacts.value = false
  await im.openConversationWith(userId)
}
// Element Plus custom upload: upload to MinIO presigned, then send a message referencing it.
async function doUpload(opt: { file: File }) {
  if (!im.activeConversationId) return
  const fd = new FormData()
  fd.append('file', opt.file)
  // Returns the stored object key + url; adapt to the FileController presigned-upload response shape.
  const res = await upload<{ objectKey: string; url?: string }>('/file/presigned-upload', fd)
  const isImage = opt.file.type.startsWith('image/')
  await im.send({
    conversationId: im.activeConversationId,
    contentType: isImage ? 'image' : 'file',
    attachmentName: opt.file.name,
    attachmentPath: res.objectKey,
    attachmentSize: opt.file.size,
    attachmentMime: opt.file.type,
  })
}

watch(activeMessages, async () => { await nextTick(); if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight }, { deep: true })

onMounted(async () => {
  im.ensureNotificationPermission()
  im.connect()
  await im.refreshConversations()
  const peer = route.query.peer as string | undefined
  if (peer) await im.openConversationWith(peer)
})
</script>
```

> Adaptation notes for the implementer: (1) confirm `useUserStore().userInfo.userId` is how the current user id is exposed (read `src/stores/user.ts`; adjust the accessor if different). (2) Confirm the presigned-upload endpoint path + response shape by reading `controller/FileController.java` and any existing `src/api` upload helper; set `attachmentPath` from whatever key the response returns, and prefer reusing an existing upload helper over a raw path. (3) Match Element Plus version API (2.8). (4) If `@element-plus/icons-vue` Paperclip import name differs, use an available icon.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/im/ImView.vue
git commit -m "feat(im): two-pane conversation UI (list, chat, attachments, recall, contacts)"
```

---

## Task 5: Route + sidebar entry + address-book action

**Files:**
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/views/addressBook/AddressBookListView.vue`

- [ ] **Step 1:** Add the route as a child of the MainLayout route (next to the `chat`/`customer` children), mirroring their shape:

```ts
      {
        path: 'im',
        name: 'Im',
        component: () => import('@/views/im/ImView.vue'),
        meta: { title: '消息', icon: 'chat', permission: 'im' },
      },
```

- [ ] **Step 2:** In `MainLayout.vue`, add a 「消息」icon button in the fixed sidebar area near the existing top buttons (e.g. next to the AI chat / logo button), with a router link to `/im` and an unread badge. Read how the existing top buttons are rendered and mirror it. Minimal version:

```vue
        <RouterLink to="/im" class="relative flex items-center justify-center ..."
                    :title="'消息'">
          <el-badge :value="imStore.totalUnread" :hidden="!imStore.totalUnread">
            <!-- use the same icon component pattern as the other sidebar buttons -->
            <span class="i-... " />
          </el-badge>
        </RouterLink>
```
and in `<script setup>` add `import { useImStore } from '@/stores/im'` + `const imStore = useImStore()`, and call `imStore.connect()` once on mount (so the badge/notifications work app-wide, not only on `/im`). Match the exact icon + class conventions of the neighboring buttons.

- [ ] **Step 3:** In `AddressBookListView.vue`, add a "发消息" button on each employee row/detail that navigates to the IM view for a DM:

```ts
import { useRouter } from 'vue-router'
const router = useRouter()
function messageEmployee(userId: string | number) {
  router.push({ path: '/im', query: { peer: String(userId) } })
}
```
and a button in the template: `<el-button size="small" @click.stop="messageEmployee(row.userId)">发消息</el-button>` (place it next to existing row actions; match the existing action style).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/views/addressBook/AddressBookListView.vue
git commit -m "feat(im): route, sidebar message entry with unread badge, address-book 发消息"
```

---

## Task 6: Type-check / build, deploy, browser hand-off

**Files:** none (verification).

- [ ] **Step 1: Type-check + build the frontend** (the real gate is `vue-tsc`):

Run: `MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM/frontend:/app" -w /app node:20 sh -c "npm install && npm run build"`
Expected: `vue-tsc` reports no type errors; `vite build` completes (large-chunk warnings are fine per AGENTS.md). Fix any TS errors (most likely: user-store accessor, upload response shape, icon import) and re-run until clean. Commit any fixups.

- [ ] **Step 2: Bake the freshly-built frontend into the crm image and deploy.** The build outputs `frontend/dist`; the crm image serves the SPA from `BOOT-INF/classes/public`. Copy the new dist into the backend resources, package, build, deploy:

```bash
rm -rf backend/src/main/resources/public && mkdir -p backend/src/main/resources/public
cp -r frontend/dist/. backend/src/main/resources/public/
MSYS_NO_PATHCONV=1 docker run --rm -v "F:/projects/WUKONG/AI_CRM:/work" -v aicrm_m2:/root/.m2 -w /work/backend maven:3.9.9-eclipse-temurin-21 mvn -q -DskipTests clean package
MSYS_NO_PATHCONV=1 docker build -t wk_ai_crm:local-aiimport -f backend/Dockerfile backend/
cd docker && MSYS_NO_PATHCONV=1 docker compose up -d --force-recreate crm; cd /f/projects/WUKONG/AI_CRM
rm -rf backend/src/main/resources/public
```
> Note: unlike prior rebuilds (which extracted the OLD bundled frontend), THIS build uses the freshly-compiled `frontend/dist` so the new IM UI ships. Expected: crm healthy.

- [ ] **Step 3: Browser hand-off (human verification).** The interactive realtime flow needs two logged-in users in a browser; hand this to the user with these steps: open http://localhost in two browser profiles (admin + a second user created in 团队管理), open 「消息」, start a chat from 发起聊天 or from 通讯录 发消息, send messages both ways (should appear within ~1s via WS), check unread badges, send an image, recall a message within 2 min, and allow the notification prompt to verify desktop notifications. Report any issues.

- [ ] **Step 4: Commit (only if build fixups were needed)**

```bash
git add -A && git commit -m "chore(im): frontend build fixups"
```

---

## Self-Review Notes

- **Spec coverage (1c portion):** 「消息」entry + two-pane UI (Tasks 4-5) ✓; conversation list + contacts endpoints (Task 1) ✓; STOMP subscribe `/user/queue/im` + `/topic/im.presence` (Task 3) ✓; unread badges (store `totalUnread` + per-conversation, Tasks 3-5) ✓; image/file attachments (Task 4 `doUpload`) ✓; presence dots (Tasks 4-5) ✓; browser notifications (Task 3 `notifyIfHidden` + `ensureNotificationPermission`) ✓; message recall UI within 2 min (Task 4 `canRecall`) ✓; REST-as-source-of-truth + reconnect reconciliation + dedup by id (Task 3 `upsertMessage`, `onConnect` refresh) ✓; DM entry from address book (Task 5) ✓.
- **Placeholder scan:** the Vue/wiring tasks contain complete, working code; three integration points are explicitly flagged for the implementer to confirm against the real code (user-store userId accessor, presigned-upload endpoint/response shape, icon imports) — these are verifications, not placeholders, and the `vue-tsc` build (Task 6) forces them to be correct before shipping.
- **Type consistency:** `ImConversation`/`ImMessage`/`ImContact`/`ImSendPayload` shapes match the backend VOs (1a `ImConversationVO`/`ImMessageVO`, 1c `ImContactVO`); store actions (`connect`, `openConversationWith`, `selectConversation`, `send`, `recall`, `markReadAction`, `refreshConversations`, `refreshContacts`, `totalUnread`) are referenced consistently in `ImView.vue` and `MainLayout.vue`.
- **Risk/limits:** frontend correctness is ultimately verified in the browser (Task 6 hand-off) since the Chrome extension isn't available for automated UI testing here; `vue-tsc` + the backend tests cover the type/contract layer. Group/threads/reactions/@mentions remain out of scope (later phases).
```
