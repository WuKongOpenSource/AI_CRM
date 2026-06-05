import { get, post } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  ProjectCreatePayload,
  ProjectAttachment,
  ProjectEntity,
  ProjectLane,
  ProjectListQuery,
  ProjectMember,
  ProjectMemberPayload,
  ProjectPermission,
  ProjectRole,
  ProjectRolePermissionConfig,
  ProjectRolePermissionConfigPayload,
  ProjectRolePermissionConfigVO,
  ProjectSchedule,
  ProjectTask,
  ProjectTaskPayload,
  ProjectTaskUpdatePayload,
  ProjectUpdatePayload
} from '@/types/project'

const PROJECT_ROLES: ProjectRole[] = ['OWNER', 'ADMIN', 'MEMBER', 'READONLY', 'EXTERNAL']
const PROJECT_ROLE_PERMISSION_PATHS = ['/project/role-permissions', '/project/rolePermissions'] as const

type RawProject = Record<string, any>

function id(value: unknown): string | undefined {
  if (value === null || value === undefined || value === '') return undefined
  return String(value)
}

function date(value: unknown): string | undefined {
  if (value === null || value === undefined || value === '') return undefined
  if (typeof value === 'number') return new Date(value).toISOString()
  return String(value)
}

function list<T>(value: unknown): T[] {
  return Array.isArray(value) ? value as T[] : []
}

function isProjectWelcomeMessage(content: string) {
  return content.startsWith('已进入项目「') && content.includes('上下文') && content.includes('创建任务')
}

function isTaskWelcomeMessage(content: string) {
  return content.startsWith('当前对话对象：任务 - ') && content.includes('修改截止时间') && content.includes('追加备注')
}

export function normalizeProject(raw: RawProject): ProjectEntity {
  return {
    projectId: id(raw.projectId) || '',
    name: String(raw.name || ''),
    description: raw.description || '',
    customerId: id(raw.customerId),
    customerName: raw.customerName || '',
    ownerId: id(raw.ownerId),
    ownerName: raw.ownerName || '',
    startDate: date(raw.startDate),
    dueDate: date(raw.dueDate),
    status: raw.status || 'NOT_STARTED',
    createTime: date(raw.createTime) || new Date().toISOString(),
    updateTime: date(raw.updateTime) || new Date().toISOString(),
    taskCount: Number(raw.taskCount ?? raw.tasks?.length ?? 0),
    incompleteTaskCount: Number(raw.incompleteTaskCount ?? 0),
    currentUserPermissions: list<ProjectPermission>(raw.currentUserPermissions),
    currentUserRole: raw.currentUserRole,
    systemAdmin: Boolean(raw.systemAdmin),
    lanes: list<any>(raw.lanes).map((lane): ProjectLane => ({
      laneId: id(lane.laneId) || '',
      name: lane.name || '',
      order: Number(lane.order ?? 0),
      system: Boolean(lane.system)
    })),
    tasks: list<any>(raw.tasks).map((task): ProjectTask => ({
      taskId: id(task.taskId) || '',
      projectId: id(task.projectId) || id(raw.projectId) || '',
      title: task.title || '',
      description: task.description || '',
      laneId: id(task.laneId) || '',
      status: task.status || '',
      dueDate: date(task.dueDate),
      ownerId: id(task.ownerId),
      ownerName: task.ownerName || '',
      participantIds: list<any>(task.participantIds).map(item => String(item)),
      participantNames: list<string>(task.participantNames),
      priority: task.priority || 'MEDIUM',
      customerId: id(task.customerId),
      customerName: task.customerName || raw.customerName || '',
      hasAttachments: Boolean(task.hasAttachments),
      hasSchedule: Boolean(task.hasSchedule),
      generatedByAi: Boolean(task.generatedByAi),
      source: task.source || (task.generatedByAi ? 'ai' : 'manual'),
      aiSourceText: task.aiSourceText,
      attachments: list<any>(task.attachments).map(item => ({
        attachmentId: id(item.attachmentId) || '',
        name: item.name || '',
        createTime: date(item.createTime) || new Date().toISOString(),
        createdByName: item.createdByName || ''
      })),
      schedules: list<any>(task.schedules).map(item => ({
        scheduleId: id(item.scheduleId) || '',
        title: item.title || '',
        scheduleTime: date(item.scheduleTime),
        createTime: date(item.createTime) || new Date().toISOString(),
        createdByName: item.createdByName || ''
      })),
      notes: list<any>(task.notes).map(item => ({
        noteId: id(item.noteId) || '',
        content: item.content || '',
        createTime: date(item.createTime) || new Date().toISOString(),
        createdByName: item.createdByName || ''
      })),
      chatMessages: list<any>(task.chatMessages)
        .filter(item => !isTaskWelcomeMessage(String(item.content || '')))
        .map(item => ({
          messageId: id(item.messageId) || '',
          role: item.role === 'user' ? 'user' : 'assistant',
          content: item.content || '',
          createTime: date(item.createTime) || new Date().toISOString()
        })),
      createTime: date(task.createTime) || new Date().toISOString(),
      updateTime: date(task.updateTime) || new Date().toISOString()
    })),
    attachments: list<any>(raw.attachments).map((item): ProjectAttachment => ({
      attachmentId: id(item.attachmentId) || '',
      name: item.name || '',
      fileUrl: item.fileUrl || '',
      createTime: date(item.createTime) || new Date().toISOString(),
      createdByName: item.createdByName || ''
    })),
    schedules: list<any>(raw.schedules).map((item): ProjectSchedule => ({
      scheduleId: id(item.scheduleId) || '',
      title: item.title || '',
      scheduleTime: date(item.scheduleTime),
      createTime: date(item.createTime) || new Date().toISOString(),
      createdByName: item.createdByName || ''
    })),
    chatMessages: list<any>(raw.chatMessages)
      .filter(item => !isProjectWelcomeMessage(String(item.content || '')))
      .map(item => ({
        messageId: id(item.messageId) || '',
        role: item.role === 'user' ? 'user' : 'assistant',
        content: item.content || '',
        createTime: date(item.createTime) || new Date().toISOString()
      })),
    members: list<any>(raw.members).map((member): ProjectMember => ({
      memberId: id(member.memberId) || '',
      userId: id(member.userId) || '',
      memberName: member.memberName || '',
      account: member.account || '',
      role: member.role || 'MEMBER',
      deptName: member.deptName || '',
      joinedAt: date(member.joinedAt) || new Date().toISOString(),
      lastActionTime: date(member.lastActionTime) || new Date().toISOString(),
      status: member.status || 'ACTIVE',
      permissions: list<ProjectPermission>(member.permissions),
      remark: member.remark || ''
    })),
    memberLogs: list<any>(raw.memberLogs).map(item => ({
      logId: id(item.logId) || '',
      actionType: item.actionType,
      operatorId: id(item.operatorId) || '',
      operatorName: item.operatorName || '',
      targetUserId: id(item.targetUserId) || '',
      targetUserName: item.targetUserName || '',
      beforeSummary: item.beforeSummary,
      afterSummary: item.afterSummary,
      createTime: date(item.createTime) || new Date().toISOString()
    }))
  }
}

function projectPayload(payload: ProjectCreatePayload | ProjectUpdatePayload) {
  return {
    ...payload,
    customerId: payload.customerId || undefined,
    ownerId: payload.ownerId || undefined
  }
}

function taskPayload(payload: ProjectTaskPayload | ProjectTaskUpdatePayload) {
  return {
    ...payload,
    taskId: 'taskId' in payload && payload.taskId ? payload.taskId : undefined,
    laneId: payload.laneId || undefined,
    ownerId: payload.ownerId || undefined,
    customerId: payload.customerId || undefined,
    participantIds: payload.participantIds
  }
}

function memberPayload(payload: ProjectMemberPayload) {
  return {
    ...payload,
    userId: payload.userId
  }
}

async function unwrapProject(request: Promise<RawProject>): Promise<ProjectEntity> {
  return normalizeProject(await request)
}

export async function queryProjectList(): Promise<ProjectEntity[]> {
  const projects = await get<RawProject[]>('/project/list')
  return list<RawProject>(projects).map(normalizeProject)
}

export async function queryProjectPageList(query: ProjectListQuery = {}): Promise<PageResult<ProjectEntity>> {
  const result = await post<PageResult<RawProject>>('/project/queryPageList', {
    page: query.page || 1,
    limit: query.limit || 10,
    keyword: query.keyword?.trim() || undefined,
    status: query.status && query.status !== 'all' ? query.status : undefined
  })
  return {
    ...result,
    list: list<RawProject>(result.list).map(normalizeProject)
  }
}

export function getProjectDetail(projectId: string, taskKeyword?: string): Promise<ProjectEntity> {
  const keyword = taskKeyword?.trim()
  return unwrapProject(get(`/project/detail/${projectId}`, keyword ? { params: { taskKeyword: keyword } } : undefined))
}

export function createProject(payload: ProjectCreatePayload): Promise<ProjectEntity> {
  return unwrapProject(post('/project/add', projectPayload(payload)))
}

export function updateProject(payload: ProjectUpdatePayload): Promise<ProjectEntity> {
  return unwrapProject(post('/project/update', {
    ...projectPayload(payload),
    projectId: payload.projectId
  }))
}

export function archiveProject(projectId: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/archive/${projectId}`))
}

export function deleteProject(projectId: string): Promise<void> {
  return post(`/project/delete/${projectId}`)
}

function normalizeRolePermissionConfig(raw: any): ProjectRolePermissionConfig {
  const source = raw?.rolePermissions && typeof raw.rolePermissions === 'object' ? raw.rolePermissions : {}
  return PROJECT_ROLES.reduce((config, role) => {
    config[role] = list<ProjectPermission>(source[role])
    return config
  }, {} as ProjectRolePermissionConfig)
}

export async function getProjectRolePermissionConfig(): Promise<ProjectRolePermissionConfigVO> {
  const raw = await requestProjectRolePermissionConfig(path => get(path, { silentError: true }))
  return {
    rolePermissions: normalizeRolePermissionConfig(raw)
  }
}

export async function updateProjectRolePermissionConfig(
  payload: ProjectRolePermissionConfigPayload
): Promise<ProjectRolePermissionConfigVO> {
  const raw = await requestProjectRolePermissionConfig(path => post(path, payload, { silentError: true }))
  return {
    rolePermissions: normalizeRolePermissionConfig(raw)
  }
}

async function requestProjectRolePermissionConfig<T>(
  request: (path: string) => Promise<T>
): Promise<T> {
  let lastError: unknown
  for (const path of PROJECT_ROLE_PERMISSION_PATHS) {
    try {
      return await request(path)
    } catch (error) {
      lastError = error
    }
  }
  throw lastError
}

export function addLane(projectId: string, name: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/lane/add`, { name }))
}

export function updateLane(projectId: string, laneId: string, name: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/lane/update`, { laneId, name }))
}

export function deleteLane(projectId: string, laneId: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/lane/delete/${laneId}`))
}

export function createProjectTask(projectId: string, payload: ProjectTaskPayload): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/task/add`, taskPayload(payload)))
}

export function updateProjectTask(projectId: string, payload: ProjectTaskUpdatePayload): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/task/update`, taskPayload(payload)))
}

export function deleteProjectTask(projectId: string, taskId: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/task/delete/${taskId}`))
}

export function moveProjectTask(projectId: string, taskId: string, laneId: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/task/move`, { taskId, laneId }))
}

export function addProjectMember(projectId: string, payload: ProjectMemberPayload): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/member/add`, memberPayload(payload)))
}

export function updateProjectMemberRole(projectId: string, userId: string, role: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/member/role`, { userId, role }))
}

export function updateProjectMemberPermissions(projectId: string, userId: string, permissions: ProjectPermission[]): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/member/permissions`, { userId, permissions }))
}

export function updateProjectMemberStatus(projectId: string, userId: string, status: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/member/status`, { userId, status }))
}

export function sendProjectAiCommand(projectId: string, content: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/ai-command`, { content }))
}

export function sendTaskAiCommand(projectId: string, taskId: string, content: string): Promise<ProjectEntity> {
  return unwrapProject(post(`/project/${projectId}/task/${taskId}/ai-command`, { content }))
}
