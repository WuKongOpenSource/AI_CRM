<template>
  <div class="tm-view">
    <header class="tm-view__header">
      <div class="tm-view__title min-w-0">
        <div class="tm-view__title-row">
          <span class="tm-view__title-icon material-symbols-outlined">video_camera_front</span>
          <h2>腾讯会议</h2>
          <span class="tm-view__sync-chip" :class="{ 'is-success': syncStatus === 'success', 'is-failed': syncStatus === 'failed' }">
            {{ syncStatusLabel }}
          </span>
        </div>
        <p>{{ total }} 场会议 · {{ activeFilterSummary }}</p>
      </div>
      <div class="tm-view__actions">
        <el-button type="primary" :disabled="!oauthStatus.authorized" @click="openCreateDialog">
          <span class="material-symbols-outlined mr-1 text-[18px]">add</span>
          创建会议
        </el-button>
        <el-button v-if="!oauthStatus.authorized" :loading="oauthLoading" @click="handleAuthorize">
          <span class="material-symbols-outlined mr-1 text-[18px]">verified_user</span>
          授权腾讯会议
        </el-button>
        <el-button v-else :loading="oauthLoading" @click="handleAuthorize">
          <span class="material-symbols-outlined mr-1 text-[18px]">published_with_changes</span>
          重新授权
        </el-button>
        <el-button v-if="oauthStatus.authorized" @click="handleOAuthUnbind">
          <span class="material-symbols-outlined mr-1 text-[18px]">link_off</span>
          取消授权
        </el-button>
        <el-button :disabled="!oauthStatus.authorized" :loading="syncing" @click="handleSync">
          <span class="material-symbols-outlined mr-1 text-[18px]">sync</span>
          同步我的会议
        </el-button>
        <el-button :loading="loading" @click="loadMeetings">
          <span class="material-symbols-outlined mr-1 text-[18px]">refresh</span>
          刷新
        </el-button>
      </div>
    </header>

    <section class="tm-view__filters">
      <el-input
        v-model="keyword"
        clearable
        class="tm-view__search"
        placeholder="搜索会议名称、会议号、发起人、参会人"
        @clear="loadMeetings"
        @keyup.enter="loadMeetings"
      >
        <template #prefix>
          <span class="material-symbols-outlined text-[18px] text-slate-400">search</span>
        </template>
      </el-input>
      <el-select v-model="status" clearable class="tm-filter-select" placeholder="会议状态" @change="loadMeetings">
        <el-option label="未开始" value="not_started" />
        <el-option label="已结束" value="ended" />
        <el-option label="已取消" value="cancelled" />
      </el-select>
      <el-select v-model="bindStatus" clearable class="tm-filter-select" placeholder="关联状态" @change="loadMeetings">
        <el-option label="已关联客户" value="BOUND" />
        <el-option label="未关联客户" value="UNBOUND" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DDTHH:mm:ss"
        @change="loadMeetings"
      />
    </section>

    <main class="tm-view__body">
      <el-table
        v-loading="loading"
        :data="meetings"
        height="100%"
        row-key="id"
        @row-click="openDetail"
      >
        <template #empty>
          <div class="tm-empty">
            <span class="material-symbols-outlined">event_busy</span>
            <strong>暂无会议</strong>
            <p>同步腾讯会议后，会议会显示在这里。</p>
          </div>
        </template>
        <el-table-column label="会议" min-width="280">
          <template #default="{ row }">
            <div class="tm-view__meeting">
              <span class="tm-view__meeting-icon material-symbols-outlined">video_camera_front</span>
              <div class="min-w-0">
                <strong class="truncate">{{ row.subject || '腾讯会议' }}</strong>
                <p class="truncate">{{ row.meetingCode || row.meetingId || '-' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="会议时间" width="190">
          <template #default="{ row }">{{ formatDate(row.startTime) || '-' }}</template>
        </el-table-column>
        <el-table-column label="发起人" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.creatorName || row.creatorUserId || '-' }}</template>
        </el-table-column>
        <el-table-column label="参会人" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.participantNames || `${row.participantCount || 0} 人` }}</template>
        </el-table-column>
        <el-table-column label="时长" width="100">
          <template #default="{ row }">{{ formatDuration(row.durationSeconds) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="tm-status" :class="`is-${statusClass(row.status)}`">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联客户" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.customerName" class="tm-customer is-bound">{{ row.customerName }}</span>
            <span v-else class="tm-customer">未关联</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right" align="right">
          <template #default="{ row }">
            <div class="tm-row-actions">
              <button
                v-if="canOpenMeeting(row)"
                type="button"
                class="tm-row-action is-open"
                :disabled="isOpeningMeeting(row)"
                title="进入腾讯会议"
                @click.stop="openMeeting(row)"
              >
                <span class="material-symbols-outlined">{{ isOpeningMeeting(row) ? 'progress_activity' : 'login' }}</span>
                入会
              </button>
              <button
                v-if="canCopyMeetingLink(row)"
                type="button"
                class="tm-row-action is-copy"
                title="复制会议链接"
                @click.stop="copyRowMeetingLink(row)"
              >
                <span class="material-symbols-outlined">content_copy</span>
                复制链接
              </button>
              <button type="button" class="tm-row-action" @click.stop="openDetail(row)">
                <span class="material-symbols-outlined">open_in_new</span>
                详情
              </button>
              <button v-if="row.bindStatus !== 'BOUND'" type="button" class="tm-row-action is-primary" @click.stop="openBindDialog(row)">
                <span class="material-symbols-outlined">link</span>
                关联
              </button>
              <button v-else type="button" class="tm-row-action is-danger" @click.stop="handleUnbind(row)">
                <span class="material-symbols-outlined">link_off</span>
                解绑
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </main>

    <footer class="tm-view__pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="limit"
        :total="total"
        :page-sizes="[15, 30, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadMeetings"
        @size-change="loadMeetings"
      />
    </footer>

    <el-drawer v-model="detailVisible" :title="detail?.subject || '会议详情'" size="560px" class="tm-detail-drawer">
      <div v-if="detailLoading" class="space-y-3">
        <div v-for="index in 5" :key="index" class="h-16 animate-pulse rounded-xl bg-slate-100" />
      </div>
      <div v-else-if="detail" class="tm-detail">
        <section>
          <h3>会议信息</h3>
          <dl>
            <div><dt>会议时间</dt><dd>{{ formatDate(detail.startTime) || '-' }}</dd></div>
            <div><dt>发起人</dt><dd>{{ detail.creatorName || detail.creatorUserId || '-' }}</dd></div>
            <div><dt>会议时长</dt><dd>{{ formatDuration(detail.durationSeconds) }}</dd></div>
            <div><dt>关联客户</dt><dd>{{ detail.customerName || '未关联' }}</dd></div>
          </dl>
        </section>

        <section>
          <h3>会议摘要</h3>
          <p class="tm-detail__text">{{ detail.summary || '暂无摘要' }}</p>
          <p v-if="detail.todoText" class="tm-detail__todo">{{ detail.todoText }}</p>
        </section>

        <section>
          <h3>参会人</h3>
          <div v-if="!detail.participants?.length" class="tm-detail__empty">暂无参会人记录</div>
          <div v-else class="tm-detail__participants">
            <span v-for="item in detail.participants" :key="item.id">{{ item.userName || item.userId }}</span>
          </div>
        </section>

        <section>
          <h3>录制文件</h3>
          <div v-if="!detail.recordings?.length" class="tm-detail__empty">暂无录制文件</div>
          <template v-else>
            <a
              v-for="recording in detail.recordings"
              :key="recording.id"
              class="tm-detail__recording"
              :href="recording.playUrl || recording.downloadUrl"
              target="_blank"
              rel="noreferrer"
            >
              <span class="material-symbols-outlined">movie</span>
              <span class="truncate">{{ recording.fileName || recording.recordFileId }}</span>
              <small>{{ formatDuration(recording.durationSeconds) }}</small>
            </a>
          </template>
        </section>

        <section>
          <h3>文字转录</h3>
          <div v-if="detail.transcriptSegments?.length" class="tm-detail__transcript">
            <article v-for="segment in detail.transcriptSegments" :key="segment.id">
              <strong>{{ segment.speakerName || '未知发言人' }}</strong>
              <p>{{ segment.text }}</p>
            </article>
          </div>
          <p v-else class="tm-detail__text">{{ detail.transcriptText || '暂无文字转录' }}</p>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="createDialogVisible" title="创建腾讯会议" width="560px" class="tm-create-dialog">
      <el-form label-width="92px">
        <el-form-item label="会议主题" required>
          <el-input v-model="createForm.subject" maxlength="80" placeholder="请输入会议主题" />
        </el-form-item>
        <el-form-item label="会议时间" required>
          <el-date-picker
            v-model="createMeetingRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="会议密码">
          <el-input v-model="createForm.password" clearable maxlength="6" placeholder="可选，最多6位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="tm-bind-dialog__footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="handleCreateMeeting">创建</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="bindDialogVisible" title="关联系统客户" width="760px" class="tm-bind-dialog">
      <div class="tm-bind-dialog__toolbar">
        <el-input
          v-model="customerKeyword"
          clearable
          placeholder="搜索系统客户"
          @clear="loadCrmCustomers"
          @keyup.enter="loadCrmCustomers"
        >
          <template #prefix>
            <span class="material-symbols-outlined text-[18px] text-slate-400">search</span>
          </template>
        </el-input>
        <el-button :loading="crmCustomerLoading" @click="loadCrmCustomers">搜索</el-button>
      </div>
      <el-table
        v-loading="crmCustomerLoading"
        :data="crmCustomers"
        height="360"
        row-key="customerId"
        highlight-current-row
        @row-click="selectedCrmCustomerId = $event.customerId"
      >
        <el-table-column label="客户名称" prop="companyName" min-width="260" show-overflow-tooltip />
        <el-table-column label="负责人" prop="ownerName" width="140" show-overflow-tooltip />
        <el-table-column label="阶段" prop="stage" width="120" />
      </el-table>
      <template #footer>
        <div class="tm-bind-dialog__footer">
          <el-button @click="bindDialogVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!selectedCrmCustomerId" :loading="binding" @click="handleBind">
            关联
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="createdMeetingVisible" title="会议创建成功" width="620px" class="tm-created-dialog">
      <div v-if="createdMeeting" class="tm-created">
        <div class="tm-created__hero">
          <span class="material-symbols-outlined">video_camera_front</span>
          <div class="min-w-0">
            <strong class="truncate">{{ createdMeeting.subject || '腾讯会议' }}</strong>
            <p>{{ formatMeetingTimeRange(createdMeeting) }}</p>
          </div>
        </div>
        <dl class="tm-created__meta">
          <div>
            <dt>会议号</dt>
            <dd>{{ createdMeeting.meetingCode || createdMeeting.meetingId || '-' }}</dd>
          </div>
          <div>
            <dt>主持人</dt>
            <dd>{{ createdMeeting.creatorName || createdMeeting.creatorUserId || '-' }}</dd>
          </div>
          <div>
            <dt>时长</dt>
            <dd>{{ formatDuration(createdMeeting.durationSeconds) }}</dd>
          </div>
        </dl>
        <div class="tm-created__link">
          <span class="material-symbols-outlined">link</span>
          <a v-if="meetingJoinUrl" :href="meetingJoinUrl" target="_blank" rel="noreferrer">{{ meetingJoinUrl }}</a>
          <span v-else>腾讯会议未返回入会链接，可使用会议号入会</span>
        </div>
      </div>
      <template #footer>
        <div class="tm-bind-dialog__footer">
          <el-button :disabled="!meetingJoinUrl" @click="copyMeetingLink">复制链接</el-button>
          <el-button @click="copyMeetingInfo">复制会议信息</el-button>
          <el-button type="primary" @click="createdMeetingVisible = false">完成</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryCustomerList } from '@/api/customer'
import {
  bindTencentMeeting,
  createTencentMeeting,
  getTencentMeetingDetail,
  getTencentMeetingOAuthAuthorizeUrl,
  getTencentMeetingOAuthStatus,
  getTencentMeetingSyncStatus,
  queryTencentMeetings,
  refreshTencentMeetingJoinUrl,
  runTencentMeetingSync,
  unbindTencentMeetingOAuth,
  unbindTencentMeeting
} from '@/api/tencentMeeting'
import type { CustomerListVO } from '@/types/customer'
import type {
  TencentMeetingCreatePayload,
  TencentMeetingDetailVO,
  TencentMeetingOAuthStatusVO,
  TencentMeetingVO
} from '@/types/tencentMeeting'

const route = useRoute()

function routeQueryString(value: unknown) {
  if (typeof value === 'string') return value
  if (Array.isArray(value)) return typeof value[0] === 'string' ? value[0] : ''
  return ''
}

function routeBindStatus(value: unknown) {
  const actual = routeQueryString(value)
  return actual === 'BOUND' || actual === 'UNBOUND' ? actual : ''
}

const keyword = ref('')
const status = ref('')
const bindStatus = ref(routeBindStatus(route.query.bindStatus))
const dateRange = ref<[string, string] | null>(null)
const page = ref(1)
const limit = ref(15)
const total = ref(0)
const loading = ref(false)
const syncing = ref(false)
const meetings = ref<TencentMeetingVO[]>([])
const openingMeetingIds = ref<Set<string>>(new Set())
const syncStatus = ref('')
const syncError = ref('')
const oauthLoading = ref(false)
const oauthStatus = ref<TencentMeetingOAuthStatusVO>({ configured: false, authorized: false })

const createDialogVisible = ref(false)
const createdMeetingVisible = ref(false)
const creating = ref(false)
const createdMeeting = ref<TencentMeetingVO | null>(null)
const createMeetingRange = ref<[string, string] | null>(null)
const createForm = ref({
  subject: '',
  password: ''
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<TencentMeetingDetailVO | null>(null)

const bindDialogVisible = ref(false)
const selectedMeeting = ref<TencentMeetingVO | null>(null)
const selectedCrmCustomerId = ref('')
const customerKeyword = ref('')
const crmCustomers = ref<CustomerListVO[]>([])
const crmCustomerLoading = ref(false)
const binding = ref(false)

const preferredCustomerId = computed(() => {
  return routeQueryString(route.query.customerId)
})

const preferredBindCustomerId = computed(() => {
  return routeQueryString(route.query.bindCustomerId)
})

const meetingJoinUrl = computed(() => createdMeeting.value?.joinUrl || createdMeeting.value?.hostJoinUrl || '')

const syncStatusLabel = computed(() => {
  if (!syncStatus.value) return '尚未同步'
  if (syncStatus.value === 'success') return '最近同步成功'
  if (syncStatus.value === 'failed') return `同步失败${syncError.value ? `：${syncError.value}` : ''}`
  return syncStatus.value
})

const activeFilterSummary = computed(() => {
  const filters: string[] = []
  if (keyword.value.trim()) filters.push('关键词')
  if (status.value) filters.push(statusLabel(status.value))
  if (bindStatus.value) filters.push(bindStatus.value === 'BOUND' ? '已关联' : '未关联')
  if (dateRange.value?.length) filters.push('时间范围')
  return filters.length ? `已筛选 ${filters.join(' / ')}` : '全部会议'
})

// 处理腾讯会议 OAuth 回调返回：路由用 hash 模式，后端把 tencentMeetingOAuth 拼在 # 之前的
// location.search 里，vue-router 读不到，需要手动解析并在提示后清掉，避免参数滞留地址栏、
// 并在下次授权时被层层叠加成 ?tencentMeetingOAuth=success&tencentMeetingOAuth=success...
function handleOAuthReturn() {
  const params = new URLSearchParams(window.location.search)
  const result = params.get('tencentMeetingOAuth')
  if (!result) return
  if (result === 'success') {
    ElMessage.success('腾讯会议授权成功')
  } else {
    const message = params.get('message')
    ElMessage.error(message ? `腾讯会议授权失败：${message}` : '腾讯会议授权失败')
  }
  params.delete('tencentMeetingOAuth')
  params.delete('message')
  const search = params.toString()
  const cleanUrl = window.location.pathname + (search ? `?${search}` : '') + window.location.hash
  window.history.replaceState(null, '', cleanUrl)
}

onMounted(() => {
  handleOAuthReturn()
  void loadMeetings()
  void loadSyncStatus()
  void loadOAuthStatus()
})

async function loadMeetings() {
  loading.value = true
  try {
    const data = await queryTencentMeetings({
      page: page.value,
      limit: limit.value,
      keyword: keyword.value.trim() || undefined,
      status: status.value || undefined,
      bindStatus: bindStatus.value || undefined,
      customerId: preferredCustomerId.value || undefined,
      startTimeFrom: dateRange.value?.[0],
      startTimeTo: dateRange.value?.[1]
    })
    meetings.value = data.list || []
    total.value = data.totalRow || 0
  } finally {
    loading.value = false
  }
}

async function loadSyncStatus() {
  try {
    const data = await getTencentMeetingSyncStatus()
    syncStatus.value = data.lastSyncStatus || ''
    syncError.value = data.lastSyncError || ''
  } catch (err) {
    console.error('Load Tencent Meeting sync status failed:', err)
  }
}

async function loadOAuthStatus() {
  oauthLoading.value = true
  try {
    oauthStatus.value = await getTencentMeetingOAuthStatus()
  } catch (err) {
    console.error('Load Tencent Meeting OAuth status failed:', err)
  } finally {
    oauthLoading.value = false
  }
}

async function handleAuthorize() {
  oauthLoading.value = true
  try {
    const { authorizeUrl } = await getTencentMeetingOAuthAuthorizeUrl(window.location.href)
    window.location.href = authorizeUrl
  } finally {
    oauthLoading.value = false
  }
}

async function handleOAuthUnbind() {
  await ElMessageBox.confirm('确认取消当前用户的腾讯会议授权？取消后将无法创建和同步你的会议。', '提示', {
    type: 'warning',
    confirmButtonText: '取消授权',
    cancelButtonText: '保留'
  })
  await unbindTencentMeetingOAuth()
  ElMessage.success('已取消腾讯会议授权')
  await loadOAuthStatus()
}

async function handleSync() {
  if (!oauthStatus.value.authorized) {
    ElMessage.warning('请先授权腾讯会议账号')
    return
  }
  syncing.value = true
  try {
    const data = await runTencentMeetingSync({ syncDays: 30, syncRecordings: true, syncTranscripts: true })
    syncStatus.value = data.lastSyncStatus || ''
    syncError.value = data.lastSyncError || ''
    if (data.lastSyncStatus === 'failed') {
      ElMessage.error(data.lastSyncError || '同步失败')
      return
    }
    ElMessage.success(`同步已完成，获取 ${data.fetchedCount || 0} 场会议`)
    await loadMeetings()
  } finally {
    syncing.value = false
  }
}

async function openCreateDialog() {
  if (!oauthStatus.value.authorized) {
    ElMessage.warning('请先授权腾讯会议账号')
    return
  }
  const now = new Date()
  const start = new Date(now.getTime() + 30 * 60 * 1000)
  start.setSeconds(0, 0)
  const end = new Date(start.getTime() + 60 * 60 * 1000)
  createForm.value = {
    subject: '',
    password: ''
  }
  createMeetingRange.value = [formatDateTimeInput(start), formatDateTimeInput(end)]
  createDialogVisible.value = true
}

async function handleCreateMeeting() {
  const subject = createForm.value.subject.trim()
  if (!subject) {
    ElMessage.warning('请输入会议主题')
    return
  }
  if (!createMeetingRange.value?.[0] || !createMeetingRange.value?.[1]) {
    ElMessage.warning('请选择会议时间')
    return
  }
  const payload: TencentMeetingCreatePayload = {
    subject,
    startTime: createMeetingRange.value[0],
    endTime: createMeetingRange.value[1],
    password: createForm.value.password.trim() || undefined
  }
  creating.value = true
  try {
    createdMeeting.value = await createTencentMeeting(payload)
    ElMessage.success('腾讯会议创建成功')
    createDialogVisible.value = false
    createdMeetingVisible.value = true
    await loadMeetings()
    await loadOAuthStatus()
  } finally {
    creating.value = false
  }
}

async function copyMeetingLink() {
  if (!meetingJoinUrl.value) return
  await copyText(meetingJoinUrl.value, '会议链接已复制')
}

async function copyRowMeetingLink(meeting: TencentMeetingVO) {
  const link = getMeetingJoinUrl(meeting)
  if (!link) return
  await copyText(link, '会议链接已复制')
}

async function openMeeting(meeting: TencentMeetingVO) {
  let link = getMeetingJoinUrl(meeting)
  if (link) {
    openMeetingUrl(link)
    return
  }

  setOpeningMeeting(meeting.id, true)
  try {
    const refreshed = await refreshTencentMeetingJoinUrl(meeting.id)
    updateMeetingRow(refreshed)
    link = getMeetingJoinUrl(refreshed)
    if (!link) {
      ElMessage.warning('腾讯会议未返回入会链接，请复制会议号在腾讯会议中加入')
      return
    }
    openMeetingUrl(link)
  } catch (err) {
    console.error('Open Tencent Meeting failed:', err)
    ElMessage.error('获取入会链接失败，请确认当前账号已授权并有权限查看该会议')
  } finally {
    setOpeningMeeting(meeting.id, false)
  }
}

function openMeetingUrl(link: string) {
  const opened = window.open(link, '_blank')
  if (opened) {
    opened.opener = null
    return
  }
  void copyText(link, '会议链接已复制')
  ElMessage.warning('浏览器拦截了新窗口，已为你复制入会链接')
}

function updateMeetingRow(updated: TencentMeetingVO) {
  const index = meetings.value.findIndex(item => item.id === updated.id)
  if (index >= 0) {
    meetings.value.splice(index, 1, { ...meetings.value[index], ...updated })
  }
  if (detail.value?.id === updated.id) {
    detail.value = { ...detail.value, ...updated }
  }
  if (createdMeeting.value?.id === updated.id) {
    createdMeeting.value = { ...createdMeeting.value, ...updated }
  }
}

function setOpeningMeeting(id: string, opening: boolean) {
  const next = new Set(openingMeetingIds.value)
  if (opening) {
    next.add(id)
  } else {
    next.delete(id)
  }
  openingMeetingIds.value = next
}

async function copyMeetingInfo() {
  if (!createdMeeting.value) return
  const meeting = createdMeeting.value
  const lines = [
    `腾讯会议：${meeting.subject || '-'}`,
    `会议号：${meeting.meetingCode || meeting.meetingId || '-'}`,
    `时间：${formatMeetingTimeRange(meeting)}`,
    meetingJoinUrl.value ? `链接：${meetingJoinUrl.value}` : ''
  ].filter(Boolean)
  await copyText(lines.join('\n'), '会议信息已复制')
}

async function copyText(text: string, successMessage: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
  } catch (err) {
    console.error('Copy Tencent Meeting text failed:', err)
    ElMessage.error('复制失败，请手动复制')
  }
}

async function openDetail(row: TencentMeetingVO) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getTencentMeetingDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function openBindDialog(row: TencentMeetingVO) {
  selectedMeeting.value = row
  selectedCrmCustomerId.value = preferredBindCustomerId.value || preferredCustomerId.value || ''
  bindDialogVisible.value = true
  await loadCrmCustomers()
}

async function loadCrmCustomers() {
  crmCustomerLoading.value = true
  try {
    const data = await queryCustomerList({
      page: 1,
      limit: 20,
      keyword: customerKeyword.value.trim() || undefined
    })
    crmCustomers.value = data.list || []
  } finally {
    crmCustomerLoading.value = false
  }
}

async function handleBind() {
  if (!selectedMeeting.value || !selectedCrmCustomerId.value) return
  binding.value = true
  try {
    await bindTencentMeeting({
      meetingId: selectedMeeting.value.id,
      customerId: selectedCrmCustomerId.value
    })
    ElMessage.success('关联成功')
    bindDialogVisible.value = false
    await loadMeetings()
  } finally {
    binding.value = false
  }
}

async function handleUnbind(row: TencentMeetingVO) {
  await ElMessageBox.confirm('确认解除该会议与客户的关联？', '提示', {
    type: 'warning',
    confirmButtonText: '解绑',
    cancelButtonText: '取消'
  })
  await unbindTencentMeeting({ meetingId: row.id })
  ElMessage.success('已解绑')
  await loadMeetings()
}

function statusLabel(value?: string) {
  if (value === 'not_started') return '未开始'
  if (value === 'started') return '进行中'
  if (value === 'ended') return '已结束'
  if (value === 'cancelled') return '已取消'
  return value || '-'
}

function statusClass(value?: string) {
  if (value === 'started') return 'started'
  if (value === 'ended') return 'ended'
  if (value === 'cancelled') return 'cancelled'
  if (value === 'not_started') return 'scheduled'
  return 'default'
}

function canCopyMeetingLink(meeting: TencentMeetingVO) {
  return canOpenMeeting(meeting) && Boolean(getMeetingJoinUrl(meeting))
}

function canOpenMeeting(meeting: TencentMeetingVO) {
  return !isClosedMeetingStatus(meeting.status)
}

function isClosedMeetingStatus(value?: string) {
  const status = (value || '').trim().toLowerCase()
  return status === 'ended' || status === 'cancelled' || status.includes('meeting_state_ended')
    || status.includes('cancel') || status.includes('结束') || status.includes('取消')
}

function isOpeningMeeting(meeting: TencentMeetingVO) {
  return openingMeetingIds.value.has(meeting.id)
}

function getMeetingJoinUrl(meeting: TencentMeetingVO) {
  return meeting.joinUrl || meeting.hostJoinUrl || ''
}

function formatDateTimeInput(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function formatMeetingTimeRange(meeting: TencentMeetingVO) {
  const start = formatDate(meeting.startTime)
  const end = formatDate(meeting.endTime)
  if (start && end) return `${start} - ${end}`
  return start || end || '-'
}

function formatDate(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatDuration(seconds?: number) {
  if (!seconds) return '-'
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours > 0) return `${hours}小时${rest}分`
  return `${Math.max(minutes, 1)}分钟`
}
</script>

<style scoped>
.tm-view {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #f6f8fb;
  padding: 18px 24px;
}

.tm-view__header,
.tm-view__filters,
.tm-view__pager {
  flex: 0 0 auto;
}

.tm-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.tm-view__title {
  display: grid;
  gap: 5px;
}

.tm-view__title-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.tm-view__title-icon {
  display: flex;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #e8f0ff;
  color: #2854c5;
  font-size: 20px;
}

.tm-view__header h2 {
  margin: 0;
  color: #111827;
  font-size: 24px;
  font-weight: 750;
  letter-spacing: 0;
}

.tm-view__header p {
  margin: 0 0 0 44px;
  color: #6b7280;
  font-size: 13px;
}

.tm-view__sync-chip {
  max-width: min(48vw, 520px);
  overflow: hidden;
  border: 1px solid #d7dde8;
  border-radius: 999px;
  background: #fff;
  padding: 4px 9px;
  color: #5b6472;
  font-size: 12px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tm-view__sync-chip.is-success {
  border-color: #b7dfbd;
  background: #f1fbf3;
  color: #1f7a36;
}

.tm-view__sync-chip.is-failed {
  border-color: #f1b8b8;
  background: #fff5f5;
  color: #bd2c2c;
}

.tm-view__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tm-view__actions :deep(.el-button) {
  height: 36px;
  border-radius: 8px;
  padding: 0 13px;
  font-weight: 650;
}

.tm-view__actions :deep(.el-button--primary) {
  border-color: #1f2937;
  background: #111827;
  color: #fff;
}

.tm-view__filters {
  display: grid;
  grid-template-columns: minmax(260px, 420px) 150px 160px minmax(300px, 420px);
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.tm-view__search {
  width: 100%;
}

.tm-filter-select {
  width: 100%;
}

.tm-view__filters :deep(.el-input__wrapper),
.tm-view__filters :deep(.el-select__wrapper) {
  min-height: 36px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 0 0 1px #d9e1ec inset;
}

.tm-view__filters :deep(.el-input__wrapper:hover),
.tm-view__filters :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #9fb4d8 inset;
}

.tm-view__filters :deep(.el-date-editor) {
  width: 100%;
}

.tm-view__body {
  min-height: 0;
  flex: 1 1 auto;
  overflow: hidden;
  border: 1px solid #dce3ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(29, 39, 57, 0.06);
}

.tm-view__body :deep(.el-table) {
  --el-table-header-bg-color: #f8fafc;
  --el-table-header-text-color: #4b5563;
  --el-table-row-hover-bg-color: #f5f8ff;
  color: #334155;
}

.tm-view__body :deep(.el-table__cell) {
  padding: 12px 0;
}

.tm-view__body :deep(.el-table__header th) {
  height: 46px;
  font-size: 13px;
  font-weight: 750;
}

.tm-view__body :deep(.el-table__row) {
  cursor: pointer;
}

.tm-empty {
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 54px 12px;
  color: #94a3b8;
}

.tm-empty .material-symbols-outlined {
  font-size: 34px;
}

.tm-empty strong {
  color: #475569;
  font-size: 14px;
}

.tm-empty p {
  margin: 0;
  font-size: 13px;
}

.tm-view__meeting {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.tm-view__meeting strong {
  display: block;
  color: #111827;
  font-size: 14px;
  font-weight: 750;
}

.tm-view__meeting p {
  margin: 3px 0 0;
  color: #8b98aa;
  font-size: 12px;
}

.tm-view__meeting-icon {
  display: flex;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #eef4ff;
  color: #2854c5;
  font-size: 20px;
}

.tm-status,
.tm-customer {
  display: inline-flex;
  min-height: 26px;
  align-items: center;
  border-radius: 999px;
  padding: 0 9px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.tm-status.is-scheduled {
  border: 1px solid #c8d4e6;
  background: #f6f8fb;
  color: #526073;
}

.tm-status.is-ended {
  border: 1px solid #b7dfbd;
  background: #f1fbf3;
  color: #1f7a36;
}

.tm-status.is-cancelled {
  border: 1px solid #f1b8b8;
  background: #fff5f5;
  color: #bd2c2c;
}

.tm-status.is-default {
  border: 1px solid #d7dde8;
  background: #fff;
  color: #5b6472;
}

.tm-customer {
  border: 1px solid #d7dde8;
  background: #fff;
  color: #8b98aa;
}

.tm-customer.is-bound {
  border-color: #c7d2fe;
  background: #f1f4ff;
  color: #2d4eb1;
}

.tm-row-actions {
  display: inline-flex;
  justify-content: flex-end;
  gap: 6px;
}

.tm-row-action {
  display: inline-flex;
  height: 30px;
  align-items: center;
  gap: 4px;
  border: 1px solid #d7dde8;
  border-radius: 8px;
  background: #fff;
  padding: 0 8px;
  color: #4b5563;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.tm-row-action .material-symbols-outlined {
  font-size: 16px;
}

.tm-row-action:hover {
  border-color: #9fb4d8;
  background: #f5f8ff;
  color: #2854c5;
}

.tm-row-action:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.tm-row-action.is-primary {
  border-color: #c7d2fe;
  color: #2854c5;
}

.tm-row-action.is-open {
  border-color: #9fc7ff;
  color: #165dba;
}

.tm-row-action.is-open:hover:not(:disabled) {
  border-color: #72a8f5;
  background: #eff6ff;
  color: #0f4d9a;
}

.tm-row-action.is-open .material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 500, 'GRAD' 0, 'opsz' 20;
}

.tm-row-action.is-open:disabled .material-symbols-outlined {
  animation: tm-row-action-spin 1s linear infinite;
}

@keyframes tm-row-action-spin {
  to {
    transform: rotate(360deg);
  }
}

.tm-row-action.is-copy {
  border-color: #b7dfbd;
  color: #1f7a36;
}

.tm-row-action.is-copy:hover {
  border-color: #8ccd96;
  background: #f1fbf3;
  color: #16672c;
}

.tm-row-action.is-danger {
  border-color: #ffd4d4;
  color: #d64545;
}

.tm-row-action.is-danger:hover {
  border-color: #f3a6a6;
  background: #fff5f5;
  color: #bd2c2c;
}

.tm-view__pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.tm-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.tm-detail section {
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 16px;
}

.tm-detail section:last-child {
  border-bottom: 0;
}

.tm-detail h3 {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.tm-detail dl {
  display: grid;
  gap: 8px;
}

.tm-detail dl div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px;
  font-size: 13px;
}

.tm-detail dt {
  color: #94a3b8;
}

.tm-detail dd {
  margin: 0;
  color: #334155;
}

.tm-detail__text,
.tm-detail__empty {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.tm-detail__todo {
  margin: 10px 0 0;
  border-left: 3px solid #2563eb;
  background: #eff6ff;
  padding: 8px 10px;
  color: #1e40af;
  font-size: 13px;
  line-height: 1.6;
}

.tm-detail__participants {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tm-detail__participants span {
  border-radius: 8px;
  background: #f1f5f9;
  padding: 5px 8px;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.tm-detail__recording {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 9px 10px;
  color: #334155;
  text-decoration: none;
}

.tm-detail__recording + .tm-detail__recording {
  margin-top: 8px;
}

.tm-detail__recording small {
  margin-left: auto;
  color: #94a3b8;
}

.tm-detail__transcript {
  display: grid;
  gap: 10px;
}

.tm-detail__transcript article {
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px 12px;
}

.tm-detail__transcript strong {
  color: #1e293b;
  font-size: 12px;
}

.tm-detail__transcript p {
  margin: 5px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.tm-bind-dialog__toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.tm-bind-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.tm-created {
  display: grid;
  gap: 14px;
}

.tm-created__hero {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #eff6ff;
  padding: 14px;
}

.tm-created__hero > .material-symbols-outlined {
  display: flex;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-size: 22px;
}

.tm-created__hero strong {
  display: block;
  color: #0f172a;
  font-size: 16px;
  line-height: 22px;
}

.tm-created__hero p {
  margin: 3px 0 0;
  color: #475569;
  font-size: 13px;
}

.tm-created__meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.tm-created__meta div,
.tm-created__link {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
}

.tm-created__meta dt {
  color: #94a3b8;
  font-size: 12px;
}

.tm-created__meta dd {
  margin: 4px 0 0;
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
}

.tm-created__link {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.tm-created__link .material-symbols-outlined {
  color: #2563eb;
  font-size: 18px;
}

.tm-created__link a {
  min-width: 0;
  overflow: hidden;
  color: #2563eb;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .tm-view {
    padding: 14px;
  }

  .tm-view__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .tm-view__header p {
    margin-left: 0;
  }

  .tm-view__title-row {
    flex-wrap: wrap;
  }

  .tm-view__search,
  .tm-view__filters :deep(.el-select),
  .tm-view__filters :deep(.el-date-editor) {
    width: 100%;
  }

  .tm-created__meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1180px) {
  .tm-view__filters {
    grid-template-columns: minmax(260px, 1fr) minmax(150px, 180px);
  }

  .tm-view__filters :deep(.el-date-editor) {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .tm-view__filters {
    grid-template-columns: 1fr;
  }

  .tm-view__filters :deep(.el-date-editor) {
    grid-column: auto;
  }
}
</style>
