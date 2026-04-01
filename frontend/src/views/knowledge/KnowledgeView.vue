<template>
  <div class="flex h-full min-h-0 bg-slate-50/30 overflow-hidden">
    <!-- Sidebar Navigation (Desktop) -->
    <aside
      v-if="!isMobile"
      :class="[
        'bg-white border-r border-slate-100 flex flex-col shrink-0 transition-all duration-300 relative',
        sidebarCollapsed ? 'w-16' : 'w-64'
      ]"
    >
      <button
        type="button"
        class="absolute -right-3 top-10 z-10 size-6 flex items-center justify-center rounded-full border border-slate-200 bg-white text-slate-400 shadow-sm hover:text-primary"
        :title="sidebarCollapsed ? '展开侧栏' : '收起侧栏'"
        @click="sidebarCollapsed = !sidebarCollapsed"
      >
        <span class="material-symbols-outlined text-sm">{{ sidebarCollapsed ? 'chevron_right' : 'chevron_left' }}</span>
      </button>

      <div
        :class="[
          'border-b border-slate-50',
          sidebarCollapsed ? 'px-2 py-6' : 'p-6'
        ]"
      >
        <el-upload
          ref="uploadRef"
          class="w-full"
          :show-file-list="false"
          :before-upload="handleBeforeUpload"
          :http-request="handleUpload"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
        >
          <button
            type="button"
            :class="[
              'w-full py-3 rounded-xl font-bold flex items-center justify-center gap-2 bg-primary text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90',
              sidebarCollapsed ? 'px-0' : ''
            ]"
          >
            <span class="material-symbols-outlined text-sm">upload</span>
            <span v-if="!sidebarCollapsed">上传新知识</span>
          </button>
        </el-upload>
      </div>
      <div class="flex-1 min-h-0 overflow-y-auto space-y-1 p-2">
        <p
          v-if="!sidebarCollapsed"
          class="px-4 py-2 text-[10px] font-bold uppercase tracking-widest text-slate-400"
        >
          知识分类
        </p>
        <button
          v-for="cat in categories"
          :key="cat.id"
          type="button"
          :title="sidebarCollapsed ? cat.label : ''"
          @click="handleCategoryFilter(cat.id)"
          :class="[
            'w-full rounded-xl py-3 transition-all',
            sidebarCollapsed ? 'flex items-center justify-center px-0' : 'flex items-center gap-3 px-4 text-left',
            selectedCategory === cat.id
              ? 'bg-primary/5 font-bold text-primary'
              : 'text-slate-600 hover:bg-slate-50'
          ]"
        >
          <span class="material-symbols-outlined text-lg shrink-0">{{ cat.icon }}</span>
          <span v-if="!sidebarCollapsed" class="text-sm">{{ cat.label }}</span>
        </button>

        <div v-if="!sidebarCollapsed" class="px-4 pt-8">
          <p class="mb-4 text-[10px] font-bold uppercase tracking-widest text-slate-400">最近使用</p>
          <div class="space-y-4">
            <div
              v-for="doc in knowledgeList.slice(0, 3)"
              :key="'recent-' + doc.knowledgeId"
              class="group flex cursor-pointer items-center gap-3"
              @click="openDetail(doc)"
            >
              <div
                class="flex size-8 items-center justify-center rounded-lg bg-slate-50 text-slate-400 transition-colors group-hover:bg-primary/10 group-hover:text-primary"
              >
                <span class="material-symbols-outlined text-sm">history</span>
              </div>
              <span class="truncate text-xs text-slate-600 transition-colors group-hover:text-primary">{{ doc.name }}</span>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="flex min-w-0 min-h-0 flex-1 flex-col">
      <!-- Search & AI Ask Header -->
      <div class="shrink-0 border-b border-slate-100 bg-white px-6 py-8 md:p-10">
        <div class="mx-auto flex max-w-5xl flex-col items-center gap-6 md:gap-8">
          <div class="flex flex-col items-center gap-2 text-center">
            <h2 class="text-2xl font-bold tracking-tight text-slate-900 md:text-3xl">语义知识中心</h2>
            <p class="text-sm text-slate-500">不再只是搜索文档，直接向 AI 提问获取业务答案。</p>
          </div>

          <div class="relative w-full">
            <div
              class="flex items-center rounded-full border border-slate-200 bg-white p-1.5 shadow-sm transition-all focus-within:border-primary focus-within:ring-4 focus-within:ring-primary/10"
            >
              <div class="flex shrink-0 items-center justify-center pl-4 pr-2 text-slate-400">
                <span class="material-symbols-outlined text-xl">search</span>
              </div>
              <input
                v-model="queryParams.keyword"
                type="text"
                placeholder="检索文档或向 AI 提问..."
                class="min-w-0 flex-1 border-none bg-transparent px-2 py-3 text-base text-slate-900 outline-none placeholder:text-slate-400 focus:ring-0"
                @keydown.enter="handleSearch"
              />
              <button
                type="button"
                class="shrink-0 rounded-full bg-primary px-6 py-3 text-sm font-bold text-white transition-all hover:bg-primary/90 md:px-8"
                @click="handleSearch"
              >
                {{ isMobile ? '搜索' : 'AI 检索' }}
              </button>
            </div>

            <div
              v-if="!isMobile"
              class="mt-5 flex flex-wrap items-center justify-center gap-3"
            >
              <span class="mr-1 text-xs text-slate-400">热门搜索：</span>
              <button
                v-for="tag in hotSearchTags"
                :key="'hot-' + tag"
                type="button"
                class="rounded-full bg-slate-50 px-4 py-1.5 text-xs text-slate-500 transition-all hover:bg-slate-100 hover:text-slate-700"
                @click="queryParams.keyword = tag"
              >
                {{ tag }}
              </button>
            </div>
          </div>

          <!-- Category Pills (Mobile) -->
          <div v-if="isMobile" class="flex w-full gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            <button
              v-for="cat in categories"
              :key="'pill-' + cat.id"
              type="button"
              @click="handleCategoryFilter(cat.id)"
              :class="[
                'shrink-0 rounded-full px-4 py-2 text-xs font-bold whitespace-nowrap transition-all',
                selectedCategory === cat.id
                  ? 'bg-primary text-white'
                  : 'border border-slate-100 bg-slate-50 text-slate-500 hover:border-primary hover:text-primary'
              ]"
            >
              {{ cat.label }}
            </button>
          </div>
        </div>
      </div>

      <!-- Content Grid -->
      <div class="min-h-0 flex-1 overflow-y-auto p-6 md:p-8">
        <div class="mx-auto max-w-7xl">
          <!-- Section Header -->
          <div class="mb-6 flex flex-wrap items-center justify-between gap-4">
            <div class="flex flex-wrap items-center gap-3">
              <div class="flex size-6 items-center justify-center rounded bg-primary/10 text-primary">
                <span class="material-symbols-outlined text-sm">book</span>
              </div>
              <h3 class="text-lg font-bold text-slate-900">{{ getCategoryLabel() }}</h3>
              <span class="ml-1 text-sm text-slate-400">{{ totalCount }} 项结果</span>
            </div>
            <div class="flex items-center gap-2">
              <!-- View Mode Toggle -->
              <div class="flex rounded-lg border border-slate-200 bg-slate-50 p-1">
                <button
                  type="button"
                  @click="setViewMode('card')"
                  :class="[
                    'rounded-md p-1.5 transition-all',
                    viewMode === 'card' ? 'bg-white text-primary shadow-sm' : 'text-slate-400 hover:text-slate-600'
                  ]"
                  title="网格视图"
                >
                  <span class="material-symbols-outlined block text-sm">grid_view</span>
                </button>
                <button
                  type="button"
                  @click="setViewMode('list')"
                  :class="[
                    'rounded-md p-1.5 transition-all',
                    viewMode === 'list' ? 'bg-white text-primary shadow-sm' : 'text-slate-400 hover:text-slate-600'
                  ]"
                  title="列表视图"
                >
                  <span class="material-symbols-outlined block text-sm">list</span>
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
          <div
            v-else-if="viewMode === 'card'"
            class="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"
          >
            <!-- AI 话术生成器（首位） -->
            <div
              v-if="knowledgeList.length > 0"
              class="relative flex min-h-[240px] flex-col overflow-hidden rounded-2xl bg-[#1e293b] p-6 text-white"
            >
              <div class="relative z-10 flex flex-1 flex-col">
                <div
                  class="mb-4 flex size-10 items-center justify-center rounded-xl bg-primary/20 text-primary"
                >
                  <span class="material-symbols-outlined text-xl">auto_awesome</span>
                </div>
                <h4 class="mb-2 text-lg font-bold text-white">AI 话术生成器</h4>
                <p class="mb-6 text-xs leading-relaxed text-slate-400">
                  自动生成针对性销售话术，用知识库内容辅助获客转化
                </p>
              </div>
              <button
                type="button"
                class="relative z-10 w-full rounded-xl bg-primary py-2.5 text-sm font-bold text-white transition-all hover:bg-primary/90"
                @click="queryParams.keyword = '销售话术'"
              >
                立即开始
              </button>
              <span
                class="material-symbols-outlined pointer-events-none absolute -bottom-6 -right-6 select-none text-[8rem] text-white/5"
                aria-hidden="true"
                >auto_awesome</span
              >
            </div>

            <div
              v-for="item in knowledgeList"
              :key="item.knowledgeId"
              class="group flex cursor-pointer flex-col rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md"
              @click="openDetail(item)"
            >
              <div class="mb-4 flex items-start justify-between">
                <div
                  :class="[
                    'flex size-8 items-center justify-center rounded-lg',
                    getTypeIconBg(item.type)
                  ]"
                >
                  <span class="material-symbols-outlined text-sm">{{ getTypeIconName(item.type) }}</span>
                </div>
                <div class="text-right">
                  <p class="mb-0.5 text-[10px] text-slate-400">文件大小</p>
                  <p class="text-sm font-bold text-slate-900">{{ formatFileSize(item.fileSize) }}</p>
                </div>
              </div>

              <h4
                class="mb-3 line-clamp-2 text-base font-bold leading-snug text-slate-900 transition-colors group-hover:text-primary"
              >
                {{ item.name }}
              </h4>

              <div class="mb-4 flex min-h-[4.5rem] flex-1 flex-col rounded-xl bg-primary/5 p-3">
                <div class="mb-1.5 flex items-center justify-between">
                  <div class="flex items-center gap-1.5">
                    <span class="material-symbols-outlined text-sm text-primary">auto_awesome</span>
                    <span class="text-xs font-bold text-primary">AI 摘要</span>
                  </div>
                </div>
                <p class="line-clamp-2 text-xs leading-relaxed text-slate-500">
                  {{ item.summary || getTypeLabel(item.type) + ' · ' + (item.customerName || '未关联客户') }}
                </p>
              </div>

              <div v-if="item.weKnoraParseStatus" class="mb-3">
                <span
                  :class="[
                    'inline-flex items-center gap-1 rounded-full px-2 py-1 text-[10px] font-bold',
                    getParseStatusClass(item.weKnoraParseStatus)
                  ]"
                >
                  <span class="material-symbols-outlined text-xs">{{
                    getParseStatusIcon(item.weKnoraParseStatus)
                  }}</span>
                  {{ getParseStatusLabel(item.weKnoraParseStatus) }}
                </span>
              </div>

              <div
                class="mt-auto flex items-center justify-between border-t border-slate-100 pt-4"
              >
                <span class="text-xs text-slate-400">{{ formatDate(item.createTime) }}</span>
                <div class="flex items-center gap-1">
                  <el-dropdown trigger="click">
                    <button
                      type="button"
                      class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-all hover:bg-slate-100 hover:text-slate-600"
                      @click.stop
                    >
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
                          <span class="flex items-center gap-2 text-red-500">
                            <span class="material-symbols-outlined text-sm">delete</span>删除
                          </span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                  <span
                    class="material-symbols-outlined text-sm text-slate-300 transition-colors group-hover:text-primary"
                    >open_in_new</span
                  >
                </div>
              </div>
            </div>
          </div>

          <!-- Document List View -->
          <div
            v-else
            class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"
          >
            <!-- Table Header -->
            <div
              class="hidden grid-cols-12 gap-4 border-b border-slate-100 bg-slate-50/50 px-6 py-3 text-[10px] font-bold uppercase tracking-widest text-slate-400 md:grid"
            >
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
              class="group grid cursor-pointer grid-cols-1 items-center gap-2 border-b border-slate-50 px-4 py-4 transition-colors last:border-0 hover:bg-primary/5 md:grid-cols-12 md:gap-4 md:px-6"
              @click="openDetail(item)"
            >
              <!-- Name -->
              <div class="flex min-w-0 items-center gap-3 md:col-span-4">
                <div
                  :class="[
                    'flex size-8 shrink-0 items-center justify-center rounded-lg',
                    getTypeIconBg(item.type)
                  ]"
                >
                  <span class="material-symbols-outlined text-lg">{{ getTypeIconName(item.type) }}</span>
                </div>
                <span
                  class="truncate text-sm font-bold text-slate-900 transition-colors group-hover:text-primary"
                  >{{ item.name }}</span
                >
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
              <div class="md:col-span-2 flex flex-wrap items-center justify-end gap-1" @click.stop>
                <button
                  type="button"
                  class="rounded-lg px-3 py-1.5 text-xs font-bold text-primary transition-colors hover:bg-primary/10"
                  @click.stop="openDetail(item)"
                >
                  阅读
                </button>
                <button
                  type="button"
                  class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-all hover:bg-primary/10 hover:text-primary"
                  title="下载"
                  @click.stop="handleDownload(item)"
                >
                  <span class="material-symbols-outlined text-sm">download</span>
                </button>
                <button
                  type="button"
                  class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-all hover:bg-red-50 hover:text-red-500"
                  title="删除"
                  @click.stop="handleDelete(item)"
                >
                  <span class="material-symbols-outlined text-sm">delete</span>
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
/** 桌面侧栏折叠，仅 UI */
const sidebarCollapsed = ref(false)
const hotSearchTags = ['产品知识库', '销售话术', '售后服务', '入职培训', '客户FAQ']
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
  if (selectedCategory.value === 'all') return '全部知识推荐'
  const cat = categories.find(c => c.id === selectedCategory.value)
  return cat?.label ?? '推荐知识'
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
.line-clamp-2 {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-3 {
  display: -webkit-box;
  line-clamp: 3;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

:deep(.el-upload) {
  width: 100%;
}
</style>
