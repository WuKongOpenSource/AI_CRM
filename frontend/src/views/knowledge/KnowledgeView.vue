<template>
  <div :class="['flex h-full min-h-0 bg-slate-50/30', isMobile ? 'overflow-y-auto' : 'overflow-hidden']">
    <!-- Main Content -->
    <div class="flex min-w-0 min-h-0 flex-1 flex-col">
      <!-- Desktop Header -->
      <div v-if="!isMobile" class="wk-knowledge-desktop-header shrink-0">
        <div class="flex min-h-[84px] items-center justify-between gap-6 px-3 py-4">
          <div class="flex min-w-0 flex-col gap-1.5">
            <h1 class="text-[22px] font-bold leading-7 text-slate-900">知识库</h1>
            <p class="text-[13px] leading-5 text-slate-500">
              保存产品文档、方案等资料，随时通过 AI 智能检索与赋能销售
            </p>
          </div>

          <div class="flex min-w-0 flex-1 items-center justify-end gap-3">
            <div
              class="flex h-10 w-[386px] max-w-[38vw] min-w-[260px] items-center rounded-xl border border-[#dbe8f8] bg-[#f8fbff] px-1.5 shadow-[0_2px_8px_rgba(15,81,159,0.08)] transition-all focus-within:border-primary/60 focus-within:ring-4 focus-within:ring-primary/10"
            >
              <span class="material-symbols-outlined shrink-0 pl-2 pr-1 text-[22px] leading-none text-[#8fa6c5]">search</span>
              <input
                v-model="queryParams.keyword"
                type="text"
                placeholder="搜索文档或向 AI 提问..."
                class="min-w-0 flex-1 border-none bg-transparent px-1 text-[13px] leading-5 text-slate-900 outline-none placeholder:text-[#8fa6c5] focus:ring-0"
                @keydown.enter="handleSearch"
              />
              <button
                type="button"
                class="inline-flex h-8 shrink-0 items-center gap-1.5 rounded-lg bg-primary px-3 text-[12px] text-white shadow-[0_4px_10px_rgba(22,119,255,0.28)] transition-all hover:bg-primary/90"
                @click="handleSearch"
              >
                <span class="material-symbols-outlined text-[17px] leading-none">auto_awesome</span>
                AI 提问
              </button>
            </div>

            <button
              type="button"
              class="inline-flex h-10 shrink-0 items-center gap-2 rounded-xl border border-[#dbe8f8] bg-[#f8fbff] px-4 text-[13px] text-primary shadow-[0_2px_8px_rgba(15,81,159,0.06)] transition-all hover:border-primary/30 hover:bg-primary/5 disabled:cursor-not-allowed disabled:text-slate-400"
              :disabled="totalCount === 0"
              @click="openScriptGenerator"
            >
              <span class="material-symbols-outlined text-[21px] leading-none">forum</span>
              AI话术生成
            </button>

            <el-upload
              class="wk-knowledge-desktop-upload shrink-0"
              :show-file-list="false"
              :before-upload="onUploadTriggerBeforeUpload"
              :http-request="noopHttpRequest"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
            >
              <button
                type="button"
                class="inline-flex h-10 items-center gap-2 rounded-xl bg-primary px-5 text-[13px] text-white shadow-[0_6px_16px_rgba(22,119,255,0.28)] transition-all hover:bg-primary/90"
              >
                <span class="material-symbols-outlined text-[21px] leading-none">upload</span>
                上传
              </button>
            </el-upload>
          </div>
        </div>
      </div>

      <!-- Content Grid -->
      <div :class="['min-h-0 flex-1', isMobile ? 'p-6' : 'overflow-y-auto px-3 py-6']">
        <div class="mx-auto px-[0px]">
          <!-- max-w-7xl -->
          <!-- Section Header -->
          <div v-if="isMobile" class="mb-6 flex flex-wrap items-center gap-3 md:gap-4">
            <div
              v-if="!showAiSearchResult"
              class="flex min-w-0 flex-wrap items-center gap-3"
            >
              <div class="flex size-6 items-center justify-center rounded bg-primary/10 text-primary">
                <span class="material-symbols-outlined text-sm">book</span>
              </div>
              <h3 class="text-lg font-bold text-slate-900">{{ getCategoryLabel() }}</h3>
              <span class="ml-1 text-sm text-slate-400">{{ totalCount }} 项结果</span>
            </div>
            <div
              class="flex min-w-0 w-full flex-1 items-center rounded-xl border border-slate-200 bg-white px-1 shadow-sm transition-all focus-within:border-primary focus-within:ring-4 focus-within:ring-primary/10 md:max-w-md lg:max-w-lg"
              :class="showAiSearchResult ? 'md:ml-auto' : ''"
            >
              <div class="flex shrink-0 items-center justify-center pl-3 pr-1 text-slate-400">
                <span class="material-symbols-outlined text-lg">search</span>
              </div>
              <input
                v-model="queryParams.keyword"
                type="text"
                placeholder="检索文档或向 AI 提问..."
                class="min-w-0 flex-1 border-none bg-transparent px-2 py-2 text-sm text-slate-900 outline-none placeholder:text-slate-400 focus:ring-0 text-base md:py-2.5"
                @keydown.enter="handleSearch"
              />
              <button
                type="button"
                class="shrink-0 rounded-full bg-primary px-4 py-2 text-xs font-bold text-white transition-all hover:bg-primary/90 md:px-5 md:py-2.5 text-base"
                @click="handleSearch"
              >
                {{ isMobile ? '搜索' : 'AI 检索' }}
              </button>
            </div>
            <div
              v-if="!showAiSearchResult"
              class="flex w-full shrink-0 items-center justify-end gap-2 md:ml-auto md:w-auto"
            >
              <!-- Mobile Upload Button -->
              <el-upload
                v-if="isMobile"
                :show-file-list="false"
                :before-upload="onUploadTriggerBeforeUpload"
                :http-request="noopHttpRequest"
                accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
              >
                <button class="flex items-center gap-1 px-3 py-1.5 bg-primary text-white text-xs font-bold rounded-lg">
                  <span class="material-symbols-outlined text-sm">upload</span>
                  上传
                </button>
              </el-upload>
              <button
                type="button"
                class="inline-flex rounded-xl border border-primary/15 bg-primary/5 px-4 py-2 text-xs font-bold text-primary transition-colors hover:bg-primary/10 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400 md:text-sm"
                :disabled="totalCount === 0"
                @click="openScriptGenerator"
              >
                AI 话术 / SOP 生成
              </button>
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
            </div>
          </div>

          <div
            v-if="!isMobile && !showAiSearchResult"
            class="wk-knowledge-desktop-filterbar mb-6 flex items-center justify-between gap-4"
          >
            <div class="flex min-w-0 flex-1 flex-wrap items-center gap-2">
              <button
                v-for="cat in categories"
                :key="'desktop-pill-' + cat.id"
                type="button"
                @click="handleCategoryFilter(cat.id)"
                :class="[
                  'inline-flex h-8 shrink-0 items-center gap-1.5 rounded-full border px-3 text-[13px] transition-all',
                  selectedCategory === cat.id
                    ? 'border-primary/25 bg-primary/10 text-primary shadow-[0_3px_8px_rgba(22,119,255,0.08)]'
                    : 'border-[#dce7f5] bg-white text-[#284462] hover:border-primary/25 hover:bg-primary/5 hover:text-primary'
                ]"
              >
                <span class="material-symbols-outlined text-[17px] leading-none">{{ cat.icon }}</span>
                {{ getDesktopCategoryLabel(cat.id, cat.label) }}
              </button>
            </div>

            <div class="flex shrink-0 items-center gap-3">
              <el-dropdown trigger="click">
                <button
                  type="button"
                  class="inline-flex size-9 items-center justify-center rounded-lg border border-[#dbe8f8] bg-[#f8fbff] text-primary shadow-[0_3px_8px_rgba(15,81,159,0.08)] transition-all hover:border-primary/30 hover:bg-primary/5"
                  title="文件类型"
                >
                  <span class="material-symbols-outlined text-[21px] leading-none">filter_list</span>
                </button>
                <template #dropdown>
                  <el-dropdown-menu class="wk-knowledge-file-type-menu">
                    <div class="px-5 pb-2 pt-3 text-xs font-bold text-[#8aa1c2]">文件类型</div>
                    <el-dropdown-item
                      v-for="item in fileTypeMenuItems"
                      :key="item.label"
                    >
                      <span class="flex min-w-[130px] items-center gap-3 py-1 text-[13px] text-[#526982]">
                        <span class="material-symbols-outlined text-[21px] leading-none text-[#8aa1c2]">{{ item.icon }}</span>
                        {{ item.label }}
                      </span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <div class="inline-flex h-9 items-center rounded-lg border border-[#dbe8f8] bg-[#f8fbff] p-1 shadow-[0_3px_8px_rgba(15,81,159,0.08)]">
                <button
                  type="button"
                  @click="setViewMode('card')"
                  :class="[
                    'inline-flex size-7 items-center justify-center rounded-md transition-all',
                    viewMode === 'card' ? 'bg-white text-primary shadow-sm' : 'text-[#8aa1c2] hover:text-primary'
                  ]"
                  title="网格视图"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">grid_view</span>
                </button>
                <button
                  type="button"
                  @click="setViewMode('list')"
                  :class="[
                    'inline-flex size-7 items-center justify-center rounded-md transition-all',
                    viewMode === 'list' ? 'bg-white text-primary shadow-sm' : 'text-[#8aa1c2] hover:text-primary'
                  ]"
                  title="列表视图"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">list</span>
                </button>
              </div>
            </div>
          </div>

          <KnowledgeSearchResultPanel
            v-if="showAiSearchResult"
            :loading="aiSearchLoading"
            :keyword="queryParams.keyword || ''"
            :result="aiSearchResult"
            @back="resetAiSearchView"
            @open="openDetailById"
          />

          <template v-else>

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
              class="relative hidden min-h-[240px] flex-col overflow-hidden rounded-2xl bg-[#1e293b] p-6 text-white md:flex"
            >
              <div class="relative z-10 flex flex-1 flex-col">
                <div
                  class="mb-4 flex size-10 items-center justify-center rounded-xl bg-primary/20 text-primary"
                >
                  <WkIcon name="ai" class="text-xl" />
                </div>
                <h4 class="mb-2 text-lg font-bold text-white">AI 话术生成器</h4>
                <p class="mb-6 text-xs leading-relaxed text-slate-400">
                  自动生成针对性销售话术，用知识库内容辅助获客转化
                </p>
              </div>
              <button
                type="button"
                class="relative z-10 w-full rounded-xl bg-primary py-2.5 text-sm font-bold text-white transition-all hover:bg-primary/90"
                @click="openScriptGenerator"
              >
                立即开始
              </button>
              <span
                class="pointer-events-none absolute -bottom-6 -right-6 select-none"
                aria-hidden="true"
              >
                <WkIcon name="ai" class="text-[8rem] text-white/5" />
              </span>
            </div>

            <div
              v-for="item in knowledgeList"
              :key="item.knowledgeId"
              class="group flex cursor-pointer flex-col rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md"
              @click="openDetail(item)"
            >
              <div class="mb-4 flex items-start justify-between">
                <FileTypeIcon :file-name="item.name" :mime-type="item.mimeType" :knowledge-type="item.type" size="md" />
                <div class="text-right">
                  <p class="mb-0.5 text-xs text-slate-400">文件大小</p>
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
                    <WkIcon name="ai" class="text-sm text-primary" />
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
                    'inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-bold',
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
                        <el-dropdown-item v-if="canUploadKnowledge" @click="openAssociateDialog(item)">
                          <span class="flex items-center gap-2">
                            <span class="material-symbols-outlined text-sm">link</span>关联客户
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
            :class="[
              'overflow-hidden bg-white',
              isMobile
                ? 'rounded-2xl border border-slate-200 shadow-sm'
                : 'wk-knowledge-list-shell rounded-2xl border border-[#dbe8f8] shadow-[0_3px_10px_rgba(15,81,159,0.08)]'
            ]"
          >
            <!-- Table Header -->
            <div
              class="hidden border-b border-[#edf3fb] bg-[#fbfdff] px-6 py-3 text-[11px] font-bold text-[#8aa1c2] md:grid md:grid-cols-[minmax(260px,3fr)_100px_minmax(250px,3fr)_150px_130px_152px] md:items-center md:gap-4"
            >
              <div>文档名称</div>
              <div>分类</div>
              <div>AI 摘要</div>
              <div>关联业务</div>
              <div>更新时间</div>
              <div class="text-right">操作</div>
            </div>
            <!-- Table Rows -->
            <div
              v-for="item in knowledgeList"
              :key="'list-' + item.knowledgeId"
              class="group grid cursor-pointer grid-cols-[minmax(0,1fr)_auto] items-center gap-2 border-b border-[#f2f6fb] px-4 py-4 transition-colors last:border-0 hover:bg-primary/5 md:grid-cols-[minmax(260px,3fr)_100px_minmax(250px,3fr)_150px_130px_152px] md:gap-4 md:px-6 md:py-4"
              @click="openDetail(item)"
            >
              <!-- Name -->
              <div class="col-span-2 flex min-w-0 items-center gap-3 md:col-span-1">
                <FileTypeIcon :file-name="item.name" :mime-type="item.mimeType" :knowledge-type="item.type" size="md" />
                <span
                  class="truncate text-sm font-bold text-slate-900 transition-colors group-hover:text-primary"
                  >{{ item.name }}</span
                >
              </div>
              <!-- Category -->
              <div class="flex items-center md:col-span-1">
                <span :class="[
                  'inline-flex items-center rounded-md px-2 py-0.5 text-xs font-bold',
                  getTypeTagClass(item.type)
                ]">
                  {{ getTypeLabel(item.type) }}
                </span>
              </div>
              <!-- AI Summary (hidden on mobile) -->
              <div class="hidden min-w-0 items-center md:col-span-1 md:flex">
                <p class="truncate text-xs text-[#526982]">{{ item.summary || '暂无摘要' }}</p>
              </div>
              <!-- Related Business (hidden on mobile) -->
              <div class="hidden items-center md:col-span-1 md:flex">
                <span v-if="item.customerName" class="flex min-w-0 items-center gap-1.5 text-xs text-[#526982]">
                  <span class="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[10px] font-bold text-primary">
                    {{ item.customerName.slice(0, 1) }}
                  </span>
                  <span class="truncate">
                  {{ item.customerName }}
                  </span>
                </span>
                <span v-else class="text-xs text-[#b8c5d6]">-</span>
              </div>
              <!-- Date -->
              <div class="hidden items-center md:col-span-1 md:flex">
                <span class="text-xs text-[#8aa1c2]">{{ formatDate(item.createTime) }}</span>
              </div>
              <!-- Actions -->
              <div class="flex flex-nowrap items-center justify-end gap-1 md:col-span-1" @click.stop>
                <button
                  type="button"
                  class="flex size-8 items-center justify-center rounded-lg text-[#8aa1c2] transition-all hover:bg-primary/10 hover:text-primary"
                  title="阅读"
                  @click.stop="openDetail(item)"
                >
                  <span class="material-symbols-outlined text-[19px] leading-none">visibility</span>
                </button>
                <button
                  type="button"
                  class="flex size-8 items-center justify-center rounded-lg text-[#8aa1c2] transition-all hover:bg-primary/10 hover:text-primary"
                  title="下载"
                  @click.stop="handleDownload(item)"
                >
                  <span class="material-symbols-outlined text-[19px] leading-none">download</span>
                </button>
                <button
                  v-if="canUploadKnowledge"
                  type="button"
                  class="flex size-8 items-center justify-center rounded-lg text-[#8aa1c2] transition-all hover:bg-primary/10 hover:text-primary"
                  title="关联客户"
                  @click.stop="openAssociateDialog(item)"
                >
                  <span class="material-symbols-outlined text-[19px] leading-none">link</span>
                </button>
                <button
                  type="button"
                  class="flex size-8 items-center justify-center rounded-lg text-[#8aa1c2] transition-all hover:bg-red-50 hover:text-red-500"
                  title="删除"
                  @click.stop="handleDelete(item)"
                >
                  <span class="material-symbols-outlined text-[19px] leading-none">delete</span>
                </button>
              </div>
            </div>
          </div>

          <!-- Pagination -->
          <div
            v-if="showPagination"
            :class="[
              'mt-6 flex items-center',
              isMobile ? 'justify-center' : 'justify-end'
            ]"
          >
            <div class="flex flex-wrap items-center justify-center gap-4">
              <p class="text-sm font-semibold text-[#0f2a4d]">
                共 {{ totalCount }} 个文件
                <span class="font-medium text-[#6f86a6]">（第 {{ currentPage }} / {{ totalPages }} 页）</span>
              </p>
              <div class="flex items-center gap-2">
                <button
                  class="flex size-8 items-center justify-center rounded-lg border border-[#dbe8f8] bg-white text-[#8aa1c2] shadow-[0_2px_6px_rgba(15,81,159,0.06)] transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
                  :disabled="currentPage <= 1"
                  title="上一页"
                  aria-label="上一页"
                  @click="handlePageChange(currentPage - 1)"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">chevron_left</span>
                </button>
                <button
                  v-for="p in visiblePages"
                  :key="p"
                  @click="handlePageChange(p)"
                  :aria-current="p === currentPage ? 'page' : undefined"
                  :aria-label="`第 ${p} 页`"
                  :class="[
                    'flex size-8 items-center justify-center rounded-lg text-sm font-bold shadow-[0_2px_6px_rgba(15,81,159,0.06)] transition-colors',
                    p === currentPage
                      ? 'bg-primary text-white shadow-[0_6px_14px_rgba(22,119,255,0.25)]'
                      : 'border border-[#dbe8f8] bg-white text-[#284462] hover:bg-slate-50'
                  ]"
                >
                  {{ p }}
                </button>
                <button
                  class="flex size-8 items-center justify-center rounded-lg border border-[#dbe8f8] bg-white text-[#8aa1c2] shadow-[0_2px_6px_rgba(15,81,159,0.06)] transition-colors hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
                  :disabled="currentPage >= totalPages"
                  title="下一页"
                  aria-label="下一页"
                  @click="handlePageChange(currentPage + 1)"
                >
                  <span class="material-symbols-outlined text-[20px] leading-none">chevron_right</span>
                </button>
              </div>
            </div>
          </div>
          </template>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showAssociateDialog"
      title="关联客户"
      :width="isMobile ? '92%' : '460px'"
      destroy-on-close
    >
      <div class="space-y-4">
        <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
          <p class="text-xs font-bold uppercase tracking-widest text-slate-400">文档</p>
          <p class="mt-2 break-all text-sm font-bold text-slate-900">{{ associateTarget?.name || '-' }}</p>
        </div>

        <div>
          <p class="mb-2 text-xs font-bold uppercase tracking-widest text-slate-400">客户</p>
          <el-select
            v-model="associateCustomerId"
            filterable
            remote
            reserve-keyword
            clearable
            default-first-option
            placeholder="搜索客户名称"
            :remote-method="searchAssociateCustomers"
            :loading="customerSearchLoading"
            class="w-full"
          >
            <el-option
              v-for="item in customerOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <p class="mt-2 text-xs text-slate-400">不选择客户也可以保存，用于取消当前关联。</p>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-3">
          <button
            type="button"
            class="flex-1 rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-50"
            @click="showAssociateDialog = false"
          >
            取消
          </button>
          <button
            type="button"
            class="flex-1 rounded-xl bg-primary px-4 py-2.5 text-sm font-bold text-white transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="associateSubmitting"
            @click="handleConfirmAssociate"
          >
            {{ associateSubmitting ? '保存中...' : '保存关联' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <KnowledgeUploadDialog
      ref="knowledgeUploadDialogRef"
      v-model="showUploadDialog"
      @success="onKnowledgeUploadSuccess"
    />

    <!-- Document Detail Modal -->
    <KnowledgeDetailModal
      v-model="showDetailModal"
      :knowledge-id="selectedKnowledgeId"
      @summary-updated="handleKnowledgeSummaryUpdated"
    />
    <KnowledgeScriptGeneratorDialog v-model="showScriptDialog" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useResponsive } from '@/composables/useResponsive'
import { useUserStore } from '@/stores/user'
import { queryCustomerList } from '@/api/customer'
import {
  queryKnowledgeList,
  deleteKnowledge,
  downloadKnowledge,
  reparseKnowledge,
  aiSearchKnowledge,
  updateKnowledgeCustomer
} from '@/api/knowledge'
import { ElMessage, ElMessageBox, UploadRequestOptions } from 'element-plus'
import type { Knowledge, KnowledgeQueryBO, KnowledgeType, KnowledgeAiSearchVO } from '@/types/common'
import { formatFileSize as formatFileSizeBytes, resolveKnowledgeFileSizeBytes } from '@/utils/formatFileSize'
import KnowledgeDetailModal from '@/components/knowledge/KnowledgeDetailModal.vue'
import KnowledgeSearchResultPanel from '@/components/knowledge/KnowledgeSearchResultPanel.vue'
import KnowledgeScriptGeneratorDialog from '@/components/knowledge/KnowledgeScriptGeneratorDialog.vue'
import KnowledgeUploadDialog from '@/components/knowledge/KnowledgeUploadDialog.vue'
import FileTypeIcon from '@/components/common/FileTypeIcon.vue'

const { isMobile } = useResponsive()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const DEFAULT_PAGE_SIZE = 10
const DESKTOP_LIST_PAGE_SIZE = 10
const viewMode = ref<'card' | 'list'>('list')
const loading = ref(false)
const showUploadDialog = ref(false)
const knowledgeUploadDialogRef = ref<InstanceType<typeof KnowledgeUploadDialog> | null>(null)
const showDetailModal = ref(false)
const showScriptDialog = ref(false)
const showAssociateDialog = ref(false)
const selectedKnowledgeId = ref('')
const knowledgeList = ref<Knowledge[]>([])
const totalCount = ref(0)
const aiSearchLoading = ref(false)
const aiSearchResult = ref<KnowledgeAiSearchVO | null>(null)
const selectedCategory = ref('all')
const associateSubmitting = ref(false)
const customerSearchLoading = ref(false)
const associateTarget = ref<Knowledge | null>(null)
const associateCustomerId = ref('')
const customerOptions = ref<Array<{ value: string; label: string }>>([])

const categories = [
  { id: 'all', label: '全部知识', icon: 'auto_stories' },
  { id: 'document', label: '产品文档', icon: 'description' },
  { id: 'proposal', label: '方案资料', icon: 'verified' },
  { id: 'meeting', label: '会议记录', icon: 'forum' },
  { id: 'contract', label: '合同文件', icon: 'payments' },
  { id: 'email', label: '邮件往来', icon: 'mail' },
  { id: 'recording', label: '录音文件', icon: 'mic' }
]

const fileTypeMenuItems = [
  { label: '图片', icon: 'image' },
  { label: '文档', icon: 'description' },
  { label: '电子表格', icon: 'table_chart' },
  { label: '演示文稿', icon: 'slideshow' },
  { label: 'PDF', icon: 'picture_as_pdf' },
  { label: '音频', icon: 'audio_file' },
  { label: '视频', icon: 'video_file' }
]

const queryParams = reactive<KnowledgeQueryBO>({
  page: 1,
  limit: DEFAULT_PAGE_SIZE,
  keyword: '',
  type: undefined
})

const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / (queryParams.limit || DEFAULT_PAGE_SIZE))))
const currentPage = computed(() => queryParams.page || 1)
const showAiSearchResult = computed(() => aiSearchLoading.value || aiSearchResult.value !== null)
const canUploadKnowledge = computed(() => userStore.hasPermission('knowledge:upload'))
const showPagination = computed(() => totalCount.value > (queryParams.limit || DEFAULT_PAGE_SIZE))

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages: number[] = []
  let start = Math.max(1, current - 2)
  const end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

onMounted(async () => {
  applyKnowledgePageSize()
  await fetchList()

  if (typeof route.query.openKnowledgeId === 'string') {
    await openKnowledgeFromRouteQuery(route.query.openKnowledgeId)
  }
})

watch(
  () => route.query.openKnowledgeId,
  (knowledgeId) => {
    if (typeof knowledgeId === 'string') {
      void openKnowledgeFromRouteQuery(knowledgeId)
    }
  }
)

watch(showAssociateDialog, (visible) => {
  if (!visible) {
    associateSubmitting.value = false
    associateTarget.value = null
    associateCustomerId.value = ''
    customerOptions.value = []
  }
})

watch([isMobile, viewMode], () => {
  if (!applyKnowledgePageSize() || showAiSearchResult.value) return
  void fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const result = await queryKnowledgeList(queryParams)
    knowledgeList.value = result.list
    totalCount.value = result.totalRow
    const lastPage = Math.max(1, Math.ceil(totalCount.value / (queryParams.limit || DEFAULT_PAGE_SIZE)))
    if ((queryParams.page || 1) > lastPage) {
      queryParams.page = lastPage
      const fallbackResult = await queryKnowledgeList(queryParams)
      knowledgeList.value = fallbackResult.list
      totalCount.value = fallbackResult.totalRow
    }
  } finally {
    loading.value = false
  }
}

function applyKnowledgePageSize(): boolean {
  const nextLimit = !isMobile.value && viewMode.value === 'list'
    ? DESKTOP_LIST_PAGE_SIZE
    : DEFAULT_PAGE_SIZE
  if (queryParams.limit === nextLimit) return false
  queryParams.limit = nextLimit
  queryParams.page = 1
  return true
}

async function handleSearch() {
  const keyword = queryParams.keyword?.trim()
  if (!keyword) {
    ElMessage.warning('请输入检索关键词')
    return
  }

  queryParams.page = 1

  aiSearchLoading.value = true
  aiSearchResult.value = null
  try {
    aiSearchResult.value = await aiSearchKnowledge({
      keyword,
      type: queryParams.type,
      limit: 5
    })
  } finally {
    aiSearchLoading.value = false
  }
}

function handleCategoryFilter(categoryId: string) {
  selectedCategory.value = categoryId
  queryParams.type = categoryId === 'all' ? undefined : categoryId as KnowledgeType
  queryParams.page = 1
  if (showAiSearchResult.value && queryParams.keyword?.trim()) {
    void handleSearch()
    return
  }
  fetchList()
}

function handlePageChange(page: number) {
  const nextPage = Math.min(Math.max(1, page), totalPages.value)
  if (currentPage.value === nextPage || loading.value) return
  queryParams.page = nextPage
  void fetchList()
}

function onUploadTriggerBeforeUpload(file: File) {
  knowledgeUploadDialogRef.value?.onTriggerBeforeUpload(file)
  return false
}

function noopHttpRequest(_options: UploadRequestOptions) {
  /* 实际上传在 KnowledgeUploadDialog 内 */
}

function onKnowledgeUploadSuccess() {
  if (showAiSearchResult.value && queryParams.keyword?.trim()) {
    void handleSearch()
  } else {
    fetchList()
  }
}

function openDetail(item: Knowledge) {
  selectedKnowledgeId.value = item.knowledgeId
  showDetailModal.value = true
}

function openDetailById(knowledgeId: string | number) {
  selectedKnowledgeId.value = String(knowledgeId)
  showDetailModal.value = true
}

function handleKnowledgeSummaryUpdated(payload: { knowledgeId: string; summary: string }) {
  const summary = payload.summary.trim()
  if (!summary) return

  knowledgeList.value = knowledgeList.value.map(item =>
    item.knowledgeId === payload.knowledgeId
      ? { ...item, summary }
      : item
  )

  if (aiSearchResult.value) {
    aiSearchResult.value = {
      ...aiSearchResult.value,
      references: aiSearchResult.value.references.map(item =>
        item.knowledgeId === payload.knowledgeId
          ? { ...item, summary }
          : item
      )
    }
  }
}

function openAssociateDialog(item: Knowledge) {
  associateTarget.value = item
  associateCustomerId.value = item.customerId ? String(item.customerId) : ''
  customerOptions.value = item.customerId && item.customerName
    ? [{ value: String(item.customerId), label: item.customerName }]
    : []
  showAssociateDialog.value = true
}

async function searchAssociateCustomers(query: string) {
  const keyword = query.trim()
  if (!keyword) {
    customerOptions.value = associateTarget.value?.customerId && associateTarget.value.customerName
      ? [{ value: String(associateTarget.value.customerId), label: associateTarget.value.customerName }]
      : []
    return
  }

  customerSearchLoading.value = true
  try {
    const res = await queryCustomerList({ keyword, page: 1, limit: 20 })
    customerOptions.value = (res.list || []).map((customer: { customerId: string; companyName?: string }) => ({
      value: String(customer.customerId),
      label: customer.companyName || ''
    }))
  } catch (error) {
    console.warn('Customer search failed:', error)
    customerOptions.value = []
  } finally {
    customerSearchLoading.value = false
  }
}

async function handleConfirmAssociate() {
  if (!associateTarget.value) return

  associateSubmitting.value = true
  try {
    await updateKnowledgeCustomer(associateTarget.value.knowledgeId, associateCustomerId.value || undefined)
    ElMessage.success(associateCustomerId.value ? '关联客户已更新' : '已取消关联客户')
    showAssociateDialog.value = false
    await fetchList()
  } finally {
    associateSubmitting.value = false
  }
}

function openScriptGenerator() {
  if (totalCount.value === 0) {
    ElMessage.warning('请先上传知识库文档')
    return
  }
  showScriptDialog.value = true
}

async function openKnowledgeFromRouteQuery(knowledgeId: string) {
  try {
    selectedKnowledgeId.value = knowledgeId
    showDetailModal.value = true
  } finally {
    const nextQuery = { ...route.query }
    delete nextQuery.openKnowledgeId
    await router.replace({ path: route.path, query: nextQuery })
  }
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
    if (showAiSearchResult.value && queryParams.keyword?.trim()) {
      await handleSearch()
    } else {
      fetchList()
    }
  } catch {
    // error handled by axios interceptor
  }
}

async function handleDelete(item: Knowledge) {
  try {
    await ElMessageBox.confirm(`确定要删除「${item.name}」吗？`, '提示', { type: 'warning' })
    await deleteKnowledge(item.knowledgeId)
    ElMessage.success('删除成功')
    if (showAiSearchResult.value && queryParams.keyword?.trim()) {
      await handleSearch()
    } else {
      fetchList()
    }
  } catch {
    // Cancelled
  }
}

function resetAiSearchView() {
  aiSearchLoading.value = false
  aiSearchResult.value = null
  fetchList()
}

function setViewMode(mode: 'card' | 'list') {
  viewMode.value = mode
}

function getCategoryLabel(): string {
  if (selectedCategory.value === 'all') return '全部知识推荐'
  const cat = categories.find(c => c.id === selectedCategory.value)
  return cat?.label ?? '推荐知识'
}

function getDesktopCategoryLabel(id: string, label: string): string {
  return id === 'all' ? '全部' : label
}

function getTypeTagClass(type: string): string {
  const normalized = String(type || '').toLowerCase()
  const classes: Record<string, string> = {
    meeting: 'bg-blue-50 text-blue-600',
    email: 'bg-green-50 text-green-600',
    recording: 'bg-purple-50 text-purple-600',
    document: 'bg-red-50 text-red-600',
    proposal: 'bg-amber-50 text-amber-600',
    contract: 'bg-slate-100 text-slate-600'
  }
  return classes[normalized] || 'bg-slate-100 text-slate-600'
}

function getTypeLabel(type: string): string {
  const normalized = String(type || '').toLowerCase()
  const labels: Record<string, string> = {
    meeting: '会议记录',
    email: '邮件',
    recording: '录音',
    document: '文档',
    proposal: '方案',
    contract: '合同'
  }
  return labels[normalized] || '文档'
}

function formatFileSize(bytes?: number | string | null): string {
  if (resolveKnowledgeFileSizeBytes(bytes) <= 0) return '未知'
  return formatFileSizeBytes(bytes)
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

:deep(.wk-knowledge-desktop-upload),
:deep(.wk-knowledge-desktop-upload .el-upload) {
  width: auto;
}

:deep(.wk-knowledge-file-type-menu) {
  min-width: 206px;
  padding: 10px 8px 12px;
  border-radius: 22px;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.12);
}

:deep(.wk-knowledge-file-type-menu .el-dropdown-menu__item) {
  border-radius: 12px;
  line-height: 1;
  padding: 7px 12px;
}
</style>
