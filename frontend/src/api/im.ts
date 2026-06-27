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
  senderName?: string | null
}

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
