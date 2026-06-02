<template>
  <div class="tm-view">
    <header class="tm-view__header">
      <div class="min-w-0">
        <h2>腾讯会议</h2>
        <p>{{ total }} 场会议 · {{ syncStatusLabel }}</p>
      </div>
      <div class="tm-view__actions">
        <el-button :loading="syncing" @click="handleSync">
          <span class="material-symbols-outlined mr-1 text-[18px]">sync</span>
          同步
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
      <el-select v-model="status" clearable placeholder="会议状态" @change="loadMeetings">
        <el-option label="未开始" value="not_started" />
        <el-option label="已结束" value="ended" />
        <el-option label="已取消" value="cancelled" />
      </el-select>
      <el-select v-model="bindStatus" clearable placeholder="关联状态" @change="loadMeetings">
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
            <el-tag :type="statusTagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联客户" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.customerName" class="font-medium text-slate-800">{{ row.customerName }}</span>
            <span v-else class="text-slate-400">未关联</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
            <el-button v-if="row.bindStatus !== 'BOUND'" link type="primary" @click.stop="openBindDialog(row)">关联</el-button>
            <el-button v-else link type="danger" @click.stop="handleUnbind(row)">解绑</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryCustomerList } from '@/api/customer'
import {
  bindTencentMeeting,
  getTencentMeetingDetail,
  getTencentMeetingSyncStatus,
  queryTencentMeetings,
  runTencentMeetingSync,
  unbindTencentMeeting
} from '@/api/tencentMeeting'
import type { CustomerListVO } from '@/types/customer'
import type { TencentMeetingDetailVO, TencentMeetingVO } from '@/types/tencentMeeting'

const route = useRoute()
const keyword = ref('')
const status = ref('')
const bindStatus = ref('')
const dateRange = ref<[string, string] | null>(null)
const page = ref(1)
const limit = ref(15)
const total = ref(0)
const loading = ref(false)
const syncing = ref(false)
const meetings = ref<TencentMeetingVO[]>([])
const syncStatus = ref('')
const syncError = ref('')

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
  const raw = route.query.customerId
  return typeof raw === 'string' ? raw : Array.isArray(raw) ? raw[0] || '' : ''
})

const syncStatusLabel = computed(() => {
  if (!syncStatus.value) return '尚未同步'
  if (syncStatus.value === 'success') return '最近同步成功'
  if (syncStatus.value === 'failed') return `同步失败${syncError.value ? `：${syncError.value}` : ''}`
  return syncStatus.value
})

onMounted(() => {
  void loadMeetings()
  void loadSyncStatus()
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

async function handleSync() {
  syncing.value = true
  try {
    const data = await runTencentMeetingSync({ syncDays: 30, syncRecordings: true, syncTranscripts: true })
    syncStatus.value = data.lastSyncStatus || ''
    syncError.value = data.lastSyncError || ''
    ElMessage.success('同步已完成')
    await loadMeetings()
  } finally {
    syncing.value = false
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
  selectedCrmCustomerId.value = preferredCustomerId.value || ''
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
  if (value === 'ended') return '已结束'
  if (value === 'cancelled') return '已取消'
  return value || '-'
}

function statusTagType(value?: string) {
  if (value === 'ended') return 'success'
  if (value === 'cancelled') return 'danger'
  return 'info'
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
  background: #f8fafc;
  padding: 20px 24px;
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
  margin-bottom: 14px;
}

.tm-view__header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
}

.tm-view__header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.tm-view__actions,
.tm-view__filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tm-view__filters {
  margin-bottom: 12px;
}

.tm-view__search {
  width: min(420px, 100%);
}

.tm-view__body {
  min-height: 0;
  flex: 1 1 auto;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #fff;
}

.tm-view__meeting {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.tm-view__meeting strong {
  display: block;
  color: #0f172a;
  font-size: 14px;
}

.tm-view__meeting p {
  margin: 3px 0 0;
  color: #94a3b8;
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
  background: #eef6ff;
  color: #2563eb;
  font-size: 20px;
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

@media (max-width: 768px) {
  .tm-view {
    padding: 14px;
  }

  .tm-view__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .tm-view__search,
  .tm-view__filters :deep(.el-select),
  .tm-view__filters :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
