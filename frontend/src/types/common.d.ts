// Task types
export interface Task {
  taskId: string
  title: string
  description?: string
  customerId?: string
  customerName?: string
  relationId?: string
  relationName?: string
  projectId?: string
  projectName?: string
  laneId?: string
  laneName?: string
  priority: TaskPriority
  priorityName?: string
  status: TaskStatus
  statusName?: string
  dueDate?: string
  overdue?: boolean
  assignedTo?: string
  assignedToName?: string
  taskType?: string
  participantNames?: string
  createUserId?: string
  createUserName?: string
  generatedByAi: boolean
  createTime: string
  updateTime: string
  valuePriorityScore?: number
  valuePriorityTier?: 'HIGH' | 'MEDIUM' | 'LOW'
  valuePriorityReason?: string
  highValue?: boolean
}

export type TaskPriority = 'HIGH' | 'MEDIUM' | 'LOW'
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'

export interface TaskAddBO {
  title: string
  description?: string
  customerId?: string
  priority?: TaskPriority
  relationId?: string
  projectId?: string
  laneId?: string
  dueDate?: string
  assignedTo?: string
  taskType?: string
  participantNames?: string
}

export interface TaskUpdateBO extends TaskAddBO {
  taskId: string
  status?: TaskStatus
}

export interface TaskQueryBO {
  taskId?: string
  keyword?: string
  status?: TaskStatus
  priority?: TaskPriority
  customerId?: string
  assignedTo?: string
  relationId?: string
  projectId?: string
  laneId?: string
  filter?: 'all' | 'today' | 'thisWeek' | 'overdue' | 'mine'
  sortMode?: 'default' | 'value'
  highValueOnly?: boolean
  page?: number
  limit?: number
}

// Knowledge types
export interface Knowledge {
  knowledgeId: string
  name: string
  type: KnowledgeType
  customerId?: string
  customerName?: string
  filePath?: string
  fileSize?: number
  fileSizeFormatted?: string
  mimeType?: string
  summary?: string
  contentText?: string
  aiAnalyzeResult?: KnowledgeAiAnalyzeVO
  aiAnalysisTime?: string
  typeName?: string
  weKnoraParseStatus?: 'pending' | 'processing' | 'completed' | 'failed' | 'unsupported'
  createUserId: string
  createUserName?: string
  createTime: string
}

export type KnowledgeType = 'meeting' | 'email' | 'recording' | 'document' | 'proposal' | 'contract'
export type KnowledgeFileType = 'image' | 'document' | 'spreadsheet' | 'presentation' | 'pdf' | 'audio' | 'video'

export interface KnowledgeAddBO {
  name: string
  type: KnowledgeType
  customerId?: string
  summary?: string
}

export interface KnowledgeQueryBO {
  keyword?: string
  type?: KnowledgeType
  fileType?: KnowledgeFileType
  customerId?: string
  page?: number
  limit?: number
}

export interface KnowledgeAiSearchBO {
  keyword: string
  type?: KnowledgeType
  limit?: number
}

export interface KnowledgeAiSearchReferenceItem {
  knowledgeId: string
  name: string
  type?: KnowledgeType | string
  customerName?: string
  summary?: string
  excerpt?: string
  matchPercent?: number
  fileSize?: number
  createTime?: string
}

export interface KnowledgeAiSearchVO {
  keyword: string
  answer: string
  tookMs: number
  matchPercent: number
  totalHits: number
  references: KnowledgeAiSearchReferenceItem[]
}

export interface KnowledgePreviewTokenVO {
  url: string
  expiresAt: string
  expiresInSeconds: number
}

export interface KnowledgeTargetedScriptBO {
  knowledgeIds: string[]
  customerId: string
}

export interface KnowledgeAiAnalyzeVO {
  coreHighlights: string
  talkingPoints: string[]
  relatedEntities: Array<{
    name: string
    type: string
  }>
}

// AI Agent types
export interface AiAgent {
  agentId: string
  label: string
  iconName?: string
  prompt: string
  persona?: string
  knowledgeBaseTypes?: string[]
  enabled: boolean | number
  sortOrder?: number
  category?: 'default' | 'custom'
  createTime?: string
}

export interface AiAgentAddBO {
  label: string
  iconName?: string
  prompt: string
  persona?: string
  knowledgeBaseTypes?: string[]
  enabled?: boolean
  sortOrder?: number
}

export interface AiAgentUpdateBO {
  agentId: string
  label?: string
  iconName?: string
  prompt?: string
  persona?: string
  knowledgeBaseTypes?: string[]
  enabled?: boolean
  sortOrder?: number
}

// Chat types
export interface ChatSession {
  sessionId: string
  title?: string
  agentId?: string
  customerId?: string
  customerName?: string
  customerLogoUrl?: string
  employeeId?: string
  employeeName?: string
  employeeAvatarUrl?: string
  relationId?: string
  relationName?: string
  relationAvatarUrl?: string
  productId?: string
  productName?: string
  productCode?: string
  productImageUrl?: string
  projectId?: string
  projectName?: string
  projectTaskId?: string
  projectTaskTitle?: string
  appCode?: string
  pinned?: boolean
  pinnedTime?: string
  createTime: string
  updateTime: string
}

export interface ChatAppOption {
  code: string
  label: string
  iconName?: string
  description?: string
  defaultRagEnabled?: boolean
  recommendedQuestions?: string[]
}

export interface ChatModelOption {
  provider: string
  providerLabel?: string
  modelName: string
  displayName?: string
  modelSource?: 'custom' | 'system' | string
  icon?: string
  capabilities?: {
    supportsStream?: boolean
    supportsToolCall?: boolean
    supportsVision?: boolean
    supportsAudioTranscription?: boolean
  }
}

export interface ChatMessage {
  messageId: string
  sessionId: string
  role: 'user' | 'assistant' | 'system'
  content: string
  functionCall?: string
  functionResult?: string
  tokensUsed?: number
  tokens?: number
  modelName?: string
  attachments?: ChatAttachmentVO[]
  createTime: string
}

export interface ChatAttachmentVO {
  id: string
  fileName: string
  filePath: string
  fileSize: number
  mimeType: string
  accessUrl: string
}

export interface ChatAttachmentDTO {
  fileName: string
  filePath: string
  fileSize: number
  mimeType: string
}

export interface ChatSendBO {
  sessionId: string
  content: string
  attachments?: ChatAttachmentDTO[]
  ragEnabled?: boolean
  appCode?: string
  customerId?: string
  employeeId?: string
  relationId?: string
  productId?: string
  projectId?: string
  projectTaskId?: string
  knowledgeIds?: string[]
  modelProvider?: string
  modelName?: string
  modelSource?: string
}

// Enum types
export interface EnumOption {
  value: string
  label: string
  description?: string
  color?: string
}
