import { download, get, post, upload } from '@/utils/request'
import type {
  FollowUp,
  FollowUpAddBO,
  FollowUpAttachment,
  FollowUpAttachmentDraft,
  FollowUpQueryBO,
  FollowUpUpdateBO
} from '@/types/customer'
import type { PageResult } from '@/types/api'

/**
 * Add follow-up record
 */
export function addFollowUp(data: FollowUpAddBO): Promise<string> {
  return post('/followup/add', data)
}

/**
 * Update follow-up record
 */
export function updateFollowUp(data: FollowUpUpdateBO): Promise<void> {
  return post('/followup/update', data)
}

/**
 * Query follow-ups by customer
 */
export function queryFollowUpsByCustomer(customerId: string): Promise<FollowUp[]> {
  return post('/followup/queryByCustomer', null, { params: { customerId } })
}

/**
 * Delete follow-up record
 */
export function deleteFollowUp(followUpId: string): Promise<void> {
  return post(`/followup/delete/${followUpId}`)
}

/**
 * Query follow-ups with pagination
 */
export function queryFollowUpPageList(query: FollowUpQueryBO): Promise<PageResult<FollowUp>> {
  return post('/followup/queryPageList', query)
}

/**
 * AI parse follow-up content
 */
export interface AiFollowUpParseBO {
  content: string
  customerName?: string
  customerId?: string
}

export interface AiFollowUpParseVO {
  summary: string
  type: string
  followTime: string
  nextFollowTime: string
  keyPoints: string[]
  todos: string[]
}

export function aiParseFollowUp(data: AiFollowUpParseBO): Promise<AiFollowUpParseVO> {
  return post('/followup/ai-parse', data)
}

export function uploadFollowUpAttachment(file: File): Promise<FollowUpAttachmentDraft> {
  const formData = new FormData()
  formData.append('file', file)
  return upload('/followup/attachment/upload', formData)
}

export function transcribeFollowUpAudio(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return upload('/followup/ai-transcribe', formData)
}

export function analyzeFollowUpAttachment(attachmentId: string): Promise<FollowUpAttachment> {
  return post(`/followup/attachment/${attachmentId}/ai-analyze`)
}

export function downloadFollowUpAttachment(attachmentId: string, fileName?: string): Promise<void> {
  return download(`/followup/attachment/${attachmentId}/download`, fileName)
}

export function getFollowUpAttachmentBlob(attachmentId: string): Promise<Blob> {
  return get(`/followup/attachment/${attachmentId}/download`, { responseType: 'blob' })
}

export function deleteFollowUpAttachment(attachmentId: string): Promise<void> {
  return post(`/followup/attachment/${attachmentId}/delete`)
}
