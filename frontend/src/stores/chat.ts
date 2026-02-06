import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  createSession,
  getSessionList,
  deleteSession,
  getMessageList,
  sendMessageStream,
  sendMessageSync
} from '@/api/chat'
import type { ChatSession } from '@/types/common'

interface LocalMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  isStreaming?: boolean
}

export const useChatStore = defineStore('chat', () => {
  // State
  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<string | null>(null)
  const messages = ref<LocalMessage[]>([])
  const isStreaming = ref(false)
  const loading = ref(false)

  // Getters
  const currentSession = computed(() =>
    sessions.value.find(s => s.sessionId === currentSessionId.value)
  )

  // Actions
  async function fetchSessions() {
    loading.value = true
    try {
      sessions.value = await getSessionList()
    } finally {
      loading.value = false
    }
  }

  async function startNewSession(title?: string, agentId?: string, customerId?: string): Promise<string> {
    const sessionId = await createSession({ title, agentId, customerId })
    await fetchSessions()
    currentSessionId.value = sessionId
    messages.value = []
    return sessionId
  }

  async function selectSession(sessionId: string) {
    currentSessionId.value = sessionId
    loading.value = true
    try {
      const dbMessages = await getMessageList(sessionId)
      messages.value = dbMessages.map(m => ({
        id: m.messageId,
        role: m.role as 'user' | 'assistant',
        content: m.content,
        timestamp: new Date(m.createTime),
        isStreaming: false
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

  async function sendMessage(content: string): Promise<void> {
    if (!currentSessionId.value) {
      // Create new session if none exists
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
        }
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

  async function sendMessageWithSync(content: string): Promise<string> {
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
      const response = await sendMessageSync(currentSessionId.value!, content)

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

  return {
    // State
    sessions,
    currentSessionId,
    messages,
    isStreaming,
    loading,
    // Getters
    currentSession,
    // Actions
    fetchSessions,
    startNewSession,
    selectSession,
    removeSession,
    sendMessage,
    sendMessageWithSync,
    clearMessages
  }
})
