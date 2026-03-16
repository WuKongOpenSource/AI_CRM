<template>
  <Teleport to="body">
    <!-- Backdrop -->
    <Transition name="fade">
      <div
        v-if="isOpen"
        class="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-40"
        @click="closeChatDrawer"
      />
    </Transition>

    <!-- Drawer Panel -->
    <Transition name="slide-right">
      <div
        v-if="isOpen"
        class="fixed inset-y-0 right-0 z-50 flex flex-col bg-white shadow-2xl"
        :class="isMobile ? 'w-full' : 'w-full max-w-lg'"
      >
        <!-- Header -->
        <div class="p-4 border-b border-slate-200 flex items-center justify-between shrink-0">
          <div class="flex items-center gap-3">
            <div class="size-9 bg-primary rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary/20">
              <span class="material-symbols-outlined text-lg">smart_toy</span>
            </div>
            <div>
              <h2 class="text-base font-bold text-slate-900">悟空AI 助手</h2>
              <div class="flex items-center gap-1.5">
                <span class="size-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                <span class="text-[10px] text-slate-500 font-medium">在线服务中</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-1">
            <button
              @click="handleOpenFullPage"
              class="size-9 flex items-center justify-center text-slate-400 hover:text-primary hover:bg-primary/5 rounded-lg transition-colors"
              title="在新页面打开"
            >
              <span class="material-symbols-outlined text-xl">open_in_full</span>
            </button>
            <button
              @click="closeChatDrawer"
              class="size-9 flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
            >
              <span class="material-symbols-outlined text-xl">close</span>
            </button>
          </div>
        </div>

        <!-- Tab Bar -->
        <div class="flex border-b border-slate-100 px-4">
          <button
            class="flex items-center gap-2 px-4 py-3 text-sm font-bold border-b-2 transition-colors"
            :class="currentTab === 'chat'
              ? 'text-primary border-primary'
              : 'text-slate-400 border-transparent hover:text-slate-600'"
            @click="currentTab = 'chat'"
          >
            <span class="material-symbols-outlined text-sm">chat</span>
            AI 助手
          </button>
          <button
            class="flex items-center gap-2 px-4 py-3 text-sm font-bold border-b-2 transition-colors relative"
            :class="currentTab === 'notifications'
              ? 'text-primary border-primary'
              : 'text-slate-400 border-transparent hover:text-slate-600'"
            @click="currentTab = 'notifications'"
          >
            <span class="material-symbols-outlined text-sm">notifications</span>
            系统通知
            <span class="size-2 rounded-full bg-red-500 absolute top-2.5 right-2"></span>
          </button>
        </div>

        <!-- Chat Content -->
        <template v-if="currentTab === 'chat'">
          <!-- Messages Area -->
          <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-6 scroll-smooth pb-4">
            <!-- Welcome Section -->
            <template v-if="chatStore.messages.length === 0">
              <div class="flex flex-col items-center text-center space-y-3 py-8">
                <div class="size-14 bg-primary/5 rounded-2xl flex items-center justify-center text-primary border border-primary/10">
                  <span class="material-symbols-outlined text-3xl">auto_awesome</span>
                </div>
                <h3 class="text-lg font-bold text-slate-900">您好，{{ userStore.realname || '用户' }}。</h3>
                <p class="text-slate-400 text-sm max-w-xs">我是您的智能销售助手。有什么我可以帮您的吗？</p>
                <!-- Quick Actions -->
                <div class="flex flex-wrap gap-2 justify-center pt-2">
                  <button
                    v-for="action in quickActions"
                    :key="action.label"
                    class="px-3 py-1.5 bg-white border border-slate-200 rounded-full text-[11px] font-bold text-slate-500 hover:border-primary hover:text-primary transition-all shadow-sm"
                    @click="sendQuickMessage(action.text)"
                  >
                    {{ action.label }}
                  </button>
                </div>
              </div>
            </template>

            <!-- Messages -->
            <template v-else>
              <div
                v-for="message in chatStore.messages"
                :key="message.id"
                class="message-enter"
              >
                <!-- AI Message -->
                <div v-if="message.role !== 'user'" class="flex gap-3">
                  <div class="size-8 rounded-lg bg-primary flex items-center justify-center text-white shrink-0 shadow-sm shadow-primary/20">
                    <span class="material-symbols-outlined text-base">smart_toy</span>
                  </div>
                  <div class="flex-1 space-y-2 min-w-0">
                    <div class="bg-slate-50 text-slate-700 rounded-2xl rounded-tl-none p-3 inline-block max-w-full text-sm leading-relaxed border border-slate-100">
                      <div class="whitespace-pre-wrap" :class="{ 'streaming-cursor': message.isStreaming }">
                        {{ message.content || '...' }}
                      </div>
                    </div>
                    <!-- Attachments -->
                    <div v-if="message.attachments && message.attachments.length > 0" class="space-y-1">
                      <div v-for="att in message.attachments" :key="att.id || att.fileName">
                        <template v-if="att.mimeType && att.mimeType.startsWith('image/')">
                          <el-image
                            :src="att.accessUrl"
                            :preview-src-list="[att.accessUrl]"
                            fit="cover"
                            class="rounded-xl max-h-[150px] max-w-[200px] border border-slate-100"
                            lazy
                          />
                        </template>
                        <template v-else>
                          <a
                            :href="att.accessUrl"
                            target="_blank"
                            class="flex items-center gap-2 p-2 rounded-lg border border-slate-100 hover:bg-slate-50 transition-colors text-xs max-w-[200px]"
                          >
                            <span class="material-symbols-outlined text-slate-400 text-sm">description</span>
                            <span class="truncate text-slate-700">{{ att.fileName }}</span>
                          </a>
                        </template>
                      </div>
                    </div>
                    <div class="text-[10px] text-slate-400 font-medium">{{ formatTime(message.timestamp) }}</div>
                  </div>
                </div>

                <!-- User Message -->
                <div v-else class="flex gap-3 flex-row-reverse">
                  <div class="size-8 rounded-lg bg-slate-100 shrink-0 border border-slate-200 flex items-center justify-center">
                    <span class="material-symbols-outlined text-slate-400 text-base">person</span>
                  </div>
                  <div class="space-y-2 min-w-0 max-w-[80%]">
                    <div class="bg-primary text-white rounded-2xl rounded-tr-none p-3 shadow-sm shadow-primary/10 text-sm leading-relaxed">
                      <div class="whitespace-pre-wrap">{{ message.content || '...' }}</div>
                    </div>
                    <!-- User Attachments -->
                    <div v-if="message.attachments && message.attachments.length > 0" class="space-y-1">
                      <div v-for="att in message.attachments" :key="att.id || att.fileName">
                        <template v-if="att.mimeType && att.mimeType.startsWith('image/')">
                          <el-image
                            :src="att.accessUrl"
                            :preview-src-list="[att.accessUrl]"
                            fit="cover"
                            class="rounded-xl max-h-[150px] max-w-[200px]"
                            lazy
                          />
                        </template>
                        <template v-else>
                          <a
                            :href="att.accessUrl"
                            target="_blank"
                            class="flex items-center gap-2 p-2 rounded-lg border border-white/20 hover:bg-white/10 transition-colors text-xs max-w-[200px]"
                          >
                            <span class="material-symbols-outlined text-white/70 text-sm">description</span>
                            <span class="truncate text-white">{{ att.fileName }}</span>
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
          <div class="shrink-0 border-t border-slate-100 p-3 bg-white">
            <!-- Selected Files Preview -->
            <div v-if="selectedFiles.length > 0" class="flex flex-wrap gap-2 mb-2">
              <div
                v-for="(file, index) in selectedFiles"
                :key="index"
                class="flex items-center gap-1.5 px-2 py-1 bg-slate-50 rounded-lg text-xs text-slate-700 border border-slate-100"
              >
                <span class="material-symbols-outlined text-xs" :class="file.type.startsWith('image/') ? 'text-blue-500' : 'text-slate-400'">
                  {{ file.type.startsWith('image/') ? 'image' : 'description' }}
                </span>
                <span class="truncate max-w-[100px]">{{ file.name }}</span>
                <span
                  class="material-symbols-outlined text-xs text-slate-400 hover:text-red-500 cursor-pointer"
                  @click="removeSelectedFile(index)"
                >close</span>
              </div>
            </div>

            <!-- Input Box -->
            <div class="flex items-center bg-slate-50 border border-slate-200 rounded-xl p-1 focus-within:border-primary transition-all">
              <input
                ref="fileInputRef"
                type="file"
                multiple
                accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.json,.xml"
                class="hidden"
                @change="handleFileSelect"
              />
              <button
                class="size-9 flex items-center justify-center text-slate-400 hover:text-primary transition-colors"
                :disabled="isUploading"
                @click="handleUpload"
              >
                <span class="material-symbols-outlined text-xl">attach_file</span>
              </button>
              <input
                v-model="inputText"
                type="text"
                class="flex-1 bg-transparent border-none focus:ring-0 focus:outline-none text-slate-900 px-2 py-2 text-sm placeholder:text-slate-400"
                placeholder="输入您的问题..."
                :disabled="chatStore.isStreaming || isUploading"
                @keydown.enter.exact.prevent="handleSend"
              />
              <button
                class="size-9 rounded-lg bg-primary text-white flex items-center justify-center hover:bg-primary/90 shadow-sm shadow-primary/20 transition-all disabled:opacity-50"
                :disabled="(!inputText.trim() && selectedFiles.length === 0) || chatStore.isStreaming || isUploading"
                @click="handleSend"
              >
                <span v-if="chatStore.isStreaming || isUploading" class="material-symbols-outlined text-lg animate-spin">progress_activity</span>
                <span v-else class="material-symbols-outlined text-lg">send</span>
              </button>
            </div>
          </div>
        </template>

        <!-- Notifications Content -->
        <template v-else>
          <div class="flex-1 overflow-y-auto p-4 space-y-4">
            <div v-for="notif in notifications" :key="notif.id" class="space-y-2">
              <div class="flex items-center gap-2 px-1">
                <div class="size-7 rounded-lg bg-primary flex items-center justify-center text-white">
                  <span class="material-symbols-outlined text-sm">smart_toy</span>
                </div>
                <span class="text-xs font-bold text-slate-900">Nexus AI 助手</span>
                <span class="text-[10px] text-slate-400">{{ notif.time }}</span>
              </div>
              <div
                class="p-4 bg-white border rounded-xl transition-all"
                :class="notif.unread ? 'border-primary/20 shadow-sm' : 'border-slate-100'"
              >
                <div class="flex gap-3">
                  <div
                    class="size-10 rounded-lg flex items-center justify-center shrink-0"
                    :class="{
                      'bg-blue-50 text-blue-500': notif.type === 'info',
                      'bg-amber-50 text-amber-500': notif.type === 'warning',
                      'bg-emerald-50 text-emerald-500': notif.type === 'success'
                    }"
                  >
                    <span class="material-symbols-outlined text-xl">
                      {{ notif.type === 'info' ? 'upgrade' : notif.type === 'warning' ? 'security' : 'analytics' }}
                    </span>
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-1">
                      <span
                        class="text-[9px] font-bold px-1.5 py-0.5 rounded uppercase tracking-tight"
                        :class="{
                          'bg-blue-50 text-blue-600': notif.type === 'info',
                          'bg-amber-50 text-amber-600': notif.type === 'warning',
                          'bg-emerald-50 text-emerald-600': notif.type === 'success'
                        }"
                      >{{ notif.category }}</span>
                    </div>
                    <h4 class="font-bold text-slate-900 text-sm mb-1">{{ notif.title }}</h4>
                    <p class="text-slate-600 text-xs leading-relaxed">{{ notif.content }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useChatDrawer } from '@/composables/useChatDrawer'
import { useChatStore } from '@/stores/chat'
import { useAgentStore } from '@/stores/agent'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage } from 'element-plus'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import type { ChatAttachmentDTO, ChatAttachmentVO } from '@/types/common'

const router = useRouter()
const { isOpen, closeChatDrawer } = useChatDrawer()
const chatStore = useChatStore()
const agentStore = useAgentStore()
const userStore = useUserStore()
const { isMobile } = useResponsive()

const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<File[]>([])
const isUploading = ref(false)
const currentTab = ref<'chat' | 'notifications'>('chat')

const MAX_FILE_SIZE = 50 * 1024 * 1024
const MAX_FILE_COUNT = 5

const quickActions = [
  { label: '分析客户意向', text: '帮我分析客户意向' },
  { label: '今日待办摘要', text: '帮我总结今日待办事项' },
  { label: '生成今日日报', text: '帮我生成今日工作日报' },
  { label: '查看本周周报', text: '帮我查看本周工作周报' },
  { label: '查询产品报价', text: '帮我查询产品报价信息' },
]

// Mock notifications
const notifications = ref([
  {
    id: 1,
    title: '系统核心引擎升级完成',
    content: 'Nexus AI 已升级至最新版本。本次更新优化了长文本理解能力。',
    time: '1小时前',
    type: 'info',
    category: '系统更新',
    unread: true
  },
  {
    id: 2,
    title: '异地登录安全提醒',
    content: '检测到您的账号存在异地登录行为。如果这不是您的操作，请立即重置密码。',
    time: '3小时前',
    type: 'warning',
    category: '安全警报',
    unread: true
  },
  {
    id: 3,
    title: '本月销售业绩分析报告',
    content: '您上个月的销售目标达成率为 112%。AI 已为您生成了详细的客户贡献度分析。',
    time: '昨天',
    type: 'success',
    category: '业务报告',
    unread: false
  }
])

// Initialize when drawer opens
watch(isOpen, async (open) => {
  if (open) {
    currentTab.value = 'chat'
    if (chatStore.sessions.length === 0) {
      await chatStore.fetchSessions()
    }
    if (agentStore.enabledAgents.length === 0) {
      await agentStore.fetchEnabledAgents()
    }
  }
})

// Auto scroll on new messages
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

function handleOpenFullPage() {
  closeChatDrawer()
  router.push('/chat')
}

async function handleSend() {
  const text = inputText.value.trim()
  const hasFiles = selectedFiles.value.length > 0
  if ((!text && !hasFiles) || chatStore.isStreaming || isUploading.value) return

  const content = text || '请分析这些文件'
  inputText.value = ''

  let attachmentDTOs: ChatAttachmentDTO[] | undefined
  let attachmentVOs: ChatAttachmentVO[] | undefined

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

  if (selectedFiles.value.length + newFiles.length > MAX_FILE_COUNT) {
    ElMessage.warning(`最多只能上传${MAX_FILE_COUNT}个文件`)
    input.value = ''
    return
  }

  for (const file of newFiles) {
    if (file.size > MAX_FILE_SIZE) {
      ElMessage.warning(`文件"${file.name}"超过50MB限制`)
      input.value = ''
      return
    }
  }

  selectedFiles.value.push(...newFiles)
  input.value = ''
}

function removeSelectedFile(index: number) {
  selectedFiles.value.splice(index, 1)
}

function formatTime(date: Date): string {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}
</script>

<style scoped>
/* Drawer slide-in animation */
.slide-right-enter-active {
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-right-leave-active {
  transition: transform 0.25s ease-in;
}
.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}

/* Backdrop fade */
.fade-enter-active {
  transition: opacity 0.3s ease;
}
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
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
