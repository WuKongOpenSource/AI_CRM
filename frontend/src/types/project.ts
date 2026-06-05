export type ProjectStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'PAUSED' | 'ARCHIVED'

export type ProjectTaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type ProjectTaskSource = 'manual' | 'ai'

export type ProjectViewMode = 'ai' | 'board' | 'list' | 'cards' | 'members' | 'task_ai'

export type ProjectListViewMode = 'card' | 'table'

export type ProjectListStatusFilter = 'all' | 'IN_PROGRESS' | 'COMPLETED'

export interface ProjectListStats {
  all: number
  inProgress: number
  completed: number
}

export interface ProjectListQuery {
  keyword?: string
  status?: ProjectListStatusFilter
  page?: number
  limit?: number
}

export type ProjectRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'READONLY' | 'EXTERNAL'

export type ProjectMemberStatus = 'ACTIVE' | 'REMOVED' | 'DISABLED'

export type ProjectPermission =
  | 'VIEW_PROJECT'
  | 'EDIT_PROJECT'
  | 'DELETE_PROJECT'
  | 'ARCHIVE_PROJECT'
  | 'ADD_MEMBER'
  | 'REMOVE_MEMBER'
  | 'MODIFY_MEMBER_PERMISSION'
  | 'CREATE_TASK'
  | 'EDIT_TASK'
  | 'DELETE_TASK'
  | 'MOVE_TASK'
  | 'ADD_LANE'
  | 'EDIT_LANE'
  | 'DELETE_LANE'
  | 'USE_AI_CHAT'
  | 'AI_CREATE_TASK'
  | 'UPLOAD_ATTACHMENT'
  | 'DELETE_ATTACHMENT'
  | 'CREATE_SCHEDULE'
  | 'VIEW_STATISTICS'

export type ProjectMemberActionType =
  | 'ADD_MEMBER'
  | 'REMOVE_MEMBER'
  | 'UPDATE_ROLE'
  | 'UPDATE_PERMISSION'
  | 'UPDATE_STATUS'

export interface ProjectLane {
  laneId: string
  name: string
  order: number
  system?: boolean
}

export interface ProjectTaskAttachment {
  attachmentId: string
  name: string
  createTime: string
  createdByName?: string
}

export interface ProjectTaskSchedule {
  scheduleId: string
  title: string
  scheduleTime?: string
  createTime: string
  createdByName?: string
}

export interface ProjectTaskNote {
  noteId: string
  content: string
  createTime: string
  createdByName?: string
}

export interface ProjectTaskChatMessage {
  messageId: string
  role: 'user' | 'assistant'
  content: string
  createTime: string
}

export interface ProjectTask {
  taskId: string
  projectId: string
  title: string
  description?: string
  laneId: string
  status: string
  dueDate?: string
  ownerId?: string
  ownerName?: string
  participantIds?: string[]
  participantNames?: string[]
  priority: ProjectTaskPriority
  customerId?: string
  customerName?: string
  hasAttachments: boolean
  hasSchedule: boolean
  generatedByAi: boolean
  source: ProjectTaskSource
  aiSourceText?: string
  attachments: ProjectTaskAttachment[]
  schedules: ProjectTaskSchedule[]
  notes: ProjectTaskNote[]
  chatMessages: ProjectTaskChatMessage[]
  createTime: string
  updateTime: string
}

export interface ProjectChatMessage {
  messageId: string
  role: 'user' | 'assistant'
  content: string
  createTime: string
}

export interface ProjectAttachment {
  attachmentId: string
  name: string
  fileUrl?: string
  createTime: string
  createdByName?: string
}

export interface ProjectSchedule {
  scheduleId: string
  title: string
  scheduleTime?: string
  createTime: string
  createdByName?: string
}

export interface ProjectMember {
  memberId: string
  userId: string
  memberName: string
  account: string
  role: ProjectRole
  deptName?: string
  joinedAt: string
  lastActionTime: string
  status: ProjectMemberStatus
  permissions: ProjectPermission[]
  remark?: string
}

export interface ProjectMemberLog {
  logId: string
  actionType: ProjectMemberActionType
  operatorId: string
  operatorName: string
  targetUserId: string
  targetUserName: string
  beforeSummary?: string
  afterSummary?: string
  createTime: string
}

export interface ProjectEntity {
  projectId: string
  name: string
  description?: string
  customerId?: string
  customerName?: string
  ownerId?: string
  ownerName?: string
  startDate?: string
  dueDate?: string
  status: ProjectStatus
  createTime: string
  updateTime: string
  taskCount?: number
  incompleteTaskCount?: number
  currentUserPermissions?: ProjectPermission[]
  currentUserRole?: ProjectRole
  systemAdmin?: boolean
  lanes: ProjectLane[]
  tasks: ProjectTask[]
  attachments: ProjectAttachment[]
  schedules: ProjectSchedule[]
  chatMessages: ProjectChatMessage[]
  members: ProjectMember[]
  memberLogs: ProjectMemberLog[]
}

export interface ProjectCreatePayload {
  name: string
  description?: string
  customerId?: string
  customerName?: string
  ownerId?: string
  ownerName?: string
  ownerAccount?: string
  ownerDeptName?: string
  startDate?: string
  dueDate?: string
  status?: ProjectStatus
}

export interface ProjectUpdatePayload extends Partial<ProjectCreatePayload> {
  projectId: string
}

export interface ProjectTaskPayload {
  title: string
  description?: string
  dueDate?: string
  ownerId?: string
  ownerName?: string
  participantIds?: string[]
  participantNames?: string[]
  priority?: ProjectTaskPriority
  customerId?: string
  customerName?: string
  laneId?: string
  hasAttachments?: boolean
  hasSchedule?: boolean
  generatedByAi?: boolean
  aiSourceText?: string
}

export interface ProjectTaskUpdatePayload extends Partial<ProjectTaskPayload> {
  taskId: string
}

export interface ProjectMemberPayload {
  userId: string
  memberName: string
  account: string
  role: ProjectRole
  deptName?: string
  permissions?: ProjectPermission[]
  remark?: string
  status?: ProjectMemberStatus
}
