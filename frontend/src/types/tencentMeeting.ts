import type { PageResult } from '@/types/api'

export type TencentMeetingStatus = 'not_started' | 'ended' | 'cancelled' | string
export type TencentMeetingBindStatus = 'BOUND' | 'UNBOUND' | string

export interface TencentMeetingConfigVO {
  id?: string
  appId?: string
  sdkId?: string
  corpName?: string
  secretIdMasked?: string
  operatorUserId?: string
  syncEnabled?: boolean
  transcriptEnabled?: boolean
  archiveToKnowledge?: boolean
  stsTokenExpireTime?: string
  lastSyncTime?: string
  lastSyncStatus?: string
  lastSyncError?: string
}

export interface TencentMeetingSyncStatusVO {
  appId?: string
  lastSyncTime?: string
  lastSyncStatus?: string
  lastSyncError?: string
  fetchedCount?: number
  savedCount?: number
  failedCount?: number
}

export interface TencentMeetingVO {
  id: string
  meetingId?: string
  meetingCode?: string
  subject?: string
  status?: TencentMeetingStatus
  creatorUserId?: string
  creatorName?: string
  crmCreatorUserId?: string
  participantNames?: string
  participantCount?: number
  startTime?: string
  endTime?: string
  durationSeconds?: number
  bindStatus?: TencentMeetingBindStatus
  customerId?: string
  customerName?: string
  summary?: string
  todoText?: string
}

export interface TencentMeetingParticipantVO {
  id: string
  userId?: string
  userName?: string
  role?: string
  joinTime?: string
  leaveTime?: string
  durationSeconds?: number
}

export interface TencentMeetingRecordingVO {
  id: string
  recordFileId?: string
  fileName?: string
  downloadUrl?: string
  playUrl?: string
  fileSize?: number
  durationSeconds?: number
  transcriptStatus?: string
  summary?: string
  todoText?: string
}

export interface TencentMeetingTranscriptSegmentVO {
  id: string
  pid?: string
  speakerUserId?: string
  speakerName?: string
  startTimeMs?: number
  endTimeMs?: number
  text?: string
}

export interface TencentMeetingDetailVO extends TencentMeetingVO {
  transcriptText?: string
  participants?: TencentMeetingParticipantVO[]
  recordings?: TencentMeetingRecordingVO[]
  transcriptSegments?: TencentMeetingTranscriptSegmentVO[]
}

export interface TencentMeetingCustomerBindingVO {
  id: string
  meetingId: string
  meetingExternalId?: string
  customerId: string
  customerName?: string
  bindTime?: string
  status?: number
}

export interface TencentMeetingCandidateVO {
  id: string
  meetingId?: string
  subject?: string
  creatorName?: string
  participantNames?: string
  startTime?: string
  durationSeconds?: number
  summary?: string
  score?: number
  matchReason?: string
}

export interface TencentMeetingQuery {
  page?: number
  limit?: number
  keyword?: string
  status?: string
  bindStatus?: string
  customerId?: string
  startTimeFrom?: string
  startTimeTo?: string
}

export interface TencentMeetingBindPayload {
  meetingId: string
  customerId: string
}

export interface TencentMeetingUnbindPayload {
  meetingId: string
}

export interface TencentMeetingCandidateQuery {
  customerId?: string
  keyword?: string
  inputText?: string
  startTimeFrom?: string
  startTimeTo?: string
  limit?: number
}

export type TencentMeetingPage = PageResult<TencentMeetingVO>
