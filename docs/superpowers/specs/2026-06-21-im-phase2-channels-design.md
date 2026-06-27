# IM Phase 2 Design — Group Channels

## Goal

Add multi-member channels to the instant-messaging feature built in Phase 1. Users can create public or private channels, send/receive channel messages in real time, see channels in their conversation list, browse and join public channels, add other members, and leave. Channels reuse Phase 1's conversation/member/message tables and the WebSocket fan-out, which is already N-member ready.

## Scope

- **In:** create a channel (name, optional description, public/private visibility, optional initial members); channel messaging (reuses the Phase 1 send/recall/read/unread/realtime flow); the conversation list shows channels (`#` avatar, name, unread); browse + join **public** channels; **any member can add other members** to a channel (public or private); leave a channel; a channel chat header showing member count + a member list with an "add member" action.
- **Out (later phases):** rename / archive / delete a channel; removing other members; channel admin roles; per-channel mute/notification settings; threads, emoji reactions, @mentions (Phase 3).
- Single-tenant, gated by the existing `im` permission. Not an AI feature.

## Data Model

New Flyway migration **V42** (next free version after V41 — verify at implementation). It **alters the existing `crm_im_conversation`** (does not create new tables); `crm_im_conversation_member` and `crm_im_message` are reused unchanged.

`ALTER TABLE crm_im_conversation ADD`:
- `name` VARCHAR(100) — channel display name (null for `direct`).
- `description` VARCHAR(500) — optional channel description.
- `visibility` VARCHAR(16) — `public` / `private` for channels; null for `direct`.
- `owner_id` BIGINT — the creator's user id (null for `direct`).

`type` now takes `direct` | `channel`. `member_key` remains used only for `direct` (find-or-create dedup); it stays null for channels (the partial unique index on `member_key WHERE member_key IS NOT NULL` from V40 already permits many null-keyed channel rows). `crm_im_conversation_member` already supports N members with per-member `last_read_message_id`; `crm_im_message` already keys messages by `conversation_id`.

## Behaviors

- **Create channel.** `createChannel(creatorId, name, description, visibility, memberIds)` inserts a conversation with `type=channel`, `owner_id=creatorId`, the given name/description/visibility, then inserts member rows for the creator plus each distinct `memberId`. Returns the channel as an `ImConversationVO`.
- **Browse public channels.** `browsePublicChannels(userId, keyword)` returns `public` channels the user is **not** already a member of, optionally filtered by name. Used by the "browse channels" UI.
- **Join.** `joinChannel(userId, channelId)` adds a member row, allowed only when the channel is `public` (private channels are join-by-invite only). Idempotent if already a member.
- **Add members.** `addMembers(actorId, channelId, userIds)` requires the actor to be a current member (per the chosen "any member can add" rule), then adds member rows for each not-yet-member user (works for public and private channels).
- **Leave.** `leaveChannel(userId, channelId)` deletes the caller's own member row. (Removing other members is out of scope for v1.)
- **List members.** `listMembers(channelId)` returns the channel's members (id, name, avatar) for the header/member panel; caller must be a member.
- **Messaging.** Sending, history, recall, mark-read, unread, and realtime push are **unchanged from Phase 1** — they are conversation-membership based, so they already work for channels. `send` pushes the new message to every `memberUserIds` of the conversation, so channel messages fan out to all members.
- **Conversation list.** `listMyConversations` returns both `direct` and `channel` conversations the user belongs to. For channels the VO carries `name`, `type=channel`, `memberCount`, and `visibility`; the peer fields are null.

## REST Surface (additions; all `@RequirePermission("im")`, membership-checked)

- `POST /im/channels` — create `{name, description?, visibility, memberIds?}` → channel VO.
- `GET /im/channels/public?keyword=` — public channels I'm not in.
- `POST /im/channels/{id}/join` — join a public channel.
- `POST /im/channels/{id}/leave` — leave a channel.
- `POST /im/channels/{id}/members` — add `{userIds}` (actor must be a member).
- `GET /im/channels/{id}/members` — list members (caller must be a member).

Existing endpoints are reused for channel messages: `GET /im/conversations`, `GET /im/conversations/{id}/messages`, `POST /im/messages`, `POST /im/messages/{id}/recall`, `POST /im/conversations/{id}/read`. WS push (`/user/queue/im`) is unchanged.

## VO changes

`ImConversationVO` gains: `type` (`direct`/`channel`), `name` (channel name), `memberCount` (channels), `visibility` (channels). Direct conversations keep `peerUserId`/`peerName`/`peerAvatarUrl`; for channels those are null and the frontend renders a `#` avatar from the name. A new `ImChannelMemberVO`/reuse of `ImContactVO` carries member rows (`userId`, `name`, `avatarUrl`, `online`).

## Frontend

- **Conversation list:** add the **「频道」** filter chip (alongside 全部/未读 — the design already shows it). Channel rows render a `#`/initial avatar (no online dot), name, unread badge, last-message preview. The store's avatar/name/type logic branches on `conv.type`.
- **Create / browse entry:** next to "发起聊天", add **「创建频道」** (dialog: name, description, public/private toggle, multi-select members from contacts) and **「浏览频道」** (lists public channels with a Join button).
- **Channel chat pane:** header shows the channel name, a `#` avatar, member count, and a members panel with an "add member" action (member multi-select); the message area and composer reuse the Phase 1 Slack-style components.
- **`im` store:** add `createChannel`, `browsePublicChannels`, `joinChannel`, `leaveChannel`, `addMembers`, `listMembers`; conversations already include channels via `listMyConversations`. After create/join → refresh conversations + select the channel; after leave → drop it from the list and clear the active selection if it was active.

## Realtime

A new member starts receiving channel pushes as soon as their member row exists (push fans out to current `memberUserIds`). On join/create the client refreshes its conversation list and selects the channel; on leave it removes it. Other members see a new member when they next open the member panel (refreshed on open) — acceptable for v1; no presence dot for channels.

## Error Handling

- Join is rejected for `private` channels (only invite/add adds members); non-members are rejected from reading messages, listing members, sending, and adding members (membership check in the service, consistent with Phase 1).
- Creating a channel with no members still creates it with just the creator.
- `addMembers`/`join` are idempotent (skip users already members).
- Channel name is required and trimmed; empty name is rejected.

## Testing

Backend unit tests: `createChannel` (channel row has type/owner/visibility, creator + distinct members added); `joinChannel` (allowed for public, rejected for private, idempotent); `leaveChannel` (removes only the caller); `addMembers` (actor must be a member, adds non-members, idempotent); membership enforcement for channel message read/send and member listing (non-member rejected); `listMyConversations` returns channels with correct VO (type/name/memberCount). Tests follow the project convention (pure JUnit5 + Mockito, no Spring context). Frontend is verified by `vue-tsc` build + in-browser run-through.

## Out of Scope (explicitly deferred)

Rename/archive/delete channel; remove other members; channel admin roles; per-channel notification/mute settings; threads, reactions, @mentions and mention notifications (Phase 3); multi-instance WebSocket scaling.
