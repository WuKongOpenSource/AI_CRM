import { post } from '@/utils/request'
import type { FollowUp, FollowUpAddBO, FollowUpQueryBO } from '@/types/customer'
import type { PageResult } from '@/types/api'

/**
 * Add follow-up record
 */
export function addFollowUp(data: FollowUpAddBO): Promise<string> {
  return post('/followup/add', data)
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
