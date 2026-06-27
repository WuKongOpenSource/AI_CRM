# IM Phase 3 Design — Reactions, @Mentions, Threads

## Goal

Add three independent collaboration features to the IM built in Phases 1–2: emoji **reactions** on messages, **@mentions** (individual + @all) with highlight and browser notification, and **threads** (reply-in-thread, replies live only in the thread, main timeline shows a reply count). All three reuse the Phase 1 conversation/member/message tables and the WebSocket member fan-out. Built as one spec/plan/cycle.

## Scope

- **In:**
  - **Reactions:** a curated quick-emoji row (👍 ❤️ 😂 🎉 👀 ✅ 🙏 😄); click to toggle; reaction chips with counts under each message (own reactions highlighted); live sync. Works on normal messages and thread replies.
  - **@Mentions:** `@` autocomplete in the composer (channel members + `@所有人`; for DMs just the peer); store mentioned user ids + `mentionAll` on the message; render `@name` highlighted; browser notification + an "@你" marker when a received message mentions the current user (or is `@所有人`).
  - **Threads:** reply to a message to start/continue a thread; the root message accumulates a reply count and shows "💬 N 条回复" in the main timeline; a right-side thread drawer shows the root + replies with its own composer. Thread replies do **not** appear in the main timeline. Reactions and @mentions work inside threads.
- **Out (YAGNI):** full emoji picker (categories/search); a dedicated "mentions me" filter/inbox; broadcasting thread replies to the main channel; a hover "who reacted" roster (v1 shows count + whether I reacted only); separate per-thread-participant notifications (reuse member fan-out); editing messages.
- Single-tenant, gated by the existing `im` permission. Not an AI feature.

## Data Model — migration V43 (next free after V42; verify at implementation)

New table **`crm_im_message_reaction`**:
- `id` BIGINT PK, `message_id` BIGINT, `conversation_id` BIGINT, `user_id` BIGINT, `emoji` VARCHAR(32), `create_time` TIMESTAMP.
- `UNIQUE (message_id, user_id, emoji)`; index `(message_id)`. `conversation_id` is carried for membership checks and future cleanup.

Alter **`crm_im_message`** (additive `ADD COLUMN IF NOT EXISTS`, no backfill):
- `parent_id` BIGINT — the root message id for a thread reply; null for normal/root messages.
- `reply_count` INT DEFAULT 0 — accumulated on a root message.
- `last_reply_time` TIMESTAMP — last reply time on a root message (for ordering/affordance).
- `mentioned_user_ids` VARCHAR(500) — csv of mentioned user ids.
- `mention_all` BOOLEAN DEFAULT false.

Index `(conversation_id, parent_id)` to keep the main-timeline "exclude replies" query fast.

## Behaviors

### Reactions
- `toggleReaction(userId, messageId, emoji)`: assert the caller is a member of the message's conversation; if a `(message_id, user_id, emoji)` row exists, delete it, else insert it (idempotent under concurrency via the unique constraint + caught `DuplicateKeyException`). Returns the message's aggregated reactions.
- `aggregateReactions(messageId, viewerId)` → `List<ImReactionVO>` = `{emoji, count, mine}` grouped by emoji, ordered by first-reacted. Used both in the toggle response and when building message VOs.
- History and thread fetches populate each `ImMessageVO.reactions`.
- After a toggle commits, push a `reaction` envelope (`messageId`, `conversationId`, aggregated reactions) to all conversation members so chips update live.

### @Mentions
- `POST /im/messages` (existing send) accepts optional `mentionedUserIds` (array) and `mentionAll` (bool) in the send BO; the service stores them as csv / boolean on the message. The message `content` keeps the literal `@名字` display text the user typed.
- `ImMessageVO` carries `mentionedUserIds` (string[]) and `mentionAll`.
- Notification: when a pushed message arrives and (`mentionAll` || the current user id ∈ `mentionedUserIds`), the frontend raises a browser notification even if the conversation is not active (extends the existing `notifyIfHidden`), and marks the message with an "@你" highlight.
- Autocomplete is frontend-only: channel members come from `GET /im/channels/{id}/members`; `@所有人` maps to `mentionAll=true`; in a DM the only candidate is the peer. No new backend endpoint.

### Threads
- `POST /im/messages` accepts optional `parentId`. When set: assert membership; store `parent_id` on the new reply; increment the root message's `reply_count` and set `last_reply_time`; push the reply with `parentId` set so clients route it to the thread (and bump the root's reply count) instead of the main timeline.
- Main-timeline history (`GET /im/conversations/{id}/messages`) excludes rows with `parent_id IS NOT NULL`.
- `GET /im/messages/{rootId}/thread` returns the root message followed by its replies ordered by `create_time` (membership asserted). Each carries reactions/mentions like any message.
- `ImMessageVO` carries `parentId` and `replyCount` (replyCount meaningful on root messages).
- Recall, attachments, reactions, and @mentions all work on thread replies (they are ordinary messages with a `parent_id`).

## REST Surface (additions; all `@RequirePermission("im")`, membership-checked)

- `POST /im/messages/{id}/reactions` — body `{emoji}` → toggles, returns aggregated reactions for that message.
- `GET /im/messages/{rootId}/thread` — root + replies.
- Reused/extended: `POST /im/messages` send BO gains `mentionedUserIds`, `mentionAll`, `parentId`. WS push (`/user/queue/im`) gains a `reaction` envelope type; message envelopes carry the new VO fields.

## VO changes

`ImMessageVO` gains: `reactions` (`List<ImReactionVO>` = `{emoji, count, mine}`), `mentionedUserIds` (string[]), `mentionAll` (bool), `parentId` (string|null), `replyCount` (int). New `ImReactionVO`. The realtime `reaction` push envelope carries `{type:"reaction", messageId, conversationId, reactions}`.

## Frontend

`api/im.ts`: extend `ImMessage` with `reactions`, `mentionedUserIds`, `mentionAll`, `parentId`, `replyCount`; extend `ImSendPayload` with `mentionedUserIds?`, `mentionAll?`, `parentId?`; add `toggleReaction(messageId, emoji)` and `fetchThread(rootId)`.

`stores/im.ts`: handle the `reaction` push (update the target message's `reactions` in `messagesByConv` and in any open thread); route incoming messages with `parentId` to a `threadsByRoot` map + bump the root's `replyCount` instead of appending to the main list; `toggleReactionAction`, `openThread`, `sendThreadReply`; extend `notifyIfHidden` for mentions.

`views/im/ImView.vue`:
- **Reactions:** the existing hover action bar gains an emoji button → a small popover with the curated row; clicking toggles via the store. Render reaction chips beneath each message (emoji + count, highlighted when `mine`); clicking a chip toggles.
- **@Mentions:** the composer watches for `@`; shows an autocomplete list (channel members + `@所有人`, or the DM peer); selecting inserts `@名字 ` and records the mention; on send compute `mentionedUserIds`/`mentionAll`. Render message text with `@name` tokens highlighted; show an "@你" badge on messages mentioning the current user.
- **Threads:** a "回复" hover action and a "💬 N 条回复" affordance on root messages open a right-side thread drawer (root + replies + a dedicated composer that sends with `parentId`). Replies are excluded from the main timeline.

## Realtime

All three reuse the Phase 1 per-member fan-out. New `reaction` envelopes and the extended message VO fields require no new transport. A thread reply pushes to all conversation members (so unread/notifications still work); clients with the matching thread drawer open append it, others just bump the root reply count. Reaction toggles push the fresh aggregate to all members.

## Error Handling

- Reaction/thread/reply endpoints assert conversation membership (consistent with Phases 1–2); non-members are rejected.
- Reaction toggle is idempotent under concurrency (unique constraint + caught `DuplicateKeyException`); emoji is validated against the curated set server-side (reject unknown emoji).
- A reply to a non-existent or non-member root is rejected. `reply_count` is only ever incremented (recall of a reply does not decrement in v1 — acceptable; documented).
- `mentionedUserIds` are stored as given; the frontend resolves names. Invalid/blank entries are ignored.

## Testing

Backend unit tests (pure JUnit5 + Mockito): `toggleReaction` adds then removes (idempotent), rejects unknown emoji, asserts membership; `aggregateReactions` groups by emoji with correct `mine`; sending with `parentId` increments root `reply_count` + sets `last_reply_time` and stores `parent_id`; main-timeline query excludes replies; `fetchThread` returns root + replies; send stores `mentioned_user_ids`/`mention_all`; VO carries the new fields. Frontend verified by `vue-tsc` build + in-browser run-through.

## Out of Scope (explicitly deferred)

Full emoji picker; "mentions me" filter/inbox; thread replies echoed to the main channel; "who reacted" roster; reply-count decrement on reply recall; message editing; per-thread-participant notification routing; multi-instance WebSocket scaling.
