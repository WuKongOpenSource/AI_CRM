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
  createUserId?: string
  createUserName?: string
  generatedByAi: boolean
  createTime: string
  updateTime: string
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
}

export interface TaskUpdateBO extends TaskAddBO {
  taskId: string
  status?: TaskStatus
}

export interface TaskQueryBO {
  keyword?: string
  status?: TaskStatus
  priority?: TaskPriority
  customerId?: string
  assignedTo?: string
  filter?: 'all' | 'today' | 'thisWeek' | 'overdue' | 'mine'
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
  summary?: string
  contentText?: string
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
}

// Enum types
export interface EnumOption {
  value: string
  label: string
  description?: string
  color?: string
}
