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

export const useTaskStore = defineStore('task', () => {
  // State
  const taskList = ref<Task[]>([])
  const myTasks = ref<Task[]>([])
  const totalCount = ref(0)
  const loading = ref(false)

  // Query params
  const queryParams = ref<TaskQueryBO>({
    page: 1,
    limit: 10,
    keyword: '',
    status: undefined,
    priority: undefined,
    filter: 'all'
  })

  // Getters
  const pendingTasks = computed(() => myTasks.value.filter(t => t.status === 'PENDING'))
  const inProgressTasks = computed(() => myTasks.value.filter(t => t.status === 'IN_PROGRESS'))
  const overdueTasks = computed(() => {
    const now = new Date()
    return myTasks.value.filter(t => t.dueDate && new Date(t.dueDate) < now && t.status !== 'COMPLETED')
  })

  // Actions
  async function fetchTaskList(reset = false) {
    if (reset) {
      queryParams.value.page = 1
    }

    loading.value = true
    try {
      const result = await queryTaskList(queryParams.value)
      taskList.value = result.list
      totalCount.value = result.totalRow
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
