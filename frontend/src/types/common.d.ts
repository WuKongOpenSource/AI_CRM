// Task types
export interface Task {
  taskId: string
  title: string
  description?: string
  customerId?: string
  customerName?: string
  priority: TaskPriority
  status: TaskStatus
  dueDate?: string
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
  /** 文件大小，单位：字节 (B)，来自知识库列表/详情等接口 */
  fileSize?: number | string
  /** 后端可选返回的已格式化大小（若存在可优先展示） */
  fileSizeFormatted?: string
  mimeType?: string
  summary?: string
  contentText?: string
  aiAnalyzeResult?: KnowledgeAiAnalyzeVO
  aiAnalysisTime?: string
  weKnoraParseStatus?: 'pending' | 'processing' | 'completed' | 'failed' | 'unsupported'
  createUserId: string
  createUserName?: string
  createTime: string
}

export type KnowledgeType = 'MEETING' | 'EMAIL' | 'RECORDING' | 'DOCUMENT' | 'PROPOSAL' | 'CONTRACT'

export interface KnowledgeAddBO {
  name: string
  type: KnowledgeType
  customerId?: string
  summary?: string
}

export interface KnowledgeQueryBO {
  keyword?: string
  type?: KnowledgeType
  customerId?: string
  page?: number
  limit?: number
}

export interface KnowledgeAiAnalyzeVO {
  coreHighlights: string
  talkingPoints: string[]
  relatedEntities: Array<{
    name: string
    type: string
  }>
}

export interface KnowledgeAiSearchVO {
  keyword: string
  answer: string
  tookMs: number
  matchPercent: number
  totalHits: number
  references: Array<{
    knowledgeId: string
    name: string
    type: string
    customerName?: string
    summary?: string
    excerpt?: string
    matchPercent: number
    fileSize?: number
    createTime?: string
  }>
}

export interface KnowledgeAiSearchBO {
  keyword: string
  type?: KnowledgeType
  limit?: number
}

export interface KnowledgeTargetedScriptBO {
  knowledgeIds: string[]
  customerId: string
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
  createTime: string
  updateTime: string
}

export interface ChatMessage {
  messageId: string
  sessionId: string
  role: 'user' | 'assistant' | 'system'
  content: string
  functionCall?: string
  functionResult?: string
  tokens?: number
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
  /** 知识库文档 ID，服务端按 ID 限定 RAG，不经过文件下载上传 */
  knowledgeIds?: string[]
}

// Enum types
export interface EnumOption {
  value: string
  label: string
  description?: string
  color?: string
}
