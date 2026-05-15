import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  createSession,
  getSessionList,
  deleteSession,
  getMessageList,
  getChatModelOptions,
  getChatAppOptions,
  sendMessageStream,
  sendMessageSync
} from '@/api/chat'
import type { ChatSession, ChatAttachmentDTO, ChatAttachmentVO, ChatMessage, ChatModelOption, ChatAppOption } from '@/types/common'

interface LocalMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  isStreaming?: boolean
  attachments?: ChatAttachmentVO[]
}

interface StreamingTask {
  sessionId: string
  userMessageId: string
  assistantMessageId: string
  startedAt: number
}

export const useChatStore = defineStore('chat', () => {
  const RAG_ENABLED_STORAGE_KEY = 'wk_ai_crm:chat_rag_enabled:v1'
  const MODEL_STORAGE_KEY = 'wk_ai_crm:chat_selected_model:v1'
  const CRM_CONTEXT_ENABLED_STORAGE_KEY = 'wk_ai_crm:chat_crm_context_enabled:v1'
  const APP_CODE_STORAGE_KEY = 'wk_ai_crm:chat_selected_app_code:v1'
  const DRAFT_SESSION_STORAGE_KEY = 'wk_ai_crm:chat_draft_session_id:v1'
  const GENERAL_APP_CODE = 'general'
  const CRM_APP_CODE = 'crm'
  const KNOWLEDGE_APP_CODE = 'knowledge'

  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<string | null>(null)
  /** Incremented so ChatView can focus the composer after sidebar session / new-chat actions. */
  const composerFocusNonce = ref(0)
  function requestComposerFocus() {
    composerFocusNonce.value += 1
  }
  // Tracks the last "new session" that is still empty (draft).
  // Used to avoid creating multiple empty sessions by repeated clicks.
  // Persisted so refresh / new tab still treats the same empty session as draft.
  const draftSessionId = ref<string | null>(loadDraftSessionIdFromStorage())

  function loadDraftSessionIdFromStorage(): string | null {
    try {
      const v = localStorage.getItem(DRAFT_SESSION_STORAGE_KEY)
      return v && v.length > 0 ? v : null
    } catch {
      return null
    }
  }

  function persistDraftSessionId(id: string | null) {
    try {
      if (id) localStorage.setItem(DRAFT_SESSION_STORAGE_KEY, id)
      else localStorage.removeItem(DRAFT_SESSION_STORAGE_KEY)
    } catch {
      // ignore storage failures
    }
  }

  function setDraftSessionId(id: string | null) {
    draftSessionId.value = id
    persistDraftSessionId(id)
  }

  async function reconcileDraftSessionAfterFetch() {
    const id = draftSessionId.value
    if (!id) return
    const exists = sessions.value.some(s => s.sessionId === id)
    if (!exists) {
      setDraftSessionId(null)
      return
    }
    try {
      const dbMessages = await getMessageList(id)
      if (dbMessages.length > 0) {
        setDraftSessionId(null)
      }
    } catch {
      setDraftSessionId(null)
    }
  }
  const messagesBySessionId = ref<Record<string, LocalMessage[]>>({})
  const streamingTasks = ref<Record<string, StreamingTask>>({})
  const loading = ref(false)
  const sessionsLoading = ref(false)
  const modelOptionsLoading = ref(false)
  const appOptionsLoading = ref(false)
  const modelOptions = ref<ChatModelOption[]>([])
  const appOptions = ref<ChatAppOption[]>([])
  const selectedAppCode = ref(loadSelectedAppCode())
  const selectedModelKey = ref(loadSelectedModelKey())
  const ragEnabled = computed(() => selectedAppCode.value === KNOWLEDGE_APP_CODE)
  const crmContextEnabled = computed(() => selectedAppCode.value === CRM_APP_CODE)

  const messages = computed(() => {
    if (!currentSessionId.value) return []
    return messagesBySessionId.value[currentSessionId.value] || []
  })

  const isStreaming = computed(() => Object.keys(streamingTasks.value).length > 0)
  const streamingSessionIds = computed(() => Object.keys(streamingTasks.value))
  const currentSessionIsStreaming = computed(() =>
    currentSessionId.value ? Boolean(streamingTasks.value[currentSessionId.value]) : false
  )

  const currentSession = computed(() =>
    sessions.value.find(s => s.sessionId === currentSessionId.value)
  )

  const selectedModel = computed(() => {
    if (!modelOptions.value.length) return null
    return modelOptions.value.find(option => toModelKey(option) === selectedModelKey.value) || modelOptions.value[0]
  })

  const selectedApp = computed(() =>
    appOptions.value.find(option => option.code === selectedAppCode.value)
    || appOptions.value.find(option => option.code === GENERAL_APP_CODE)
    || null
  )

  async function fetchSessions() {
    sessionsLoading.value = true
    try {
      sessions.value = await getSessionList()
      await reconcileDraftSessionAfterFetch()
    } finally {
      sessionsLoading.value = false
    }
  }

  async function fetchModelOptions() {
    modelOptionsLoading.value = true
    try {
      modelOptions.value = await getChatModelOptions()
      if (modelOptions.value.length) {
        const exists = modelOptions.value.some(option => toModelKey(option) === selectedModelKey.value)
        if (!selectedModelKey.value || !exists) {
          setSelectedModelKey(toModelKey(modelOptions.value[0]))
        }
      } else {
        selectedModelKey.value = ''
      }
    } finally {
      modelOptionsLoading.value = false
    }
  }

  async function fetchAppOptions() {
    appOptionsLoading.value = true
    try {
      appOptions.value = await getChatAppOptions()
      const exists = appOptions.value.some(option => option.code === selectedAppCode.value)
      if (!exists) {
        setSelectedAppCode(GENERAL_APP_CODE)
      }
    } finally {
      appOptionsLoading.value = false
    }
  }

  async function startNewSession(
    title?: string,
    agentId?: string,
    customerId?: string,
    appCode = selectedAppCode.value
  ): Promise<string> {
    const normalizedAppCode = normalizeAppCode(appCode)
    const sessionId = await createSession({ title, agentId, customerId, appCode: normalizedAppCode })
    setSessionMessages(sessionId, [])
    await fetchSessions()
    currentSessionId.value = sessionId
    setSelectedAppCode(normalizedAppCode)
    setDraftSessionId(sessionId)
    return sessionId
  }

  async function startNewSessionIfNeeded(
    title?: string,
    agentId?: string,
    customerId?: string,
    appCode = selectedAppCode.value
  ): Promise<string> {
    const normalizedAppCode = normalizeAppCode(appCode)
    const id = draftSessionId.value
    if (id) {
      if (!streamingTasks.value[id]) {
        let msgs = messagesBySessionId.value[id] || []
        const draftSession = sessions.value.find(s => s.sessionId === id)
        const draftAppCode = normalizeAppCode(draftSession?.appCode)
        if (msgs.length === 0) {
          await selectSession(id)
          msgs = messagesBySessionId.value[id] || []
        }
        if (msgs.length === 0 && !streamingTasks.value[id] && draftAppCode === normalizedAppCode) {
          setSelectedAppCode(normalizedAppCode)
          return id
        }
      }
      setDraftSessionId(null)
    }
    return await startNewSession(title, agentId, customerId, normalizedAppCode)
  }

  async function selectSession(sessionId: string) {
    currentSessionId.value = sessionId
    const session = sessions.value.find(s => s.sessionId === sessionId)
    if (session?.appCode) {
      setSelectedAppCode(session.appCode)
    }
    loading.value = true
    try {
      const dbMessages = await getMessageList(sessionId)
      const loadedMessages = dbMessages.map(toLocalMessage)
      setSessionMessages(sessionId, mergeLoadedMessagesWithStreaming(sessionId, loadedMessages))
    } finally {
      loading.value = false
    }
  }

  async function removeSession(sessionId: string) {
    await deleteSession(sessionId)
    sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
    delete messagesBySessionId.value[sessionId]
    delete streamingTasks.value[sessionId]

    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
    }
    if (draftSessionId.value === sessionId) {
      setDraftSessionId(null)
    }
  }

  async function sendMessage(
    content: string,
    attachments?: ChatAttachmentDTO[],
    attachmentVOs?: ChatAttachmentVO[],
    appCodeOrUseRag?: string | boolean,
    knowledgeIds?: string[]
  ): Promise<void> {
    if (isStreaming.value) return

    const effectiveAppCode = resolveEffectiveAppCode(appCodeOrUseRag, knowledgeIds)
    if (!currentSessionId.value) {
      await startNewSession(undefined, undefined, undefined, effectiveAppCode)
    } else if (shouldStartNewSessionForApp(effectiveAppCode)) {
      await startNewSession(undefined, undefined, undefined, effectiveAppCode)
    } else {
      setSelectedAppCode(effectiveAppCode)
    }

    const sessionId = currentSessionId.value!
    const userMessageId = createLocalMessageId('user')
    const assistantMessageId = createLocalMessageId('assistant')

    appendSessionMessage(sessionId, {
      id: userMessageId,
      role: 'user',
      content,
      timestamp: new Date(),
      attachments: attachmentVOs
    })
    if (draftSessionId.value === sessionId) {
      setDraftSessionId(null)
    }

    appendSessionMessage(sessionId, {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true
    })

    streamingTasks.value[sessionId] = {
      sessionId,
      userMessageId,
      assistantMessageId,
      startedAt: Date.now()
    }

    try {
      await sendMessageStream(
        sessionId,
        content,
        (chunk) => {
          const assistantMessage = ensureStreamingAssistantMessage(sessionId, assistantMessageId)
          assistantMessage.content += chunk
        },
        async () => {
          markAssistantMessageDone(sessionId, assistantMessageId)
          delete streamingTasks.value[sessionId]
          await fetchSessions()
        },
        (error) => {
          console.error('Stream error:', error)
          const assistantMessage = ensureStreamingAssistantMessage(sessionId, assistantMessageId)
          if (!assistantMessage.content) {
            assistantMessage.content = '抱歉，发生错误，请重试。'
          }
          assistantMessage.isStreaming = false
        },
        attachments,
        effectiveAppCode,
        effectiveAppCode === KNOWLEDGE_APP_CODE,
        selectedModel.value?.provider,
        selectedModel.value?.modelName,
        knowledgeIds
      )
    } catch (error) {
      console.error('sendMessage error:', error)
    } finally {
      delete streamingTasks.value[sessionId]
      markAssistantMessageDone(sessionId, assistantMessageId)
    }
  }

  async function sendMessageWithSync(content: string, appCodeOrUseRag?: string | boolean): Promise<string> {
    const effectiveAppCode = resolveEffectiveAppCode(appCodeOrUseRag)
    if (!currentSessionId.value) {
      await startNewSession(undefined, undefined, undefined, effectiveAppCode)
    } else if (shouldStartNewSessionForApp(effectiveAppCode)) {
      await startNewSession(undefined, undefined, undefined, effectiveAppCode)
    } else {
      setSelectedAppCode(effectiveAppCode)
    }

    const sessionId = currentSessionId.value!

    appendSessionMessage(sessionId, {
      id: createLocalMessageId('user'),
      role: 'user',
      content,
      timestamp: new Date()
    })

    loading.value = true
    try {
      const response = await sendMessageSync(
        sessionId,
        content,
        undefined,
        effectiveAppCode,
        effectiveAppCode === KNOWLEDGE_APP_CODE,
        selectedModel.value?.provider,
        selectedModel.value?.modelName
      )

      appendSessionMessage(sessionId, {
        id: createLocalMessageId('assistant'),
        role: 'assistant',
        content: response,
        timestamp: new Date()
      })

      return response
    } finally {
      loading.value = false
    }
  }

  function clearMessages() {
    const sessionId = currentSessionId.value
    if (sessionId && !streamingTasks.value[sessionId]) {
      setSessionMessages(sessionId, [])
    }
    currentSessionId.value = null
  }

  function setRagEnabled(value: boolean) {
    setSelectedAppCode(value ? KNOWLEDGE_APP_CODE : GENERAL_APP_CODE)
  }

  function setSelectedModelKey(value: string) {
    selectedModelKey.value = value
    try {
      localStorage.setItem(MODEL_STORAGE_KEY, value)
    } catch {
      // ignore storage failures
    }
  }

  function setCrmContextEnabled(value: boolean) {
    setSelectedAppCode(value ? CRM_APP_CODE : GENERAL_APP_CODE)
  }

  function setSelectedAppCode(value: string) {
    const normalized = normalizeAppCode(value)
    selectedAppCode.value = normalized
    try {
      localStorage.setItem(APP_CODE_STORAGE_KEY, normalized)
      localStorage.setItem(RAG_ENABLED_STORAGE_KEY, normalized === KNOWLEDGE_APP_CODE ? '1' : '0')
      localStorage.setItem(CRM_CONTEXT_ENABLED_STORAGE_KEY, normalized === CRM_APP_CODE ? '1' : '0')
    } catch {
      // ignore storage failures
    }
  }

  function isSessionStreaming(sessionId: string): boolean {
    return Boolean(streamingTasks.value[sessionId])
  }

  function getSessionMessages(sessionId: string): LocalMessage[] {
    if (!messagesBySessionId.value[sessionId]) {
      messagesBySessionId.value[sessionId] = []
    }
    return messagesBySessionId.value[sessionId]
  }

  function setSessionMessages(sessionId: string, nextMessages: LocalMessage[]) {
    messagesBySessionId.value[sessionId] = nextMessages
  }

  function appendSessionMessage(sessionId: string, message: LocalMessage) {
    getSessionMessages(sessionId).push(message)
  }

  function findSessionMessage(sessionId: string, messageId: string): LocalMessage | undefined {
    return getSessionMessages(sessionId).find(message => message.id === messageId)
  }

  function ensureStreamingAssistantMessage(sessionId: string, assistantMessageId: string): LocalMessage {
    const existingMessage = findSessionMessage(sessionId, assistantMessageId)
    if (existingMessage) return existingMessage

    const message: LocalMessage = {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true
    }
    appendSessionMessage(sessionId, message)
    return message
  }

  function markAssistantMessageDone(sessionId: string, assistantMessageId: string) {
    const assistantMessage = findSessionMessage(sessionId, assistantMessageId)
    if (assistantMessage) {
      assistantMessage.isStreaming = false
    }
  }

  function mergeLoadedMessagesWithStreaming(
    sessionId: string,
    loadedMessages: LocalMessage[]
  ): LocalMessage[] {
    const task = streamingTasks.value[sessionId]
    if (!task) return loadedMessages

    const mergedMessages = [...loadedMessages]
    const localMessages = messagesBySessionId.value[sessionId] || []

    for (const localMessage of localMessages) {
      if (mergedMessages.some(message => message.id === localMessage.id)) {
        continue
      }

      if (localMessage.id === task.assistantMessageId || localMessage.isStreaming) {
        mergedMessages.push(localMessage)
        continue
      }

      if (localMessage.id === task.userMessageId && !hasEquivalentLoadedMessage(mergedMessages, localMessage)) {
        mergedMessages.push(localMessage)
      }
    }

    return mergedMessages.sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime())
  }

  function hasEquivalentLoadedMessage(messagesToCheck: LocalMessage[], target: LocalMessage): boolean {
    const equivalenceWindow = 5 * 60 * 1000
    return messagesToCheck.some(message =>
      message.role === target.role &&
      message.content === target.content &&
      Math.abs(message.timestamp.getTime() - target.timestamp.getTime()) <= equivalenceWindow
    )
  }

  function toLocalMessage(message: ChatMessage): LocalMessage {
    return {
      id: message.messageId,
      role: message.role as 'user' | 'assistant',
      content: message.content,
      timestamp: new Date(message.createTime),
      isStreaming: false,
      attachments: message.attachments
    }
  }

  function createLocalMessageId(prefix: 'user' | 'assistant'): string {
    return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  }

  function resolveEffectiveAppCode(appCodeOrUseRag?: string | boolean, knowledgeIds?: string[]): string {
    if (typeof appCodeOrUseRag === 'string' && appCodeOrUseRag.trim()) {
      return normalizeAppCode(appCodeOrUseRag)
    }
    if (typeof appCodeOrUseRag === 'boolean') {
      return appCodeOrUseRag ? KNOWLEDGE_APP_CODE : selectedAppCode.value
    }
    if (knowledgeIds?.length && selectedAppCode.value === GENERAL_APP_CODE) {
      return KNOWLEDGE_APP_CODE
    }
    return normalizeAppCode(selectedAppCode.value)
  }

  function shouldStartNewSessionForApp(appCode: string): boolean {
    const sessionId = currentSessionId.value
    if (!sessionId) return false
    const session = sessions.value.find(s => s.sessionId === sessionId)
    const currentAppCode = normalizeAppCode(session?.appCode || selectedAppCode.value)
    if (currentAppCode === appCode) return false
    return (messagesBySessionId.value[sessionId] || []).length > 0
  }

  function normalizeAppCode(appCode?: string): string {
    const code = (appCode || GENERAL_APP_CODE).trim().toLowerCase()
    const knownCodes = appOptions.value.map(option => option.code)
    if (knownCodes.length === 0) {
      return [GENERAL_APP_CODE, CRM_APP_CODE, KNOWLEDGE_APP_CODE].includes(code) ? code : GENERAL_APP_CODE
    }
    return knownCodes.includes(code) ? code : GENERAL_APP_CODE
  }

  function loadSelectedAppCode(): string {
    try {
      const saved = localStorage.getItem(APP_CODE_STORAGE_KEY)
      if (saved) return normalizeStoredAppCode(saved)
      if (localStorage.getItem(CRM_CONTEXT_ENABLED_STORAGE_KEY) === '1') return CRM_APP_CODE
      if (localStorage.getItem(RAG_ENABLED_STORAGE_KEY) === '1') return KNOWLEDGE_APP_CODE
      return GENERAL_APP_CODE
    } catch {
      return GENERAL_APP_CODE
    }
  }

  function normalizeStoredAppCode(appCode: string): string {
    const code = appCode.trim().toLowerCase()
    return [GENERAL_APP_CODE, CRM_APP_CODE, KNOWLEDGE_APP_CODE].includes(code) ? code : GENERAL_APP_CODE
  }

  function loadSelectedModelKey(): string {
    try {
      return localStorage.getItem(MODEL_STORAGE_KEY) || ''
    } catch {
      return ''
    }
  }

  function toModelKey(option: ChatModelOption): string {
    return `${option.provider}:${option.modelName}`
  }

  return {
    sessions,
    currentSessionId,
    composerFocusNonce,
    requestComposerFocus,
    messages,
    messagesBySessionId,
    streamingTasks,
    streamingSessionIds,
    isStreaming,
    currentSessionIsStreaming,
    loading,
    sessionsLoading,
    modelOptionsLoading,
    appOptionsLoading,
    modelOptions,
    appOptions,
    selectedModelKey,
    selectedModel,
    selectedAppCode,
    selectedApp,
    ragEnabled,
    crmContextEnabled,
    currentSession,
    fetchSessions,
    fetchModelOptions,
    fetchAppOptions,
    startNewSession,
    startNewSessionIfNeeded,
    selectSession,
    removeSession,
    sendMessage,
    sendMessageWithSync,
    clearMessages,
    setRagEnabled,
    setSelectedModelKey,
    setSelectedAppCode,
    toModelKey,
    setCrmContextEnabled,
    isSessionStreaming
  }
})
