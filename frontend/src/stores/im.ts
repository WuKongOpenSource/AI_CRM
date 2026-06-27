import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { Client, type IMessage } from '@stomp/stompjs'
import { getToken } from '@/utils/request'
import {
  listConversations, listContacts, openDirect, fetchHistory,
  sendMessage, recallMessage, markRead,
  createChannel, browsePublicChannels, joinChannel, leaveChannel, addChannelMembers, listChannelMembers,
  type ImConversation, type ImMessage, type ImContact, type ImSendPayload, type ImCreateChannelPayload,
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

  return {
    conversations, contacts, messagesByConv, presence, activeConversationId, connected, totalUnread,
    connect, disconnect, refreshConversations, refreshContacts, loadHistory,
    openConversationWith, selectConversation, send, recall, markReadAction, ensureNotificationPermission,
    createChannelAction, browseChannels, joinChannelAction, leaveChannelAction, addMembersAction, fetchChannelMembers,
  }
})
