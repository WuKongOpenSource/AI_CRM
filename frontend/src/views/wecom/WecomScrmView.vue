<template>
  <div class="wecom-scrm-view">
    <header class="wecom-scrm-view__header">
      <div>
        <h2>企业微信</h2>
        <p>{{ statusText }}</p>
      </div>
      <div class="wecom-scrm-view__actions">
        <el-tag v-if="canManageWecom && wecomConfig.thirdPartyEnabled" :type="wecomConfig.thirdPartyAuthorized ? 'success' : 'warning'" effect="plain">
          {{ wecomConfig.thirdPartyAuthorized ? '已授权' : '未授权' }}
        </el-tag>
        <el-tag v-if="canManageWecom" :type="wecomConfig.customerContactEnabled ? 'success' : 'warning'" effect="plain">
          {{ wecomConfig.customerContactEnabled ? '客户联系已启用' : '客户联系未启用' }}
        </el-tag>
        <el-tag v-if="canManageWecom" :type="archiveReady ? 'success' : 'warning'" effect="plain">
          {{ archiveReady ? '会话存档已配置' : '会话存档未配置' }}
        </el-tag>
        <el-button v-if="canManageWecom" :loading="authorizing" @click="handleAuthorize">
          <span class="material-symbols-outlined mr-1 text-[18px]">verified_user</span>
          {{ wecomConfig.thirdPartyAuthorized ? '重新授权' : '授权企业微信' }}
        </el-button>
        <el-button v-if="canManageWecom" @click="openArchiveConfig">
          <span class="material-symbols-outlined mr-1 text-[18px]">settings</span>
          存档配置
        </el-button>
        <el-segmented v-model="conversationType" :options="conversationTypeOptions" @change="loadConversations" />
        <el-button v-if="canManageWecom" :loading="orgSyncing" @click="handleSyncOrg">
          <span class="material-symbols-outlined mr-1 text-[18px]">account_tree</span>
          同步组织
        </el-button>
        <el-button :loading="customerSyncing" :title="customerSyncButtonText" @click="handleSyncCustomers">
          <span class="material-symbols-outlined mr-1 text-[18px]">sync</span>
          {{ customerSyncButtonText }}
        </el-button>
        <el-button v-if="canManageWecom" :loading="archiveSyncing" @click="handleSyncConversations">
          <span class="material-symbols-outlined mr-1 text-[18px]">history</span>
          同步会话
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
            <WecomOpenDataName
              class="min-w-0 flex-1"
              :user-id="employee.userId"
              :corp-id="employee.corpId || wecomConfig.corpId"
              :fallback="employee.name || employee.userId"
            />
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
              <span class="wecom-scrm-view__conversation-name">
                <template v-if="isEmployeeConversation(conversation)">
                  <template v-for="(participant, index) in conversationParticipants(conversation)" :key="participant">
                    <span v-if="index > 0">:</span>
                    <WecomOpenDataName
                      :user-id="participant"
                      :corp-id="conversation.corpId || wecomConfig.corpId"
                      :fallback="participant"
                    />
                  </template>
                </template>
                <template v-else>{{ conversationDisplayTitle(conversation) }}</template>
              </span>
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
            <h3>
              <span class="wecom-scrm-view__message-title">
                <template v-if="activeConversation && isEmployeeConversation(activeConversation)">
                  <template v-for="(participant, index) in conversationParticipants(activeConversation)" :key="participant">
                    <span v-if="index > 0">:</span>
                    <WecomOpenDataName
                      :user-id="participant"
                      :corp-id="activeConversation.corpId || wecomConfig.corpId"
                      :fallback="participant"
                    />
                  </template>
                </template>
                <template v-else>{{ activeConversation ? conversationDisplayTitle(activeConversation) : '会话详情' }}</template>
              </span>
            </h3>
            <span>{{ activeConversation ? typeLabel(activeConversation.conversationType) : '未选择' }}</span>
          </div>
          <el-tag v-if="activeConversation" type="info" effect="plain">只读</el-tag>
        </div>
        <WecomMessageList
          :messages="messages"
          :loading="messageLoading"
          :corp-id="activeConversation?.corpId || wecomConfig.corpId"
        />
      </section>
    </main>

    <el-dialog
      v-model="archiveConfigVisible"
      title="会话存档配置"
      width="540px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="会话存档 Secret">
          <el-input
            v-model="archiveForm.archiveSecret"
            type="password"
            show-password
            :placeholder="wecomConfig.archiveSecretConfigured ? '已配置，留空则不修改' : '企业微信管理端 - 管理工具 - 会话内容存档'"
          />
        </el-form-item>
        <el-form-item label="RSA 私钥（PEM）">
          <el-input
            v-model="archiveForm.archivePrivateKey"
            type="textarea"
            :rows="4"
            :placeholder="wecomConfig.archivePrivateKeyConfigured ? '已配置，留空则不修改' : '-----BEGIN PRIVATE KEY-----'"
          />
        </el-form-item>
        <el-form-item label="公钥版本">
          <el-input v-model="archiveForm.archivePublicKeyVersion" placeholder="上传公钥时设置的版本号，如 1" />
        </el-form-item>
        <el-form-item label="真实 corpId（可选）">
          <el-input
            v-model="archiveForm.archiveCorpId"
            placeholder="留空则使用授权返回的 corpId；若为密文 open_corpid 需填企业真实 corpid"
          />
        </el-form-item>
        <el-form-item label="启用会话存档">
          <el-switch v-model="archiveForm.archiveEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="archiveConfigVisible = false">取消</el-button>
        <el-button type="primary" :loading="archiveSaving" @click="saveArchiveConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getWecomConfig,
  getWecomConversationMessages,
  getWecomOpenAuthorizeUrl,
  getWecomSyncStatus,
  queryWecomConversations,
  queryWecomEmployees,
  runMyWecomCustomerSync,
  runWecomOrgSync,
  runWecomSync,
  saveWecomConfig
} from '@/api/wecom'
import { useUserStore } from '@/stores/user'
import type {
  WecomConfigVO,
  WecomConfigSavePayload,
  WecomConversationType,
  WecomConversationVO,
  WecomEmployeeSessionVO,
  WecomMessageVO,
  WecomSyncStatusVO
} from '@/types/wecom'
import WecomMessageList from './components/WecomMessageList.vue'
import WecomOpenDataName from './components/WecomOpenDataName.vue'

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
const customerSyncing = ref(false)
const archiveSyncing = ref(false)
const orgSyncing = ref(false)
const authorizing = ref(false)
const lastSyncText = ref('')
const wecomConfig = ref<WecomConfigVO>({})
const userStore = useUserStore()
const canManageWecom = computed(() => userStore.permissionsLoaded && userStore.hasPermission('config:ai'))
const customerSyncButtonText = computed(() => (canManageWecom.value ? '同步客户' : '同步我的客户'))
const archiveReady = computed(() => Boolean(
  wecomConfig.value.archiveEnabled
    && wecomConfig.value.archiveSecretConfigured
    && wecomConfig.value.archivePrivateKeyConfigured
    && wecomConfig.value.archivePublicKeyVersion
))

const archiveConfigVisible = ref(false)
const archiveSaving = ref(false)
const archiveForm = ref<{
  archiveSecret: string
  archivePrivateKey: string
  archivePublicKeyVersion: string
  archiveCorpId: string
  archiveEnabled: boolean
}>({
  archiveSecret: '',
  archivePrivateKey: '',
  archivePublicKeyVersion: '',
  archiveCorpId: '',
  archiveEnabled: false
})

const conversationTypeOptions = [
  { label: '客户会话', value: 'customer' },
  { label: '员工会话', value: 'employee' },
  { label: '群会话', value: 'group' }
]

const statusText = computed(() => {
  if (wecomConfig.value.thirdPartyEnabled && !wecomConfig.value.thirdPartyAuthorized) {
    return '请先授权企业微信第三方应用'
  }
  return lastSyncText.value || '企微会话存档'
})

onMounted(async () => {
  await Promise.all([loadConfig(), loadEmployees(), loadStatus()])
  await loadConversations()
})

async function loadConfig() {
  wecomConfig.value = await getWecomConfig().catch(() => ({}))
}

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

async function handleSyncCustomers() {
  if (!ensureWecomAuthorized()) {
    return
  }
  customerSyncing.value = true
  try {
    const status = canManageWecom.value
      ? await runWecomSync({ syncEmployees: false, syncCustomers: true, syncConversations: false })
      : await runMyWecomCustomerSync()
    showSyncMessage(status, canManageWecom.value ? '客户同步完成' : '我的客户同步完成')
    await Promise.all([loadConfig(), loadEmployees(), loadStatus(), loadConversations()])
  } finally {
    customerSyncing.value = false
  }
}

async function handleSyncConversations() {
  if (!ensureWecomAuthorized()) {
    return
  }
  if (!archiveReady.value) {
    ElMessage.warning('请先配置并启用企业微信会话存档')
    return
  }
  archiveSyncing.value = true
  try {
    const status = await runWecomSync({ syncEmployees: false, syncCustomers: false, syncConversations: true })
    showSyncMessage(status, '会话存档同步完成')
    await Promise.all([loadConfig(), loadStatus(), loadConversations()])
  } finally {
    archiveSyncing.value = false
  }
}

async function handleSyncOrg() {
  if (!ensureWecomAuthorized()) {
    return
  }
  orgSyncing.value = true
  try {
    const status = await runWecomOrgSync()
    showSyncMessage(status, '组织同步完成')
    await Promise.all([loadConfig(), loadEmployees(), loadStatus(), loadConversations()])
  } finally {
    orgSyncing.value = false
  }
}

function showSyncMessage(status: WecomSyncStatusVO, successText: string) {
  if (status.lastSyncStatus === 'failed') {
    ElMessage.warning(status.lastSyncError || `${successText}，但已记录失败状态`)
    return
  }
  if ((status.failedCount || 0) > 0) {
    ElMessage.warning(`${successText}，${status.failedCount} 条失败`)
    return
  }
  ElMessage.success(successText)
}

function ensureWecomAuthorized() {
  if (!wecomConfig.value.thirdPartyEnabled) {
    ElMessage.warning('企业微信第三方应用未配置')
    return false
  }
  if (!wecomConfig.value.thirdPartyAuthorized) {
    ElMessage.warning(canManageWecom.value ? '请先点击“授权企业微信”完成第三方应用授权' : '请联系管理员先授权企业微信')
    return false
  }
  return true
}

async function handleAuthorize() {
  authorizing.value = true
  try {
    const redirect = `${window.location.origin}${window.location.pathname}${window.location.hash || '#/wecom/scrm'}`
    const data = await getWecomOpenAuthorizeUrl(redirect)
    if (data.authorizeUrl) {
      window.location.href = data.authorizeUrl
    }
  } finally {
    authorizing.value = false
  }
}

function openArchiveConfig() {
  archiveForm.value = {
    archiveSecret: '',
    archivePrivateKey: '',
    archivePublicKeyVersion: wecomConfig.value.archivePublicKeyVersion || '',
    archiveCorpId: wecomConfig.value.archiveCorpId || '',
    archiveEnabled: Boolean(wecomConfig.value.archiveEnabled)
  }
  archiveConfigVisible.value = true
}

async function saveArchiveConfig() {
  archiveSaving.value = true
  try {
    const form = archiveForm.value
    const payload: WecomConfigSavePayload = {
      archivePublicKeyVersion: form.archivePublicKeyVersion.trim(),
      archiveCorpId: form.archiveCorpId.trim() || undefined,
      archiveEnabled: form.archiveEnabled,
      // 保留现有开关，避免被后端整体重置为 false
      customerContactEnabled: wecomConfig.value.customerContactEnabled ?? true,
      syncEnabled: wecomConfig.value.syncEnabled ?? true
    }
    const secret = form.archiveSecret.trim()
    const privateKey = form.archivePrivateKey.trim()
    if (secret) payload.archiveSecret = secret
    if (privateKey) payload.archivePrivateKey = privateKey
    await saveWecomConfig(payload)
    ElMessage.success('会话存档配置已保存')
    archiveConfigVisible.value = false
    await loadConfig()
  } finally {
    archiveSaving.value = false
  }
}

function initials(value?: string) {
  return (value || '?').trim().slice(0, 1).toUpperCase()
}

function isEmployeeConversation(conversation?: WecomConversationVO | null) {
  return conversation?.conversationType === 'employee'
}

function isUnmatchedCustomerConversation(conversation?: WecomConversationVO | null) {
  return conversation?.conversationType === 'customer'
    && !!conversation.matchStatus
    && conversation.matchStatus !== 'MATCHED'
}

function conversationParticipants(conversation?: WecomConversationVO | null) {
  const raw = conversation?.chatId || [conversation?.employeeUserId, conversation?.externalUserId]
    .filter(Boolean)
    .join(':')
  const participants = raw
    .split(':')
    .map(item => item.trim())
    .filter(Boolean)
  return participants.length > 0 ? participants : [conversationDisplayTitle(conversation)]
}

function conversationDisplayTitle(conversation?: WecomConversationVO | null) {
  if (isUnmatchedCustomerConversation(conversation)) return '未匹配客户'
  if (!conversation) return '会话详情'
  return conversation.title || conversation.peerName || conversation.chatId || '企微会话'
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

.wecom-scrm-view__conversation-name,
.wecom-scrm-view__message-title {
  display: inline-flex;
  min-width: 0;
  max-width: 100%;
  align-items: center;
  gap: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wecom-scrm-view__conversation-name {
  flex: 1;
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

.wecom-scrm-view__message-header > div {
  min-width: 0;
}

.wecom-scrm-view__message-header h3 {
  display: flex;
  min-width: 0;
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
