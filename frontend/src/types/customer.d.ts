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
  logo?: string
  logoUrl?: string
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
  // Team & source
  teamMemberNames?: string[]
  source?: string
}

export interface CustomerDetailVO extends Customer {
  source?: string
  stageName?: string
  quotation?: number
  contractAmount?: number
  revenue?: number
  lastContactTime?: string
  nextFollowTime?: string
  remark?: string
  ownerAvatar?: string
  createUserId?: string | number
  createUserName?: string
  contacts: ContactVO[]
  recentFollowUps: FollowUpVO[]
  tags: CustomerTag[]
  documents: KnowledgeVO[]
  tasks: Task[]
  teamMembers?: TeamMemberVO[]
  customFields?: Record<string, any>
}

export interface ContactVO extends Contact {
  // Backend ContactVO fields if needed
}

export interface FollowUpVO extends FollowUp {
  customerName?: string
  contactId?: string
  contactName?: string
  typeName?: string
}

export interface KnowledgeVO {
  knowledgeId: string
  name: string
  type: string
  summary?: string
  createTime: string
}

export interface TeamMemberVO {
  userId: string
  name: string
  avatar?: string
  role?: string
}

export interface CustomerAddBO {
  companyName: string
  industry?: string
  stage?: CustomerStage
  level?: CustomerLevel
  phone?: string
  email?: string
  source?: string
  website?: string
  logo?: string
  address?: string
  quotation?: number | null
  remark?: string
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
  stages?: CustomerStage[]
  level?: CustomerLevel
  industry?: string
  tag?: string
  source?: string
  quotationMin?: number
  quotationMax?: number
  contractAmountMin?: number
  contractAmountMax?: number
  revenueMin?: number
  revenueMax?: number
  lastContactStart?: string
  lastContactEnd?: string
  includeNoLastContact?: boolean
  nextFollowStart?: string
  nextFollowEnd?: string
  createTimeStart?: string
  createTimeEnd?: string
  contactCountMin?: number
  contactCountMax?: number
  sortBy?: CustomerQuerySortBy
  sortOrder?: 'asc' | 'desc'
  ownerId?: string
  page?: number
  limit?: number
}

export type CustomerQuerySortBy =
  | 'createTime'
  | 'quotation'
  | 'contractAmount'
  | 'revenue'
  | 'lastContactTime'
  | 'nextFollowTime'
  | 'contactCount'

export interface CustomerExportBO extends CustomerQueryBO {
  customerIds?: string[]
}

export interface CustomerAiSearchParseBO {
  query: string
}

export interface CustomerAiSearchQuery {
  keyword?: string
  stage?: CustomerStage
  stages?: CustomerStage[]
  level?: CustomerLevel
  industry?: string
  tag?: string
  source?: string
  quotationMin?: number
  quotationMax?: number
  contractAmountMin?: number
  contractAmountMax?: number
  revenueMin?: number
  revenueMax?: number
  lastContactStart?: string
  lastContactEnd?: string
  includeNoLastContact?: boolean
  nextFollowStart?: string
  nextFollowEnd?: string
  createTimeStart?: string
  createTimeEnd?: string
  contactCountMin?: number
  contactCountMax?: number
  sortBy?: CustomerQuerySortBy
  sortOrder?: 'asc' | 'desc'
}

export interface CustomerAiSearchDisplayChip {
  key: string
  label: string
}

export interface CustomerAiSearchParseVO {
  originalQuery: string
  normalizedQuery: string
  parsedQuery: CustomerAiSearchQuery
  displayChips: CustomerAiSearchDisplayChip[]
  explanation?: string
  confidence?: number
  fallbackKeywordSearch?: boolean
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
  customFields?: Record<string, any>
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
  customerId?: string
  relationId?: string
  relationName?: string
  contactId?: string
  contactName?: string
  type: FollowUpType
  content: string
  summary?: string
  sceneType?: string
  aiGenerated?: number
  followTime: string
  result?: string
  nextPlan?: string
  nextFollowTime?: string
  createUserId: string
  createUserName?: string
  createTime: string
  attachments?: FollowUpAttachment[]
}

export interface FollowUpAttachment {
  attachmentId: string
  followUpId?: string
  fileName: string
  filePath: string
  fileSize?: number
  mimeType?: string
  sort?: number
  analysisStatus?: string
  analysisContent?: string
  analysisTime?: string
}

export interface FollowUpAttachmentDraft {
  fileName: string
  filePath: string
  fileSize?: number
  mimeType?: string
}

export type FollowUpType = 'call' | 'meeting' | 'email' | 'visit' | 'other'

export interface FollowUpAddBO {
  customerId?: string
  relationId?: string
  type: string
  content: string
  followTime: string
  contactId?: string
  nextFollowTime?: string
  summary?: string
  sceneType?: string
  aiGenerated?: number
  attachments?: FollowUpAttachmentDraft[]
}

export interface FollowUpUpdateBO {
  followUpId: string
  relationId?: string
  contactId?: string
  type: string
  content: string
  followTime: string
  nextFollowTime?: string
  summary?: string
  sceneType?: string
  aiGenerated?: number
}

export interface FollowUpQueryBO {
  customerId?: string
  relationId?: string
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

// Import/Export types
export interface CustomerImportRow {
  rowNum: number
  companyName: string
  industry: string
  stage: string
  level: string
  source: string
  address: string
  website: string
  quotation: number | null
  remark: string
  contactName: string
  contactPosition: string
  contactPhone: string
  contactEmail: string
  contactWechat: string
  customFields: Record<string, any>
  duplicate: boolean
  existingCustomerId: string | null
  handleMode: 'skip' | 'overwrite' | ''
  errors?: string[]
}

export interface CustomerImportPreview {
  totalRows: number
  validRows: number
  duplicateRows: number
  errorRows: number
  rows: CustomerImportRow[]
  errors: string[]
}

export interface CustomerImportResult {
  imported: number
  updated: number
  skipped: number
  errors: string[]
}
