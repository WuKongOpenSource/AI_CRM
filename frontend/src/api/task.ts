import { post, get } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { Task, TaskAddBO, TaskUpdateBO, TaskQueryBO } from '@/types/common'

/**
 * Create task
 */
export function addTask(data: TaskAddBO): Promise<string> {
  return post('/task/add', data)
}

/**
 * Update task
 */
export function updateTask(data: TaskUpdateBO): Promise<void> {
  return post('/task/update', data)
}

/**
 * Delete task
 */
export function deleteTask(id: string): Promise<void> {
  return post(`/task/delete/${id}`)
}

/**
 * Query tasks with pagination
 */
export function queryTaskList(query: TaskQueryBO): Promise<PageResult<Task>> {
  return post('/task/queryPageList', query)
}

/**
 * Update task status
 */
export function updateTaskStatus(taskId: string, status: string): Promise<void> {
  return post('/task/updateStatus', null, { params: { taskId, status } })
}

/**
 * Get my tasks
 */
export function getMyTasks(filter: string = 'all'): Promise<Task[]> {
  return get('/task/myTasks', { params: { filter } })
}
