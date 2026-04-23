import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  queryTaskList,
  addTask,
  updateTask,
  deleteTask,
  updateTaskStatus,
  getMyTasks
} from '@/api/task'
import type { Task, TaskAddBO, TaskUpdateBO, TaskQueryBO } from '@/types/common'
import type { PageResult } from '@/types/api'

type TaskStatusCounts = {
  all: number
  PENDING: number
  IN_PROGRESS: number
  COMPLETED: number
}

export const useTaskStore = defineStore('task', () => {
  const HIGH_VALUE_FALLBACK_LIMIT = 5

  // State
  const taskList = ref<Task[]>([])
  const myTasks = ref<Task[]>([])
  const totalCount = ref(0)
  const loading = ref(false)
  const highValueFallbackActive = ref(false)
  const highValueFallbackCount = ref(0)
  const statusCounts = ref<TaskStatusCounts>({
    all: 0,
    PENDING: 0,
    IN_PROGRESS: 0,
    COMPLETED: 0
  })

  // Query params
  const queryParams = ref<TaskQueryBO>({
    page: 1,
    limit: 10,
    keyword: '',
    status: undefined,
    priority: undefined,
    filter: 'all',
    sortMode: 'default',
    highValueOnly: false
  })

  // Getters
  const pendingTasks = computed(() => myTasks.value.filter(t => t.status === 'PENDING'))
  const inProgressTasks = computed(() => myTasks.value.filter(t => t.status === 'IN_PROGRESS'))
  const overdueTasks = computed(() => {
    const now = new Date()
    return myTasks.value.filter(t => t.dueDate && new Date(t.dueDate) < now && t.status !== 'COMPLETED')
  })

  function buildFallbackStatusCounts(tasks: Task[]): TaskStatusCounts {
    return {
      all: tasks.length,
      PENDING: tasks.filter(t => t.status === 'PENDING').length,
      IN_PROGRESS: tasks.filter(t => t.status === 'IN_PROGRESS').length,
      COMPLETED: tasks.filter(t => t.status === 'COMPLETED').length
    }
  }

  function extractStatusCounts(result: PageResult<Task>, fallbackTasks?: Task[]): TaskStatusCounts {
    const raw = result.extraData?.statusCounts
    if (raw && typeof raw === 'object') {
      return {
        all: Number(raw.all ?? fallbackTasks?.length ?? result.totalRow ?? 0),
        PENDING: Number(raw.PENDING ?? 0),
        IN_PROGRESS: Number(raw.IN_PROGRESS ?? 0),
        COMPLETED: Number(raw.COMPLETED ?? 0)
      }
    }

    if (fallbackTasks) {
      return buildFallbackStatusCounts(fallbackTasks)
    }

    return buildFallbackStatusCounts(result.list || [])
  }

  // Actions
  async function fetchTaskList(reset = false) {
    if (reset) {
      queryParams.value.page = 1
    }

    loading.value = true
    try {
      const query = { ...queryParams.value }
      const result = await queryTaskList(query)
      const serverSideFallbackActive = Boolean(query.highValueOnly)
        && Array.isArray(result.list)
        && result.list.length > 0
        && result.list.every(task => task.highValue !== true)

      if (query.highValueOnly && result.totalRow === 0) {
        const fallbackLimit = Math.min(query.limit || HIGH_VALUE_FALLBACK_LIMIT, HIGH_VALUE_FALLBACK_LIMIT)
        const fallbackResult = await queryTaskList({
          ...query,
          page: 1,
          limit: fallbackLimit,
          sortMode: 'value',
          highValueOnly: false
        })

        taskList.value = fallbackResult.list.slice(0, fallbackLimit)
        totalCount.value = taskList.value.length
        highValueFallbackActive.value = taskList.value.length > 0
        highValueFallbackCount.value = taskList.value.length
        statusCounts.value = extractStatusCounts(fallbackResult, taskList.value)
        queryParams.value.page = 1
        return fallbackResult
      }

      taskList.value = result.list
      totalCount.value = result.totalRow
      highValueFallbackActive.value = serverSideFallbackActive
      highValueFallbackCount.value = serverSideFallbackActive ? result.list.length : 0
      statusCounts.value = extractStatusCounts(result)
      return result
    } finally {
      loading.value = false
    }
  }

  async function fetchMyTasks(filter: string = 'all') {
    loading.value = true
    try {
      myTasks.value = await getMyTasks(filter)
    } finally {
      loading.value = false
    }
  }

  async function createTask(data: TaskAddBO): Promise<string> {
    const taskId = await addTask(data)
    await fetchTaskList(true)
    await fetchMyTasks()
    return taskId
  }

  async function editTask(data: TaskUpdateBO): Promise<void> {
    await updateTask(data)
    await fetchTaskList(true)
    await fetchMyTasks()
  }

  async function removeTask(taskId: string): Promise<void> {
    await deleteTask(taskId)
    await fetchTaskList(true)
    await fetchMyTasks()
  }

  async function changeTaskStatus(taskId: string, status: string): Promise<void> {
    await updateTaskStatus(taskId, status)
    // Update local state
    const task = taskList.value.find(t => t.taskId === taskId)
    if (task) {
      task.status = status as any
    }
    const myTask = myTasks.value.find(t => t.taskId === taskId)
    if (myTask) {
      myTask.status = status as any
    }
  }

  function setQueryParams(params: Partial<TaskQueryBO>) {
    queryParams.value = { ...queryParams.value, ...params, page: 1 }
  }

  return {
    // State
    taskList,
    myTasks,
    totalCount,
    loading,
    queryParams,
    highValueFallbackActive,
    highValueFallbackCount,
    statusCounts,
    // Getters
    pendingTasks,
    inProgressTasks,
    overdueTasks,
    // Actions
    fetchTaskList,
    fetchMyTasks,
    createTask,
    editTask,
    removeTask,
    changeTaskStatus,
    setQueryParams
  }
})
