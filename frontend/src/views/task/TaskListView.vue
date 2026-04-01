<template>
  <div class="h-full flex bg-background-light overflow-hidden">
    <!-- Task List Section -->
    <div class="flex-1 overflow-y-auto p-4 md:p-8">
      <div class="max-w-4xl mx-auto space-y-6">
        <!-- Header -->
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h2 class="text-xl md:text-2xl font-bold text-slate-900">AI 优先行动中心</h2>
            <p class="text-sm text-slate-500 mt-1">基于客户价值与成交概率，AI 已为您自动排序今日任务。</p>
          </div>
          <div class="flex items-center gap-3">
            <!-- Segmented filter -->
            <div class="hidden md:flex bg-white p-1 rounded-xl border border-slate-200 shadow-sm">
              <button
                @click="valueFilter = 'all'"
                :class="[
                  'px-4 py-1.5 text-xs font-bold rounded-lg transition-all',
                  valueFilter === 'all' ? 'bg-primary text-white' : 'text-slate-500 hover:bg-slate-50'
                ]"
              >
                全部任务
              </button>
              <button
                @click="valueFilter = 'high-impact'"
                :class="[
                  'px-4 py-1.5 text-xs font-bold rounded-lg transition-all',
                  valueFilter === 'high-impact' ? 'bg-primary text-white' : 'text-slate-500 hover:bg-slate-50'
                ]"
              >
                高价值优先
              </button>
            </div>
            <!-- Add task button -->
            <button
              class="flex items-center gap-1.5 px-4 py-2 bg-primary text-white text-sm font-medium rounded-xl hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20"
              @click="handleAddTask"
            >
              <span class="material-symbols-outlined text-lg">add</span>
              <span>{{ isMobile ? '新建' : '新建任务' }}</span>
            </button>
          </div>
        </div>

        <!-- Status Filter Tabs -->
        <div class="flex gap-2 overflow-x-auto">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            @click="handleStatusFilter(tab.value)"
            :class="[
              'px-4 py-1.5 text-xs font-bold rounded-full transition-all whitespace-nowrap',
              currentStatus === tab.value
                ? 'bg-primary text-white'
                : 'bg-white text-slate-500 border border-slate-200 hover:bg-slate-50'
            ]"
          >
            {{ tab.label }} ({{ tab.count }})
          </button>
        </div>

        <!-- Loading -->
        <div v-if="taskStore.loading" class="text-center py-16">
          <span class="material-symbols-outlined text-4xl text-slate-300 animate-spin">progress_activity</span>
        </div>

        <!-- Empty State -->
        <div v-else-if="displayedTasks.length === 0" class="text-center py-16 text-slate-400">
          <span class="material-symbols-outlined text-5xl">task_alt</span>
          <p class="mt-4 text-sm">暂无任务</p>
        </div>

        <!-- Task Cards -->
        <div v-else class="space-y-4">
          <div
            v-for="task in displayedTasks"
            :key="task.taskId"
            @click="handleViewDetail(task)"
            :class="[
              'group bg-white border rounded-2xl p-5 cursor-pointer transition-all hover:shadow-xl hover:shadow-slate-200/50',
              selectedTask?.taskId === task.taskId ? 'border-primary ring-1 ring-primary/20' : 'border-slate-200',
              task.status === 'COMPLETED' ? 'opacity-75' : ''
            ]"
          >
            <div class="flex items-start gap-4 md:gap-5">
              <!-- AI Score -->
              <div class="flex flex-col items-center gap-1 shrink-0">
                <div
                  :class="[
                    'size-12 rounded-xl flex flex-col items-center justify-center border',
                    task.status === 'COMPLETED'
                      ? 'bg-slate-50 border-slate-100'
                      : 'bg-primary/5 border-primary/10'
                  ]"
                >
                  <template v-if="task.status === 'COMPLETED'">
                    <span class="material-symbols-outlined text-emerald-500">check_circle</span>
                  </template>
                  <template v-else>
                    <span class="text-lg font-black text-primary leading-none">{{ getAiScore(task) }}</span>
                    <span class="text-[8px] font-bold text-slate-400 uppercase tracking-tighter">AI 评分</span>
                  </template>
                </div>
                <div class="h-4 w-px bg-slate-100"></div>
                <div
                  :class="[
                    'size-2 rounded-full',
                    task.status === 'COMPLETED'
                      ? 'bg-slate-200'
                      : task.priority === 'HIGH' ? 'bg-red-500'
                      : task.priority === 'MEDIUM' ? 'bg-amber-500'
                      : 'bg-slate-300'
                  ]"
                ></div>
              </div>

              <!-- Content -->
              <div class="flex-1 min-w-0">
                <!-- Title + Status + Date -->
                <div class="flex items-center justify-between mb-1">
                  <div class="flex items-center gap-2 min-w-0">
                    <h3
                      :class="[
                        'font-bold truncate group-hover:text-primary transition-colors',
                        task.status === 'COMPLETED' ? 'text-slate-400 line-through' : 'text-slate-900'
                      ]"
                    >
                      {{ task.title }}
                    </h3>
                    <span
                      v-if="isOverdue(task)"
                      class="px-2 py-0.5 bg-red-50 text-red-600 text-[10px] font-bold rounded uppercase animate-pulse shrink-0"
                    >
                      已延期
                    </span>
                    <span
                      v-else-if="task.status === 'COMPLETED'"
                      class="px-2 py-0.5 bg-emerald-50 text-emerald-600 text-[10px] font-bold rounded uppercase shrink-0"
                    >
                      已完成
                    </span>
                    <span
                      v-else-if="task.status === 'IN_PROGRESS'"
                      class="px-2 py-0.5 bg-blue-50 text-blue-600 text-[10px] font-bold rounded uppercase shrink-0"
                    >
                      进行中
                    </span>
                    <span
                      v-else
                      class="px-2 py-0.5 bg-slate-100 text-slate-500 text-[10px] font-bold rounded uppercase shrink-0"
                    >
                      待处理
                    </span>
                  </div>
                  <span v-if="task.dueDate" class="text-[10px] font-bold text-slate-400 uppercase shrink-0 ml-2 hidden md:block">
                    {{ formatDate(task.dueDate) }}
                    <span class="text-slate-300 ml-1">({{ getRelativeTime(task.dueDate) }})</span>
                  </span>
                </div>

                <!-- Customer + Category + Owner -->
                <div class="flex items-center gap-2 mb-3 flex-wrap">
                  <span v-if="task.customerName" class="text-xs font-medium text-slate-500 truncate max-w-[150px]">{{ task.customerName }}</span>
                  <span v-if="task.customerName" class="size-1 rounded-full bg-slate-200"></span>
                  <span
                    v-if="task.generatedByAi"
                    class="text-[10px] font-bold px-2 py-0.5 rounded uppercase bg-blue-50 text-blue-600"
                  >
                    AI 生成
                  </span>
                  <span
                    v-else
                    class="text-[10px] font-bold px-2 py-0.5 rounded uppercase bg-slate-100 text-slate-600"
                  >
                    手动创建
                  </span>
                  <template v-if="task.assignedToName">
                    <span class="size-1 rounded-full bg-slate-200"></span>
                    <div class="flex items-center gap-1 text-xs text-slate-400">
                      <span class="material-symbols-outlined text-[14px]">person</span>
                      <span>{{ task.assignedToName }}</span>
                    </div>
                  </template>
                </div>

                <!-- AI Insight -->
                <div class="p-3 bg-primary/5 rounded-xl border border-primary/10 flex items-start gap-2">
                  <span class="material-symbols-outlined text-primary text-sm mt-0.5">psychology</span>
                  <p class="text-xs text-slate-600 leading-relaxed italic">"{{ getAiInsight(task) }}"</p>
                </div>

                <!-- Action Buttons -->
                <div class="mt-3 flex items-center gap-2" @click.stop>
                  <button
                    v-if="task.status === 'PENDING'"
                    class="px-3 py-1 text-xs font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
                    @click="handleStartTask(task)"
                  >
                    开始处理
                  </button>
                  <button
                    v-if="task.status !== 'COMPLETED'"
                    class="px-3 py-1 text-xs font-medium text-emerald-600 bg-emerald-50 rounded-lg hover:bg-emerald-100 transition-colors"
                    @click="handleToggleComplete(task)"
                  >
                    标记完成
                  </button>
                  <div class="flex-1"></div>
                  <el-dropdown trigger="click">
                    <button class="size-8 flex items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors">
                      <span class="material-symbols-outlined text-lg">more_horiz</span>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="handleEdit(task)">编辑</el-dropdown-item>
                        <el-dropdown-item divided @click="handleDelete(task)">
                          <span class="text-red-500">删除</span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="taskStore.totalCount > (taskStore.queryParams.limit || 10)" class="mt-6 flex justify-center">
          <div class="flex items-center gap-2">
            <button
              class="size-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              :disabled="(taskStore.queryParams.page || 1) <= 1"
              @click="handlePageChange((taskStore.queryParams.page || 1) - 1)"
            >
              <span class="material-symbols-outlined text-lg">chevron_left</span>
            </button>
            <button
              v-for="p in visiblePages"
              :key="p"
              @click="handlePageChange(p)"
              :class="[
                'size-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors',
                p === (taskStore.queryParams.page || 1)
                  ? 'bg-primary text-white'
                  : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
              ]"
            >
              {{ p }}
            </button>
            <button
              class="size-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              :disabled="(taskStore.queryParams.page || 1) >= totalPages"
              @click="handlePageChange((taskStore.queryParams.page || 1) + 1)"
            >
              <span class="material-symbols-outlined text-lg">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Task Detail Side Panel (Desktop) -->
    <transition name="slide-right">
      <aside
        v-if="selectedTask && !isMobile"
        class="w-[400px] bg-white border-l border-slate-200 shadow-2xl z-20 flex flex-col shrink-0"
      >
        <div class="p-8 flex-1 overflow-y-auto">
          <div class="flex items-center justify-between mb-8">
            <span class="px-3 py-1 bg-primary/10 text-primary text-[10px] font-bold rounded-full uppercase tracking-widest">
              任务详情
            </span>
            <button
              @click="selectedTask = null"
              class="size-8 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 transition-colors"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <!-- Title -->
          <h2 class="text-xl font-bold text-slate-900 mb-6 line-clamp-2">{{ selectedTask.title }}</h2>

          <!-- Info Grid -->
          <div class="grid grid-cols-2 gap-4 mb-8">
            <div class="p-3 bg-slate-50 rounded-xl border border-slate-100">
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">截止时间</p>
              <p :class="['text-xs font-bold', isOverdue(selectedTask) ? 'text-red-500' : 'text-slate-700']">
                {{ selectedTask.dueDate ? formatDateTime(selectedTask.dueDate) : '未设定' }}
              </p>
              <p v-if="isOverdue(selectedTask)" class="text-[10px] text-red-500 font-bold mt-1">(已延期)</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl border border-slate-100">
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">优先级</p>
              <p :class="['text-xs font-bold uppercase', getPriorityColor(selectedTask.priority)]">
                {{ getPriorityLabel(selectedTask.priority) }}
              </p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl border border-slate-100">
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">负责人</p>
              <p class="text-xs font-bold text-slate-700">{{ selectedTask.assignedToName || '未分配' }}</p>
            </div>
            <div class="p-3 bg-slate-50 rounded-xl border border-slate-100">
              <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">任务状态</p>
              <p class="text-xs font-bold text-primary uppercase">{{ getStatusLabel(selectedTask.status) }}</p>
            </div>
          </div>

          <div class="space-y-8">
            <!-- Participants -->
            <section v-if="selectedTask.participantNames">
              <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4">参与人</h3>
              <p class="text-sm text-slate-700">{{ selectedTask.participantNames }}</p>
            </section>

            <!-- Customer -->
            <section v-if="selectedTask.customerName">
              <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4">关联客户</h3>
              <div class="p-4 bg-white border border-slate-200 rounded-2xl flex items-center gap-3 cursor-pointer hover:bg-slate-50 transition-colors">
                <div class="size-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold">
                  {{ selectedTask.customerName.charAt(0) }}
                </div>
                <div>
                  <p class="text-sm font-bold text-slate-900 truncate">{{ selectedTask.customerName }}</p>
                  <p class="text-[10px] text-slate-400">点击查看客户详情</p>
                </div>
                <span class="material-symbols-outlined ml-auto text-slate-300">chevron_right</span>
              </div>
            </section>

            <!-- Description -->
            <section v-if="selectedTask.description">
              <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4">任务描述</h3>
              <div class="p-4 bg-slate-50 rounded-xl">
                <p class="text-sm text-slate-600 leading-relaxed">{{ selectedTask.description }}</p>
              </div>
            </section>

            <!-- AI Recommended Script -->
            <section class="p-6 bg-slate-900 rounded-[2rem] text-white">
              <div class="flex items-center gap-2 mb-4">
                <span class="material-symbols-outlined text-emerald-400">auto_awesome</span>
                <h3 class="text-sm font-bold">AI 推荐沟通话术</h3>
              </div>
              <p class="text-xs text-slate-300 leading-relaxed italic">
                "{{ getAiInsight(selectedTask) }}"
              </p>
            </section>
          </div>
        </div>

        <!-- Bottom Actions -->
        <div class="p-6 border-t border-slate-100 space-y-4">
          <div class="flex gap-3">
            <button
              @click="handleEdit(selectedTask)"
              class="flex-1 py-3 border border-slate-200 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-50 transition-all flex items-center justify-center gap-2"
            >
              <span class="material-symbols-outlined text-lg">edit</span>
              编辑任务
            </button>
            <button
              v-if="selectedTask.status !== 'COMPLETED'"
              @click="handleToggleComplete(selectedTask)"
              class="flex-1 py-3 bg-emerald-500 text-white rounded-xl text-sm font-bold hover:bg-emerald-600 transition-all shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2"
            >
              <span class="material-symbols-outlined text-lg">check_circle</span>
              标记为完成
            </button>
            <button
              v-else
              @click="handleToggleComplete(selectedTask)"
              class="flex-1 py-3 bg-slate-100 text-slate-600 rounded-xl text-sm font-bold hover:bg-slate-200 transition-all flex items-center justify-center gap-2"
            >
              <span class="material-symbols-outlined text-lg">undo</span>
              重新打开
            </button>
          </div>
          <button
            @click="handleDelete(selectedTask)"
            class="w-full flex items-center justify-center gap-1.5 py-2 text-xs text-slate-400 hover:text-red-500 transition-colors"
          >
            <span class="material-symbols-outlined text-sm">delete</span>
            删除此任务
          </button>
        </div>
      </aside>
    </transition>

    <!-- Task Detail Dialog (Mobile) -->
    <el-dialog v-model="showDetailDialog" title="任务详情" width="95%" fullscreen>
      <template v-if="selectedTask">
        <h2 class="text-lg font-bold text-slate-900 mb-4">{{ selectedTask.title }}</h2>

        <div class="grid grid-cols-2 gap-3 mb-6">
          <div class="p-3 bg-slate-50 rounded-xl">
            <p class="text-[10px] font-bold text-slate-400 uppercase mb-1">截止时间</p>
            <p :class="['text-xs font-bold', isOverdue(selectedTask) ? 'text-red-500' : 'text-slate-700']">
              {{ selectedTask.dueDate ? formatDateTime(selectedTask.dueDate) : '未设定' }}
            </p>
          </div>
          <div class="p-3 bg-slate-50 rounded-xl">
            <p class="text-[10px] font-bold text-slate-400 uppercase mb-1">优先级</p>
            <p :class="['text-xs font-bold', getPriorityColor(selectedTask.priority)]">
              {{ getPriorityLabel(selectedTask.priority) }}
            </p>
          </div>
          <div class="p-3 bg-slate-50 rounded-xl">
            <p class="text-[10px] font-bold text-slate-400 uppercase mb-1">负责人</p>
            <p class="text-xs font-bold text-slate-700">{{ selectedTask.assignedToName || '未分配' }}</p>
          </div>
          <div class="p-3 bg-slate-50 rounded-xl">
            <p class="text-[10px] font-bold text-slate-400 uppercase mb-1">状态</p>
            <p class="text-xs font-bold text-primary">{{ getStatusLabel(selectedTask.status) }}</p>
          </div>
        </div>

        <div v-if="selectedTask.participantNames" class="mb-6">
          <h3 class="text-xs font-bold text-slate-400 uppercase mb-2">参与人</h3>
          <p class="text-sm text-slate-700">{{ selectedTask.participantNames }}</p>
        </div>

        <div v-if="selectedTask.customerName" class="mb-6">
          <h3 class="text-xs font-bold text-slate-400 uppercase mb-2">关联客户</h3>
          <div class="p-3 bg-white border border-slate-200 rounded-xl flex items-center gap-3">
            <div class="size-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center font-bold text-sm">
              {{ selectedTask.customerName.charAt(0) }}
            </div>
            <p class="text-sm font-bold text-slate-900">{{ selectedTask.customerName }}</p>
          </div>
        </div>

        <div v-if="selectedTask.description" class="mb-6">
          <h3 class="text-xs font-bold text-slate-400 uppercase mb-2">描述</h3>
          <p class="text-sm text-slate-600">{{ selectedTask.description }}</p>
        </div>

        <!-- AI Recommended Script -->
        <div class="p-4 bg-slate-900 rounded-2xl text-white">
          <div class="flex items-center gap-2 mb-3">
            <span class="material-symbols-outlined text-emerald-400 text-sm">auto_awesome</span>
            <h3 class="text-sm font-bold">AI 推荐沟通话术</h3>
          </div>
          <p class="text-xs text-slate-300 leading-relaxed italic">"{{ getAiInsight(selectedTask) }}"</p>
        </div>
      </template>
      <template #footer>
        <div class="space-y-3">
          <div class="flex gap-3">
            <button
              @click="handleEdit(selectedTask!); showDetailDialog = false"
              class="flex-1 py-2.5 border border-slate-200 rounded-xl text-sm font-bold text-slate-600 flex items-center justify-center gap-2"
            >
              <span class="material-symbols-outlined text-base">edit</span>
              编辑任务
            </button>
            <button
              v-if="selectedTask?.status !== 'COMPLETED'"
              @click="handleToggleComplete(selectedTask!); showDetailDialog = false"
              class="flex-1 py-2.5 bg-emerald-500 text-white rounded-xl text-sm font-bold flex items-center justify-center gap-2"
            >
              <span class="material-symbols-outlined text-base">check_circle</span>
              标记完成
            </button>
          </div>
          <button
            @click="handleDelete(selectedTask!); showDetailDialog = false"
            class="w-full flex items-center justify-center gap-1.5 py-2 text-xs text-slate-400 hover:text-red-500 transition-colors"
          >
            <span class="material-symbols-outlined text-sm">delete</span>
            删除此任务
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="showAddDialog"
      :width="isMobile ? '95%' : '720px'"
      :show-close="false"
      destroy-on-close
      top="10vh"
      :fullscreen="isMobile"
      class="!rounded-2xl !p-0 overflow-hidden task-dialog"
    >
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div :class="[
              'size-10 rounded-xl flex items-center justify-center',
              editingTask ? 'bg-blue-50' : 'bg-primary/10'
            ]">
              <span :class="[
                'material-symbols-outlined text-xl',
                editingTask ? 'text-blue-500' : 'text-primary'
              ]">
                {{ editingTask ? 'edit_note' : 'task_alt' }}
              </span>
            </div>
            <div>
              <h2 class="text-lg font-bold text-slate-900">{{ editingTask ? '编辑任务' : '新建任务' }}</h2>
              <p class="text-xs text-slate-500 mt-0.5">{{ editingTask ? '修改任务详细信息' : '手动填写或使用 AI 智能解析' }}</p>
            </div>
          </div>
          <button
            @click="showAddDialog = false"
            class="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"
          >
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </template>

      <div class="space-y-5 bg-slate-50/50 p-5 md:p-6">
        <!-- AI Parse Section (Create only) -->
        <div v-if="!editingTask" class="bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
          <div class="flex items-center gap-2 mb-3">
            <span class="material-symbols-outlined text-primary text-sm">auto_awesome</span>
            <span class="text-xs font-bold text-primary">AI 智能解析 (可选)</span>
          </div>
          <div class="relative">
            <textarea
              v-model="aiParseInput"
              placeholder="例如：明天下午两点前给科技创新有限公司的张总发一份 Q4 扩容方案的报价单，标记为高优先级..."
              class="w-full text-sm text-slate-600 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-xl px-4 py-3 outline-none transition-all resize-none h-24"
            />
            <button
              @click="handleAiParse"
              :disabled="!aiParseInput.trim() || aiParsing"
              class="absolute right-3 bottom-3 flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 text-white text-xs font-bold rounded-lg hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <span v-if="aiParsing" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
              <span v-else class="material-symbols-outlined text-sm">auto_awesome</span>
              {{ aiParsing ? '解析中...' : '一键解析' }}
            </button>
          </div>
        </div>

        <!-- Form Fields -->
        <div class="bg-white p-4 md:p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
          <!-- Title -->
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">任务标题 <span class="text-red-500">*</span></label>
            <input
              v-model="formData.title"
              type="text"
              placeholder="请输入任务标题"
              class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
            />
          </div>

          <!-- Due Date + Priority -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">截止时间 <span class="text-red-500">*</span></label>
              <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 focus-within:border-primary focus-within:bg-white rounded-lg px-3 py-2.5 transition-all">
                <span class="material-symbols-outlined text-slate-400 text-sm">calendar_today</span>
                <input
                  v-model="formData.dueDate"
                  type="datetime-local"
                  class="w-full text-sm text-slate-900 bg-transparent outline-none"
                />
              </div>
            </div>
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">优先级</label>
              <select
                v-model="formData.priority"
                class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
              >
                <option value="HIGH">高</option>
                <option value="MEDIUM">中</option>
                <option value="LOW">低</option>
              </select>
            </div>
          </div>

          <!-- Task Type + Customer -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">任务类型</label>
              <select
                v-model="formData.taskType"
                class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
              >
                <option value="">请选择</option>
                <option value="跟进">跟进</option>
                <option value="文档">文档</option>
                <option value="会议">会议</option>
                <option value="电话">电话</option>
                <option value="其他">其他</option>
              </select>
            </div>
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">关联客户</label>
              <input
                v-model="formData.customerName"
                type="text"
                placeholder="请输入关联客户名称"
                class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
              />
            </div>
          </div>

          <!-- Participants + Assignee (Edit mode shows both in grid) -->
          <div :class="editingTask ? 'grid grid-cols-1 sm:grid-cols-2 gap-4' : ''">
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">参与人 (逗号分隔)</label>
              <input
                v-model="formData.participantNames"
                type="text"
                placeholder="例如: 张三, 李四"
                class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
              />
            </div>
            <div v-if="editingTask">
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">负责人</label>
              <input
                v-model="formData.assignedToName"
                type="text"
                placeholder="请输入负责人"
                class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all"
              />
            </div>
          </div>

          <!-- Description -->
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">任务描述</label>
            <textarea
              v-model="formData.description"
              placeholder="请输入详细描述..."
              class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2.5 outline-none transition-all resize-none h-24"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-3">
          <button
            @click="showAddDialog = false"
            class="flex-1 py-2.5 text-sm font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
          >
            取消
          </button>
          <button
            @click="handleSubmit"
            :disabled="!formData.title.trim() || submitting"
            class="flex-1 py-2.5 text-sm font-bold text-white bg-primary hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl transition-colors shadow-sm"
          >
            {{ submitting ? '提交中...' : (editingTask ? '保存修改' : '确认创建') }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useTaskStore } from '@/stores/task'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiParseTask } from '@/api/task'
import type { Task, TaskAddBO, TaskStatus } from '@/types/common'

const taskStore = useTaskStore()
const { isMobile } = useResponsive()

const currentStatus = ref('all')
const valueFilter = ref<'all' | 'high-impact'>('all')
const showAddDialog = ref(false)
const showDetailDialog = ref(false)
const editingTask = ref<Task | null>(null)
const selectedTask = ref<Task | null>(null)
const submitting = ref(false)
const aiParseInput = ref('')
const aiParsing = ref(false)

const formData = reactive<TaskAddBO & { status?: TaskStatus; customerName?: string; assignedToName?: string }>({
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueDate: undefined,
  status: undefined,
  taskType: '',
  participantNames: '',
  customerName: '',
  assignedToName: ''
})

// Computed properties
const statusTabs = computed(() => {
  const tasks = taskStore.taskList
  return [
    { value: 'all', label: '全部', count: taskStore.totalCount },
    { value: 'PENDING', label: '待处理', count: tasks.filter(t => t.status === 'PENDING').length },
    { value: 'IN_PROGRESS', label: '进行中', count: tasks.filter(t => t.status === 'IN_PROGRESS').length },
    { value: 'COMPLETED', label: '已完成', count: tasks.filter(t => t.status === 'COMPLETED').length }
  ]
})

const displayedTasks = computed(() => {
  if (valueFilter.value === 'high-impact') {
    return taskStore.taskList.filter(t => t.priority === 'HIGH')
  }
  return taskStore.taskList
})

const totalPages = computed(() => Math.ceil(taskStore.totalCount / (taskStore.queryParams.limit || 10)))

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = taskStore.queryParams.page || 1
  const pages: number[] = []
  let start = Math.max(1, current - 2)
  const end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

onMounted(() => {
  taskStore.fetchTaskList(true)
})



function handleStatusFilter(status: string) {
  currentStatus.value = status
  taskStore.queryParams.status = status === 'all' ? undefined : status as TaskStatus
  taskStore.queryParams.page = 1
  taskStore.fetchTaskList(false)
}

function handlePageChange(page: number) {
  if (taskStore.queryParams.page === page) return
  taskStore.queryParams.page = page
  taskStore.fetchTaskList(false)
}

async function handleToggleComplete(task: Task) {
  const newStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
  await taskStore.changeTaskStatus(task.taskId, newStatus)
  await taskStore.fetchTaskList(false)
}

async function handleStartTask(task: Task) {
  if (task.status === 'PENDING') {
    await taskStore.changeTaskStatus(task.taskId, 'IN_PROGRESS')
    await taskStore.fetchTaskList(false)
    ElMessage.success('任务已开始处理')
  }
}

function handleViewDetail(task: Task) {
  selectedTask.value = task
  if (isMobile.value) {
    showDetailDialog.value = true
  }
}

function handleAddTask() {
  resetForm()
  showAddDialog.value = true
}

function handleEdit(task: Task) {
  editingTask.value = task
  Object.assign(formData, {
    title: task.title,
    description: task.description || '',
    priority: task.priority,
    dueDate: task.dueDate ? formatDateTimeLocal(task.dueDate) : undefined,
    status: task.status,
    taskType: task.taskType || '',
    participantNames: task.participantNames || '',
    customerName: task.customerName || '',
    assignedToName: task.assignedToName || ''
  })
  showAddDialog.value = true
}

async function handleDelete(task: Task) {
  try {
    await ElMessageBox.confirm(`确定要删除任务「${task.title}」吗？`, '提示', { type: 'warning' })
    await taskStore.removeTask(task.taskId)
    ElMessage.success('删除成功')
  } catch {
    // Cancelled
  }
}

async function handleSubmit() {
  if (!formData.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }

  submitting.value = true
  try {
    const submitData: any = {
      title: formData.title,
      description: formData.description,
      priority: formData.priority,
      dueDate: formData.dueDate,
      taskType: formData.taskType,
      participantNames: formData.participantNames
    }
    if (editingTask.value) {
      await taskStore.editTask({ ...submitData, taskId: editingTask.value.taskId, status: formData.status })
      ElMessage.success('更新成功')
    } else {
      await taskStore.createTask(submitData)
      ElMessage.success('创建成功')
    }
    showAddDialog.value = false
    resetForm()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  editingTask.value = null
  aiParseInput.value = ''
  Object.assign(formData, {
    title: '', description: '', priority: 'MEDIUM', dueDate: undefined, status: undefined,
    taskType: '', participantNames: '', customerName: '', assignedToName: ''
  })
}

async function handleAiParse() {
  if (!aiParseInput.value.trim()) return
  aiParsing.value = true
  try {
    const result = await aiParseTask(aiParseInput.value)
    if (result.title) formData.title = result.title
    if (result.dueDate) formData.dueDate = result.dueDate
    if (result.priority) formData.priority = result.priority.toUpperCase() as any
    if (result.taskType) formData.taskType = result.taskType
    if (result.customerName) formData.customerName = result.customerName
    if (result.participantNames) formData.participantNames = result.participantNames
    if (result.description) formData.description = result.description
    if (result.assignedToName) formData.assignedToName = result.assignedToName
    ElMessage.success('AI 解析完成，请确认并补充信息')
  } catch {
    ElMessage.error('AI 解析失败，请手动填写')
  } finally {
    aiParsing.value = false
  }
}

// AI Score - deterministic based on priority + taskId
function getAiScore(task: Task): number {
  const base = task.priority === 'HIGH' ? 90 : task.priority === 'MEDIUM' ? 60 : 30
  const offset = Number(task.taskId) % 10
  return Math.min(99, base + offset)
}

// AI Insight - use description or generate from priority
function getAiInsight(task: Task): string {
  if (task.description) return task.description
  if (task.priority === 'HIGH') return '此任务优先级较高，建议尽快处理以推进业务进展。'
  if (task.priority === 'MEDIUM') return '常规跟进任务，按计划执行即可。'
  return '低优先级任务，可在空闲时间处理。'
}

// Check if task is overdue
function isOverdue(task: Task): boolean {
  if (!task.dueDate || task.status === 'COMPLETED') return false
  return new Date(task.dueDate) < new Date()
}

function getPriorityColor(priority: string): string {
  switch (priority) {
    case 'HIGH': return 'text-red-500'
    case 'MEDIUM': return 'text-orange-500'
    default: return 'text-green-500'
  }
}

function getPriorityLabel(priority: string): string {
  return { HIGH: '高优先级', MEDIUM: '中优先级', LOW: '低优先级' }[priority] || priority
}

function getStatusLabel(status: string): string {
  return { PENDING: '待处理', IN_PROGRESS: '进行中', COMPLETED: '已完成' }[status] || status
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function formatDateTime(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatDateTimeLocal(dateStr: string): string {
  const d = new Date(dateStr)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getRelativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  date.setHours(0, 0, 0, 0)
  now.setHours(0, 0, 0, 0)
  const diff = Math.ceil((date.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))

  if (diff < 0) return `已逾期${-diff}天`
  if (diff === 0) return '今天到期'
  if (diff === 1) return '明天到期'
  return `${diff}天后到期`
}
</script>

<style scoped>
.slide-right-enter-active {
  transition: all 0.3s ease;
}
.slide-right-leave-active {
  transition: all 0.2s ease;
}
.slide-right-enter-from {
  transform: translateX(100%);
  opacity: 0;
}
.slide-right-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
</style>

<style>
.task-dialog .el-dialog__header {
  padding: 20px 24px 0;
  margin-right: 0;
}
.task-dialog .el-dialog__body {
  padding: 0;
  max-height: 65vh;
  overflow-y: auto;
}
.task-dialog .el-dialog__footer {
  padding: 16px 24px 20px;
}
/* Prevent overlay from scrolling — dialog body scrolls internally */
.el-overlay:has(.task-dialog) {
  overflow: hidden;
}
</style>
