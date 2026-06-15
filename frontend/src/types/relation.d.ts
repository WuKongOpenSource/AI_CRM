import type { PageResult } from '@/types/api'

export type RelationType = 'decision_maker' | 'influencer' | 'partner' | 'customer_contact' | 'other' | string
export type RelationSource = 'manual' | 'customer_contact' | string

export interface Relation {
  relationId: string
  name: string
  avatar?: string
  avatarUrl?: string
  phone?: string
  wechat?: string
  email?: string
  relationType?: RelationType
  relationTypeName?: string
  company?: string
  customerId?: string
  customerName?: string
  customerLogo?: string
  customerLogoUrl?: string
  remark?: string
  source?: RelationSource
  sourceName?: string
  sourceCustomerId?: string
  sourceCustomerName?: string
  sourceContactId?: string
  customFields?: Record<string, any>
  createUserId?: string
  createTime?: string
  updateTime?: string
}

export interface RelationDetail {
  relation: Relation
  tasks?: any[]
  schedules?: any[]
  attachments?: any[]
  histories?: any[]
}

export interface RelationQuery {
  keyword?: string
  relationType?: string
  source?: string
  sourceCustomerId?: string
  customerId?: string
  sourceContactId?: string
  page?: number
  limit?: number
}

export interface RelationForm {
  relationId?: string
  name: string
  avatar?: string
  phone?: string
  wechat?: string
  email?: string
  relationType?: string
  customerId?: string
  remark?: string
  customFields?: Record<string, any>
}

export type RelationPageResult = PageResult<Relation>
export type RelationVO = Relation
export type RelationAddBO = RelationForm
export interface RelationUpdateBO extends RelationForm {
  relationId: string
}
export type RelationDetailVO = RelationDetail
export type RelationQueryBO = RelationQuery
