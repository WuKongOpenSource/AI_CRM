<template>
  <div class="flex h-full bg-background-light">
    <!-- Sidebar Navigation (Desktop) -->
    <aside v-if="!isMobile" class="w-64 bg-white border-r border-slate-100 flex flex-col shrink-0">
      <div class="p-6 border-b border-slate-50">
        <el-upload
          ref="uploadRef"
          class="w-full"
          :show-file-list="false"
          :before-upload="handleBeforeUpload"
          :http-request="handleUpload"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
        >
          <button class="w-full py-3 bg-primary text-white rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-primary/90 transition-all shadow-lg shadow-primary/20">
            <span class="material-symbols-outlined text-sm">upload</span>
            上传新知识
          </button>
        </el-upload>
      </div>
      <div class="flex-1 overflow-y-auto p-4 space-y-1">
        <p class="px-4 py-2 text-[10px] font-bold text-slate-400 uppercase tracking-widest">知识分类</p>
        <button
          v-for="cat in categories"
          :key="cat.id"
          @click="handleCategoryFilter(cat.id)"
          :class="[
            'w-full px-4 py-3 rounded-xl flex items-center gap-3 transition-all text-left',
            selectedCategory === cat.id ? 'bg-primary/5 text-primary font-bold' : 'text-slate-500 hover:bg-slate-50'
          ]"
        >
          <span class="material-symbols-outlined text-lg">{{ cat.icon }}</span>
          <span class="text-sm">{{ cat.label }}</span>
        </button>

        <div class="pt-8 px-4">
          <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-4">最近使用</p>
          <div class="space-y-4">
            <div
              v-for="doc in knowledgeList.slice(0, 3)"
              :key="'recent-' + doc.knowledgeId"
              class="flex items-center gap-3 group cursor-pointer"
            >
              <div class="size-8 rounded-lg bg-slate-50 flex items-center justify-center text-slate-400 group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                <span class="material-symbols-outlined text-sm">history</span>
              </div>
              <span class="text-xs text-slate-600 truncate group-hover:text-primary transition-colors">{{ doc.name }}</span>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- Search & AI Ask Header -->
      <div class="p-6 md:p-12 bg-white border-b border-slate-100">
        <div class="max-w-4xl mx-auto space-y-6 md:space-y-8">
          <div class="text-center space-y-2">
            <h2 class="text-2xl md:text-3xl font-black text-slate-900 tracking-tight">语义知识中心</h2>
            <p class="text-slate-500 text-sm">不再只是搜索文档，直接向 AI 提问获取业务答案。</p>
          </div>

          <!-- AI Search Bar -->
          <div class="relative">
            <div class="absolute inset-0 bg-primary/5 blur-2xl rounded-3xl"></div>
            <div class="relative flex items-center bg-white border border-slate-200 rounded-2xl md:rounded-[2rem] p-1.5 md:p-2 shadow-2xl shadow-slate-200/50 focus-within:border-primary transition-all">
              <div class="size-10 md:size-12 flex items-center justify-center text-primary shrink-0">
                <span class="material-symbols-outlined text-2xl md:text-3xl">psychology</span>
              </div>
              <input
                v-model="queryParams.keyword"
                type="text"
                placeholder="搜索文件名称或关键词..."
                class="flex-1 bg-transparent border-none focus:ring-0 focus:outline-none text-slate-900 px-2 md:px-4 py-3 md:py-4 text-sm md:text-lg placeholder:text-slate-300"
                @keydown.enter="handleSearch"
              />
              <button
                class="px-4 md:px-8 py-3 md:py-4 bg-primary text-white rounded-xl md:rounded-[1.5rem] font-bold hover:bg-primary/90 transition-all shadow-lg shadow-primary/20 text-sm md:text-base shrink-0"
                @click="handleSearch"
              >
                {{ isMobile ? '搜索' : 'AI 检索' }}
              </button>
            </div>
          </div>

          <!-- Category Pills (Mobile) -->
          <div v-if="isMobile" class="flex gap-2 overflow-x-auto">
            <button
              v-for="cat in categories"
              :key="'pill-' + cat.id"
              @click="handleCategoryFilter(cat.id)"
              :class="[
                'px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all',
                selectedCategory === cat.id
                  ? 'bg-primary text-white'
                  : 'bg-slate-50 border border-slate-100 text-slate-500 hover:border-primary hover:text-primary'
              ]"
            >
              {{ cat.label }}
            </button>
          </div>

          <!-- Category Pills (Desktop) -->
          <div v-else class="flex justify-center gap-4">
            <button
              v-for="cat in categories.slice(1)"
              :key="'tag-' + cat.id"
              @click="handleCategoryFilter(cat.id)"
              :class="[
                'px-4 py-2 rounded-xl text-xs font-bold transition-all',
                selectedCategory === cat.id
                  ? 'bg-primary/10 border border-primary/20 text-primary'
                  : 'bg-slate-50 border border-slate-100 text-slate-500 hover:bg-white hover:border-primary hover:text-primary'
              ]"
            >
              {{ cat.label }}
            </button>
          </div>
        </div>
      </div>

      <!-- Content Grid -->
      <div class="flex-1 overflow-y-auto p-4 md:p-8 lg:p-12">
        <div class="max-w-6xl mx-auto">
          <!-- Section Header -->
          <div class="flex items-center justify-between mb-6 md:mb-8">
            <h3 class="text-base md:text-lg font-bold text-slate-900 flex items-center gap-2">
              <span class="material-symbols-outlined text-primary">auto_stories</span>
              {{ getCategoryLabel() }}
              <span class="text-sm font-normal text-slate-400 ml-1">{{ totalCount }} 项结果</span>
            </h3>
            <div class="flex items-center gap-2">
              <!-- View Mode Toggle -->
              <div class="flex items-center bg-slate-100 rounded-lg p-0.5">
                <button
                  @click="setViewMode('card')"
                  :class="[
                    'size-8 flex items-center justify-center rounded-md transition-all',
                    viewMode === 'card' ? 'bg-white text-primary shadow-sm' : 'text-slate-400 hover:text-slate-600'
                  ]"
                  title="卡片视图"
                >
                  <span class="material-symbols-outlined text-lg">grid_view</span>
                </button>
                <button
                  @click="setViewMode('list')"
                  :class="[
                    'size-8 flex items-center justify-center rounded-md transition-all',
                    viewMode === 'list' ? 'bg-white text-primary shadow-sm' : 'text-slate-400 hover:text-slate-600'
                  ]"
                  title="列表视图"
                >
                  <span class="material-symbols-outlined text-lg">format_list_bulleted</span>
                </button>
              </div>
              <!-- Mobile Upload Button -->
              <el-upload
                v-if="isMobile"
                :show-file-list="false"
                :before-upload="handleBeforeUpload"
                :http-request="handleUpload"
                accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
              >
                <button class="flex items-center gap-1 px-3 py-1.5 bg-primary text-white text-xs font-bold rounded-lg">
                  <span class="material-symbols-outlined text-sm">upload</span>
                  上传
                </button>
              </el-upload>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="text-center py-16">
            <span class="material-symbols-outlined text-4xl text-slate-300 animate-spin">progress_activity</span>
          </div>

          <!-- Empty State -->
          <div v-else-if="knowledgeList.length === 0" class="text-center py-16">
            <div class="size-20 bg-slate-100 rounded-[2rem] flex items-center justify-center mx-auto mb-6">
              <span class="material-symbols-outlined text-4xl text-slate-300">folder_open</span>
            </div>
            <p class="text-slate-400 font-medium">暂无文件</p>
            <p class="text-sm text-slate-300 mt-2">点击上传按钮添加文件</p>
          </div>

          <!-- Document Cards Grid -->
          <div v-else-if="viewMode === 'card'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-8">
            <div
              v-for="item in knowledgeList"
              :key="item.knowledgeId"
              class="bg-white border border-slate-100 rounded-2xl md:rounded-[2rem] p-5 md:p-8 shadow-sm hover:shadow-xl hover:shadow-slate-200/40 transition-all group cursor-pointer"
              @click="openDetail(item)"
            >
              <!-- Header: Icon + Usage -->
              <div class="flex items-start justify-between mb-4 md:mb-6">
                <div :class="[
                  'size-12 md:size-14 rounded-xl md:rounded-2xl flex items-center justify-center',
                  getTypeIconBg(item.type)
                ]">
                  <span class="material-symbols-outlined text-2xl md:text-3xl">{{ getTypeIconName(item.type) }}</span>
                </div>
                <div class="text-right">
                  <p class="text-[10px] font-bold text-slate-300 uppercase tracking-widest mb-1">文件大小</p>
                  <p class="text-sm font-black text-slate-900">{{ formatFileSize(item.fileSize) }}</p>
                </div>
              </div>

              <!-- Title -->
              <h4 class="text-base md:text-lg font-bold text-slate-900 mb-3 group-hover:text-primary transition-colors leading-tight truncate">
                {{ item.name }}
              </h4>

              <!-- AI Summary / Description -->
              <div class="p-3 md:p-4 bg-slate-50 rounded-xl md:rounded-2xl border border-slate-100 mb-4 md:mb-6">
                <div class="flex items-center gap-2 mb-2">
                  <span class="material-symbols-outlined text-primary text-xs">auto_awesome</span>
                  <span class="text-[10px] font-bold text-primary uppercase">AI 摘要</span>
                </div>
                <p class="text-xs text-slate-500 leading-relaxed line-clamp-3">
                  {{ item.summary || getTypeLabel(item.type) + ' · ' + (item.customerName || '未关联客户') }}
                </p>
              </div>

              <!-- Parse Status -->
              <div v-if="item.weKnoraParseStatus" class="mb-4">
                <span :class="[
                  'inline-flex items-center gap-1 px-2 py-1 rounded-full text-[10px] font-bold',
                  getParseStatusClass(item.weKnoraParseStatus)
                ]">
                  <span class="material-symbols-outlined text-xs">{{ getParseStatusIcon(item.weKnoraParseStatus) }}</span>
                  {{ getParseStatusLabel(item.weKnoraParseStatus) }}
                </span>
              </div>

              <!-- Footer: Date + Actions -->
              <div class="flex items-center justify-between pt-3 md:pt-4 border-t border-slate-50">
                <div class="flex items-center gap-2">
                  <span class="text-[10px] font-bold text-slate-400 uppercase">{{ formatDate(item.createTime) }}</span>
                  <span v-if="item.customerName" class="text-[10px] text-slate-300">· {{ item.customerName }}</span>
                </div>
                <div class="flex gap-1">
                  <button
                    class="px-3 py-1.5 text-xs font-bold text-primary hover:bg-primary/5 rounded-lg transition-colors"
                    @click.stop="handleDownload(item)"
                  >
                    下载
                  </button>
                  <el-dropdown trigger="click" @click.stop>
                    <button class="size-8 flex items-center justify-center rounded-full bg-slate-50 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-all">
                      <span class="material-symbols-outlined text-sm">more_horiz</span>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="handleDownload(item)">
                          <span class="flex items-center gap-2">
                            <span class="material-symbols-outlined text-sm">download</span>下载
                          </span>
                        </el-dropdown-item>
                        <el-dropdown-item v-if="item.weKnoraParseStatus === 'failed'" @click="handleReparse(item)">
                          <span class="flex items-center gap-2">
                            <span class="material-symbols-outlined text-sm">refresh</span>重新解析
                          </span>
                        </el-dropdown-item>
                        <el-dropdown-item divided @click="handleDelete(item)">
                          <span class="text-red-500 flex items-center gap-2">
                            <span class="material-symbols-outlined text-sm">delete</span>删除
                          </span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </div>
          </div>

          <!-- Document List View -->
          <div v-else class="bg-white rounded-2xl border border-slate-100 overflow-hidden">
            <!-- Table Header -->
            <div class="hidden md:grid grid-cols-12 gap-4 px-6 py-3 bg-slate-50 border-b border-slate-100 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
              <div class="col-span-4">文档名称</div>
              <div class="col-span-1">分类</div>
              <div class="col-span-3">AI 摘要</div>
              <div class="col-span-1">关联业务</div>
              <div class="col-span-1">更新时间</div>
              <div class="col-span-2 text-right">操作</div>
            </div>
            <!-- Table Rows -->
            <div
              v-for="item in knowledgeList"
              :key="'list-' + item.knowledgeId"
              class="grid grid-cols-1 md:grid-cols-12 gap-2 md:gap-4 px-4 md:px-6 py-4 border-b border-slate-50 last:border-0 hover:bg-slate-50/50 transition-colors cursor-pointer items-center"
              @click="openDetail(item)"
            >
              <!-- Name -->
              <div class="md:col-span-4 flex items-center gap-3 min-w-0">
                <div :class="[
                  'size-9 rounded-lg flex items-center justify-center shrink-0',
                  getTypeIconBg(item.type)
                ]">
                  <span class="material-symbols-outlined text-lg">{{ getTypeIconName(item.type) }}</span>
                </div>
                <span class="text-sm font-semibold text-slate-900 truncate">{{ item.name }}</span>
              </div>
              <!-- Category -->
              <div class="md:col-span-1 flex items-center">
                <span :class="[
                  'inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold',
                  getTypeTagClass(item.type)
                ]">
                  {{ getTypeLabel(item.type) }}
                </span>
              </div>
              <!-- AI Summary (hidden on mobile) -->
              <div class="hidden md:flex md:col-span-3 items-center min-w-0">
                <p class="text-xs text-slate-400 truncate">{{ item.summary || '暂无摘要' }}</p>
              </div>
              <!-- Related Business (hidden on mobile) -->
              <div class="hidden md:flex md:col-span-1 items-center">
                <span v-if="item.customerName" class="flex items-center gap-1.5 text-xs text-slate-500">
                  <span class="size-1.5 rounded-full bg-emerald-400"></span>
                  {{ item.customerName }}
                </span>
                <span v-else class="text-xs text-slate-300">—</span>
              </div>
              <!-- Date -->
              <div class="hidden md:flex md:col-span-1 items-center">
                <span class="text-xs text-slate-400">{{ formatDate(item.createTime) }}</span>
              </div>
              <!-- Actions -->
              <div class="md:col-span-2 flex items-center justify-end gap-1">
                <button
                  class="px-3 py-1.5 text-xs font-bold text-primary hover:bg-primary/5 rounded-lg transition-colors"
                  @click.stop="openDetail(item)"
                >
                  阅读
                </button>
                <button
                  class="size-8 flex items-center justify-center rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-all"
                  @click.stop="handleDownload(item)"
                  title="下载"
                >
                  <span class="material-symbols-outlined text-sm">link</span>
                </button>
              </div>
            </div>
          </div>

          <!-- Pagination -->
          <div v-if="totalCount > (queryParams.limit || 12)" class="mt-6 md:mt-8 flex justify-center">
            <div class="flex items-center gap-2">
              <button
                class="size-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                :disabled="(queryParams.page || 1) <= 1"
                @click="handlePageChange((queryParams.page || 1) - 1)"
              >
                <span class="material-symbols-outlined text-lg">chevron_left</span>
              </button>
              <button
                v-for="p in visiblePages"
                :key="p"
                @click="handlePageChange(p)"
                :class="[
                  'size-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors',
                  p === (queryParams.page || 1)
                    ? 'bg-primary text-white'
                    : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                ]"
              >
                {{ p }}
              </button>
              <button
                class="size-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                :disabled="(queryParams.page || 1) >= totalPages"
                @click="handlePageChange((queryParams.page || 1) + 1)"
              >
                <span class="material-symbols-outlined text-lg">chevron_right</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Upload Dialog -->
    <el-dialog v-model="showUploadDialog" title="上传文件" :width="isMobile ? '95%' : '500px'" :fullscreen="isMobile">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="文件">
          <span class="text-primary font-medium">{{ uploadingFile?.name }}</span>
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
        <div class="flex justify-end gap-3">
          <button
            class="px-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-600 hover:bg-slate-50 transition-colors"
            @click="showUploadDialog = false"
          >
            取消
          </button>
          <button
            class="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20 disabled:opacity-50"
            :disabled="uploading"
            @click="handleConfirmUpload"
          >
            {{ uploading ? '上传中...' : '上传' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- Document Detail Modal -->
    <KnowledgeDetailModal v-model="showDetailModal" :knowledge-id="selectedKnowledgeId" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useResponsive } from '@/composables/useResponsive'
import { queryKnowledgeList, uploadKnowledge, deleteKnowledge, downloadKnowledge, reparseKnowledge } from '@/api/knowledge'
import { ElMessage, ElMessageBox, UploadInstance, UploadRequestOptions } from 'element-plus'
import type { Knowledge, KnowledgeQueryBO, KnowledgeType } from '@/types/common'
import KnowledgeDetailModal from '@/components/knowledge/KnowledgeDetailModal.vue'

const { isMobile } = useResponsive()
const viewMode = ref<'card' | 'list'>(
  (localStorage.getItem('knowledge-view-mode') as 'card' | 'list') || 'card'
)
const loading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const showDetailModal = ref(false)
const selectedKnowledgeId = ref('')
const knowledgeList = ref<Knowledge[]>([])
const totalCount = ref(0)
const uploadingFile = ref<File | null>(null)
const uploadRef = ref<UploadInstance>()
const selectedCategory = ref('all')

const categories = [
  { id: 'all', label: '全部知识', icon: 'auto_stories' },
  { id: 'document', label: '产品文档', icon: 'description' },
  { id: 'proposal', label: '方案资料', icon: 'verified' },
  { id: 'meeting', label: '会议记录', icon: 'forum' },
  { id: 'contract', label: '合同文件', icon: 'payments' },
  { id: 'email', label: '邮件往来', icon: 'mail' },
  { id: 'recording', label: '录音文件', icon: 'mic' }
]

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

const totalPages = computed(() => Math.ceil(totalCount.value / (queryParams.limit || 12)))

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = queryParams.page || 1
  const pages: number[] = []
  let start = Math.max(1, current - 2)
  const end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
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

function handleCategoryFilter(categoryId: string) {
  selectedCategory.value = categoryId
  queryParams.type = categoryId === 'all' ? undefined : categoryId as KnowledgeType
  queryParams.page = 1
  fetchList()
}

function handlePageChange(page: number) {
  if (queryParams.page === page) return
  queryParams.page = page
  fetchList()
}

function handleBeforeUpload(file: File) {
  uploadingFile.value = file
  showUploadDialog.value = true
  return false
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

function openDetail(item: Knowledge) {
  selectedKnowledgeId.value = item.knowledgeId
  showDetailModal.value = true
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

function setViewMode(mode: 'card' | 'list') {
  viewMode.value = mode
  localStorage.setItem('knowledge-view-mode', mode)
}

function getCategoryLabel(): string {
  const cat = categories.find(c => c.id === selectedCategory.value)
  return cat ? cat.label : '全部推荐知识'
}

function getTypeIconName(type: string): string {
  const icons: Record<string, string> = {
    meeting: 'groups',
    email: 'mail',
    recording: 'mic',
    document: 'description',
    proposal: 'slideshow',
    contract: 'gavel'
  }
  return icons[type] || 'description'
}

function getTypeIconBg(type: string): string {
  const colors: Record<string, string> = {
    meeting: 'bg-blue-50 text-blue-500',
    email: 'bg-green-50 text-green-500',
    recording: 'bg-purple-50 text-purple-500',
    document: 'bg-red-50 text-red-500',
    proposal: 'bg-amber-50 text-amber-500',
    contract: 'bg-slate-100 text-slate-500'
  }
  return colors[type] || 'bg-slate-100 text-slate-500'
}

function getTypeTagClass(type: string): string {
  const classes: Record<string, string> = {
    meeting: 'bg-blue-50 text-blue-600',
    email: 'bg-green-50 text-green-600',
    recording: 'bg-purple-50 text-purple-600',
    document: 'bg-red-50 text-red-600',
    proposal: 'bg-amber-50 text-amber-600',
    contract: 'bg-slate-100 text-slate-600'
  }
  return classes[type] || 'bg-slate-100 text-slate-600'
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

function getParseStatusClass(status?: string): string {
  switch (status) {
    case 'completed': return 'bg-emerald-50 text-emerald-600'
    case 'processing': return 'bg-blue-50 text-blue-600'
    case 'pending': return 'bg-amber-50 text-amber-600'
    case 'failed': return 'bg-red-50 text-red-600'
    case 'unsupported': return 'bg-slate-100 text-slate-500'
    default: return 'bg-slate-100 text-slate-500'
  }
}

function getParseStatusIcon(status?: string): string {
  switch (status) {
    case 'completed': return 'check_circle'
    case 'processing': return 'sync'
    case 'pending': return 'schedule'
    case 'failed': return 'error'
    case 'unsupported': return 'block'
    default: return 'help'
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
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

:deep(.el-upload) {
  width: 100%;
}
</style>
