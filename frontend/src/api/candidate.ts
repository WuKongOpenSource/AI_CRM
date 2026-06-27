import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  CandidateAddBO,
  CandidateDetailVO,
  CandidateFieldUpdateBO,
  CandidateListVO,
  CandidateQueryBO,
  CandidateResumeParseBO,
  CandidateResumeParseVO,
  CandidateUpdateBO
} from '@/types/candidate'

export function addCandidate(data: CandidateAddBO): Promise<string> {
  return post('/candidate/add', data)
}

export function updateCandidate(data: CandidateUpdateBO): Promise<void> {
  return post('/candidate/update', data)
}

export function updateCandidateField(data: CandidateFieldUpdateBO): Promise<CandidateDetailVO> {
  return post('/candidate/updateField', data)
}

export function deleteCandidate(candidateId: string): Promise<void> {
  return post(`/candidate/delete/${candidateId}`)
}

export function queryCandidateList(query: CandidateQueryBO = {}): Promise<PageResult<CandidateListVO>> {
  return post('/candidate/queryPageList', { page: 1, limit: 20, ...query })
}

export function getCandidateDetail(candidateId: string): Promise<CandidateDetailVO> {
  return get(`/candidate/detail/${candidateId}`)
}

export function updateCandidateStage(candidateId: string, stage: string): Promise<void> {
  return post('/candidate/updateStage', null, { params: { candidateId, stage } })
}

export function parseCandidateResume(data: CandidateResumeParseBO): Promise<CandidateResumeParseVO> {
  return post('/candidate/ai-parse-resume', data)
}
