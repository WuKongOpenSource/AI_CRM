import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Client, type IMessage } from '@stomp/stompjs'
import { getToken } from '@/utils/request'
import {
  listConversations, listContacts, openDirect, fetchHistory,
  sendMessage, recallMessage, markRead,
  createChannel, browsePublicChannels, joinChannel, leaveChannel, addChannelMembers, listChannelMembers,
  toggleReaction, fetchThread,
  type ImConversation, type ImMessage, type ImContact, type ImSendPayload, type ImCreateChannelPayload, type ImReaction,
} from '@/api/im'

interface PushEnvelope {
  type: 'message' | 'unread' | 'reaction'
  conversationId: string
  message?: ImMessage | null
  unread?: number | null
  // reaction envelope fields
  messageId?: string
  reactions?: ImReaction[]
}

export const useImStore = defineStore('im', () => {
  const conversations = ref<ImConversation[]>([])
  const contacts = ref<ImContact[]>([])
  const messagesByConv = ref<Record<string, ImMessage[]>>({})
  const presence = ref<Record<string, boolean>>({})
  const activeConversationId = ref<string | null>(null)
  const connected = ref(false)
  // myId is set from outside (ImView) so mention notifications can reference it
  const myId = ref<string>('')
  // Thread state
  const threadRoot = ref<ImMessage | null>(null)
  const threadMessages = ref<ImMessage[]>([])

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

  function maybeNotifyMention(msg: ImMessage) {
    const mentionsMe = msg.mentionAll === true || (msg.mentionedUserIds || []).includes(myId.value)
    if (!mentionsMe || msg.senderId === myId.value) return
    const elsewhere = document.hidden || msg.conversationId !== activeConversationId.value
    if (!elsewhere) return
    try {
      if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
        new Notification(`${msg.senderName || ''} 提及了你`, { body: msg.content || '' })
      }
    } catch { /* ignore */ }
  }

  function onPush(env: PushEnvelope) {
    const conv = conversations.value.find((c) => c.id === env.conversationId)

    // Handle reaction envelope
    if (env.type === 'reaction') {
      if (env.messageId && env.reactions) {
        const list = messagesByConv.value[env.conversationId]
        const target = list?.find((m) => m.id === env.messageId)
        if (target) target.reactions = env.reactions
        if (threadRoot.value && threadRoot.value.id === env.messageId) threadRoot.value.reactions = env.reactions
        const tm = threadMessages.value.find((m) => m.id === env.messageId)
        if (tm) tm.reactions = env.reactions
      }
      return
    }

    if (env.type === 'message' && env.message) {
      const msg = env.message

      // Thread reply: do NOT add to main timeline; bump root replyCount; append to open thread
      if (msg.parentId) {
        const list = messagesByConv.value[env.conversationId]
        const root = list?.find((m) => m.id === msg.parentId)
        if (root) root.replyCount = (root.replyCount ?? 0) + 1
        if (threadRoot.value && threadRoot.value.id === msg.parentId) threadMessages.value.push(msg)
        maybeNotifyMention(msg)
        // still update unread for mentions but don't add to timeline
        if (env.conversationId !== activeConversationId.value) {
          if (conv && typeof env.unread === 'number') conv.unreadCount = env.unread
        }
        return
      }

      upsertMessage(env.conversationId, msg)
      if (conv) {
        conv.lastMessage = msg
        conv.updateTime = msg.createTime
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
      maybeNotifyMention(msg)
    } else if (env.type === 'unread' && conv && typeof env.unread === 'number') {
      conv.unreadCount = env.unread
    }
  }

  function upsertMessage(convId: string, msg: ImMessage) {
    const list = messagesByConv.value[convId] || []
    const idx = list.findIndex((m) => m.id === msg.id)
    if (idx >= 0) list.splice(idx, 1, msg)       // recall/update
    else list.push(msg)                           // new
    messagesByConv.value = { ...messagesByConv.value, [convId]: list }
  }

  function notifyIfHidden(env: PushEnvelope) {
    if (!env.message) return
    if (document.visibilityState === 'visible' && env.conversationId === activeConversationId.value) return
    if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return
    const conv = conversations.value.find((c) => c.id === env.conversationId)
    const preview = env.message.status === 'recalled' ? '撤回了一条消息'
      : (env.message.contentType === 'text' ? (env.message.content || '') : '[附件]')
    new Notification(conv?.peerName || conv?.name || '新消息', { body: preview })
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

  async function toggleReactionAction(messageId: string, emoji: string) {
    const reactions = await toggleReaction(messageId, emoji)
    // optimistic local update for the actor; WS push also arrives shortly
    for (const cid in messagesByConv.value) {
      const m = messagesByConv.value[cid].find((x) => x.id === messageId)
      if (m) m.reactions = reactions
    }
    if (threadRoot.value?.id === messageId) threadRoot.value.reactions = reactions
    const tm = threadMessages.value.find((x) => x.id === messageId)
    if (tm) tm.reactions = reactions
  }

  async function openThread(rootId: string) {
    const rows = await fetchThread(rootId)
    threadRoot.value = rows[0] ?? null
    threadMessages.value = rows.slice(1)
  }

  function closeThread() {
    threadRoot.value = null
    threadMessages.value = []
  }

  async function sendThreadReply(payload: ImSendPayload) {
    if (!threadRoot.value) return
    await sendMessage({ ...payload, parentId: threadRoot.value.id })
    // The WS push appends the reply to the open thread; no local append needed here
  }

  return {
    conversations, contacts, messagesByConv, presence, activeConversationId, connected, totalUnread,
    myId,
    threadRoot, threadMessages,
    connect, disconnect, refreshConversations, refreshContacts, loadHistory,
    openConversationWith, selectConversation, send, recall, markReadAction, ensureNotificationPermission,
    createChannelAction, browseChannels, joinChannelAction, leaveChannelAction, addMembersAction, fetchChannelMembers,
    toggleReactionAction, openThread, closeThread, sendThreadReply,
  }
})
