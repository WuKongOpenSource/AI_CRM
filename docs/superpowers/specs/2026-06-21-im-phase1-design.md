# Instant Messaging — Phase 1 Design (Real-time foundation + 1:1 Direct Messages)

## Goal

Give every address-book user (`manager_user`) the ability to send and receive messages with any other user in the same tenant, Slack-style, in near real time over WebSocket. Phase 1 delivers the real-time foundation plus 1:1 direct messages with unread counts, image/file attachments, online presence, browser notifications, and message recall. Channels/groups, threads, reactions, @mentions, and search are explicitly out of scope (later phases).

## Scope

- **In:** 1:1 direct messages; WebSocket/STOMP real-time delivery; conversation list + chat pane UI as a new "消息" sidebar module; unread counts; image/file attachments; online presence; browser notifications; recall of one's own message; a removable `im` permission (granted to all roles by default).
- **Out (later phases):** group channels, threads, emoji reactions, @mentions and mention notifications, message search, message editing.
- IM is **not** an AI feature: it does not touch `AiQuotaService`/billing. All data is tenant-scoped (you can only DM users in your own tenant).

## Architecture

**Transport.** Add `spring-boot-starter-websocket`. Use STOMP over WebSocket at endpoint `/ws`, with an in-memory `SimpleBroker` (the deployment is a single `crm` instance; scaling out to multiple instances would require an external broker/relay and is deferred). The frontend uses `@stomp/stompjs` over a native WebSocket.

**Authentication.** Browser WebSocket handshakes cannot carry custom headers, so the frontend STOMP client puts `Manager-Token` in the STOMP **CONNECT frame headers**. A backend `ChannelInterceptor` on the inbound channel intercepts `CONNECT`, validates the token by reusing `TokenService` (JWT parse + Redis `LoginUser` lookup), and sets the authenticated `userId` as the STOMP session `Principal`. All later frames are bound to that principal; unauthenticated connects are rejected.

**Push model.** Server→client delivery uses Spring "user destinations": each connected user subscribes to `/user/queue/im`, and the server pushes with `SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/im", payload)` so only the relevant conversation members receive an event. Payloads are typed envelopes: `message` (new/recalled message), `unread` (updated unread count), `presence` (a contact's online state changed).

**Send model.** Sending is a **REST** call (`POST /im/messages`), not a STOMP send. REST persists the message (tenant-stamped, validated, attachment-aware), updates the conversation's `last_message_id`/`update_time`, then pushes the new message over WS to each member's user-queue. Rationale: REST gives reliable persistence/validation/attachment handling and keeps WS as a pure realtime push channel, avoiding complex inbound-STOMP handling. WS being down never blocks sending or loses messages (see Error Handling).

## Data Model

New Flyway migration (next free `V*` after the current max — verify at implementation). IDs are Snowflake `BIGINT`. **Correction (verified against the actual code):** this codebase is effectively single-tenant — there is no tenant interceptor, no `tenant_id` columns, and no `TenantContextHolder` (e.g. `crm_chat_session` is scoped only by `user_id`). So the IM tables do **not** carry `tenant_id`; scoping is enforced by conversation membership in the service layer.

- **`crm_im_conversation`** — `id`, `tenant_id`, `type` (`direct` in Phase 1), `member_key` (for `direct`: the two member user IDs sorted and joined as `<minId>_<maxId>`, **unique per tenant**, used to find-or-create the DM and prevent duplicate conversations), `last_message_id`, `create_time`, `update_time` (last activity, for sorting the conversation list).
- **`crm_im_conversation_member`** — `id`, `tenant_id`, `conversation_id`, `user_id`, `last_read_message_id` (0/null = nothing read), `create_time`. Unique `(conversation_id, user_id)`.
- **`crm_im_message`** — `id`, `tenant_id`, `conversation_id`, `sender_id`, `content_type` (`text`/`image`/`file`), `content` (text body; for image/file this may be empty), `attachment_name`, `attachment_path` (MinIO object key), `attachment_size`, `attachment_mime` (all nullable), `status` (`normal`/`recalled`), `create_time`. Index `(conversation_id, id)` for history paging.

## Behaviors

**Open/create a DM.** Opening a chat with user B resolves the `direct` conversation by `member_key` (find-or-create), creating the conversation + two member rows on first contact. Returns the conversation id.

**Unread.** A member's unread count for a conversation = number of messages with `id > last_read_message_id`, `sender_id != me`, `status = normal`. Marking read is `POST /im/conversations/{id}/read` (sets `last_read_message_id` to the latest), after which the server pushes the member's updated unread (now 0) over WS. The sidebar "消息" badge is the sum of unread across conversations.

**Message recall.** The sender may recall their own message within **2 minutes** of sending. Recall (`POST /im/messages/{id}/recall`) sets `status = recalled` and clears `content`/attachment fields; both parties render a tombstone ("该消息已撤回"). The server pushes a `message` envelope with the recalled state; unread recomputes (a recalled message no longer counts as unread). The 2-minute window is a constant for Phase 1.

**Presence.** On WS connect, the user is marked online in Redis (`im:online:<userId>`, short TTL refreshed by STOMP heartbeats); on disconnect the key expires/clears. Presence changes are pushed to other online users; the UI shows an online dot. Presence is approximate (a stale key may lag by one TTL), which is acceptable.

**Attachments.** Reuse the existing MinIO presigned upload (`FileController` presigned-upload flow): the client uploads the file, then sends a message of `content_type` `image`/`file` referencing the returned object key. The chat pane renders images inline and files as a download chip, using the browser-reachable `/s3` URL. Size/type limits reuse existing upload limits.

**Browser notifications.** Pure frontend: when a `message` envelope arrives while the tab is hidden or the message's conversation is not the active one, fire a `Notification` (after the user has granted permission). Clicking focuses the app and opens that conversation.

## REST + WS Surface

REST (all under tenant + `im` permission):
- `GET /im/conversations` — list my conversations (with peer info, last message, unread).
- `POST /im/conversations/direct` — find-or-create a DM with `{userId}`; returns conversation.
- `GET /im/conversations/{id}/messages?beforeId=&limit=` — paged history (newest-first, scroll-up).
- `POST /im/messages` — send `{conversationId, contentType, content, attachment...}`.
- `POST /im/messages/{id}/recall` — recall own message.
- `POST /im/conversations/{id}/read` — mark read.
- `GET /im/contacts` — address-book users I can DM (reuses `ManageUserService` within tenant), with presence.

WS:
- Endpoint `/ws` (STOMP); client subscribes `/user/queue/im`.
- Server envelopes: `{type: "message"|"unread"|"presence", ...}`.

## Frontend

- New top-level **"消息"** sidebar module + route `/im` (hash route), guarded by `meta.permission = "im"`; address-book person rows get a **"发消息"** action that opens/creates the DM and navigates to `/im`.
- New Pinia `im` store owns: one STOMP connection (connect after login; auto-reconnect with exponential backoff; refresh token header on reconnect), conversation list, per-conversation message lists (lazy-loaded + paged), unread map, presence map, and the active conversation.
- Two-pane UI: left = conversation list (avatar, peer name, last message preview, unread badge, online dot); right = message pane (history with scroll-up paging, composer with text + attachment button, hover "撤回" on own recent messages).
- REST is the source of truth; WS delivers live deltas. On initial load and on every reconnect, the store fetches conversations + recent messages via REST and reconciles with anything WS pushed, de-duplicating by message id.

## Permission

Add an `im` permission/menu to the RBAC tables (`manager_menu` + grant rows in `manager_role_menu`) via the Flyway migration, **granted to all existing roles by default** so IM is available to everyone out of the box, while remaining removable per role in 角色管理. The frontend route and the IM controllers enforce `im` (`@RequirePermission("im")` on the controllers; `meta.permission` on the route). Both new mappers (`crm_im_*`) are business tables — confirm whether row-level data-permission should apply; for Phase 1 IM is self-scoped (you only see conversations you are a member of), enforced by membership checks in the service, not by `GlobalDataPermissionHandler`.

## Error Handling

- **WS reconnect:** exponential backoff; on reconnect, re-fetch messages newer than the last known id per open conversation via REST and de-duplicate by id, so a dropped socket never loses messages.
- **Send while disconnected:** REST send still persists; the recipient receives it on their next WS push or REST catch-up. The sender's own message is rendered optimistically and confirmed by the REST response.
- **Auth failure on CONNECT:** the interceptor rejects; the client surfaces a re-login prompt (consistent with `Manager-Token` expiry handling elsewhere).
- **Membership enforcement:** every conversation/message REST endpoint verifies the caller is a member of the conversation (and same tenant) before reading/writing.

## Infrastructure

- **nginx** (frontend container, `docker/nginx/default.conf.template`): add a `/ws` location proxying to `crm:8088` with WebSocket upgrade headers (`proxy_set_header Upgrade $http_upgrade; proxy_set_header Connection "upgrade"; proxy_http_version 1.1;`) and a long read timeout. Without this the WS handshake fails behind the reverse proxy.
- New backend dependency `spring-boot-starter-websocket`; new frontend dependency `@stomp/stompjs`.

## Testing

- **Backend unit tests:** DM `find-or-create` de-duplication by `member_key`; unread computation (excludes own + recalled messages); message persistence stamps `tenant_id`/`sender_id`; recall within/after the 2-minute window; CONNECT token validation (valid token → principal set, invalid/missing → rejected) via the interceptor; push-to-members fan-out (mock `SimpMessagingTemplate`, assert each member's user-queue receives the envelope); membership enforcement rejects non-members.
- **Frontend:** store reducer logic (dedup by id, unread updates, presence updates) as pure-function unit tests where feasible; the rest verified by running the app.
- Backend tests follow the project convention (no `application-test.yml`; pure JUnit5 + Mockito, no Spring context where avoidable).

## Out of Scope (explicitly deferred to later phases)

Group channels (public/private) and channel fan-out; threaded replies; emoji reactions; @mentions and mention notifications; full-text message search; message editing; multi-instance WebSocket scaling (external broker/relay).
