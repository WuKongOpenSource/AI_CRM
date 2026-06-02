import { get, post } from '@/utils/request'
import type {
  TencentMeetingBindPayload,
  TencentMeetingCandidateQuery,
  TencentMeetingCandidateVO,
  TencentMeetingConfigVO,
  TencentMeetingDetailVO,
  TencentMeetingPage,
  TencentMeetingQuery,
  TencentMeetingSyncStatusVO,
  TencentMeetingUnbindPayload,
  TencentMeetingVO
} from '@/types/tencentMeeting'

export function getTencentMeetingConfig(): Promise<TencentMeetingConfigVO> {
  return get('/tencent-meeting/config')
}

export function saveTencentMeetingConfig(data: Partial<TencentMeetingConfigVO>): Promise<TencentMeetingConfigVO> {
  return post('/tencent-meeting/config', data)
}

export function runTencentMeetingSync(data = {}): Promise<TencentMeetingSyncStatusVO> {
  return post('/tencent-meeting/sync/run', data)
}

export function getTencentMeetingSyncStatus(): Promise<TencentMeetingSyncStatusVO> {
  return get('/tencent-meeting/sync/status')
}

export function queryTencentMeetings(query: TencentMeetingQuery): Promise<TencentMeetingPage> {
  return post('/tencent-meeting/queryPageList', query)
}

export function getTencentMeetingDetail(id: string): Promise<TencentMeetingDetailVO> {
  return get(`/tencent-meeting/${id}`)
}

export function refreshTencentMeeting(id: string): Promise<void> {
  return post(`/tencent-meeting/${id}/refresh`)
}

export function bindTencentMeeting(data: TencentMeetingBindPayload): Promise<void> {
  return post('/tencent-meeting/bind', data)
}

export function unbindTencentMeeting(data: TencentMeetingUnbindPayload): Promise<void> {
  return post('/tencent-meeting/unbind', data)
}

export function queryTencentMeetingCandidates(query: TencentMeetingCandidateQuery): Promise<TencentMeetingCandidateVO[]> {
  return post('/tencent-meeting/candidates', query)
}

export function getCustomerTencentMeetings(customerId: string): Promise<TencentMeetingVO[]> {
  return get(`/customer/${customerId}/tencent-meetings`)
}

export function getCustomerTencentMeetingCandidates(
  customerId: string,
  query: TencentMeetingCandidateQuery
): Promise<TencentMeetingCandidateVO[]> {
  return post(`/customer/${customerId}/tencent-meetings/candidates`, query)
}
