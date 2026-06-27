export type RecruitmentJobStatus = 'open' | 'paused' | 'closed' | (string & {})

export interface RecruitmentJobVO {
  recruitmentJobId: string
  jobName: string
  department?: string
  headcount?: number
  workYears?: number
  education?: string
  city?: string
  salaryRange?: string
  skillTags?: string
  responsibilities?: string
  requirements?: string
  status?: RecruitmentJobStatus
  ownerId?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface RecruitmentJobAddBO {
  jobName: string
  department?: string
  headcount?: number | null
  workYears?: number | null
  education?: string
  city?: string
  salaryRange?: string
  skillTags?: string
  responsibilities?: string
  requirements?: string
  status?: RecruitmentJobStatus
  ownerId?: string
  remark?: string
}

export interface RecruitmentJobUpdateBO extends RecruitmentJobAddBO {
  recruitmentJobId: string
}

export interface RecruitmentJobQueryBO {
  keyword?: string
  status?: RecruitmentJobStatus
  ownerId?: string
  page?: number
  limit?: number
}

export const RECRUITMENT_JOB_STATUS_OPTIONS: Array<{ value: RecruitmentJobStatus; label: string }> = [
  { value: 'open', label: '招聘中' },
  { value: 'paused', label: '暂停' },
  { value: 'closed', label: '关闭' }
]
