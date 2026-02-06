<template>
  <div class="h-full flex">
    <!-- Left Sidebar - Session History -->
    <div class="w-64 border-r border-gray-200 bg-gray-50 flex flex-col">
      <!-- Sidebar Header -->
      <div class="h-14 px-4 flex items-center justify-between border-b border-gray-200">
        <span class="font-medium text-gray-700">历史对话</span>
        <el-button type="primary" size="small" @click="handleNewSession">
          <el-icon class="mr-1"><Plus /></el-icon>
          新对话
        </el-button>
      </div>

      <!-- Session List -->
      <div class="flex-1 overflow-y-auto">
        <div v-if="chatStore.loading" class="flex justify-center py-8">
          <el-icon class="is-loading text-gray-400" :size="24"><Loading /></el-icon>
        </div>

        <div v-else-if="chatStore.sessions.length === 0" class="px-4 py-8 text-center text-gray-400 text-sm">
          暂无对话记录
        </div>

        <template v-else>
          <!-- Today -->
          <template v-if="groupedSessions.today.length > 0">
            <div class="px-4 pt-4 pb-1 text-xs text-gray-400 font-medium">今天</div>
            <div
              v-for="session in groupedSessions.today"
              :key="session.sessionId"
              class="session-item group"
              :class="{ 'session-item--active': chatStore.currentSessionId === session.sessionId }"
              @click="handleSelectSession(session.sessionId)"
            >
              <div class="flex-1 min-w-0">
                <div class="text-sm truncate" :class="chatStore.currentSessionId === session.sessionId ? 'text-primary-700 font-medium' : 'text-gray-700'">
                  {{ session.title || '新对话' }}
                </div>
                <div class="text-xs text-gray-400 mt-0.5">{{ formatSessionTime(session.updateTime || session.createTime) }}</div>
              </div>
              <el-icon
                class="session-delete-btn text-gray-400 hover:text-red-500 flex-shrink-0"
                @click.stop="handleDeleteSession(session.sessionId)"
              ><Close /></el-icon>
            </div>
          </template>

          <!-- Yesterday -->
          <template v-if="groupedSessions.yesterday.length > 0">
            <div class="px-4 pt-4 pb-1 text-xs text-gray-400 font-medium">昨天</div>
            <div
              v-for="session in groupedSessions.yesterday"
              :key="session.sessionId"
              class="session-item group"
              :class="{ 'session-item--active': chatStore.currentSessionId === session.sessionId }"
              @click="handleSelectSession(session.sessionId)"
            >
              <div class="flex-1 min-w-0">
                <div class="text-sm truncate" :class="chatStore.currentSessionId === session.sessionId ? 'text-primary-700 font-medium' : 'text-gray-700'">
                  {{ session.title || '新对话' }}
                </div>
                <div class="text-xs text-gray-400 mt-0.5">{{ formatSessionTime(session.updateTime || session.createTime) }}</div>
              </div>
              <el-icon
                class="session-delete-btn text-gray-400 hover:text-red-500 flex-shrink-0"
                @click.stop="handleDeleteSession(session.sessionId)"
              ><Close /></el-icon>
            </div>
          </template>

          <!-- Earlier -->
          <template v-if="groupedSessions.earlier.length > 0">
            <div class="px-4 pt-4 pb-1 text-xs text-gray-400 font-medium">更早</div>
            <div
              v-for="session in groupedSessions.earlier"
              :key="session.sessionId"
              class="session-item group"
              :class="{ 'session-item--active': chatStore.currentSessionId === session.sessionId }"
              @click="handleSelectSession(session.sessionId)"
            >
              <div class="flex-1 min-w-0">
                <div class="text-sm truncate" :class="chatStore.currentSessionId === session.sessionId ? 'text-primary-700 font-medium' : 'text-gray-700'">
                  {{ session.title || '新对话' }}
                </div>
                <div class="text-xs text-gray-400 mt-0.5">{{ formatSessionTime(session.updateTime || session.createTime) }}</div>
              </div>
              <el-icon
                class="session-delete-btn text-gray-400 hover:text-red-500 flex-shrink-0"
                @click.stop="handleDeleteSession(session.sessionId)"
              ><Close /></el-icon>
            </div>
          </template>
        </template>
      </div>
    </div>

    <!-- Chat Area -->
    <div class="flex-1 flex flex-col bg-white">
      <!-- Header -->
      <div class="h-14 px-6 flex items-center justify-between border-b border-gray-200">
        <div>
          <h1 class="text-lg font-semibold text-gray-800">AI对话助手</h1>
          <p class="text-xs text-gray-400">通过AI对话快速建立客户档案和生成任务</p>
        </div>
        <div class="text-sm text-gray-500">
          <span class="text-gray-400">您的权限</span>
          <span class="ml-2 text-gray-700">{{ userStore.realname || '销售经理' }} · 完整权限</span>
        </div>
      </div>

      <!-- Messages -->
      <div ref="messagesContainer" class="flex-1 overflow-y-auto p-6">
        <template v-if="chatStore.messages.length === 0">
          <!-- Welcome Message -->
          <div class="welcome-message">
            <div class="text-base font-medium text-gray-800 mb-4">您好！我是AI助手，可以帮您：</div>
            <ul class="space-y-3">
              <li class="flex items-center text-sm text-gray-600">
                <span class="w-6 h-6 rounded bg-blue-100 flex items-center justify-center mr-3">
                  <el-icon class="text-blue-500" :size="14"><Document /></el-icon>
                </span>
                创建和管理客户档案
              </li>
              <li class="flex items-center text-sm text-gray-600">
                <span class="w-6 h-6 rounded bg-yellow-100 flex items-center justify-center mr-3">
                  <el-icon class="text-yellow-500" :size="14"><Folder /></el-icon>
                </span>
                上传和整理客户资料
              </li>
              <li class="flex items-center text-sm text-gray-600">
                <span class="w-6 h-6 rounded bg-purple-100 flex items-center justify-center mr-3">
                  <el-icon class="text-purple-500" :size="14"><Search /></el-icon>
                </span>
                查询客户信息
              </li>
              <li class="flex items-center text-sm text-gray-600">
                <span class="w-6 h-6 rounded bg-green-100 flex items-center justify-center mr-3">
                  <el-icon class="text-green-500" :size="14"><CircleCheck /></el-icon>
                </span>
                管理待办任务
              </li>
              <li class="flex items-center text-sm text-gray-600">
                <span class="w-6 h-6 rounded bg-indigo-100 flex items-center justify-center mr-3">
                  <el-icon class="text-indigo-500" :size="14"><DataAnalysis /></el-icon>
                </span>
                查看数据分析
              </li>
            </ul>
            <div class="mt-5 pt-4 border-t border-gray-100 text-sm text-gray-800">
              请告诉我您需要什么帮助？
            </div>
            <div class="mt-2 text-xs text-gray-400">{{ formatTime(new Date()) }}</div>
          </div>
        </template>

        <template v-else>
          <div
            v-for="message in chatStore.messages"
            :key="message.id"
            class="message-enter mb-4"
            :class="message.role === 'user' ? 'flex justify-end' : 'flex justify-start'"
          >
            <div
              :class="[
                'max-w-[70%] rounded-lg px-4 py-3',
                message.role === 'user'
                  ? 'bg-primary-500 text-white'
                  : 'bg-gray-100 text-gray-800'
              ]"
            >
              <div class="whitespace-pre-wrap" :class="{ 'streaming-cursor': message.isStreaming }">
                {{ message.content || '...' }}
              </div>
              <div
                :class="[
                  'text-xs mt-2',
                  message.role === 'user' ? 'text-primary-200' : 'text-gray-400'
                ]"
              >
                {{ formatTime(message.timestamp) }}
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- Quick Actions Bar -->
      <div class="px-6 py-3 border-t border-gray-100 bg-gray-50">
        <div class="text-xs text-gray-400 mb-2">快速操作：</div>
        <div class="flex flex-wrap gap-2">
          <el-button
            v-for="action in quickActions"
            :key="action.label"
            size="small"
            round
            class="quick-action-btn"
            @click="sendQuickMessage(action.text)"
          >
            <el-icon class="mr-1"><component :is="action.icon" /></el-icon>
            {{ action.label }}
          </el-button>
        </div>
      </div>

      <!-- Input Area -->
      <div class="px-6 py-4 bg-white border-t border-gray-200">
        <div class="flex items-center gap-3">
          <el-button :icon="Upload" circle class="upload-btn" @click="handleUpload" />
          <el-input
            v-model="inputText"
            placeholder="输入消息..."
            :disabled="chatStore.isStreaming"
            @keydown.enter.exact.prevent="handleSend"
            class="flex-1"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            circle
            :loading="chatStore.isStreaming"
            :disabled="!inputText.trim() || chatStore.isStreaming"
            @click="handleSend"
          />
        </div>
      </div>
    </div>

    <!-- Right Sidebar - Notifications & Tasks -->
    <div class="w-80 border-l border-gray-200 bg-white flex flex-col">
      <!-- Notifications Section -->
      <div class="p-4 border-b border-gray-100">
        <div class="flex items-center justify-between mb-3">
          <span class="font-medium text-gray-800">通知</span>
          <el-badge :value="notifications.length" :max="9" class="notification-badge">
            <span></span>
          </el-badge>
        </div>
        <div class="space-y-3">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            class="flex items-start gap-2"
          >
            <span
              class="w-2 h-2 rounded-full mt-1.5 flex-shrink-0"
              :class="notification.color"
            ></span>
            <div class="flex-1 min-w-0">
              <div class="text-sm text-gray-700 line-clamp-2">{{ notification.content }}</div>
              <div class="text-xs text-gray-400 mt-0.5">{{ notification.time }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tasks Section -->
      <div class="flex-1 overflow-y-auto p-4">
        <div class="flex items-center justify-between mb-3">
          <div>
            <span class="font-medium text-gray-800">任务</span>
            <span class="text-xs text-gray-400 ml-2">{{ pendingTaskCount }}个待完成</span>
          </div>
          <el-button text size="small" type="primary" @click="router.push('/task')">
            查看全部 <el-icon class="ml-1"><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div v-if="taskStore.loading" class="text-center py-4">
          <el-icon class="is-loading"><Loading /></el-icon>
        </div>
        <div v-else-if="taskStore.myTasks.length === 0" class="text-center py-8 text-gray-400 text-sm">
          暂无待办任务
        </div>
        <div v-else class="space-y-3">
          <div
            v-for="task in displayTasks"
            :key="task.taskId"
            class="task-item cursor-pointer"
            :class="{ 'task-item--completed': task.status === 'COMPLETED' }"
            @click="handleTaskClick(task)"
          >
            <div class="flex items-start gap-2">
              <el-icon
                class="mt-0.5 flex-shrink-0"
                :class="task.status === 'COMPLETED' ? 'text-green-500' : 'text-gray-400'"
              >
                <component :is="task.status === 'COMPLETED' ? CircleCheck : Clock" />
              </el-icon>
              <div class="flex-1 min-w-0">
                <div
                  class="text-sm line-clamp-1"
                  :class="task.status === 'COMPLETED' ? 'text-gray-400 line-through' : 'text-gray-700'"
                >
                  {{ task.title }}
                </div>
                <div class="flex items-center gap-2 mt-1">
                  <el-tag
                    :type="getPriorityType(task.priority)"
                    size="small"
                    class="priority-tag"
                  >
                    {{ getPriorityLabel(task.priority) }}
                  </el-tag>
                  <span class="text-xs text-gray-400">{{ formatDate(task.dueDate) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useTaskStore } from '@/stores/task'
import { useAgentStore } from '@/stores/agent'
import { useUserStore } from '@/stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  Plus,
  Promotion,
  Loading,
  User,
  Document,
  Search,
  Close,
  Upload,
  Folder,
  CircleCheck,
  DataAnalysis,
  Clock,
  ArrowRight
} from '@element-plus/icons-vue'
import type { Task, ChatSession } from '@/types/common'

const router = useRouter()
const chatStore = useChatStore()
const taskStore = useTaskStore()
const agentStore = useAgentStore()
const userStore = useUserStore()

const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

// Notifications mock data (in real app, fetch from API)
const notifications = ref([
  { id: 1, content: '客户张三的项目进度已更新', time: '5分钟前', color: 'bg-blue-500' },
  { id: 2, content: '有3个任务即将到期', time: '1小时前', color: 'bg-orange-500' },
  { id: 3, content: '知识库同步完成', time: '2小时前', color: 'bg-green-500' }
])

// Group sessions by date
const groupedSessions = computed(() => {
  const today: ChatSession[] = []
  const yesterday: ChatSession[] = []
  const earlier: ChatSession[] = []
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const yesterdayStart = todayStart - 86400000

  for (const session of chatStore.sessions) {
    const time = new Date(session.updateTime || session.createTime).getTime()
    if (time >= todayStart) {
      today.push(session)
    } else if (time >= yesterdayStart) {
      yesterday.push(session)
    } else {
      earlier.push(session)
    }
  }

  return { today, yesterday, earlier }
})

const quickActions = [
  { label: '创建新客户', text: '帮我创建一个新客户', icon: User },
  { label: '上传会议记录', text: '我要上传会议记录', icon: Upload },
  { label: '查询客户状态', text: '帮我查询客户列表', icon: Search },
  { label: '生成跟进任务', text: '帮我生成跟进任务', icon: Clock }
]

// Computed for tasks display
const pendingTaskCount = computed(() => {
  return taskStore.myTasks.filter(t => t.status !== 'COMPLETED').length
})

const displayTasks = computed(() => {
  return taskStore.myTasks.slice(0, 4)
})

onMounted(async () => {
  await Promise.all([
    chatStore.fetchSessions(),
    taskStore.fetchMyTasks(),
    agentStore.fetchEnabledAgents()
  ])
})

// Auto scroll to bottom when new messages arrive
watch(
  () => chatStore.messages.length,
  () => {
    nextTick(() => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    })
  }
)

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || chatStore.isStreaming) return

  inputText.value = ''
  await chatStore.sendMessage(text)
}

function sendQuickMessage(text: string) {
  inputText.value = text
  handleSend()
}

function handleUpload() {
  ElMessage.info('文件上传功能开发中')
}

async function handleNewSession() {
  chatStore.clearMessages()
  await chatStore.startNewSession('新对话')
}

async function handleSelectSession(sessionId: string) {
  if (chatStore.currentSessionId === sessionId) return
  await chatStore.selectSession(sessionId)
}

async function handleDeleteSession(sessionId: string) {
  try {
    await ElMessageBox.confirm('确定要删除这个对话吗？删除后无法恢复。', '删除对话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    })
    await chatStore.removeSession(sessionId)
    ElMessage.success('对话已删除')
  } catch {
    // User cancelled
  }
}

function formatSessionTime(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const time = date.getTime()

  if (time >= todayStart) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function handleTaskClick(task: Task) {
  inputText.value = `帮我查看任务「${task.title}」的详情`
}

function formatTime(date: Date): string {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function formatDate(date?: string): string {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).replace(/\//g, '-')
}

function getPriorityType(priority: string): 'danger' | 'warning' | 'success' {
  switch (priority?.toUpperCase()) {
    case 'HIGH': return 'danger'
    case 'MEDIUM': return 'warning'
    case 'LOW': return 'success'
    default: return 'warning'
  }
}

function getPriorityLabel(priority: string): string {
  switch (priority?.toUpperCase()) {
    case 'HIGH': return '高'
    case 'MEDIUM': return '中'
    case 'LOW': return '低'
    default: return priority || '中'
  }
}
</script>

<style scoped>
.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
  border-left: 2px solid transparent;
}

.session-item:hover {
  background-color: #f3f4f6;
}

.session-item--active {
  background-color: #eff6ff;
  border-left-color: var(--el-color-primary);
}

.session-item--active:hover {
  background-color: #dbeafe;
}

.session-delete-btn {
  opacity: 0;
  transition: opacity 0.15s;
}

.session-item:hover .session-delete-btn {
  opacity: 1;
}

/* Welcome message styles */
.welcome-message {
  max-width: 480px;
  background: #f9fafb;
  border-radius: 12px;
  padding: 20px 24px;
  border: 1px solid #f3f4f6;
}

/* Quick action buttons */
.quick-action-btn {
  background: white !important;
  border-color: #e5e7eb !important;
  color: #374151 !important;
}

.quick-action-btn:hover {
  background: #f9fafb !important;
  border-color: #d1d5db !important;
}

/* Upload button */
.upload-btn {
  background: #f3f4f6 !important;
  border: none !important;
  color: #6b7280 !important;
}

.upload-btn:hover {
  background: #e5e7eb !important;
  color: #374151 !important;
}

/* Task items */
.task-item {
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 8px;
  transition: background-color 0.15s;
}

.task-item:hover {
  background: #f3f4f6;
}

.task-item--completed {
  opacity: 0.6;
}

/* Priority tag */
.priority-tag {
  font-size: 10px !important;
  height: 18px !important;
  padding: 0 6px !important;
}

/* Notification badge */
.notification-badge :deep(.el-badge__content) {
  background-color: #f56c6c;
}

/* Message animation */
.message-enter {
  animation: messageSlideIn 0.3s ease-out;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Streaming cursor */
.streaming-cursor::after {
  content: '▊';
  animation: blink 1s infinite;
  margin-left: 2px;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
