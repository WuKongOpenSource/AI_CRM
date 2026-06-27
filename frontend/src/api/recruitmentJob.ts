import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  RecruitmentJobAddBO,
  RecruitmentJobQueryBO,
  RecruitmentJobUpdateBO,
  RecruitmentJobVO
} from '@/types/recruitmentJob'

export function addRecruitmentJob(data: RecruitmentJobAddBO): Promise<string> {
  return post('/recruitment-job/add', data)
}

export function updateRecruitmentJob(data: RecruitmentJobUpdateBO): Promise<void> {
  return post('/recruitment-job/update', data)
}

export function deleteRecruitmentJob(recruitmentJobId: string): Promise<void> {
  return post(`/recruitment-job/delete/${recruitmentJobId}`)
}

export function queryRecruitmentJobs(query: RecruitmentJobQueryBO = {}): Promise<PageResult<RecruitmentJobVO>> {
  return post('/recruitment-job/queryPageList', { page: 1, limit: 100, ...query })
}

export function listRecruitmentJobOptions(query: RecruitmentJobQueryBO = {}): Promise<RecruitmentJobVO[]> {
  return post('/recruitment-job/listOptions', { page: 1, limit: 100, ...query })
}

export function getRecruitmentJobDetail(recruitmentJobId: string): Promise<RecruitmentJobVO> {
  return get(`/recruitment-job/detail/${recruitmentJobId}`)
}
