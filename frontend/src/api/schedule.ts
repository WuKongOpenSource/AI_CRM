import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/api'

export interface ScheduleParticipantUser {
  userId: string
  realname: string
  username?: string
}

export interface ScheduleVO {
  scheduleId: string
  title: string
  description: string
  startTime: string
  endTime: string
  type: string
  typeName: string
  customerId: string
  customerName: string
  contactId: string
  contactName: string
  location: string
  participantNames?: string
  participantUserIds?: string[]
  participantUsers?: ScheduleParticipantUser[]
  createUserId: string
  createUserName: string
  createTime: string
}

export interface ScheduleAddBO {
  title: string
  startTime: string
  endTime?: string
  type?: string
  customerId?: string
  contactId?: string
  location?: string
  description?: string
  participantUserIds?: string[]
}

export interface ScheduleQueryBO {
  scheduleId?: string
  keyword?: string
  customerId?: string
  type?: string
  startDate?: string
  endDate?: string
  page?: number
  limit?: number
}

export interface ScheduleAiParseVO {
  title: string
  startTime: string
  endTime: string
  type: string
  customerName: string
  customerId?: string
  participantNames: string
  participantUserIds?: string[]
  participantUsers?: ScheduleParticipantUser[]
  unmatchedParticipantNames?: string
  location: string
  description: string
}

/**
 * 创建日程
 */
export function addSchedule(data: ScheduleAddBO): Promise<string> {
  return post('/schedule/add', data)
}

/**
 * 删除日程
 */
export function deleteSchedule(id: string): Promise<void> {
  return post(`/schedule/delete/${id}`)
}

/**
 * 查询我的日程
 */
export function getMySchedules(filter: string = 'all'): Promise<ScheduleVO[]> {
  return get('/schedule/mySchedules', { params: { filter } })
}

export function queryScheduleList(query: ScheduleQueryBO): Promise<PageResult<ScheduleVO>> {
  return post('/schedule/queryPageList', query)
}

/**
 * AI 智能解析日程
 */
export function aiParseSchedule(content: string): Promise<ScheduleAiParseVO> {
  return post('/schedule/ai-parse', { content })
}
