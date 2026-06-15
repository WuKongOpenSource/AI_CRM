import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  createSession,
  getChatApplications,
  getSessionList,
  deleteSession,
  updateSessionPin,
  getMessageList,
  sendMessageStream,
  sendMessageSync
} from '@/api/chat'
import type { ChatSession, ChatAttachmentDTO, ChatAttachmentVO, ChatAppOption } from '@/types/common'

interface LocalMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  isStreaming?: boolean
  attachments?: ChatAttachmentVO[]
}

export const useChatStore = defineStore('chat', () => {
  const RAG_ENABLED_STORAGE_KEY = 'wk_ai_crm:chat_rag_enabled:v1'

  // State
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<string | null>(null)
  const messages = ref<LocalMessage[]>([])
  const isStreaming = ref(false)
  const loading = ref(false)
  const sessionsLoading = ref(false)
  const ragEnabled = ref(loadRagEnabled())
  const applications = ref<ChatAppOption[]>([])
  const applicationsLoading = ref(false)
  const currentAppCode = ref('crm')

  // Getters
  const currentSession = computed(() =>
    sessions.value.find(s => s.sessionId === currentSessionId.value)
  )

  const currentApplication = computed(() =>
    applications.value.find(app => app.code === currentAppCode.value)
  )

  // Actions
  async function fetchSessions() {
    sessionsLoading.value = true
    try {
      sessions.value = await getSessionList()
    } finally {
      sessionsLoading.value = false
    }
  }

  async function fetchApplications() {
    applicationsLoading.value = true
    try {
      applications.value = await getChatApplications()
      if (!applications.value.some(app => app.code === currentAppCode.value)) {
        setCurrentAppCode(applications.value[0]?.code || 'crm')
      }
    } finally {
      applicationsLoading.value = false
    }
  }

  async function startNewSession(title?: string, agentId?: string, customerId?: string, appCode?: string): Promise<string> {
    const resolvedAppCode = appCode || currentAppCode.value
    const sessionId = await createSession({ title, agentId, customerId, appCode: resolvedAppCode })
    await fetchSessions()
    currentSessionId.value = sessionId
    setCurrentAppCode(resolvedAppCode)
    messages.value = []
    return sessionId
  }

  async function selectSession(sessionId: string) {
    currentSessionId.value = sessionId
    const session = sessions.value.find(s => s.sessionId === sessionId)
    if (session?.appCode) {
      setCurrentAppCode(session.appCode)
    }
    loading.value = true
    try {
      const dbMessages = await getMessageList(sessionId)
      messages.value = dbMessages.map(m => ({
        id: m.messageId,
        role: m.role as 'user' | 'assistant',
        content: m.content,
        timestamp: new Date(m.createTime),
        isStreaming: false,
        attachments: m.attachments
      }))
    } finally {
      loading.value = false
    }
  }

  async function removeSession(sessionId: string) {
    await deleteSession(sessionId)
    sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
  }

  async function setSessionPinned(sessionId: string, pinned: boolean) {
    await updateSessionPin(sessionId, pinned)
    await fetchSessions()
  }

  async function sendMessage(
    content: string,
    attachments?: ChatAttachmentDTO[],
    attachmentVOs?: ChatAttachmentVO[],
    useRag?: boolean,
    knowledgeIds?: string[]
  ): Promise<void> {
    if (!currentSessionId.value) {
      // Create new session if none exists
      await startNewSession('新对话')
    }

    // Add user message
    const userMessage: LocalMessage = {
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: new Date(),
      attachments: attachmentVOs
    }
    messages.value.push(userMessage)

    // Add assistant placeholder
    const assistantMessageId = (Date.now() + 1).toString()
    const assistantMessage: LocalMessage = {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true
    }
    messages.value.push(assistantMessage)

    isStreaming.value = true

    try {
      const effectiveRagEnabled = (useRag ?? ragEnabled.value) || Boolean(knowledgeIds?.length)
      await sendMessageStream(
        currentSessionId.value!,
        content,
        (chunk) => {
          // Update assistant message with chunk
          const lastMessage = messages.value[messages.value.length - 1]
          if (lastMessage && lastMessage.role === 'assistant') {
            lastMessage.content += chunk
          }
        },
        async () => {
          // Complete - mark message as done streaming
          const lastMessage = messages.value[messages.value.length - 1]
          if (lastMessage && lastMessage.role === 'assistant') {
            lastMessage.isStreaming = false
          }
          // Refresh session list to get updated title
          await fetchSessions()
        },
        (error) => {
          console.error('Stream error:', error)
          const lastMessage = messages.value[messages.value.length - 1]
          if (lastMessage && lastMessage.role === 'assistant') {
            if (!lastMessage.content) {
              lastMessage.content = '抱歉，发生错误，请重试。'
            }
            lastMessage.isStreaming = false
          }
        },
        attachments,
        effectiveRagEnabled,
        currentAppCode.value,
        knowledgeIds
      )
    } catch (error) {
      // Error already handled by onError callback, but ensure message is marked as complete
      console.error('sendMessage error:', error)
    } finally {
      // Always reset streaming state
      isStreaming.value = false
      // Ensure the assistant message is marked as not streaming
      const lastMessage = messages.value[messages.value.length - 1]
      if (lastMessage && lastMessage.role === 'assistant' && lastMessage.isStreaming) {
        lastMessage.isStreaming = false
      }
    }
  }

  async function sendMessageWithSync(content: string, useRag?: boolean, knowledgeIds?: string[]): Promise<string> {
    if (!currentSessionId.value) {
      await startNewSession('新对话')
    }

    // Add user message
    const userMessage: LocalMessage = {
      id: Date.now().toString(),
      role: 'user',
      content,
      timestamp: new Date()
    }
    messages.value.push(userMessage)

    loading.value = true
    try {
      const response = await sendMessageSync(
        currentSessionId.value!,
        content,
        undefined,
        (useRag ?? ragEnabled.value) || Boolean(knowledgeIds?.length),
        currentAppCode.value,
        knowledgeIds
      )

      const assistantMessage: LocalMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response,
        timestamp: new Date()
      }
      messages.value.push(assistantMessage)

      return response
    } finally {
      loading.value = false
    }
  }

  function clearMessages() {
    messages.value = []
    currentSessionId.value = null
  }

  function setRagEnabled(value: boolean) {
    ragEnabled.value = value
    try {
      localStorage.setItem(RAG_ENABLED_STORAGE_KEY, value ? '1' : '0')
    } catch {
      // ignore storage failures
    }
  }

  function setCurrentAppCode(appCode: string) {
    currentAppCode.value = appCode || 'crm'
    const app = applications.value.find(item => item.code === currentAppCode.value)
    if (app?.defaultRagEnabled) {
      setRagEnabled(true)
    }
  }

  function loadRagEnabled(): boolean {
    try {
      return localStorage.getItem(RAG_ENABLED_STORAGE_KEY) === '1'
    } catch {
      return false
    }
  }

  return {
    // State
    sessions,
    currentSessionId,
    messages,
    isStreaming,
    loading,
    sessionsLoading,
    ragEnabled,
    applications,
    applicationsLoading,
    currentAppCode,
    // Getters
    currentSession,
    currentApplication,
    // Actions
    fetchSessions,
    fetchApplications,
    startNewSession,
    selectSession,
    removeSession,
    setSessionPinned,
    sendMessage,
    sendMessageWithSync,
    clearMessages,
    setRagEnabled,
    setCurrentAppCode
  }
})
