import { post } from '@/utils/request'
import type { RelationDetail, RelationForm, RelationPageResult, RelationQuery } from '@/types/relation'

export function addRelation(data: RelationForm): Promise<string> {
  return post('/relation/add', data)
}

export function updateRelation(data: RelationForm & { relationId: string }): Promise<void> {
  return post('/relation/update', data)
}

export function deleteRelation(id: string): Promise<void> {
  return post(`/relation/delete/${id}`)
}

export function queryRelationPageList(query: RelationQuery = {}): Promise<RelationPageResult> {
  return post('/relation/queryPageList', { page: 1, limit: 20, ...query })
}

export function getRelationDetail(id: string): Promise<RelationDetail> {
  return post(`/relation/detail/${id}`)
}

export function addRelationFromContact(contactId: string): Promise<string> {
  return post(`/relation/addFromContact/${contactId}`)
}
