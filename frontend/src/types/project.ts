import type { PageResult } from '@/types/api'

export type ProjectStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'PAUSED' | 'ARCHIVED'
export type ProjectTaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export interface ProjectLaneVO {
  laneId: string
  name: string
  code?: string
  sortOrder?: number
  system?: boolean
}

export interface ProjectTaskVO {
  taskId: string
  projectId: string
  laneId?: string
  title: string
  description?: string
  status?: string
  dueDate?: string
  ownerId?: string
  ownerName?: string
  priority?: ProjectTaskPriority | string
  customerId?: string
  customerName?: string
  generatedByAi?: boolean
  aiSourceText?: string
  createTime?: string
  updateTime?: string
}

export interface ProjectVO {
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
  createTime?: string
  updateTime?: string
  taskCount?: number
  incompleteTaskCount?: number
  lanes?: ProjectLaneVO[]
  tasks?: ProjectTaskVO[]
}

export interface ProjectQuery {
  keyword?: string
  status?: ProjectStatus | ''
  page?: number
  limit?: number
}

export interface ProjectCreate {
  name: string
  description?: string
  customerId?: string
  customerName?: string
  ownerId?: string
  startDate?: string
  dueDate?: string
  status?: ProjectStatus
}

export interface ProjectUpdate extends ProjectCreate {
  projectId: string
}

export interface ProjectLaneSave {
  laneId?: string
  name: string
}

export interface ProjectTaskSave {
  taskId?: string
  title: string
  description?: string
  laneId?: string
  dueDate?: string
  ownerId?: string
  ownerName?: string
  priority?: ProjectTaskPriority | string
  customerId?: string
  customerName?: string
  generatedByAi?: boolean
  aiSourceText?: string
}

export interface ProjectTaskMove {
  taskId: string
  laneId: string
}

export type ProjectPageResult = PageResult<ProjectVO>
