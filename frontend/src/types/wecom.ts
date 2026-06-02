import type { PageResult } from '@/types/api'

export type WecomConversationType = 'employee' | 'customer' | 'group'
export type WecomBindStatus = 'BOUND' | 'UNBOUND' | string

export interface WecomConfigVO {
  id?: string
  corpId?: string
  corpName?: string
  agentId?: string
  appSecretConfigured?: boolean
  contactSecretConfigured?: boolean
  archiveSecretConfigured?: boolean
  archivePrivateKeyConfigured?: boolean
  archivePublicKeyVersion?: string
  archiveEnabled?: boolean
  customerContactEnabled?: boolean
  syncEnabled?: boolean
  lastSyncTime?: string
  lastSyncStatus?: string
  lastSyncError?: string
}

export interface WecomSyncStatusVO {
  corpId?: string
  lastSyncTime?: string
  lastSyncStatus?: string
  lastSyncError?: string
  fetchedCount?: number
  savedCount?: number
  failedCount?: number
}

export interface WecomEmployeeSessionVO {
  id: string
  userId: string
  crmUserId?: string
  name?: string
  avatar?: string
  position?: string
  status?: number
  conversationCount?: number
  lastMsgTime?: string
}

export interface WecomExternalCustomerVO {
  id: string
  externalUserId: string
  name?: string
  avatar?: string
  type?: number
  gender?: number
  unionId?: string
  position?: string
  corpName?: string
  corpFullName?: string
  bindStatus?: WecomBindStatus
  customerId?: string
  customerName?: string
  syncedAt?: string
}

export interface WecomConversationVO {
  id: string
  conversationType: WecomConversationType | string
  employeeId?: string
  employeeUserId?: string
  externalCustomerId?: string
  externalUserId?: string
  groupChatId?: string
  chatId?: string
  title?: string
  peerName?: string
  peerAvatar?: string
  customerId?: string
  ownerUserId?: string
  lastMsgId?: string
  lastMsgTime?: string
  lastMsgPreview?: string
  messageCount?: number
}

export interface WecomMessageVO {
  id: string
  conversationId: string
  msgId: string
  seq?: number
  action?: string
  msgType?: string
  senderId?: string
  senderType?: string
  receiverList?: string
  msgTime?: string
  contentText?: string
  contentJson?: string
  sdkFileId?: string
  fileName?: string
  fileSize?: number
  fileUrl?: string
  recalled?: boolean
}

export interface WecomCustomerBindingVO {
  id: string
  customerId: string
  customerName?: string
  externalCustomerId: string
  externalUserId?: string
  externalCustomerName?: string
  externalCustomerAvatar?: string
  corpId?: string
  bindUserId?: string
  bindTime?: string
  unbindTime?: string
  status?: number
  remark?: string
}

export interface WecomConversationTabVO {
  conversationId: string
  tabKey: string
  title: string
  employeeUserId?: string
  employeeName?: string
  conversationType?: string
  lastMsgTime?: string
  messageCount?: number
}

export interface WecomEmployeeSessionQuery {
  page?: number
  limit?: number
  keyword?: string
  userId?: string
}

export interface WecomConversationQuery {
  page?: number
  limit?: number
  conversationType?: WecomConversationType | string
  employeeId?: string
  employeeUserId?: string
  externalCustomerId?: string
  groupChatId?: string
  customerId?: string
  keyword?: string
}

export interface WecomCustomerQuery {
  page?: number
  limit?: number
  keyword?: string
  bindStatus?: string
  customerId?: string
}

export interface WecomCustomerBindPayload {
  customerId: string
  externalCustomerId: string
  remark?: string
}

export interface WecomCustomerUnbindPayload {
  bindingId?: string
  customerId?: string
  externalCustomerId?: string
}

export type WecomEmployeePage = PageResult<WecomEmployeeSessionVO>
export type WecomConversationPage = PageResult<WecomConversationVO>
export type WecomCustomerPage = PageResult<WecomExternalCustomerVO>
export type WecomMessagePage = PageResult<WecomMessageVO>
