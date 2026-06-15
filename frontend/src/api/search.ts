import { post } from '@/utils/request'
import type { PageResult } from '@/types/api'

export type GlobalSearchEntityType = 'customer' | 'contact' | 'relation' | 'product' | 'task' | 'schedule' | 'knowledge'

export interface GlobalSearchQueryBO {
  keyword?: string
  entityType?: GlobalSearchEntityType
  page?: number
  limit?: number
}

export interface GlobalSearchResult {
  entityType: GlobalSearchEntityType
  entityId: string
  title: string
  subtitle?: string
  summary?: string
  customerId?: string
  customerName?: string
  routePath: string
  sortTime?: string
  score?: number
}

export function queryGlobalSearch(query: GlobalSearchQueryBO): Promise<PageResult<GlobalSearchResult>> {
  return post('/search/global', query)
}
