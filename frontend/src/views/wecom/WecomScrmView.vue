<template>
  <div class="wecom-scrm-view">
    <header class="wecom-scrm-view__header">
      <div>
        <h2>企业微信</h2>
        <p>{{ statusText }}</p>
      </div>
      <div class="wecom-scrm-view__actions">
        <el-segmented v-model="conversationType" :options="conversationTypeOptions" @change="loadConversations" />
        <el-button :loading="syncing" @click="handleSync">
          <span class="material-symbols-outlined mr-1 text-[18px]">sync</span>
          同步
        </el-button>
      </div>
    </header>

    <main class="wecom-scrm-view__workspace">
      <section class="wecom-scrm-view__panel wecom-scrm-view__employees">
        <div class="wecom-scrm-view__toolbar">
          <el-input
            v-model="employeeKeyword"
            clearable
            placeholder="搜索员工"
            @clear="loadEmployees"
            @keyup.enter="loadEmployees"
          >
            <template #prefix>
              <span class="material-symbols-outlined text-[18px] text-slate-400">search</span>
            </template>
          </el-input>
        </div>
        <div v-loading="employeeLoading" class="wecom-scrm-view__list">
          <button
            class="wecom-scrm-view__employee"
            :class="{ 'is-active': !selectedEmployeeUserId }"
            @click="selectEmployee('')"
          >
            <span class="material-symbols-outlined">groups</span>
            <span>全部员工</span>
          </button>
          <button
            v-for="employee in employees"
            :key="employee.id"
            class="wecom-scrm-view__employee"
            :class="{ 'is-active': selectedEmployeeUserId === employee.userId }"
            @click="selectEmployee(employee.userId)"
          >
            <span class="wecom-scrm-view__avatar">{{ initials(employee.name || employee.userId) }}</span>
            <span class="min-w-0 flex-1 truncate">{{ employee.name || employee.userId }}</span>
            <span class="wecom-scrm-view__count">{{ employee.conversationCount || 0 }}</span>
          </button>
        </div>
      </section>

      <section class="wecom-scrm-view__panel wecom-scrm-view__conversations">
        <div class="wecom-scrm-view__toolbar">
          <el-input
            v-model="conversationKeyword"
            clearable
            placeholder="搜索会话"
            @clear="loadConversations"
            @keyup.enter="loadConversations"
          >
            <template #prefix>
              <span class="material-symbols-outlined text-[18px] text-slate-400">search</span>
            </template>
          </el-input>
        </div>
        <div v-loading="conversationLoading" class="wecom-scrm-view__list">
          <button
            v-for="conversation in conversations"
            :key="conversation.id"
            class="wecom-scrm-view__conversation"
            :class="{ 'is-active': activeConversation?.id === conversation.id }"
            @click="selectConversation(conversation)"
          >
            <div class="wecom-scrm-view__conversation-title">
              <span class="truncate">{{ conversation.title || conversation.peerName || conversation.chatId || '企微会话' }}</span>
              <time>{{ formatShortTime(conversation.lastMsgTime) }}</time>
            </div>
            <p>{{ conversation.lastMsgPreview || '暂无消息' }}</p>
          </button>
          <div v-if="!conversationLoading && conversations.length === 0" class="wecom-scrm-view__empty">
            暂无会话
          </div>
        </div>
      </section>

      <section class="wecom-scrm-view__panel wecom-scrm-view__messages">
        <div class="wecom-scrm-view__message-header">
          <div>
            <h3>{{ activeConversation?.title || activeConversation?.peerName || '会话详情' }}</h3>
            <span>{{ activeConversation ? typeLabel(activeConversation.conversationType) : '未选择' }}</span>
          </div>
          <el-tag v-if="activeConversation" type="info" effect="plain">只读</el-tag>
        </div>
        <WecomMessageList :messages="messages" :loading="messageLoading" />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getWecomConversationMessages,
  getWecomSyncStatus,
  queryWecomConversations,
  queryWecomEmployees,
  runWecomSync
} from '@/api/wecom'
import type { WecomConversationType, WecomConversationVO, WecomEmployeeSessionVO, WecomMessageVO } from '@/types/wecom'
import WecomMessageList from './components/WecomMessageList.vue'

const conversationType = ref<WecomConversationType>('customer')
const employeeKeyword = ref('')
const conversationKeyword = ref('')
const selectedEmployeeUserId = ref('')
const employees = ref<WecomEmployeeSessionVO[]>([])
const conversations = ref<WecomConversationVO[]>([])
const messages = ref<WecomMessageVO[]>([])
const activeConversation = ref<WecomConversationVO | null>(null)
const employeeLoading = ref(false)
const conversationLoading = ref(false)
const messageLoading = ref(false)
const syncing = ref(false)
const lastSyncText = ref('')

const conversationTypeOptions = [
  { label: '客户会话', value: 'customer' },
  { label: '员工会话', value: 'employee' },
  { label: '群会话', value: 'group' }
]

const statusText = computed(() => lastSyncText.value || '企微会话存档')

onMounted(async () => {
  await Promise.all([loadEmployees(), loadStatus()])
  await loadConversations()
})

async function loadStatus() {
  const status = await getWecomSyncStatus().catch(() => null)
  lastSyncText.value = status?.lastSyncTime ? `最近同步 ${formatShortTime(status.lastSyncTime)}` : ''
}

async function loadEmployees() {
  employeeLoading.value = true
  try {
    const data = await queryWecomEmployees({ page: 1, limit: 100, keyword: employeeKeyword.value.trim() || undefined })
    employees.value = data.list || []
  } finally {
    employeeLoading.value = false
  }
}

async function loadConversations() {
  conversationLoading.value = true
  messages.value = []
  activeConversation.value = null
  try {
    const data = await queryWecomConversations({
      page: 1,
      limit: 50,
      conversationType: conversationType.value,
      employeeUserId: selectedEmployeeUserId.value || undefined,
      keyword: conversationKeyword.value.trim() || undefined
    })
    conversations.value = data.list || []
    if (conversations.value[0]) {
      await selectConversation(conversations.value[0])
    }
  } finally {
    conversationLoading.value = false
  }
}

async function selectConversation(conversation: WecomConversationVO) {
  activeConversation.value = conversation
  messageLoading.value = true
  try {
    const data = await getWecomConversationMessages(conversation.id, 1, 100)
    messages.value = data.list || []
  } finally {
    messageLoading.value = false
  }
}

async function selectEmployee(userId: string) {
  selectedEmployeeUserId.value = userId
  await loadConversations()
}

async function handleSync() {
  syncing.value = true
  try {
    const status = await runWecomSync()
    ElMessage.success(status.lastSyncStatus === 'failed' ? '同步已记录失败状态' : '同步完成')
    await Promise.all([loadEmployees(), loadStatus(), loadConversations()])
  } finally {
    syncing.value = false
  }
}

function initials(value?: string) {
  return (value || '?').trim().slice(0, 1).toUpperCase()
}

function formatShortTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function typeLabel(value?: string) {
  if (value === 'employee') return '员工会话'
  if (value === 'group') return '群会话'
  return '客户会话'
}
</script>

<style scoped>
.wecom-scrm-view {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #f8fafc;
}

.wecom-scrm-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px 14px;
}

.wecom-scrm-view__header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 700;
  line-height: 30px;
}

.wecom-scrm-view__header p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 13px;
}

.wecom-scrm-view__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.wecom-scrm-view__workspace {
  display: grid;
  min-height: 0;
  flex: 1;
  grid-template-columns: minmax(220px, 270px) minmax(280px, 380px) minmax(360px, 1fr);
  gap: 12px;
  padding: 0 24px 24px;
}

.wecom-scrm-view__panel {
  min-height: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.wecom-scrm-view__toolbar {
  border-bottom: 1px solid #e5e7eb;
  padding: 10px;
}

.wecom-scrm-view__list {
  height: calc(100% - 54px);
  overflow-y: auto;
  padding: 8px;
}

.wecom-scrm-view__employee,
.wecom-scrm-view__conversation {
  width: 100%;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #0f172a;
  text-align: left;
  transition: background 0.16s ease;
}

.wecom-scrm-view__employee {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 10px;
  font-size: 14px;
}

.wecom-scrm-view__employee:hover,
.wecom-scrm-view__conversation:hover,
.wecom-scrm-view__employee.is-active,
.wecom-scrm-view__conversation.is-active {
  background: #f1f5f9;
}

.wecom-scrm-view__avatar {
  display: inline-flex;
  width: 28px;
  height: 28px;
  flex: 0 0 28px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #dcfce7;
  color: #166534;
  font-size: 12px;
  font-weight: 700;
}

.wecom-scrm-view__count {
  color: #94a3b8;
  font-size: 12px;
}

.wecom-scrm-view__conversation {
  margin-bottom: 6px;
  padding: 11px 12px;
}

.wecom-scrm-view__conversation-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 14px;
  font-weight: 650;
}

.wecom-scrm-view__conversation-title time {
  flex: 0 0 auto;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
}

.wecom-scrm-view__conversation p {
  margin: 4px 0 0;
  overflow: hidden;
  color: #64748b;
  display: -webkit-box;
  font-size: 13px;
  line-height: 19px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.wecom-scrm-view__messages {
  display: flex;
  flex-direction: column;
}

.wecom-scrm-view__message-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 13px 16px;
}

.wecom-scrm-view__message-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.wecom-scrm-view__message-header span {
  color: #64748b;
  font-size: 12px;
}

.wecom-scrm-view__messages :deep(.wecom-message-list) {
  flex: 1;
}

.wecom-scrm-view__empty {
  padding: 38px 10px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .wecom-scrm-view__workspace {
    grid-template-columns: 220px minmax(260px, 1fr);
  }

  .wecom-scrm-view__messages {
    grid-column: 1 / -1;
    min-height: 420px;
  }
}

@media (max-width: 720px) {
  .wecom-scrm-view__header {
    align-items: stretch;
    flex-direction: column;
    padding: 14px;
  }

  .wecom-scrm-view__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .wecom-scrm-view__workspace {
    grid-template-columns: 1fr;
    padding: 0 14px 14px;
  }
}
</style>
