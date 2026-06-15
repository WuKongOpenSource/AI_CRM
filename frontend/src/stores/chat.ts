import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  createSession,
  getSessionList,
  deleteSession,
  setSessionPinned as setSessionPinnedRequest,
  getMessageList,
  getChatModelOptions,
  getChatAppOptions,
  sendMessageStream,
  sendMessageSync
} from '@/api/chat'
import type { ChatSession, ChatAttachmentDTO, ChatAttachmentVO, ChatMessage, ChatModelOption, ChatAppOption } from '@/types/common'
import type { CustomerListVO } from '@/types/customer'
import type { AddressBookEmployee } from '@/types/addressBook'
import type { RelationVO } from '@/types/relation'
import type { ProjectEntity, ProjectTask } from '@/types/project'
import type { ProductVO } from '@/types/product'

interface LocalMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  isStreaming?: boolean
  isThinking?: boolean
  attachments?: ChatAttachmentVO[]
}

interface StreamingTask {
  sessionId: string
  userMessageId: string
  assistantMessageId: string
  startedAt: number
  abortController: AbortController
  thinkingTimerId?: number
}

interface PendingNewSessionDraft {
  title?: string
  agentId?: string
  customerId?: string
  employeeId?: string
  relationId?: string
  productId?: string
  projectId?: string
  projectTaskId?: string
  appCode: string
}

interface ProjectSessionContext {
  projectId: string
  projectTaskId?: string
}

export const useChatStore = defineStore('chat', () => {
  const MODEL_STORAGE_KEY = 'wk_ai_crm:chat_selected_model:v1'
  /** Per-session UI app selection (follows conversation across switches / refresh). */
  const SESSION_APP_BY_ID_STORAGE_KEY = 'wk_ai_crm:chat_session_app_by_id:v1'
  const PROJECT_SESSION_CONTEXT_STORAGE_KEY = 'wk_ai_crm:chat_project_session_context:v1'
  /** Unsent composer text per session (survives refresh). */
  const COMPOSER_DRAFT_BY_SESSION_STORAGE_KEY = 'wk_ai_crm:chat_composer_draft_by_session:v1'
  const GENERAL_APP_CODE = 'general'
  const CRM_APP_CODE = 'crm'
  const PROJECT_APP_CODE = 'project'
  const KNOWLEDGE_APP_CODE = 'knowledge'
  const ADDRESS_BOOK_APP_CODE = 'address_book'
  const RELATION_APP_CODE = 'relation'
  const PRODUCT_APP_CODE = 'product'
  const STREAM_IDLE_THINKING_DELAY_MS = 3000

  function loadSessionAppCodeBySessionId(): Record<string, string> {
    try {
      const raw = localStorage.getItem(SESSION_APP_BY_ID_STORAGE_KEY)
      if (!raw) return {}
      const parsed = JSON.parse(raw) as Record<string, unknown>
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
      const allowed = new Set([GENERAL_APP_CODE, CRM_APP_CODE, PROJECT_APP_CODE, KNOWLEDGE_APP_CODE, ADDRESS_BOOK_APP_CODE, RELATION_APP_CODE, PRODUCT_APP_CODE])
      const out: Record<string, string> = {}
      for (const [k, v] of Object.entries(parsed)) {
        if (typeof v === 'string' && k.length > 0) {
          const c = v.trim().toLowerCase()
          if (allowed.has(c)) out[k] = c
        }
      }
      return out
    } catch {
      return {}
    }
  }

  const MAX_COMPOSER_DRAFT_CHARS = 20000
  const MAX_COMPOSER_DRAFT_SESSIONS = 64

  function loadComposerDraftsBySessionId(): Record<string, string> {
    try {
      const raw = localStorage.getItem(COMPOSER_DRAFT_BY_SESSION_STORAGE_KEY)
      if (!raw) return {}
      const parsed = JSON.parse(raw) as Record<string, unknown>
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
      const out: Record<string, string> = {}
      for (const [k, v] of Object.entries(parsed)) {
        if (typeof v === 'string' && k.length > 0) {
          out[k] = v.length > MAX_COMPOSER_DRAFT_CHARS ? v.slice(0, MAX_COMPOSER_DRAFT_CHARS) : v
        }
      }
      return out
    } catch {
      return {}
    }
  }

  function loadProjectSessionContextBySessionId(): Record<string, ProjectSessionContext> {
    try {
      const raw = localStorage.getItem(PROJECT_SESSION_CONTEXT_STORAGE_KEY)
      if (!raw) return {}
      const parsed = JSON.parse(raw) as Record<string, unknown>
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {}
      const out: Record<string, ProjectSessionContext> = {}
      for (const [sessionId, value] of Object.entries(parsed)) {
        if (!sessionId || !value || typeof value !== 'object' || Array.isArray(value)) continue
        const context = value as Record<string, unknown>
        const projectId = typeof context.projectId === 'string' ? context.projectId : ''
        const projectTaskId = typeof context.projectTaskId === 'string' ? context.projectTaskId : undefined
        if (projectId) out[sessionId] = { projectId, projectTaskId }
      }
      return out
    } catch {
      return {}
    }
  }

  const sessions = ref<ChatSession[]>([])
  const currentSessionId = ref<string | null>(null)
  /** Incremented so ChatView can focus the composer after sidebar session / new-chat actions. */
  const composerFocusNonce = ref(0)
  function requestComposerFocus() {
    composerFocusNonce.value += 1
  }
  const pendingNewSessionDraft = ref<PendingNewSessionDraft | null>(null)
  const isNewSessionPending = computed(() => pendingNewSessionDraft.value !== null)
  const messagesBySessionId = ref<Record<string, LocalMessage[]>>({})
  const streamingTasks = ref<Record<string, StreamingTask>>({})
  const loading = ref(false)
  const sessionsLoading = ref(false)
  let fetchSessionsPromise: Promise<void> | null = null
  const modelOptionsLoading = ref(false)
  const appOptionsLoading = ref(false)
  const modelOptions = ref<ChatModelOption[]>([])
  const appOptions = ref<ChatAppOption[]>([])
  const sessionAppCodeBySessionId = ref<Record<string, string>>(loadSessionAppCodeBySessionId())
  const projectSessionContextBySessionId = ref<Record<string, ProjectSessionContext>>(loadProjectSessionContextBySessionId())
  const composerDraftBySessionId = ref<Record<string, string>>(loadComposerDraftsBySessionId())
  const selectedAppCode = ref(GENERAL_APP_CODE)
  const selectedModelKey = ref(loadSelectedModelKey())
  const ragEnabled = computed(() => selectedAppCode.value === KNOWLEDGE_APP_CODE)
  const crmContextEnabled = computed(() => selectedAppCode.value === CRM_APP_CODE)

  const messages = computed(() => {
    if (!currentSessionId.value) return []
    return messagesBySessionId.value[currentSessionId.value] || []
  })

  /** True when any session has an active SSE stream (e.g. sidebar badges). */
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
    if (fetchSessionsPromise) {
      return fetchSessionsPromise
    }

    sessionsLoading.value = true
    fetchSessionsPromise = getSessionList()
      .then((nextSessions) => {
        rememberProjectContextsFromSessions(nextSessions)
        sessions.value = nextSessions
      })
      .finally(() => {
        sessionsLoading.value = false
        fetchSessionsPromise = null
      })

    return fetchSessionsPromise
  }

  function getSessionSortTime(value?: string): number {
    if (!value) return 0
    const time = new Date(value).getTime()
    return Number.isNaN(time) ? 0 : time
  }

  function sortChatSessions(nextSessions: ChatSession[]): ChatSession[] {
    return [...nextSessions].sort((a, b) => {
      const aPinned = Boolean(a.pinned)
      const bPinned = Boolean(b.pinned)
      if (aPinned !== bPinned) return aPinned ? -1 : 1

      if (aPinned && bPinned) {
        const aPinnedTime = getSessionSortTime(a.pinnedTime)
        const bPinnedTime = getSessionSortTime(b.pinnedTime)
        if (aPinnedTime !== bPinnedTime) return bPinnedTime - aPinnedTime
      }

      const aUpdateTime = getSessionSortTime(a.updateTime || a.createTime)
      const bUpdateTime = getSessionSortTime(b.updateTime || b.createTime)
      if (aUpdateTime !== bUpdateTime) return bUpdateTime - aUpdateTime

      return String(b.sessionId).localeCompare(String(a.sessionId))
    })
  }

  function getProjectSessionContext(session?: Pick<ChatSession, 'sessionId' | 'projectId' | 'projectTaskId'> | null): ProjectSessionContext | null {
    if (!session?.sessionId) return null
    const sessionProjectId = session.projectId ? String(session.projectId) : ''
    if (sessionProjectId) {
      return {
        projectId: sessionProjectId,
        projectTaskId: session.projectTaskId ? String(session.projectTaskId) : undefined
      }
    }
    return projectSessionContextBySessionId.value[session.sessionId] || null
  }

  function getCurrentProjectSessionContext(): ProjectSessionContext | null {
    if (currentSession.value) {
      return getProjectSessionContext(currentSession.value)
    }
    return currentSessionId.value ? projectSessionContextBySessionId.value[currentSessionId.value] || null : null
  }

  function isProjectContextSession(session?: Pick<ChatSession, 'sessionId' | 'projectId' | 'projectTaskId'> | null): boolean {
    return Boolean(getProjectSessionContext(session))
  }

  function rememberProjectSessionContext(sessionId: string, projectId: string, projectTaskId?: string) {
    if (!sessionId || !projectId) return
    const next = {
      ...projectSessionContextBySessionId.value,
      [sessionId]: {
        projectId: String(projectId),
        projectTaskId: projectTaskId ? String(projectTaskId) : undefined
      }
    }
    projectSessionContextBySessionId.value = next
    try {
      localStorage.setItem(PROJECT_SESSION_CONTEXT_STORAGE_KEY, JSON.stringify(next))
    } catch {
      // ignore persistence failures
    }
  }

  function rememberProjectContextsFromSessions(nextSessions: ChatSession[]) {
    for (const session of nextSessions) {
      if (session.projectId) {
        rememberProjectSessionContext(
          session.sessionId,
          String(session.projectId),
          session.projectTaskId ? String(session.projectTaskId) : undefined
        )
      }
    }
  }

  function forgetProjectSessionContext(sessionId: string) {
    if (!(sessionId in projectSessionContextBySessionId.value)) return
    const next = { ...projectSessionContextBySessionId.value }
    delete next[sessionId]
    projectSessionContextBySessionId.value = next
    try {
      localStorage.setItem(PROJECT_SESSION_CONTEXT_STORAGE_KEY, JSON.stringify(next))
    } catch {
      // ignore persistence failures
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
    appCode: string = GENERAL_APP_CODE,
    employeeId?: string,
    relationId?: string,
    projectId?: string,
    projectTaskId?: string,
    productId?: string
  ): Promise<string> {
    const normalizedAppCode = normalizeAppCode(appCode)
    const sessionId = await createSession({
      title,
      agentId,
      customerId,
      employeeId,
      relationId,
      productId,
      projectId,
      projectTaskId,
      appCode: normalizedAppCode
    })
    pendingNewSessionDraft.value = null
    setSessionMessages(sessionId, [])
    if (projectId) {
      rememberProjectSessionContext(sessionId, projectId, projectTaskId)
    }
    await fetchSessions()
    currentSessionId.value = sessionId
    setSelectedAppCode(normalizedAppCode)
    return sessionId
  }

  function beginNewSessionDraft(
    title?: string,
    agentId?: string,
    customerId?: string,
    appCode: string = GENERAL_APP_CODE,
    employeeId?: string,
    relationId?: string,
    projectId?: string,
    projectTaskId?: string,
    productId?: string
  ) {
    const normalizedAppCode = normalizeAppCode(appCode)
    currentSessionId.value = null
    pendingNewSessionDraft.value = {
      title,
      agentId,
      customerId,
      employeeId,
      relationId,
      productId,
      projectId,
      projectTaskId,
      appCode: normalizedAppCode
    }
    selectedAppCode.value = normalizedAppCode
  }

  async function startNewSessionIfNeeded(
    title?: string,
    agentId?: string,
    customerId?: string,
    appCode: string = GENERAL_APP_CODE,
    employeeId?: string,
    relationId?: string,
    projectId?: string,
    projectTaskId?: string,
    productId?: string
  ): Promise<void> {
    beginNewSessionDraft(title, agentId, customerId, appCode, employeeId, relationId, projectId, projectTaskId, productId)
  }

  async function selectSession(sessionId: string) {
    pendingNewSessionDraft.value = null
    currentSessionId.value = sessionId
    const session = sessions.value.find(s => s.sessionId === sessionId)
    const fromMap = sessionAppCodeBySessionId.value[sessionId]
    if (fromMap !== undefined) {
      selectedAppCode.value = normalizeAppCode(fromMap)
    } else if (session?.appCode) {
      selectedAppCode.value = normalizeAppCode(session.appCode)
    } else {
      selectedAppCode.value = GENERAL_APP_CODE
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

  async function openCustomerChat(customer: Pick<CustomerListVO, 'customerId' | 'companyName'>): Promise<string> {
    const customerId = String(customer.customerId)
    if (sessions.value.length === 0) {
      await fetchSessions()
    }
    const existingSession = sessions.value
      .filter(session => String(session.customerId || '') === customerId)
      .sort((a, b) => new Date(b.updateTime || b.createTime).getTime() - new Date(a.updateTime || a.createTime).getTime())[0]

    if (existingSession) {
      await selectSession(existingSession.sessionId)
      setSelectedAppCode(CRM_APP_CODE)
      requestComposerFocus()
      return existingSession.sessionId
    }

    const sessionId = await startNewSession(customer.companyName || '客户对话', undefined, customerId, CRM_APP_CODE)
    requestComposerFocus()
    return sessionId
  }

  async function openEmployeeChat(employee: Pick<AddressBookEmployee, 'userId' | 'realname'>): Promise<string> {
    const employeeId = String(employee.userId)
    if (sessions.value.length === 0) {
      await fetchSessions()
    }
    const existingSession = sessions.value
      .filter(session => String(session.employeeId || '') === employeeId)
      .sort((a, b) => new Date(b.updateTime || b.createTime).getTime() - new Date(a.updateTime || a.createTime).getTime())[0]

    if (existingSession) {
      await selectSession(existingSession.sessionId)
      setSelectedAppCode(ADDRESS_BOOK_APP_CODE)
      requestComposerFocus()
      return existingSession.sessionId
    }

    const sessionId = await startNewSession(`与${employee.realname || '员工'}对话`, undefined, undefined, ADDRESS_BOOK_APP_CODE, employeeId)
    requestComposerFocus()
    return sessionId
  }

  async function openRelationChat(relation: Pick<RelationVO, 'relationId' | 'name'>): Promise<string> {
    const relationId = String(relation.relationId)
    if (sessions.value.length === 0) {
      await fetchSessions()
    }
    const existingSession = sessions.value
      .filter(session => String(session.relationId || '') === relationId)
      .sort((a, b) => new Date(b.updateTime || b.createTime).getTime() - new Date(a.updateTime || a.createTime).getTime())[0]

    if (existingSession) {
      await selectSession(existingSession.sessionId)
      setSelectedAppCode(RELATION_APP_CODE)
      requestComposerFocus()
      return existingSession.sessionId
    }

    const sessionId = await startNewSession(`与${relation.name || '关系人'}对话`, undefined, undefined, RELATION_APP_CODE, undefined, relationId)
    requestComposerFocus()
    return sessionId
  }

  async function openProductChat(product: Pick<ProductVO, 'productId' | 'productName'>): Promise<string> {
    const productId = String(product.productId)
    if (sessions.value.length === 0) {
      await fetchSessions()
    }
    const existingSession = sessions.value
      .filter(session => String(session.productId || '') === productId)
      .sort((a, b) => new Date(b.updateTime || b.createTime).getTime() - new Date(a.updateTime || a.createTime).getTime())[0]

    if (existingSession) {
      await selectSession(existingSession.sessionId)
      setSelectedAppCode(PRODUCT_APP_CODE)
      requestComposerFocus()
      return existingSession.sessionId
    }

    const sessionId = await startNewSession(product.productName || '产品对话', undefined, undefined, PRODUCT_APP_CODE, undefined, undefined, undefined, undefined, productId)
    requestComposerFocus()
    return sessionId
  }

  async function openProjectChat(project: Pick<ProjectEntity, 'projectId' | 'name'>): Promise<string> {
    const projectId = String(project.projectId)
    if (sessions.value.length === 0) {
      await fetchSessions()
    }
    const currentContext = getCurrentProjectSessionContext()
    if (
      currentSessionId.value
      && currentContext?.projectId === projectId
      && !currentContext.projectTaskId
    ) {
      setSelectedAppCode(PROJECT_APP_CODE)
      requestComposerFocus()
      return currentSessionId.value
    }
    const existingSession = sessions.value
      .filter((session) => {
        const context = getProjectSessionContext(session)
        return context?.projectId === projectId && !context.projectTaskId
      })
      .sort((a, b) => new Date(b.updateTime || b.createTime).getTime() - new Date(a.updateTime || a.createTime).getTime())[0]

    if (existingSession) {
      rememberProjectSessionContext(existingSession.sessionId, projectId)
      await selectSession(existingSession.sessionId)
      setSelectedAppCode(PROJECT_APP_CODE)
      requestComposerFocus()
      return existingSession.sessionId
    }

    const sessionId = await startNewSession(project.name || '项目对话', undefined, undefined, PROJECT_APP_CODE, undefined, undefined, projectId)
    requestComposerFocus()
    return sessionId
  }

  async function openProjectTaskChat(
    project: Pick<ProjectEntity, 'projectId' | 'name'>,
    task: Pick<ProjectTask, 'taskId' | 'title'>
  ): Promise<string> {
    const projectId = String(project.projectId)
    const projectTaskId = String(task.taskId)
    if (sessions.value.length === 0) {
      await fetchSessions()
    }
    const currentContext = getCurrentProjectSessionContext()
    if (
      currentSessionId.value
      && currentContext?.projectId === projectId
      && currentContext.projectTaskId === projectTaskId
    ) {
      setSelectedAppCode(PROJECT_APP_CODE)
      requestComposerFocus()
      return currentSessionId.value
    }
    const existingSession = sessions.value
      .filter((session) => {
        const context = getProjectSessionContext(session)
        return context?.projectId === projectId && context.projectTaskId === projectTaskId
      })
      .sort((a, b) => new Date(b.updateTime || b.createTime).getTime() - new Date(a.updateTime || a.createTime).getTime())[0]

    if (existingSession) {
      rememberProjectSessionContext(existingSession.sessionId, projectId, projectTaskId)
      await selectSession(existingSession.sessionId)
      setSelectedAppCode(PROJECT_APP_CODE)
      requestComposerFocus()
      return existingSession.sessionId
    }

    const sessionId = await startNewSession(
      task.title || project.name || '任务对话',
      undefined,
      undefined,
      PROJECT_APP_CODE,
      undefined,
      undefined,
      projectId,
      projectTaskId
    )
    requestComposerFocus()
    return sessionId
  }

  async function removeSession(sessionId: string) {
    await deleteSession(sessionId)
    sessions.value = sessions.value.filter(s => s.sessionId !== sessionId)
    delete messagesBySessionId.value[sessionId]
    delete streamingTasks.value[sessionId]
    deleteSessionAppCodeRecord(sessionId)
    deleteComposerDraftForSession(sessionId)
    forgetProjectSessionContext(sessionId)

    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
    }
  }

  async function setSessionPinned(sessionId: string, pinned: boolean) {
    const previousSessions = sessions.value.map(session => ({ ...session }))
    const nextPinnedTime = pinned ? new Date().toISOString() : undefined
    sessions.value = sortChatSessions(
      sessions.value.map(session =>
        session.sessionId === sessionId
          ? { ...session, pinned, pinnedTime: nextPinnedTime }
          : session
      )
    )

    try {
      await setSessionPinnedRequest(sessionId, pinned)
      await fetchSessions()
    } catch (error) {
      sessions.value = previousSessions
      throw error
    }
  }

  async function sendMessage(
    content: string,
    attachments?: ChatAttachmentDTO[],
    attachmentVOs?: ChatAttachmentVO[],
    appCodeOrUseRag?: string | boolean,
    knowledgeIds?: string[]
  ): Promise<string> {
    const effectiveAppCode = resolveEffectiveAppCode(appCodeOrUseRag, knowledgeIds)
    const sessionId = await ensureSessionForSend(effectiveAppCode)
    const projectContext = effectiveAppCode === PROJECT_APP_CODE
      ? getProjectSessionContext(sessions.value.find(session => session.sessionId === sessionId) || { sessionId })
      : null
    const productId = effectiveAppCode === PRODUCT_APP_CODE
      ? String(sessions.value.find(session => session.sessionId === sessionId)?.productId || '')
      : ''
    // Allow other sessions to stream concurrently; only block double-send on this session.
    if (streamingTasks.value[sessionId]) return sessionId

    const userMessageId = createLocalMessageId('user')
    const assistantMessageId = createLocalMessageId('assistant')

    appendSessionMessage(sessionId, {
      id: userMessageId,
      role: 'user',
      content,
      timestamp: new Date(),
      attachments: attachmentVOs
    })

    appendSessionMessage(sessionId, {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true,
      isThinking: true
    })

    const abortController = new AbortController()
    streamingTasks.value[sessionId] = {
      sessionId,
      userMessageId,
      assistantMessageId,
      startedAt: Date.now(),
      abortController
    }
    scheduleStreamingThinking(sessionId, assistantMessageId)

    try {
      await sendMessageStream(
        sessionId,
        content,
        (chunk) => {
          const assistantMessage = ensureStreamingAssistantMessage(sessionId, assistantMessageId)
          if (!chunk) return
          assistantMessage.isThinking = false
          assistantMessage.content += chunk
          scheduleStreamingThinking(sessionId, assistantMessageId)
        },
        async () => {
          finishStreamingAssistantMessage(sessionId, assistantMessageId)
          delete streamingTasks.value[sessionId]
          await fetchSessions()
        },
        (error) => {
          console.error('Stream error:', error)
          clearStreamingThinkingTimer(streamingTasks.value[sessionId])
          const assistantMessage = ensureStreamingAssistantMessage(sessionId, assistantMessageId)
          if (!assistantMessage.content) {
            assistantMessage.content = '抱歉，发生错误，请重试。'
          }
          assistantMessage.isThinking = false
          assistantMessage.isStreaming = false
        },
        attachments,
        effectiveAppCode,
        effectiveAppCode === KNOWLEDGE_APP_CODE || Boolean(knowledgeIds?.length),
        selectedModel.value?.provider,
        selectedModel.value?.modelName,
        selectedModel.value?.modelSource,
        knowledgeIds,
        projectContext?.projectId,
        projectContext?.projectTaskId,
        productId || undefined,
        abortController.signal
      )
    } catch (error) {
      console.error('sendMessage error:', error)
    } finally {
      finishStreamingAssistantMessage(sessionId, assistantMessageId)
      delete streamingTasks.value[sessionId]
    }
    return sessionId
  }

  async function sendMessageWithSync(content: string, appCodeOrUseRag?: string | boolean): Promise<string> {
    const effectiveAppCode = resolveEffectiveAppCode(appCodeOrUseRag)
    const sessionId = await ensureSessionForSend(effectiveAppCode)
    const projectContext = effectiveAppCode === PROJECT_APP_CODE
      ? getProjectSessionContext(sessions.value.find(session => session.sessionId === sessionId) || { sessionId })
      : null
    const productId = effectiveAppCode === PRODUCT_APP_CODE
      ? String(sessions.value.find(session => session.sessionId === sessionId)?.productId || '')
      : ''

    if (streamingTasks.value[sessionId]) {
      return ''
    }

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
        selectedModel.value?.modelName,
        selectedModel.value?.modelSource,
        undefined,
        projectContext?.projectId,
        projectContext?.projectTaskId,
        productId || undefined
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
    pendingNewSessionDraft.value = null
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

  function assignSessionAppCode(sessionId: string, code: string) {
    const normalized = normalizeAppCode(code)
    const next: Record<string, string> = { ...sessionAppCodeBySessionId.value, [sessionId]: normalized }
    const keys = Object.keys(next)
    if (keys.length > 200) {
      const pruned: Record<string, string> = {}
      for (const k of keys.slice(-200)) {
        pruned[k] = next[k]
      }
      sessionAppCodeBySessionId.value = pruned
    } else {
      sessionAppCodeBySessionId.value = next
    }
    try {
      localStorage.setItem(SESSION_APP_BY_ID_STORAGE_KEY, JSON.stringify(sessionAppCodeBySessionId.value))
    } catch {
      // ignore storage failures
    }
  }

  function deleteSessionAppCodeRecord(sessionId: string) {
    if (!(sessionId in sessionAppCodeBySessionId.value)) return
    const next = { ...sessionAppCodeBySessionId.value }
    delete next[sessionId]
    sessionAppCodeBySessionId.value = next
    try {
      localStorage.setItem(SESSION_APP_BY_ID_STORAGE_KEY, JSON.stringify(sessionAppCodeBySessionId.value))
    } catch {
      // ignore storage failures
    }
  }

  function persistComposerDraftsBySessionId() {
    try {
      localStorage.setItem(COMPOSER_DRAFT_BY_SESSION_STORAGE_KEY, JSON.stringify(composerDraftBySessionId.value))
    } catch {
      // ignore storage failures
    }
  }

  function setComposerDraft(sessionId: string, text: string) {
    let t = text
    if (t.length > MAX_COMPOSER_DRAFT_CHARS) t = t.slice(0, MAX_COMPOSER_DRAFT_CHARS)
    const next = { ...composerDraftBySessionId.value }
    if (!t) {
      delete next[sessionId]
    } else {
      next[sessionId] = t
    }
    const keys = Object.keys(next)
    if (keys.length > MAX_COMPOSER_DRAFT_SESSIONS) {
      const pruned: Record<string, string> = {}
      for (const k of keys.slice(-MAX_COMPOSER_DRAFT_SESSIONS)) {
        pruned[k] = next[k]!
      }
      composerDraftBySessionId.value = pruned
    } else {
      composerDraftBySessionId.value = next
    }
    persistComposerDraftsBySessionId()
  }

  function getComposerDraft(sessionId: string): string {
    return composerDraftBySessionId.value[sessionId] ?? ''
  }

  function deleteComposerDraftForSession(sessionId: string) {
    if (!(sessionId in composerDraftBySessionId.value)) return
    const next = { ...composerDraftBySessionId.value }
    delete next[sessionId]
    composerDraftBySessionId.value = next
    persistComposerDraftsBySessionId()
  }

  function setSelectedAppCode(value: string) {
    const normalized = normalizeAppCode(value)
    selectedAppCode.value = normalized
    if (pendingNewSessionDraft.value) {
      pendingNewSessionDraft.value = {
        ...pendingNewSessionDraft.value,
        appCode: normalized
      }
    }
    const sid = currentSessionId.value
    if (sid) {
      assignSessionAppCode(sid, normalized)
    }
  }

  function isSessionStreaming(sessionId: string): boolean {
    return Boolean(streamingTasks.value[sessionId])
  }

  function stopStreaming(sessionId?: string) {
    const targetSessionId = sessionId ?? currentSessionId.value
    if (!targetSessionId) return
    const task = streamingTasks.value[targetSessionId]
    if (!task) return
    clearStreamingThinkingTimer(task)
    task.abortController.abort()
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
      assistantMessage.isThinking = false
    }
  }

  function finishStreamingAssistantMessage(sessionId: string, assistantMessageId: string) {
    clearStreamingThinkingTimer(streamingTasks.value[sessionId])
    markAssistantMessageDone(sessionId, assistantMessageId)
  }

  function clearStreamingThinkingTimer(task?: StreamingTask) {
    if (task?.thinkingTimerId === undefined) return
    window.clearTimeout(task.thinkingTimerId)
    task.thinkingTimerId = undefined
  }

  function scheduleStreamingThinking(sessionId: string, assistantMessageId: string) {
    const task = streamingTasks.value[sessionId]
    if (!task || task.assistantMessageId !== assistantMessageId) return

    clearStreamingThinkingTimer(task)
    task.thinkingTimerId = window.setTimeout(() => {
      const currentTask = streamingTasks.value[sessionId]
      if (!currentTask || currentTask.assistantMessageId !== assistantMessageId) return

      const assistantMessage = findSessionMessage(sessionId, assistantMessageId)
      if (assistantMessage?.isStreaming) {
        assistantMessage.isThinking = true
      }
      currentTask.thinkingTimerId = undefined
    }, STREAM_IDLE_THINKING_DELAY_MS)
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

  async function ensureSessionForSend(effectiveAppCode: string): Promise<string> {
    const normalizedAppCode = normalizeAppCode(effectiveAppCode)
    if (!currentSessionId.value) {
      const draft = pendingNewSessionDraft.value
      return await startNewSession(
        draft?.title,
        draft?.agentId,
        draft?.customerId,
        normalizedAppCode || draft?.appCode || GENERAL_APP_CODE,
        draft?.employeeId,
        draft?.relationId,
        draft?.projectId,
        draft?.projectTaskId,
        draft?.productId
      )
    }

    pendingNewSessionDraft.value = null
    setSelectedAppCode(normalizedAppCode)
    return currentSessionId.value
  }

  function normalizeAppCode(appCode?: string): string {
    const code = (appCode || GENERAL_APP_CODE).trim().toLowerCase()
    const knownCodes = appOptions.value.map(option => option.code)
    if (knownCodes.length === 0) {
      return [GENERAL_APP_CODE, CRM_APP_CODE, PROJECT_APP_CODE, KNOWLEDGE_APP_CODE, ADDRESS_BOOK_APP_CODE, RELATION_APP_CODE, PRODUCT_APP_CODE].includes(code) ? code : GENERAL_APP_CODE
    }
    return knownCodes.includes(code) ? code : GENERAL_APP_CODE
  }

  function loadSelectedModelKey(): string {
    try {
      return localStorage.getItem(MODEL_STORAGE_KEY) || ''
    } catch {
      return ''
    }
  }

  function toModelKey(option: ChatModelOption): string {
    return `${option.modelSource || 'system'}:${option.provider}:${option.modelName}`
  }

  return {
    sessions,
    currentSessionId,
    isNewSessionPending,
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
    beginNewSessionDraft,
    startNewSessionIfNeeded,
    selectSession,
    openCustomerChat,
    openEmployeeChat,
    openRelationChat,
    openProductChat,
    openProjectChat,
    openProjectTaskChat,
    isProjectContextSession,
    removeSession,
    setSessionPinned,
    sendMessage,
    sendMessageWithSync,
    clearMessages,
    setRagEnabled,
    setSelectedModelKey,
    setSelectedAppCode,
    toModelKey,
    setCrmContextEnabled,
    setComposerDraft,
    getComposerDraft,
    isSessionStreaming,
    stopStreaming
  }
})
