<template>
  <div class="flex h-full" :class="{ 'flex-col': isMobile }">
    <!-- Internal Sidebar: Chat History -->
    <aside
      v-if="!isMobile || mobilePanel === 'sessions'"
      :class="isMobile ? 'flex-1 flex flex-col bg-slate-50/50' : 'w-72 border-r border-slate-100 bg-slate-50/50 flex flex-col shrink-0'"
    >
      <div class="p-6 pb-2">
        <button
          class="w-full flex items-center justify-center gap-2 py-2.5 bg-white border border-slate-200 rounded-xl text-sm font-bold text-slate-700 shadow-sm hover:bg-slate-50 transition-all"
          @click="handleNewSession"
        >
          <span class="material-symbols-outlined text-sm">add</span>
          开启新对话
        </button>
      </div>

      <!-- System Notifications Menu Item -->
      <div class="px-3 py-4">
        <button
          class="w-full flex items-center gap-3 p-3 rounded-xl transition-all text-left"
          :class="currentView === 'notifications'
            ? 'bg-primary/10 text-primary border border-primary/20 shadow-sm'
            : 'hover:bg-slate-100/50 text-slate-600 border border-transparent'"
          @click="currentView = 'notifications'; isMobile && (mobilePanel = 'chat')"
        >
          <span class="material-symbols-outlined" :class="currentView === 'notifications' ? 'fill-1' : ''">notifications</span>
          <div class="flex-1">
            <p class="text-sm font-bold">系统通知</p>
            <p class="text-[10px] opacity-60">{{ notifications.length }} 条未读消息</p>
          </div>
          <div v-if="currentView !== 'notifications'" class="size-2 rounded-full bg-primary animate-pulse"></div>
        </button>
      </div>

      <!-- Divider -->
      <div class="mx-6 h-px bg-slate-100 mb-4"></div>

      <!-- Session List -->
      <div class="flex-1 overflow-y-auto px-3 space-y-1">
        <p class="px-3 text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-2">最近对话</p>

        <div v-if="chatStore.loading" class="flex justify-center py-8">
          <span class="material-symbols-outlined text-slate-300 animate-spin">progress_activity</span>
        </div>

        <div v-else-if="chatStore.sessions.length === 0" class="px-3 py-8 text-center text-slate-400 text-xs">
          暂无对话记录
        </div>

        <template v-else>
          <!-- Today -->
          <template v-if="groupedSessions.today.length > 0">
            <p class="px-3 pt-3 pb-1 text-[10px] font-bold text-slate-400 uppercase tracking-widest">今天</p>
            <button
              v-for="session in groupedSessions.today"
              :key="session.sessionId"
              class="w-full flex flex-col gap-1 p-3 rounded-xl transition-all text-left group relative"
              :class="currentView === 'chat' && chatStore.currentSessionId === session.sessionId
                ? 'bg-white shadow-sm border border-slate-200'
                : 'hover:bg-slate-100/50 border border-transparent'"
              @click="handleSelectSession(session.sessionId)"
            >
              <span
                class="text-sm font-semibold truncate"
                :class="currentView === 'chat' && chatStore.currentSessionId === session.sessionId ? 'text-primary' : 'text-slate-700'"
              >{{ session.title || '新对话' }}</span>
              <span class="text-[10px] text-slate-400 font-medium">{{ formatSessionTime(session.updateTime || session.createTime) }}</span>
              <span
                class="absolute right-2 top-1/2 -translate-y-1/2 material-symbols-outlined text-sm text-slate-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                @click.stop="handleDeleteSession(session.sessionId)"
              >close</span>
            </button>
          </template>

          <!-- Yesterday -->
          <template v-if="groupedSessions.yesterday.length > 0">
            <p class="px-3 pt-3 pb-1 text-[10px] font-bold text-slate-400 uppercase tracking-widest">昨天</p>
            <button
              v-for="session in groupedSessions.yesterday"
              :key="session.sessionId"
              class="w-full flex flex-col gap-1 p-3 rounded-xl transition-all text-left group relative"
              :class="currentView === 'chat' && chatStore.currentSessionId === session.sessionId
                ? 'bg-white shadow-sm border border-slate-200'
                : 'hover:bg-slate-100/50 border border-transparent'"
              @click="handleSelectSession(session.sessionId)"
            >
              <span
                class="text-sm font-semibold truncate"
                :class="currentView === 'chat' && chatStore.currentSessionId === session.sessionId ? 'text-primary' : 'text-slate-700'"
              >{{ session.title || '新对话' }}</span>
              <span class="text-[10px] text-slate-400 font-medium">{{ formatSessionTime(session.updateTime || session.createTime) }}</span>
              <span
                class="absolute right-2 top-1/2 -translate-y-1/2 material-symbols-outlined text-sm text-slate-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                @click.stop="handleDeleteSession(session.sessionId)"
              >close</span>
            </button>
          </template>

          <!-- Earlier -->
          <template v-if="groupedSessions.earlier.length > 0">
            <p class="px-3 pt-3 pb-1 text-[10px] font-bold text-slate-400 uppercase tracking-widest">更早</p>
            <button
              v-for="session in groupedSessions.earlier"
              :key="session.sessionId"
              class="w-full flex flex-col gap-1 p-3 rounded-xl transition-all text-left group relative"
              :class="currentView === 'chat' && chatStore.currentSessionId === session.sessionId
                ? 'bg-white shadow-sm border border-slate-200'
                : 'hover:bg-slate-100/50 border border-transparent'"
              @click="handleSelectSession(session.sessionId)"
            >
              <span
                class="text-sm font-semibold truncate"
                :class="currentView === 'chat' && chatStore.currentSessionId === session.sessionId ? 'text-primary' : 'text-slate-700'"
              >{{ session.title || '新对话' }}</span>
              <span class="text-[10px] text-slate-400 font-medium">{{ formatSessionTime(session.updateTime || session.createTime) }}</span>
              <span
                class="absolute right-2 top-1/2 -translate-y-1/2 material-symbols-outlined text-sm text-slate-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                @click.stop="handleDeleteSession(session.sessionId)"
              >close</span>
            </button>
          </template>
        </template>
      </div>

      <!-- AI Model Status -->
      <div class="p-4 border-t border-slate-100">
        <div class="p-3 bg-primary/5 rounded-xl border border-primary/10">
          <p class="text-[10px] font-bold text-primary uppercase tracking-wider mb-1">AI 模型状态</p>
          <div class="flex items-center gap-2">
            <div class="size-1.5 rounded-full bg-emerald-500 animate-pulse"></div>
            <span class="text-[10px] font-medium text-slate-600">AI 模型已就绪</span>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Area -->
    <div
      v-if="!isMobile || mobilePanel === 'chat'"
      class="flex-1 flex flex-col relative bg-white overflow-hidden"
    >
      <!-- Chat View -->
      <template v-if="currentView === 'chat'">
        <div class="flex-1 flex flex-col overflow-hidden">
          <!-- Messages Area -->
          <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 md:p-8 space-y-8 scroll-smooth pb-48">
            <!-- Welcome Section (no messages) -->
            <template v-if="chatStore.messages.length === 0">
              <div class="max-w-3xl mx-auto flex flex-col items-center text-center space-y-4 py-12">
                <div class="size-16 bg-primary/5 rounded-2xl flex items-center justify-center text-primary mb-2 border border-primary/10">
                  <span class="material-symbols-outlined text-4xl">auto_awesome</span>
                </div>
                <h1 class="text-2xl font-bold tracking-tight text-slate-900">
                  您好，{{ userStore.realname || '用户' }}。
                </h1>
                <p class="text-slate-400 text-base max-w-md">
                  我是您的智能销售助手。今天想处理哪些客户或商机？
                </p>
              </div>
            </template>

            <!-- Messages -->
            <template v-else>
              <div
                v-for="message in chatStore.messages"
                :key="message.id"
                class="max-w-3xl mx-auto message-enter"
              >
                <!-- AI Message -->
                <div v-if="message.role !== 'user'" class="flex gap-4 md:gap-5">
                  <div class="size-9 rounded-xl bg-primary flex items-center justify-center text-white shrink-0 shadow-lg shadow-primary/20">
                    <span class="material-symbols-outlined text-lg">smart_toy</span>
                  </div>
                  <div class="flex-1 space-y-3 min-w-0">
                    <div class="bg-slate-50 text-slate-700 rounded-2xl rounded-tl-none p-4 inline-block max-w-full text-sm leading-relaxed border border-slate-100">
                      <div class="whitespace-pre-wrap" :class="{ 'streaming-cursor': message.isStreaming }">
                        {{ message.content || '...' }}
                      </div>
                    </div>
                    <!-- Attachments -->
                    <div v-if="message.attachments && message.attachments.length > 0" class="space-y-2">
                      <div v-for="att in message.attachments" :key="att.id || att.fileName">
                        <template v-if="att.mimeType && att.mimeType.startsWith('image/')">
                          <el-image
                            :src="att.accessUrl"
                            :preview-src-list="[att.accessUrl]"
                            fit="cover"
                            class="rounded-xl max-h-[200px] border border-slate-100"
                            :class="isMobile ? 'max-w-[200px]' : 'max-w-[300px]'"
                            lazy
                          />
                          <div class="text-xs text-slate-400 mt-1">{{ att.fileName }}</div>
                        </template>
                        <template v-else>
                          <a
                            :href="att.accessUrl"
                            target="_blank"
                            class="flex items-center gap-3 p-3 rounded-xl border border-slate-100 hover:bg-slate-50 transition-colors max-w-xs"
                          >
                            <span class="material-symbols-outlined text-slate-400">description</span>
                            <div class="flex-1 min-w-0">
                              <div class="text-sm text-slate-700 truncate">{{ att.fileName }}</div>
                              <div class="text-xs text-slate-400">{{ formatFileSize(att.fileSize) }}</div>
                            </div>
                          </a>
                        </template>
                      </div>
                    </div>
                    <div class="text-[10px] text-slate-400 font-medium">{{ formatTime(message.timestamp) }}</div>
                  </div>
                </div>

                <!-- User Message -->
                <div v-else class="flex gap-4 md:gap-5 flex-row-reverse">
                  <div class="size-9 rounded-xl bg-slate-100 overflow-hidden shrink-0 border border-slate-200 flex items-center justify-center">
                    <span class="material-symbols-outlined text-slate-400">person</span>
                  </div>
                  <div class="space-y-3 min-w-0" :class="isMobile ? 'max-w-[85%]' : 'max-w-[70%]'">
                    <div class="bg-primary text-white rounded-2xl rounded-tr-none p-4 shadow-lg shadow-primary/10 text-sm leading-relaxed">
                      <div class="whitespace-pre-wrap">{{ message.content || '...' }}</div>
                    </div>
                    <!-- User Attachments -->
                    <div v-if="message.attachments && message.attachments.length > 0" class="space-y-2">
                      <div v-for="att in message.attachments" :key="att.id || att.fileName">
                        <template v-if="att.mimeType && att.mimeType.startsWith('image/')">
                          <el-image
                            :src="att.accessUrl"
                            :preview-src-list="[att.accessUrl]"
                            fit="cover"
                            class="rounded-xl max-h-[200px]"
                            :class="isMobile ? 'max-w-[200px]' : 'max-w-[300px]'"
                            lazy
                          />
                        </template>
                        <template v-else>
                          <a
                            :href="att.accessUrl"
                            target="_blank"
                            class="flex items-center gap-3 p-3 rounded-xl border border-white/20 hover:bg-white/10 transition-colors max-w-xs"
                          >
                            <span class="material-symbols-outlined text-white/70">description</span>
                            <div class="flex-1 min-w-0">
                              <div class="text-sm text-white truncate">{{ att.fileName }}</div>
                              <div class="text-xs text-white/60">{{ formatFileSize(att.fileSize) }}</div>
                            </div>
                          </a>
                        </template>
                      </div>
                    </div>
                    <div class="text-[10px] text-slate-400 font-medium text-right">{{ formatTime(message.timestamp) }}</div>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- Input Area -->
          <div class="absolute bottom-0 left-0 right-0 p-4 md:p-8 bg-gradient-to-t from-white via-white to-transparent">
            <div class="max-w-4xl mx-auto space-y-4">
              <!-- Quick Action Chips -->
              <div v-if="chatStore.messages.length === 0" class="flex flex-wrap gap-2 justify-center">
                <button
                  v-for="action in quickActions"
                  :key="action.label"
                  class="px-4 py-1.5 bg-white border border-slate-200 rounded-full text-[11px] font-bold text-slate-500 hover:border-primary hover:text-primary transition-all shadow-sm"
                  @click="sendQuickMessage(action.text)"
                >
                  {{ action.label }}
                </button>
              </div>

              <!-- Selected Files Preview -->
              <div v-if="selectedFiles.length > 0" class="flex flex-wrap gap-2">
                <div
                  v-for="(file, index) in selectedFiles"
                  :key="index"
                  class="flex items-center gap-2 px-3 py-2 bg-slate-50 rounded-xl text-sm text-slate-700 border border-slate-100"
                >
                  <span class="material-symbols-outlined text-sm" :class="file.type.startsWith('image/') ? 'text-blue-500' : 'text-slate-400'">
                    {{ file.type.startsWith('image/') ? 'image' : 'description' }}
                  </span>
                  <span class="truncate max-w-[120px]">{{ file.name }}</span>
                  <span class="text-xs text-slate-400">{{ formatFileSize(file.size) }}</span>
                  <span
                    class="material-symbols-outlined text-sm text-slate-400 hover:text-red-500 cursor-pointer"
                    @click="removeSelectedFile(index)"
                  >close</span>
                </div>
              </div>

              <!-- Input Box -->
              <div class="relative group">
                <div class="absolute inset-0 bg-primary/5 blur-xl rounded-2xl group-focus-within:bg-primary/10 transition-all opacity-0 group-focus-within:opacity-100"></div>
                <div class="relative flex items-center bg-white border border-slate-200 rounded-2xl p-2 shadow-xl shadow-slate-200/40 focus-within:border-primary transition-all">
                  <input
                    ref="fileInputRef"
                    type="file"
                    multiple
                    accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.json,.xml"
                    class="hidden"
                    @change="handleFileSelect"
                  />
                  <button
                    class="size-10 flex items-center justify-center text-slate-400 hover:text-primary transition-colors"
                    :disabled="isUploading"
                    @click="handleUpload"
                  >
                    <span class="material-symbols-outlined">attach_file</span>
                  </button>
                  <input
                    v-model="inputText"
                    type="text"
                    class="flex-1 bg-transparent border-none focus:ring-0 focus:outline-none text-slate-900 px-3 py-3 text-sm placeholder:text-slate-400"
                    placeholder="输入指令，如：总结今天与张总的会议..."
                    :disabled="chatStore.isStreaming || isUploading"
                    @keydown.enter.exact.prevent="handleSend"
                  />
                  <div class="flex items-center gap-2 pr-1">
                    <button
                      class="size-10 rounded-xl bg-primary text-white flex items-center justify-center hover:bg-primary/90 shadow-lg shadow-primary/20 transition-all disabled:opacity-50"
                      :disabled="(!inputText.trim() && selectedFiles.length === 0) || chatStore.isStreaming || isUploading"
                      @click="handleSend"
                    >
                      <span v-if="chatStore.isStreaming || isUploading" class="material-symbols-outlined text-xl animate-spin">progress_activity</span>
                      <span v-else class="material-symbols-outlined text-xl">send</span>
                    </button>
                  </div>
                </div>
              </div>
              <p class="text-center text-[9px] text-slate-300 uppercase font-bold tracking-[0.4em]">Nexus AI 自动化业务引擎</p>
            </div>
          </div>
        </div>
      </template>

      <!-- Notifications View -->
      <template v-else>
        <div class="flex-1 overflow-y-auto p-6 md:p-12">
          <!-- Mobile back button -->
          <button v-if="isMobile" class="flex items-center gap-1 text-sm text-slate-500 mb-4" @click="mobilePanel = 'sessions'">
            <span class="material-symbols-outlined text-sm">arrow_back</span>
            返回
          </button>

          <div class="max-w-4xl mx-auto">
            <div class="flex items-center justify-between mb-10">
              <div>
                <h2 class="text-2xl font-bold text-slate-900">系统通知</h2>
                <p class="text-slate-500 text-sm mt-1">查看来自 Nexus AI 的重要更新和安全提醒</p>
              </div>
              <button class="px-4 py-2 text-sm font-bold text-primary hover:bg-primary/5 rounded-lg transition-all">
                全部标记为已读
              </button>
            </div>

            <div class="space-y-8">
              <div v-for="notif in systemNotifications" :key="notif.id" class="space-y-3">
                <!-- AI Header -->
                <div class="flex items-center gap-3 px-2">
                  <div class="size-8 rounded-lg bg-primary flex items-center justify-center text-white shadow-sm">
                    <span class="material-symbols-outlined text-sm">smart_toy</span>
                  </div>
                  <div>
                    <p class="text-sm font-bold text-slate-900">Nexus AI 助手</p>
                    <p class="text-[10px] text-slate-400 font-medium">系统自动发送</p>
                  </div>
                </div>

                <!-- Notification Card -->
                <div
                  class="p-6 bg-white border rounded-2xl transition-all flex flex-col gap-6 group relative"
                  :class="notif.unread ? 'border-primary/20 shadow-sm' : 'border-slate-100 shadow-none'"
                >
                  <div class="flex gap-5">
                    <div v-if="notif.unread" class="absolute top-6 right-6 flex items-center gap-1.5">
                      <span class="size-2 rounded-full bg-primary"></span>
                      <span class="text-[10px] font-bold text-primary uppercase tracking-tight">未读</span>
                    </div>

                    <div
                      class="size-12 rounded-xl flex items-center justify-center shrink-0"
                      :class="{
                        'bg-blue-50 text-blue-500': notif.type === 'info',
                        'bg-amber-50 text-amber-500': notif.type === 'warning',
                        'bg-emerald-50 text-emerald-500': notif.type === 'success'
                      }"
                    >
                      <span class="material-symbols-outlined">
                        {{ notif.type === 'info' ? 'upgrade' : notif.type === 'warning' ? 'security' : 'analytics' }}
                      </span>
                    </div>

                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-1">
                        <span
                          class="text-[10px] font-bold px-2 py-0.5 rounded uppercase tracking-tight"
                          :class="{
                            'bg-blue-50 text-blue-600': notif.type === 'info',
                            'bg-amber-50 text-amber-600': notif.type === 'warning',
                            'bg-emerald-50 text-emerald-600': notif.type === 'success'
                          }"
                        >{{ notif.category }}</span>
                        <span class="text-xs text-slate-400 font-medium">{{ notif.time }}</span>
                      </div>
                      <h3 class="font-bold text-slate-900 text-lg mb-2">{{ notif.title }}</h3>
                      <p class="text-slate-600 text-sm leading-relaxed max-w-2xl">{{ notif.content }}</p>
                      <div class="mt-6 flex items-center gap-4">
                        <button class="px-4 py-2 bg-slate-900 text-white text-xs font-bold rounded-lg hover:bg-slate-800 transition-all">
                          立即查看
                        </button>
                        <button class="px-4 py-2 bg-white border border-slate-200 text-slate-600 text-xs font-bold rounded-lg hover:bg-slate-50 transition-all">
                          忽略
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Empty State -->
            <div class="mt-12 p-12 border-2 border-dashed border-slate-100 rounded-[2.5rem] flex flex-col items-center text-center">
              <div class="size-16 bg-slate-50 rounded-full flex items-center justify-center text-slate-200 mb-4">
                <span class="material-symbols-outlined text-4xl">history</span>
              </div>
              <p class="text-slate-400 text-sm font-medium">没有更多历史通知了</p>
            </div>
          </div>
        </div>
      </template>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import type { ChatSession, ChatAttachmentDTO, ChatAttachmentVO } from '@/types/common'

const chatStore = useChatStore()
const agentStore = useAgentStore()
const userStore = useUserStore()
const { isMobile } = useResponsive()

const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const mobilePanel = ref<'sessions' | 'chat'>('sessions')
const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<File[]>([])
const isUploading = ref(false)
const currentView = ref<'chat' | 'notifications'>('chat')

const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
const MAX_FILE_COUNT = 5

// Notifications mock data
const notifications = ref([
  { id: 1, content: '客户张三的项目进度已更新', time: '5分钟前', color: 'bg-blue-500' },
  { id: 2, content: '有3个任务即将到期', time: '1小时前', color: 'bg-orange-500' },
  { id: 3, content: '知识库同步完成', time: '2小时前', color: 'bg-green-500' }
])

// System notifications for notification view
const systemNotifications = ref([
  {
    id: 1,
    title: '系统核心引擎升级完成',
    content: 'Nexus AI 已升级至最新版本。本次更新优化了长文本理解能力，并新增了对多语种会议摘要的支持。',
    time: '1小时前',
    type: 'info',
    category: '系统更新',
    unread: true
  },
  {
    id: 2,
    title: '异地登录安全提醒',
    content: '检测到您的账号存在异地登录行为。如果这不是您的操作，请立即重置密码并开启两步验证。',
    time: '3小时前',
    type: 'warning',
    category: '安全警报',
    unread: true
  },
  {
    id: 3,
    title: '本月销售业绩分析报告',
    content: '您上个月的销售目标达成率为 112%。AI 已为您生成了详细的客户贡献度分析和下月潜力客户预测，建议优先关注。',
    time: '昨天',
    type: 'success',
    category: '业务报告',
    unread: false
  }
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
  { label: '创建新客户', text: '帮我创建一个新客户' },
  { label: '查询客户状态', text: '帮我查询客户列表' },
  { label: '生成跟进任务', text: '帮我生成跟进任务' },
  { label: '分析本月销售目标', text: '分析本月销售目标的缺口' }
]

onMounted(async () => {
  await Promise.all([
    chatStore.fetchSessions(),
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
  const hasFiles = selectedFiles.value.length > 0
  if ((!text && !hasFiles) || chatStore.isStreaming || isUploading.value) return

  const content = text || '请分析这些文件'
  inputText.value = ''

  let attachmentDTOs: ChatAttachmentDTO[] | undefined
  let attachmentVOs: ChatAttachmentVO[] | undefined

  // Upload files to MinIO if any
  if (hasFiles) {
    isUploading.value = true
    try {
      const files = [...selectedFiles.value]
      selectedFiles.value = []

      const results = await Promise.all(
        files.map(async (file) => {
          const presigned = await getPresignedUploadUrl(file.name, file.type)
          await uploadToMinIO(file, presigned.uploadUrl)
          return {
            dto: {
              fileName: file.name,
              filePath: presigned.objectKey,
              fileSize: file.size,
              mimeType: file.type || 'application/octet-stream'
            } as ChatAttachmentDTO,
            vo: {
              id: '',
              fileName: file.name,
              filePath: presigned.objectKey,
              fileSize: file.size,
              mimeType: file.type || 'application/octet-stream',
              accessUrl: presigned.accessUrl
            } as ChatAttachmentVO
          }
        })
      )

      attachmentDTOs = results.map(r => r.dto)
      attachmentVOs = results.map(r => r.vo)
    } catch (e) {
      console.error('文件上传失败:', e)
      ElMessage.error('文件上传失败，请重试')
      isUploading.value = false
      return
    }
    isUploading.value = false
  }

  // Switch to chat view when sending
  currentView.value = 'chat'
  await chatStore.sendMessage(content, attachmentDTOs, attachmentVOs)
}

function sendQuickMessage(text: string) {
  inputText.value = text
  handleSend()
}

function handleUpload() {
  fileInputRef.value?.click()
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files) return

  const newFiles = Array.from(input.files)

  // Validate file count
  if (selectedFiles.value.length + newFiles.length > MAX_FILE_COUNT) {
    ElMessage.warning(`最多只能上传${MAX_FILE_COUNT}个文件`)
    input.value = ''
    return
  }

  // Validate file size
  for (const file of newFiles) {
    if (file.size > MAX_FILE_SIZE) {
      ElMessage.warning(`文件"${file.name}"超过50MB限制`)
      input.value = ''
      return
    }
  }

  selectedFiles.value.push(...newFiles)
  input.value = '' // Reset input for re-selecting same file
}

function removeSelectedFile(index: number) {
  selectedFiles.value.splice(index, 1)
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function handleNewSession() {
  chatStore.clearMessages()
  await chatStore.startNewSession('新对话')
  currentView.value = 'chat'
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
}

async function handleSelectSession(sessionId: string) {
  if (chatStore.currentSessionId === sessionId && currentView.value === 'chat') return
  currentView.value = 'chat'
  await chatStore.selectSession(sessionId)
  if (isMobile.value) {
    mobilePanel.value = 'chat'
  }
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

function formatTime(date: Date): string {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
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

/* Material Symbols fill variant */
.fill-1 {
  font-variation-settings: 'FILL' 1;
}
</style>
