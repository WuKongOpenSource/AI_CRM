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
