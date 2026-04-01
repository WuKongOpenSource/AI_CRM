import { post, get } from '@/utils/request'

export interface ScheduleVO {
  scheduleId: string
  title: string
  description: string
  startTime: string
  endTime: string
  type: string
  typeName: string
  customerId: string
  customerName: string
  contactId: string
  contactName: string
  location: string
  createUserId: string
  createUserName: string
  createTime: string
}

export interface ScheduleAddBO {
  title: string
  startTime: string
  endTime?: string
  type?: string
  customerId?: string
  contactId?: string
  location?: string
  description?: string
}

/**
 * 创建日程
 */
export function addSchedule(data: ScheduleAddBO): Promise<string> {
  return post('/schedule/add', data)
}

/**
 * 删除日程
 */
export function deleteSchedule(id: string): Promise<void> {
  return post(`/schedule/delete/${id}`)
}

/**
 * 查询我的日程
 */
export function getMySchedules(filter: string = 'all'): Promise<ScheduleVO[]> {
  return get('/schedule/mySchedules', { params: { filter } })
}
