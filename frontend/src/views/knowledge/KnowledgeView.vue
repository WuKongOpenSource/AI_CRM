<template>
  <div class="h-full flex flex-col bg-white">
    <!-- Header -->
    <div class="h-14 px-4 flex items-center justify-between border-b border-gray-200">
      <span class="font-medium text-lg">知识库</span>
      <el-upload
        ref="uploadRef"
        :show-file-list="false"
        :before-upload="handleBeforeUpload"
        :http-request="handleUpload"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
      >
        <el-button type="primary" :icon="Upload">上传文件</el-button>
      </el-upload>
    </div>

    <!-- Filters -->
    <div class="px-4 py-3 border-b border-gray-200 flex items-center gap-2 md:gap-4">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜索文件..."
        :prefix-icon="Search"
        clearable
        :class="isMobile ? 'flex-1' : 'w-64'"
        @change="handleSearch"
      />
      <el-select v-model="queryParams.type" placeholder="类型" clearable :class="isMobile ? 'w-24' : 'w-32'" @change="handleSearch">
        <el-option label="会议记录" value="meeting" />
        <el-option label="邮件" value="email" />
        <el-option label="录音" value="recording" />
        <el-option label="文档" value="document" />
        <el-option label="方案" value="proposal" />
        <el-option label="合同" value="contract" />
      </el-select>
    </div>

    <!-- File Grid -->
    <div class="flex-1 overflow-auto p-4">
      <div v-if="loading" class="text-center py-8">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      </div>
      <div v-else-if="knowledgeList.length === 0" class="text-center py-16 text-gray-400">
        <el-icon :size="48"><Folder /></el-icon>
        <p class="mt-4">暂无文件</p>
        <p class="mt-2 text-sm">点击上传按钮添加文件</p>
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        <div
          v-for="item in knowledgeList"
          :key="item.knowledgeId"
          class="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
          @click="handleViewDetail(item)"
        >
          <div class="flex items-start justify-between gap-2">
            <div class="flex items-center min-w-0 flex-1">
              <el-icon :size="32" :class="getTypeIconColor(item.type)">
                <component :is="getTypeIcon(item.type)" />
              </el-icon>
              <div class="ml-3 flex-1 min-w-0">
                <el-tooltip :content="item.name" placement="top" :show-after="300" :disabled="!item.name || item.name.length < 20">
                  <div class="font-medium text-gray-800 truncate">
                    {{ item.name }}
                  </div>
                </el-tooltip>
                <div class="text-xs text-gray-400 mt-1">
                  {{ getTypeLabel(item.type) }} · {{ formatFileSize(item.fileSize) }}
                </div>
                <div v-if="item.weKnoraParseStatus" class="mt-1">
                  <el-tag :type="getParseStatusType(item.weKnoraParseStatus)" size="small" round>
                    {{ getParseStatusLabel(item.weKnoraParseStatus) }}
                  </el-tag>
                </div>
              </div>
            </div>
            <el-dropdown trigger="click" @click.stop>
              <el-button text size="small">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleDownload(item)">
                    <el-icon><Download /></el-icon>下载
                  </el-dropdown-item>
                  <el-dropdown-item v-if="item.weKnoraParseStatus === 'failed'" @click="handleReparse(item)">
                    <el-icon><RefreshRight /></el-icon>重新解析
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleDelete(item)">
                    <span class="text-red-500"><el-icon><Delete /></el-icon>删除</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div v-if="item.summary" class="mt-3 text-sm text-gray-500 line-clamp-2">
            {{ item.summary }}
          </div>
          <div class="mt-3 flex items-center justify-between text-xs text-gray-400">
            <span>{{ item.customerName || '未关联客户' }}</span>
            <span>{{ formatDate(item.createTime) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Pagination -->
    <div class="px-4 py-3 border-t border-gray-200 flex justify-center md:justify-end">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.limit"
        :total="totalCount"
        :page-sizes="[12, 24, 48]"
        :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next'"
        :small="isMobile"
        @size-change="handleSearch"
        @current-change="handlePageChange"
      />
    </div>

    <!-- Upload Dialog -->
    <el-dialog v-model="showUploadDialog" title="上传文件" :width="isMobile ? '95%' : '500px'" :fullscreen="isMobile">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="文件">
          <span class="text-primary-500">{{ uploadingFile?.name }}</span>
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="uploadForm.type" class="w-full" placeholder="选择文件类型">
            <el-option label="会议记录" value="meeting" />
            <el-option label="邮件" value="email" />
            <el-option label="录音" value="recording" />
            <el-option label="文档" value="document" />
            <el-option label="方案" value="proposal" />
            <el-option label="合同" value="contract" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="uploadForm.summary" type="textarea" :rows="3" placeholder="请输入文件摘要（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleConfirmUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useResponsive } from '@/composables/useResponsive'
import { queryKnowledgeList, uploadKnowledge, deleteKnowledge, downloadKnowledge, reparseKnowledge } from '@/api/knowledge'
import { ElMessage, ElMessageBox, UploadInstance, UploadRequestOptions } from 'element-plus'
import {
  Upload, Search, Loading, Folder, MoreFilled, Download, Delete, RefreshRight,
  Document, Memo, Headset, Files, Tickets
} from '@element-plus/icons-vue'
import type { Knowledge, KnowledgeQueryBO } from '@/types/common'

const { isMobile } = useResponsive()
const loading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const knowledgeList = ref<Knowledge[]>([])
const totalCount = ref(0)
const uploadingFile = ref<File | null>(null)
const uploadRef = ref<UploadInstance>()

const queryParams = reactive<KnowledgeQueryBO>({
  page: 1,
  limit: 12,
  keyword: '',
  type: undefined
})

const uploadForm = reactive({
  type: 'document',
  summary: ''
})

onMounted(() => {
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const result = await queryKnowledgeList(queryParams)
    knowledgeList.value = result.list
    totalCount.value = result.totalRow
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.page = 1
  fetchList()
}

function handlePageChange(page: number) {
  if (queryParams.page === page) {
    return
  }
  queryParams.page = page
  fetchList()
}

function handleBeforeUpload(file: File) {
  uploadingFile.value = file
  showUploadDialog.value = true
  return false // Prevent auto upload
}

function handleUpload(_options: UploadRequestOptions) {
  // Handled by handleConfirmUpload
}

async function handleConfirmUpload() {
  if (!uploadingFile.value) return

  uploading.value = true
  try {
    await uploadKnowledge(
      uploadingFile.value,
      uploadForm.type,
      undefined,
      uploadForm.summary
    )
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    uploadingFile.value = null
    uploadForm.type = 'document'
    uploadForm.summary = ''
    fetchList()
  } finally {
    uploading.value = false
  }
}

function handleViewDetail(item: Knowledge) {
  // TODO: Open detail dialog
  console.log('View detail:', item)
}

async function handleDownload(item: Knowledge) {
  try {
    await downloadKnowledge(item.knowledgeId, item.name)
  } catch (error) {
    console.error('Download failed:', error)
  }
}

async function handleReparse(item: Knowledge) {
  try {
    await reparseKnowledge(item.knowledgeId)
    ElMessage.success('已提交重新解析')
    fetchList()
  } catch {
    // error handled by axios interceptor
  }
}

async function handleDelete(item: Knowledge) {
  try {
    await ElMessageBox.confirm(`确定要删除「${item.name}」吗？`, '提示', { type: 'warning' })
    await deleteKnowledge(item.knowledgeId)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    // Cancelled
  }
}

function getTypeIcon(type: string) {
  const icons: Record<string, any> = {
    meeting: Memo,
    email: Memo,
    recording: Headset,
    document: Document,
    proposal: Files,
    contract: Tickets
  }
  return icons[type] || Document
}

function getTypeIconColor(type: string): string {
  const colors: Record<string, string> = {
    meeting: 'text-blue-500',
    email: 'text-green-500',
    recording: 'text-purple-500',
    document: 'text-gray-500',
    proposal: 'text-orange-500',
    contract: 'text-red-500'
  }
  return colors[type] || 'text-gray-500'
}

function getTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    meeting: '会议记录',
    email: '邮件',
    recording: '录音',
    document: '文档',
    proposal: '方案',
    contract: '合同'
  }
  return labels[type] || '文档'
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return '未知'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function getParseStatusType(status?: string): 'success' | 'warning' | 'info' | 'danger' | '' {
  switch (status) {
    case 'completed': return 'success'
    case 'processing': return ''
    case 'pending': return 'warning'
    case 'failed': return 'danger'
    case 'unsupported': return 'info'
    default: return 'info'
  }
}

function getParseStatusLabel(status?: string): string {
  const labels: Record<string, string> = {
    completed: 'RAG 已就绪',
    processing: 'RAG 解析中',
    pending: 'RAG 排队中',
    failed: 'RAG 解析失败',
    unsupported: '不支持解析'
  }
  return labels[status || ''] || '未知状态'
}
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
