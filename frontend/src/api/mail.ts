import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/api'

export type MailProvider = 'imap' | 'gmail' | 'outlook'
export type MailDirection = 'received' | 'sent'

export interface MailAccount {
  accountId: string
  provider: MailProvider | string
  authType: string
  emailAddress: string
  displayName?: string
  imapHost?: string
  imapPort?: number
  smtpHost?: string
  smtpPort?: number
  enabled: boolean
  isDefault?: boolean
  connectionStatus?: string
  lastUsedTime?: string
  folders?: string[]
  lastSyncTime?: string
  lastSyncStatus?: string
  lastSyncError?: string
  createTime?: string
}

export interface MailAuthStatus {
  authorized: boolean
  currentAccount?: MailAccount
  accounts: MailAccount[]
}

export interface MailSyncResult {
  accountId: string
  logId?: string
  fetchedCount: number
  savedCount: number
  skippedCount: number
  failedCount: number
  status?: string
  errorMessage?: string
}

export interface MailSyncLog {
  logId: string
  accountId: string
  userId?: string
  syncType?: string
  status: string
  fetchedCount?: number
  savedCount?: number
  skippedCount?: number
  failedCount?: number
  startedAt?: string
  finishedAt?: string
  errorMessage?: string
}

export interface MailOAuthStart {
  provider: string
  authorizeUrl: string
  state: string
}

export interface MailMessage {
  messageId: string
  accountId: string
  provider: string
  providerMessageId: string
  internetMessageId?: string
  threadId?: string
  folder?: string
  direction: MailDirection | string
  subject?: string
  fromName?: string
  fromAddress?: string
  toAddresses?: string
  ccAddresses?: string
  sentTime?: string
  receivedTime?: string
  summary?: string
  bodyText?: string
  bodyHtml?: string
  hasAttachments?: boolean
  readStatus?: 'read' | 'unread' | string
  starred?: boolean
  deleted?: boolean
  customerId?: string
  customerName?: string
  contactId?: string
  contactName?: string
  createTime?: string
}

export interface MailDraft {
  draftId: string
  accountId?: string
  accountEmail?: string
  customerId?: string
  contactId?: string
  sourceMessageId?: string
  toAddresses: string
  ccAddresses?: string
  bccAddresses?: string
  subject: string
  bodyText: string
  attachmentRefs?: string
  status: string
  riskStatus?: string
  riskReasons?: string
  createTime?: string
  updateTime?: string
}

export interface MailTemplate {
  templateId: string
  name: string
  category?: string
  subject: string
  bodyText: string
  variables?: string
  isCommon?: boolean
  createTime?: string
  updateTime?: string
}

export interface MailListQuery {
  page?: number
  limit?: number
  accountId?: string
  keyword?: string
  status?: string
  readStatus?: string
  starred?: boolean
  category?: string
  commonOnly?: boolean
}

export interface MailDraftPayload {
  accountId?: string
  customerId?: string
  contactId?: string
  sourceMessageId?: string
  toAddresses: string
  ccAddresses?: string
  bccAddresses?: string
  subject: string
  bodyText: string
  attachmentRefs?: string
}

export interface MailTemplatePayload {
  templateId?: string
  name: string
  category?: string
  subject: string
  bodyText: string
  variables?: string
  isCommon?: boolean
}

export interface MailImapConnectPayload {
  emailAddress: string
  displayName?: string
  imapHost: string
  imapPort?: number
  imapSsl?: boolean
  smtpHost?: string
  smtpPort?: number
  smtpSsl?: boolean
  username?: string
  password: string
  testConnection?: boolean
}

export function getMailAuthStatus(): Promise<MailAuthStatus> {
  return get('/email/auth/status')
}

export function startMailOAuth(provider: 'gmail' | 'outlook'): Promise<MailOAuthStart> {
  return get(`/email/oauth/${provider}/authorize`)
}

export function connectImapMailbox(data: MailImapConnectPayload): Promise<MailAccount> {
  return post('/email/auth/connect', data)
}

export function testImapMailbox(data: MailImapConnectPayload): Promise<void> {
  return post('/email/auth/test', data)
}

export function setDefaultMailbox(accountId: string): Promise<MailAccount> {
  return post(`/email/accounts/${accountId}/default`)
}

export function disconnectMailbox(accountId: string): Promise<void> {
  return post(`/email/accounts/${accountId}/disconnect`)
}

export function syncMailbox(accountId: string): Promise<MailSyncResult> {
  return post(`/email/accounts/${accountId}/sync`)
}

export function listMailboxSyncLogs(accountId: string, limit = 20): Promise<MailSyncLog[]> {
  return get(`/email/accounts/${accountId}/sync-logs`, { params: { limit } })
}

export function queryInbox(query: MailListQuery): Promise<PageResult<MailMessage>> {
  return get('/email/inbox', { params: query })
}

export function querySent(query: MailListQuery): Promise<PageResult<MailMessage>> {
  return get('/email/sent', { params: query })
}

export function queryDrafts(query: MailListQuery): Promise<PageResult<MailDraft>> {
  return get('/email/drafts', { params: query })
}

export function saveDraft(data: MailDraftPayload): Promise<MailDraft> {
  return post('/email/draft/save', data)
}

export function updateDraft(draftId: string, data: MailDraftPayload): Promise<MailDraft> {
  return post(`/email/drafts/${draftId}`, data)
}

export function deleteDraft(draftId: string): Promise<void> {
  return post(`/email/drafts/${draftId}/delete`)
}

export function sendMail(data: { draftId?: string; draft?: MailDraftPayload }): Promise<MailMessage> {
  return post('/email/send', data)
}

export function getMailMessage(messageId: string): Promise<MailMessage> {
  return get(`/email/messages/${messageId}`)
}

export function markMailRead(messageId: string, read: boolean): Promise<void> {
  return post(`/email/messages/${messageId}/read`, null, { params: { read } })
}

export function starMail(messageId: string, starred: boolean): Promise<void> {
  return post(`/email/messages/${messageId}/star`, null, { params: { starred } })
}

export function deleteMailMessage(messageId: string): Promise<void> {
  return post(`/email/messages/${messageId}/delete`)
}

export function queryMailTemplates(query: MailListQuery): Promise<PageResult<MailTemplate>> {
  return get('/email/templates', { params: query })
}

export function createMailTemplate(data: MailTemplatePayload): Promise<MailTemplate> {
  return post('/email/template/create', data)
}

export function updateMailTemplate(templateId: string, data: MailTemplatePayload): Promise<MailTemplate> {
  return post(`/email/templates/${templateId}`, data)
}

export function copyMailTemplate(templateId: string): Promise<MailTemplate> {
  return post(`/email/templates/${templateId}/copy`)
}

export function deleteMailTemplate(templateId: string): Promise<void> {
  return post(`/email/templates/${templateId}/delete`)
}
