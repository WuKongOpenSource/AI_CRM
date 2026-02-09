import { post, get, upload, download } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { Knowledge, KnowledgeQueryBO } from '@/types/common'

/**
 * Upload file to knowledge base
 */
export function uploadKnowledge(
  file: File,
  type?: string,
  customerId?: string,
  summary?: string
): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  if (type) formData.append('type', type)
  if (customerId) formData.append('customerId', customerId)
  if (summary) formData.append('summary', summary)
  return upload('/knowledge/upload', formData)
}

/**
 * Delete knowledge file
 */
export function deleteKnowledge(id: string): Promise<void> {
  return post(`/knowledge/delete/${id}`)
}

/**
 * Query knowledge base
 */
export function queryKnowledgeList(query: KnowledgeQueryBO): Promise<PageResult<Knowledge>> {
  return post('/knowledge/queryPageList', query)
}

/**
 * Get knowledge detail
 */
export function getKnowledgeDetail(id: string): Promise<Knowledge> {
  return get(`/knowledge/detail/${id}`)
}

/**
 * Download knowledge file
 */
export function downloadKnowledge(id: string, filename: string): Promise<void> {
  return download(`/knowledge/download/${id}`, filename)
}

/**
 * Reparse knowledge file
 */
export function reparseKnowledge(id: string): Promise<void> {
  return post(`/knowledge/reparse/${id}`)
}

/**
 * Add knowledge tag
 */
export function addKnowledgeTag(knowledgeId: string, tagName: string): Promise<void> {
  return post('/knowledge/addTag', null, { params: { knowledgeId, tagName } })
}
