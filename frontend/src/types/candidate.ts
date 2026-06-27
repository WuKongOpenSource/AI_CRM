import type { ScheduleVO } from '@/api/schedule'
import type { Knowledge, Task } from '@/types/common'
import type { RecruitmentJobVO } from '@/types/recruitmentJob'

export type CandidateStage = 'new' | 'screening' | 'interview_scheduled' | 'interviewing' | 'interview_passed' | 'offer' | 'hired' | 'rejected' | (string & {})

export interface CandidateListVO {
  candidateId: string
  name: string
  avatar?: string
  avatarUrl?: string
  phone?: string
  email?: string
  wechat?: string
  currentCompany?: string
  currentPosition?: string
  appliedPosition?: string
  jobRequirements?: string
  recruitmentJobId?: string
  recruitmentJobName?: string
  recruitmentJob?: RecruitmentJobVO
  stage?: CandidateStage
  stageName?: string
  source?: string
  education?: string
  school?: string
  major?: string
  workYears?: number
  expectedCity?: string
  expectedSalary?: string
  skillTags?: string
  resumeSummary?: string
  aiEvaluation?: string
  ownerId?: string
  ownerName?: string
  remark?: string
  lastContactTime?: string
  nextStepTime?: string
  resumeCount?: number
  interviewCount?: number
  status?: number
  createUserId?: string
  createUserName?: string
  createTime?: string
  updateTime?: string
  customFields?: Record<string, unknown>
}

export interface CandidateDetailVO extends CandidateListVO {
  aiParseSnapshot?: string
  conflictSnapshot?: string
  tasks?: Task[]
  schedules?: ScheduleVO[]
  resumes?: Knowledge[]
}

export interface CandidateAddBO {
  name: string
  avatar?: string
  phone?: string
  email?: string
  wechat?: string
  currentCompany?: string
  currentPosition?: string
  appliedPosition?: string
  jobRequirements?: string
  recruitmentJobId?: string
  stage?: CandidateStage
  source?: string
  education?: string
  school?: string
  major?: string
  workYears?: number | null
  expectedCity?: string
  expectedSalary?: string
  skillTags?: string
  resumeSummary?: string
  aiEvaluation?: string
  ownerId?: string
  remark?: string
  lastContactTime?: string
  nextStepTime?: string
  customFields?: Record<string, unknown>
}

export interface CandidateUpdateBO extends CandidateAddBO {
  candidateId: string
}

export interface CandidateQueryBO {
  keyword?: string
  stage?: CandidateStage
  stages?: CandidateStage[]
  ownerId?: string
  source?: string
  appliedPosition?: string
  recruitmentJobId?: string
  createTimeStart?: string
  createTimeEnd?: string
  status?: number
  sortBy?: 'updateTime' | 'createTime' | 'lastContactTime' | 'nextStepTime'
  sortOrder?: 'asc' | 'desc'
  page?: number
  limit?: number
}

export interface CandidateFieldUpdateBO {
  candidateId: string
  fieldName: string
  value: unknown
}

export interface CandidateResumeParseBO {
  candidateId?: string
  content?: string
  fileName?: string
  filePath?: string
  fileSize?: number
  mimeType?: string
  appliedPosition?: string
  source?: string
  autoSave?: boolean
}

export interface CandidateResumeParseVO {
  candidateId?: string
  candidateName?: string
  created?: boolean
  updated?: boolean
  parsedFields?: Record<string, unknown>
  conflicts?: Record<string, unknown>
  resumeSummary?: string
  aiEvaluation?: string
  rawText?: string
}

export const CANDIDATE_STAGE_OPTIONS: Array<{ value: CandidateStage; label: string }> = [
  { value: 'new', label: '新候选人' },
  { value: 'screening', label: '初筛' },
  { value: 'interview_scheduled', label: '安排面试' },
  { value: 'interview_passed', label: '面试通过' },
  { value: 'offer', label: 'Offer' },
  { value: 'hired', label: '已入职' },
  { value: 'rejected', label: '已淘汰' }
]
