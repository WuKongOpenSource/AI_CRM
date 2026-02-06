// Customer related types
export interface Customer {
  customerId: string
  companyName: string
  industry?: string
  stage: CustomerStage
  level: CustomerLevel
  ownerId: string
  ownerName?: string
  phone?: string
  email?: string
  website?: string
  address?: string
  description?: string
  createTime: string
  updateTime: string
}

export type CustomerStage = 'lead' | 'qualified' | 'proposal' | 'negotiation' | 'closed' | 'lost'
export type CustomerLevel = 'A' | 'B' | 'C'

export interface CustomerListVO extends Customer {
  contactCount: number
  lastFollowUpTime?: string
  lastContactTime?: string
  tags: string[]
  customFields?: Record<string, any>
  // Financial info
  quotation?: number
  contractAmount?: number
  revenue?: number
  // Primary contact info
  primaryContactName?: string
  primaryContactPhone?: string
  primaryContactPosition?: string
}

export interface CustomerDetailVO extends Customer {
  contacts: Contact[]
  followUps: FollowUp[]
  tags: CustomerTag[]
  documents: Knowledge[]
  tasks: Task[]
  customFields?: Record<string, any>
}

export interface CustomerAddBO {
  companyName: string
  industry?: string
  stage?: CustomerStage
  level?: CustomerLevel
  phone?: string
  email?: string
  website?: string
  address?: string
  description?: string
  // Primary contact
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  contactPosition?: string
  // Custom fields
  customFields?: Record<string, any>
}

export interface CustomerUpdateBO extends CustomerAddBO {
  customerId: string
}

export interface CustomerQueryBO {
  keyword?: string
  stage?: CustomerStage
  level?: CustomerLevel
  ownerId?: string
  page?: number
  limit?: number
}

// Contact types
export interface Contact {
  contactId: string
  customerId: string
  name: string
  position?: string
  phone?: string
  email?: string
  wechat?: string
  isPrimary: boolean
  notes?: string
  createTime: string
}

export interface ContactAddBO {
  customerId: string
  name: string
  position?: string
  phone?: string
  email?: string
  wechat?: string
  isPrimary?: boolean
  notes?: string
}

export interface ContactQueryBO {
  customerId?: string
  keyword?: string
  isPrimary?: number
  page?: number
  limit?: number
}

// FollowUp types
export interface FollowUp {
  followUpId: string
  customerId: string
  type: FollowUpType
  content: string
  followTime: string
  result?: string
  nextPlan?: string
  nextFollowTime?: string
  createUserId: string
  createUserName?: string
  createTime: string
}

export type FollowUpType = 'call' | 'meeting' | 'email' | 'visit' | 'other'

export interface FollowUpAddBO {
  customerId: string
  type: string
  content: string
  followTime: string
  contactId?: string
  nextFollowTime?: string
}

export interface FollowUpQueryBO {
  customerId?: string
  contactId?: string
  type?: string
  startTime?: string
  endTime?: string
  page?: number
  limit?: number
}

// CustomerTag types
export interface CustomerTag {
  tagId: string
  customerId: string
  tagName: string
  color?: string
}
