import type {
  ProjectEntity,
  ProjectLane,
  ProjectMember,
  ProjectMemberStatus,
  ProjectPermission,
  ProjectRolePermissionConfig,
  ProjectRole,
  ProjectStatus,
  ProjectTask,
  ProjectTaskPriority
} from '@/types/project'

export const DEFAULT_PROJECT_LANES: ProjectLane[] = [
  { laneId: 'not-started', name: '未开始', order: 0, system: true },
  { laneId: 'in-progress', name: '进行中', order: 1, system: true },
  { laneId: 'completed', name: '已完成', order: 2, system: true }
]

export const PROJECT_STATUS_OPTIONS: Array<{ value: ProjectStatus; label: string }> = [
  { value: 'NOT_STARTED', label: '未开始' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'PAUSED', label: '已暂停' },
  { value: 'ARCHIVED', label: '已归档' }
]

export const PROJECT_TASK_PRIORITY_OPTIONS: Array<{ value: ProjectTaskPriority; label: string }> = [
  { value: 'LOW', label: '低' },
  { value: 'MEDIUM', label: '中' },
  { value: 'HIGH', label: '高' },
  { value: 'URGENT', label: '紧急' }
]

export const PROJECT_ROLE_OPTIONS: Array<{ value: ProjectRole; label: string }> = [
  { value: 'OWNER', label: '项目负责人' },
  { value: 'ADMIN', label: '项目管理员' },
  { value: 'MEMBER', label: '项目成员' },
  { value: 'READONLY', label: '只读成员' },
  { value: 'EXTERNAL', label: '外部协作人' }
]

export const PROJECT_MEMBER_STATUS_OPTIONS: Array<{ value: ProjectMemberStatus; label: string }> = [
  { value: 'ACTIVE', label: '正常' },
  { value: 'REMOVED', label: '已移除' },
  { value: 'DISABLED', label: '已停用' }
]

export const PROJECT_PERMISSION_OPTIONS: Array<{ value: ProjectPermission; label: string }> = [
  { value: 'VIEW_PROJECT', label: '查看项目' },
  { value: 'EDIT_PROJECT', label: '编辑项目信息' },
  { value: 'DELETE_PROJECT', label: '删除项目' },
  { value: 'ARCHIVE_PROJECT', label: '归档项目' },
  { value: 'ADD_MEMBER', label: '添加项目成员' },
  { value: 'REMOVE_MEMBER', label: '移除项目成员' },
  { value: 'MODIFY_MEMBER_PERMISSION', label: '修改成员权限' },
  { value: 'CREATE_TASK', label: '创建任务' },
  { value: 'EDIT_TASK', label: '编辑任务' },
  { value: 'DELETE_TASK', label: '删除任务' },
  { value: 'MOVE_TASK', label: '拖动任务状态' },
  { value: 'ADD_LANE', label: '新增泳道' },
  { value: 'EDIT_LANE', label: '编辑泳道' },
  { value: 'DELETE_LANE', label: '删除泳道' },
  { value: 'USE_AI_CHAT', label: '使用项目 AI 对话' },
  { value: 'AI_CREATE_TASK', label: '通过 AI 创建任务' },
  { value: 'UPLOAD_ATTACHMENT', label: '上传附件' },
  { value: 'DELETE_ATTACHMENT', label: '删除附件' },
  { value: 'CREATE_SCHEDULE', label: '创建日程' },
  { value: 'VIEW_STATISTICS', label: '查看项目统计' }
]

export const DEFAULT_ROLE_PERMISSIONS: Record<ProjectRole, ProjectPermission[]> = {
  OWNER: PROJECT_PERMISSION_OPTIONS.map(item => item.value),
  ADMIN: [
    'VIEW_PROJECT',
    'EDIT_PROJECT',
    'ARCHIVE_PROJECT',
    'ADD_MEMBER',
    'REMOVE_MEMBER',
    'MODIFY_MEMBER_PERMISSION',
    'CREATE_TASK',
    'EDIT_TASK',
    'DELETE_TASK',
    'MOVE_TASK',
    'ADD_LANE',
    'EDIT_LANE',
    'DELETE_LANE',
    'USE_AI_CHAT',
    'AI_CREATE_TASK',
    'UPLOAD_ATTACHMENT',
    'DELETE_ATTACHMENT',
    'CREATE_SCHEDULE',
    'VIEW_STATISTICS'
  ],
  MEMBER: [
    'VIEW_PROJECT',
    'CREATE_TASK',
    'EDIT_TASK',
    'MOVE_TASK',
    'USE_AI_CHAT',
    'AI_CREATE_TASK',
    'UPLOAD_ATTACHMENT',
    'CREATE_SCHEDULE',
    'VIEW_STATISTICS'
  ],
  READONLY: [
    'VIEW_PROJECT',
    'VIEW_STATISTICS'
  ],
  EXTERNAL: [
    'VIEW_PROJECT',
    'UPLOAD_ATTACHMENT'
  ]
}

let runtimeRolePermissions: ProjectRolePermissionConfig = cloneRolePermissions(DEFAULT_ROLE_PERMISSIONS)

const CHINESE_DIGIT_MAP: Record<string, number> = {
  零: 0,
  一: 1,
  二: 2,
  两: 2,
  三: 3,
  四: 4,
  五: 5,
  六: 6,
  七: 7,
  八: 8,
  九: 9,
  十: 10
}

export function projectStatusLabel(status: ProjectStatus): string {
  return PROJECT_STATUS_OPTIONS.find(option => option.value === status)?.label || '未开始'
}

export function projectStatusClass(status: ProjectStatus): string {
  switch (status) {
    case 'IN_PROGRESS':
      return 'bg-blue-50 text-blue-600'
    case 'COMPLETED':
      return 'bg-emerald-50 text-emerald-600'
    case 'PAUSED':
      return 'bg-amber-50 text-amber-600'
    case 'ARCHIVED':
      return 'bg-slate-200 text-slate-500'
    default:
      return 'bg-slate-100 text-slate-600'
  }
}

export function projectTaskPriorityLabel(priority: ProjectTaskPriority): string {
  return PROJECT_TASK_PRIORITY_OPTIONS.find(option => option.value === priority)?.label || '中'
}

export function projectTaskPriorityClass(priority: ProjectTaskPriority): string {
  switch (priority) {
    case 'URGENT':
      return 'bg-red-50 text-red-600'
    case 'HIGH':
      return 'bg-rose-50 text-rose-600'
    case 'MEDIUM':
      return 'bg-amber-50 text-amber-600'
    default:
      return 'bg-slate-100 text-slate-600'
  }
}

export function projectRoleLabel(role: ProjectRole): string {
  return PROJECT_ROLE_OPTIONS.find(option => option.value === role)?.label || '项目成员'
}

export function projectMemberStatusLabel(status: ProjectMemberStatus): string {
  return PROJECT_MEMBER_STATUS_OPTIONS.find(option => option.value === status)?.label || '正常'
}

export function projectMemberStatusClass(status: ProjectMemberStatus): string {
  switch (status) {
    case 'REMOVED':
      return 'bg-red-50 text-red-600'
    case 'DISABLED':
      return 'bg-amber-50 text-amber-600'
    default:
      return 'bg-emerald-50 text-emerald-600'
  }
}

export function permissionLabel(permission: ProjectPermission): string {
  return PROJECT_PERMISSION_OPTIONS.find(option => option.value === permission)?.label || permission
}

export function roleDefaultPermissions(role: ProjectRole): ProjectPermission[] {
  return [...runtimeRolePermissions[role]]
}

export function getProjectRolePermissions(): ProjectRolePermissionConfig {
  return cloneRolePermissions(runtimeRolePermissions)
}

export function setProjectRolePermissions(config?: Partial<Record<ProjectRole, ProjectPermission[]>>) {
  runtimeRolePermissions = normalizeRolePermissions(config)
}

export function resetProjectRolePermissions() {
  runtimeRolePermissions = cloneRolePermissions(DEFAULT_ROLE_PERMISSIONS)
}

function normalizeRolePermissions(config?: Partial<Record<ProjectRole, ProjectPermission[]>>): ProjectRolePermissionConfig {
  const allowedPermissions = new Set(PROJECT_PERMISSION_OPTIONS.map(item => item.value))
  const next = cloneRolePermissions(DEFAULT_ROLE_PERMISSIONS)
  PROJECT_ROLE_OPTIONS.forEach(roleOption => {
    const permissions = config?.[roleOption.value]
    if (!permissions) return
    next[roleOption.value] = Array.from(new Set(permissions.filter(permission => allowedPermissions.has(permission))))
    if (!next[roleOption.value].includes('VIEW_PROJECT')) {
      next[roleOption.value].unshift('VIEW_PROJECT')
    }
    if (roleOption.value === 'OWNER') {
      next.OWNER = [...DEFAULT_ROLE_PERMISSIONS.OWNER]
    }
  })
  return next
}

function cloneRolePermissions(config: Record<ProjectRole, ProjectPermission[]>): ProjectRolePermissionConfig {
  return PROJECT_ROLE_OPTIONS.reduce((result, roleOption) => {
    result[roleOption.value] = [...(config[roleOption.value] || [])]
    return result
  }, {} as ProjectRolePermissionConfig)
}

export function getDefaultLaneId(project: ProjectEntity): string {
  return project.lanes[0]?.laneId || DEFAULT_PROJECT_LANES[0].laneId
}

export function getCompletedLaneIds(project: ProjectEntity): string[] {
  return project.lanes.filter(lane => lane.name === '已完成').map(lane => lane.laneId)
}

export function countIncompleteTasks(project: ProjectEntity): number {
  const completedLaneIds = new Set(getCompletedLaneIds(project))
  return project.tasks.filter(task => !completedLaneIds.has(task.laneId)).length
}

export function formatDateTime(value?: string): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day} ${hour}:${minute}`
}

export function formatDate(value?: string): string {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

export function getRelativeDueLabel(value?: string): string {
  if (!value) return '未设置时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(date)
  target.setHours(0, 0, 0, 0)
  const diff = Math.round((target.getTime() - today.getTime()) / 86400000)
  if (diff < 0) return `已逾期 ${Math.abs(diff)} 天`
  if (diff === 0) return '今天到期'
  if (diff === 1) return '明天到期'
  return `${diff} 天后到期`
}

export function isTaskOverdue(task: ProjectTask): boolean {
  if (!task.dueDate) return false
  return new Date(task.dueDate).getTime() < Date.now() && task.status !== '已完成'
}

export function resolveLaneName(project: ProjectEntity, laneId: string): string {
  return project.lanes.find(lane => lane.laneId === laneId)?.name || '未开始'
}

export function findLaneIdByKeyword(project: ProjectEntity, content: string): string | undefined {
  const normalized = content.trim()
  const directMatch = project.lanes.find(lane => normalized.includes(lane.name))
  if (directMatch) return directMatch.laneId

  if (/未开始|待办|待处理/.test(normalized)) {
    return project.lanes.find(lane => lane.name === '未开始')?.laneId
  }
  if (/进行中|处理中|开始执行|推进/.test(normalized)) {
    return project.lanes.find(lane => lane.name === '进行中')?.laneId
  }
  if (/已完成|完成了|做完|关闭/.test(normalized)) {
    return project.lanes.find(lane => lane.name === '已完成')?.laneId
  }
  return undefined
}

export function createId(prefix: string): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `${prefix}-${crypto.randomUUID()}`
  }
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export function cloneDefaultLanes(): ProjectLane[] {
  return DEFAULT_PROJECT_LANES.map(lane => ({ ...lane }))
}

export function parseProjectAiTaskInput(content: string): {
  title: string
  dueDate?: string
  customerName?: string
  suggestedProjectDescription?: string
} {
  const normalized = content.replace(/\s+/g, ' ').trim()
  const taskTitle = extractTaskTitle(normalized) || '待补充任务'
  const dueDate = parseRelativeDateTime(normalized)
  const customerName = extractCustomerName(normalized)

  return {
    title: taskTitle,
    dueDate,
    customerName,
    suggestedProjectDescription: normalized
  }
}

export function summarizeProject(project: ProjectEntity): string {
  const totalTasks = project.tasks.length
  const incompleteTasks = countIncompleteTasks(project)
  const lanes = project.lanes
    .map(lane => `${lane.name}${project.tasks.filter(task => task.laneId === lane.laneId).length}个`)
    .join('，')
  return `项目“${project.name}”当前状态为${projectStatusLabel(project.status)}，共 ${totalTasks} 个任务，未完成 ${incompleteTasks} 个。当前泳道分布：${lanes}。`
}

export function summarizeTask(task: ProjectTask, project?: ProjectEntity): string {
  const noteCount = task.notes.length
  const attachmentCount = task.attachments.length
  const scheduleCount = task.schedules.length
  return `任务“${task.title}”当前位于“${task.status}”${project ? `，所属项目“${project.name}”` : ''}，优先级${projectTaskPriorityLabel(task.priority)}，截止时间${formatDateTime(task.dueDate)}，负责人${task.ownerName || '未分配'}，已有备注 ${noteCount} 条、附件 ${attachmentCount} 个、日程 ${scheduleCount} 个。`
}

export function buildTaskExecutionPlan(task: ProjectTask): string {
  const dueLabel = task.dueDate ? `建议在 ${formatDateTime(task.dueDate)} 前完成` : '建议先补充明确的截止时间'
  return [
    `任务“${task.title}”的执行建议如下：`,
    `1. 先明确交付物和验收标准。${task.description ? `当前任务说明：${task.description}` : '当前还没有详细描述，可先补充范围。'}`,
    `2. 拆分准备、执行、复盘三个阶段，并分别设置子节点。`,
    `3. ${dueLabel}，必要时提前半天准备汇报材料或确认依赖。`,
    `4. 关键沟通对象包括负责人${task.ownerName ? `“${task.ownerName}”` : ''}${task.customerName ? `与客户“${task.customerName}”` : ''}。`,
    `5. 如果涉及附件、文档或会议，建议同步挂载到任务下，方便后续追踪。`
  ].join('\n')
}

export function canAccessProject(project: ProjectEntity, currentUserId: string, isAdmin: boolean): boolean {
  if (isAdmin) return true
  if (!currentUserId) return false
  return project.members.some(member => member.userId === currentUserId && member.status === 'ACTIVE')
}

export function findProjectMember(project: ProjectEntity, userId: string): ProjectMember | null {
  return project.members.find(member => member.userId === userId) || null
}

export function hasProjectPermission(project: ProjectEntity, userId: string, permission: ProjectPermission, isAdmin: boolean): boolean {
  if (isAdmin) return true
  const member = findProjectMember(project, userId)
  if (!member || member.status !== 'ACTIVE') return false
  return member.permissions.includes(permission)
}

export function canViewTask(project: ProjectEntity, task: ProjectTask, userId: string, isAdmin: boolean): boolean {
  if (isAdmin) return true
  const member = findProjectMember(project, userId)
  if (!member || member.status !== 'ACTIVE') return false
  if (member.role === 'EXTERNAL') {
    return task.ownerId === userId || Boolean(task.participantIds?.includes(userId))
  }
  if (member.permissions.includes('VIEW_PROJECT')) return true
  return task.ownerId === userId || Boolean(task.participantIds?.includes(userId))
}

export function canEditTask(project: ProjectEntity, task: ProjectTask, userId: string, isAdmin: boolean): boolean {
  if (isAdmin) return true
  const member = findProjectMember(project, userId)
  if (!member || member.status !== 'ACTIVE') return false
  if (member.role === 'OWNER' || member.role === 'ADMIN') return member.permissions.includes('EDIT_TASK')
  if (task.ownerId === userId) return member.permissions.includes('EDIT_TASK')
  return false
}

export function canMoveTask(project: ProjectEntity, task: ProjectTask, userId: string, isAdmin: boolean): boolean {
  if (isAdmin) return true
  const member = findProjectMember(project, userId)
  if (!member || member.status !== 'ACTIVE') return false
  if (member.role === 'OWNER' || member.role === 'ADMIN') return member.permissions.includes('MOVE_TASK')
  return task.ownerId === userId && member.permissions.includes('MOVE_TASK')
}

export function memberPermissionSummary(member: ProjectMember): string {
  if (!member.permissions.length) return '无权限'
  return member.permissions.map(permissionLabel).join('、')
}

export function memberSummary(member: ProjectMember): string {
  return `${member.memberName} / ${projectRoleLabel(member.role)} / ${memberPermissionSummary(member)} / ${projectMemberStatusLabel(member.status)}`
}

export function parseRelativeDateTime(content: string, baseDate = new Date()): string | undefined {
  const base = new Date(baseDate)
  base.setSeconds(0, 0)

  if (content.includes('明天')) {
    base.setDate(base.getDate() + 1)
  } else if (content.includes('后天')) {
    base.setDate(base.getDate() + 2)
  } else if (content.includes('下周')) {
    base.setDate(base.getDate() + 7)
  }

  const parsedHour = extractHour(content)
  if (parsedHour != null) {
    const minute = extractMinute(content)
    base.setHours(parsedHour, minute, 0, 0)
  } else {
    base.setHours(10, 0, 0, 0)
  }

  return base.toISOString()
}

function extractCustomerName(content: string): string | undefined {
  const match = content.match(/和(.+?)(?:公司)?合作/)
  if (!match?.[1]) return undefined
  const customer = match[1].trim()
  return customer.endsWith('公司') ? customer : `${customer}公司`
}

function extractTaskTitle(content: string): string {
  const cleaned = content
    .replace(/帮我(?:创建|新增|安排)(?:一个)?任务/g, '')
    .replace(/创建(?:一个)?任务/g, '')
    .replace(/新增(?:一个)?任务/g, '')
    .replace(/这个项目是和.+?合作的项目[，,]?/g, '')
    .replace(/[。！!]+$/g, '')
    .trim()

  const directMatch = cleaned.match(
    /(?:今天|明天|后天|本周|下周)?(?:上午|中午|下午|晚上)?(?:\d{1,2}(?::|点|时)\d{0,2})?[^，。]*?(给[^，。]+|向[^，。]+|准备[^，。]+|整理[^，。]+|汇报[^，。]+|提交[^，。]+|发送[^，。]+|跟进[^，。]+|确认[^，。]+)/
  )
  if (directMatch?.[1]) return directMatch[1].trim()

  return cleaned
    .replace(/^(今天|明天|后天|本周|下周)/, '')
    .replace(/^(上午|中午|下午|晚上)/, '')
    .replace(/^(\d{1,2}(?::|点|时)\d{0,2})/, '')
    .replace(/^要/, '')
    .replace(/^需要/, '')
    .replace(/^去/, '')
    .replace(/^[，,\s]+/, '')
    .trim()
}

function extractHour(content: string): number | null {
  const timeMatch = content.match(/(\d{1,2})(?::|点|时)(\d{1,2})?/)
  if (timeMatch) {
    return normalizeHourByPeriod(Number(timeMatch[1]), content)
  }

  const chineseMatch = content.match(/([一二两三四五六七八九十]{1,3})点/)
  if (chineseMatch?.[1]) {
    return normalizeHourByPeriod(parseChineseNumber(chineseMatch[1]), content)
  }

  if (content.includes('下午三点')) return 15
  if (content.includes('下午')) return 15
  if (content.includes('上午')) return 10
  if (content.includes('中午')) return 12
  if (content.includes('晚上')) return 19
  return null
}

function extractMinute(content: string): number {
  const minuteMatch = content.match(/(?:\d{1,2})(?::|点|时)(\d{1,2})/)
  if (minuteMatch?.[1]) return Number(minuteMatch[1])
  if (content.includes('半')) return 30
  return 0
}

function normalizeHourByPeriod(hour: number, content: string): number {
  if (content.includes('下午') || content.includes('晚上')) {
    return hour < 12 ? hour + 12 : hour
  }
  if (content.includes('中午') && hour < 11) {
    return hour + 12
  }
  return hour
}

function parseChineseNumber(raw: string): number {
  if (raw === '十') return 10
  if (raw.startsWith('十')) {
    return 10 + (CHINESE_DIGIT_MAP[raw.slice(1)] || 0)
  }
  if (raw.endsWith('十')) {
    return (CHINESE_DIGIT_MAP[raw[0]] || 1) * 10
  }
  if (raw.includes('十')) {
    const [tens, ones] = raw.split('十')
    return (CHINESE_DIGIT_MAP[tens] || 0) * 10 + (CHINESE_DIGIT_MAP[ones] || 0)
  }
  return CHINESE_DIGIT_MAP[raw] || 0
}
