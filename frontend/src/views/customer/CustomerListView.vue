<template>
  <div class="flex flex-col gap-6 px-6 py-6">
    <!-- Header -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2 class="text-2xl font-bold text-slate-900">客户列表</h2>
        <p class="text-sm text-slate-500">管理您的客户关系并查看 AI 驱动的业务洞察。</p>
      </div>
      <div class="flex items-center gap-3 flex-wrap">
        <!-- Search -->
        <div class="relative group flex items-center">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 group-focus-within:text-primary transition-colors">search</span>
          <input
            v-model="customerStore.queryParams.keyword"
            type="text"
            placeholder="搜索客户名称、联系人..."
            class="pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm w-full sm:w-80 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all shadow-sm"
            @keydown.enter="handleSearch"
            @change="handleSearch"
          />
        </div>
        <!-- Import/Export - desktop only -->
        <div v-if="!isMobile" class="flex items-center gap-1.5 border-r border-slate-200 pr-3 mr-1">
          <button class="px-3 py-1.5 text-xs font-medium text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors flex items-center gap-1.5" @click="showImportDialog = true">
            <span class="material-symbols-outlined text-[16px]">upload</span>
            导入
          </button>
          <button class="px-3 py-1.5 text-xs font-medium text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors flex items-center gap-1.5" :disabled="exporting" @click="handleExport">
            <span class="material-symbols-outlined text-[16px]">download</span>
            导出
          </button>
        </div>
        <!-- Add Customer -->
        <button
          class="px-5 py-2.5 bg-primary text-white rounded-xl font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all flex items-center gap-2"
          @click="showAddDialog = true"
        >
          <span class="material-symbols-outlined text-sm">person_add</span>
          <span v-if="!isMobile">新增客户</span>
        </button>
      </div>
    </div>

    <!-- Main Content: Table + AI Sidebar -->
    <div class="flex flex-col xl:flex-row gap-6 items-start relative">
      <!-- Table Area -->
      <div class="flex-1 min-w-0 w-full space-y-6">
        <div class="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm" v-loading="customerStore.loading">
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50 border-b border-slate-200">
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">公司名称</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">客户级别</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">联系人</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">电话</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">行业</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">商机阶段</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">报价金额</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">最后跟进</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">负责人</th>
                  <th v-for="field in listCustomFields" :key="field.fieldId" class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap">
                    {{ field.fieldLabel }}
                  </th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap text-right">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100">
                <tr
                  v-for="customer in customerStore.customerList"
                  :key="customer.customerId"
                  class="hover:bg-blue-50 transition-colors group cursor-pointer"
                  @click="handleRowClick(customer)"
                >
                  <!-- Company Name -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center gap-3">
                      <div class="size-8 rounded bg-primary/10 text-primary flex items-center justify-center font-bold text-xs">
                        {{ customer.companyName?.charAt(0) || '?' }}
                      </div>
                      <span class="text-sm font-semibold text-slate-900 group-hover:text-primary truncate max-w-[200px] block transition-colors">{{ customer.companyName }}</span>
                    </div>
                  </td>

                  <!-- Level -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span v-if="customer.level"
                      class="inline-flex items-center px-1.5 py-0 rounded text-[10px] font-bold"
                      :class="{
                        'bg-emerald-50 text-emerald-600': customer.level === 'A',
                        'bg-blue-50 text-blue-600': customer.level === 'B',
                        'bg-slate-100 text-slate-500': customer.level === 'C'
                      }"
                    >{{ customer.level }}级</span>
                    <span v-else class="text-slate-300">-</span>
                  </td>

                  <!-- Contact -->
                  <td class="px-6 py-4 text-sm text-slate-600 whitespace-nowrap">
                    <div v-if="customer.primaryContactName">
                      <div>{{ customer.primaryContactName }}</div>
                      <div v-if="customer.primaryContactPosition" class="text-[10px] text-slate-400">{{ customer.primaryContactPosition }}</div>
                    </div>
                    <span v-else class="text-slate-300">-</span>
                  </td>

                  <!-- Phone -->
                  <td class="px-6 py-4 text-sm text-slate-600 font-mono whitespace-nowrap">
                    {{ customer.primaryContactPhone || '-' }}
                  </td>

                  <!-- Industry -->
                  <td class="px-6 py-4 text-sm text-slate-600 whitespace-nowrap">
                    {{ customer.industry || '-' }}
                  </td>

                  <!-- Stage Badge -->
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span
                      class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                      :class="getStageBadgeClass(customer.stage)"
                    >
                      <span class="size-1.5 rounded-full mr-1.5" :class="getStageDotClass(customer.stage)"></span>
                      {{ getStageLabel(customer.stage) }}
                    </span>
                  </td>

                  <!-- Quotation -->
                  <td class="px-6 py-4 text-sm font-medium text-slate-900 whitespace-nowrap">
                    {{ customer.quotation ? formatMoney(customer.quotation) : '-' }}
                  </td>

                  <!-- Last Contact -->
                  <td class="px-6 py-4 text-sm text-slate-500 whitespace-nowrap">
                    {{ formatRelativeTime(customer.lastContactTime) }}
                  </td>

                  <!-- Owner -->
                  <td class="px-6 py-4 text-sm text-slate-600 whitespace-nowrap">
                    <div class="flex items-center gap-2" @click.stop>
                      <div class="size-6 rounded-full bg-slate-100 flex items-center justify-center text-[10px] font-bold text-slate-500">
                        {{ customer.ownerName?.charAt(0) || '?' }}
                      </div>
                      <el-popover trigger="click" :width="220" @show="loadUserList">
                        <template #reference>
                          <span class="cursor-pointer hover:text-primary transition-colors truncate max-w-[100px] inline-block align-middle">{{ customer.ownerName || '-' }}</span>
                        </template>
                        <div>
                          <el-input v-model="ownerSearch" placeholder="搜索用户" size="small" clearable class="mb-2" />
                          <div class="max-h-48 overflow-auto">
                            <div
                              v-for="u in filteredUserList"
                              :key="u.userId"
                              class="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer hover:bg-slate-100 transition-colors"
                              :class="{ 'bg-primary/5': String(u.userId) === String(customer.ownerId) }"
                              @click="handleTransfer(customer, u)"
                            >
                              <div class="size-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-[10px] font-bold flex-shrink-0">{{ u.realname?.charAt(0) || '?' }}</div>
                              <span class="text-sm truncate">{{ u.realname }}</span>
                              <span v-if="String(u.userId) === String(customer.ownerId)" class="material-symbols-outlined ml-auto text-primary text-sm">check</span>
                            </div>
                            <div v-if="filteredUserList.length === 0" class="text-center text-sm text-slate-400 py-3">无匹配用户</div>
                          </div>
                        </div>
                      </el-popover>
                    </div>
                  </td>

                  <!-- Custom Fields -->
                  <td v-for="field in listCustomFields" :key="field.fieldId" class="px-6 py-4 text-sm text-slate-600 whitespace-nowrap">
                    {{ customer.customFields?.[field.fieldName] ?? '-' }}
                  </td>

                  <!-- Actions -->
                  <td class="px-6 py-4 text-right whitespace-nowrap" @click.stop>
                    <div class="flex items-center justify-end gap-2">
                      <button
                        class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-primary/10 text-primary text-xs font-bold rounded-lg hover:bg-primary/20 transition-colors"
                        @click="handleAiFollowUp(customer)"
                      >
                        <span class="material-symbols-outlined text-sm">smart_toy</span>
                        AI 跟进
                      </button>
                      <button
                        v-if="customer.stage !== 'closed' && customer.stage !== 'lost'"
                        class="inline-flex items-center gap-1 px-3 py-1.5 bg-primary/10 text-primary text-xs font-bold rounded-lg hover:bg-primary/20 transition-colors"
                        @click="handleEdit(customer)"
                      >
                        <span class="material-symbols-outlined text-sm">edit</span>
                        编辑
                      </button>
                      <el-dropdown
                        v-if="customer.stage !== 'closed' && customer.stage !== 'lost'"
                        trigger="click"
                        size="small"
                        @command="(stage: string) => handleAdvanceStage(customer.customerId, stage)"
                      >
                        <button class="size-8 flex items-center justify-center rounded-lg hover:bg-slate-100 text-slate-400 transition-colors">
                          <span class="material-symbols-outlined text-lg">more_vert</span>
                        </button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item
                              v-if="getNextStage(customer.stage)"
                              :command="getNextStage(customer.stage)"
                              style="font-weight: bold"
                            >
                              推进到「{{ allStageOptions.find(s => s.value === getNextStage(customer.stage))?.label }}」
                            </el-dropdown-item>
                            <el-dropdown-item v-if="getNextStage(customer.stage)" divided />
                            <el-dropdown-item
                              v-for="opt in allStageOptions.filter(s => s.value !== customer.stage)"
                              :key="opt.value"
                              :command="opt.value"
                            >{{ opt.label }}</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- Empty State -->
            <div v-if="!customerStore.loading && customerStore.customerList.length === 0" class="text-center py-16">
              <div class="size-16 bg-slate-50 rounded-full flex items-center justify-center text-slate-200 mx-auto mb-4">
                <span class="material-symbols-outlined text-4xl">group</span>
              </div>
              <p class="text-slate-400 text-sm font-medium">暂无客户数据</p>
            </div>
          </div>

          <!-- Pagination -->
          <div v-if="customerStore.totalCount > 0" class="px-6 py-4 bg-slate-50/50 flex items-center justify-between border-t border-slate-200">
            <span class="text-xs text-slate-500">
              共 {{ customerStore.totalCount }} 条客户数据
            </span>
            <div class="flex items-center gap-1">
              <button
                class="size-8 flex items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50"
                :disabled="(customerStore.queryParams.page || 1) <= 1"
                @click="handlePageChange((customerStore.queryParams.page || 1) - 1)"
              >
                <span class="material-symbols-outlined text-lg">chevron_left</span>
              </button>
              <button
                v-for="pageNum in visiblePages"
                :key="pageNum"
                class="size-8 flex items-center justify-center rounded border text-xs font-bold"
                :class="pageNum === (customerStore.queryParams.page || 1)
                  ? 'border-primary bg-primary text-white'
                  : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'"
                @click="handlePageChange(pageNum)"
              >{{ pageNum }}</button>
              <span v-if="totalPages > 5" class="px-1 text-slate-400 text-xs">...</span>
              <button
                class="size-8 flex items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50"
                :disabled="(customerStore.queryParams.page || 1) >= totalPages"
                @click="handlePageChange((customerStore.queryParams.page || 1) + 1)"
              >
                <span class="material-symbols-outlined text-lg">chevron_right</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- AI Insight Sidebar -->
      <div v-if="!isMobile" class="w-80 shrink-0 space-y-4">
        <div class="flex items-center gap-2 px-1 mb-2">
          <h3 class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">AI 智能洞察预警</h3>
          <span class="size-2 rounded-full bg-primary animate-pulse"></span>
        </div>

        <!-- High Potential Warning -->
        <div class="bg-white border border-slate-200 rounded-xl p-4 relative overflow-hidden group cursor-pointer hover:border-primary/50 hover:shadow-md transition-all">
          <div class="flex items-center gap-2 mb-2">
            <span class="material-symbols-outlined text-primary text-xl">auto_awesome</span>
            <h3 class="text-sm font-bold text-slate-900">高潜力客户预警</h3>
          </div>
          <p class="text-xs text-slate-500 leading-relaxed">发现 {{ negotiationCount }} 位客户近期成交概率显著提升，建议优先跟进。</p>
          <div class="mt-3 flex items-center justify-between">
            <span class="text-[10px] font-bold text-primary bg-primary/5 px-2 py-0.5 rounded">{{ negotiationCount }} 位待处理</span>
            <span class="material-symbols-outlined text-slate-300 group-hover:text-primary transition-colors text-sm">arrow_forward</span>
          </div>
        </div>

        <!-- Auto Follow-up -->
        <div class="bg-white border border-slate-200 rounded-xl p-4 relative overflow-hidden group cursor-pointer hover:border-indigo-400 hover:shadow-md transition-all">
          <div class="flex items-center gap-2 mb-2">
            <span class="material-symbols-outlined text-indigo-600 text-xl">mark_email_unread</span>
            <h3 class="text-sm font-bold text-slate-900">自动化跟进生成</h3>
          </div>
          <p class="text-xs text-slate-500 leading-relaxed">有 {{ overdueCount }} 个客户超过7天未跟进，建议尽快安排跟进计划。</p>
          <div class="mt-3 flex items-center justify-between">
            <span class="text-[10px] font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">{{ overdueCount }} 个待跟进</span>
            <span class="material-symbols-outlined text-slate-300 group-hover:text-indigo-600 transition-colors text-sm">arrow_forward</span>
          </div>
        </div>

        <!-- Forecast Update -->
        <div class="bg-white border border-slate-200 rounded-xl p-4 relative overflow-hidden group cursor-pointer hover:border-emerald-400 hover:shadow-md transition-all">
          <div class="flex items-center gap-2 mb-2">
            <span class="material-symbols-outlined text-emerald-600 text-xl">insights</span>
            <h3 class="text-sm font-bold text-slate-900">成交预测更新</h3>
          </div>
          <p class="text-xs text-slate-500 leading-relaxed">当前成交转化率 {{ conversionRate }}%，共 {{ closedCount }} 个客户已成交。</p>
          <div class="mt-3 flex items-center justify-between">
            <span class="text-[10px] font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">{{ conversionRate }}% 转化率</span>
            <span class="material-symbols-outlined text-slate-300 group-hover:text-emerald-600 transition-colors text-sm">arrow_forward</span>
          </div>
        </div>

        <div class="p-4 bg-slate-50 rounded-xl border border-slate-200 border-dashed">
          <p class="text-[10px] text-slate-400 leading-relaxed text-center italic">
            AI 助手正在实时分析您的客户数据，预警信息将在此处即时更新。
          </p>
        </div>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div v-if="showAddDialog" class="fixed inset-0 z-[100] flex items-center justify-center p-4 sm:p-6">
          <!-- Backdrop -->
          <div class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" @click="showAddDialog = false" />

          <!-- Modal Container -->
          <div :class="[
            'relative w-full bg-slate-50 shadow-2xl overflow-hidden flex flex-col',
            isMobile ? 'max-w-full max-h-full rounded-none inset-0' : 'max-w-5xl max-h-[90vh] rounded-[2.5rem]'
          ]">
            <!-- Header -->
            <div class="bg-white border-b border-slate-200 px-6 sm:px-8 py-4 sm:py-5 flex items-center justify-between shrink-0">
              <div class="flex items-center gap-3 sm:gap-4">
                <div class="size-10 sm:size-12 rounded-2xl bg-primary/10 flex items-center justify-center text-primary">
                  <span class="material-symbols-outlined">person_add</span>
                </div>
                <div>
                  <h2 class="text-lg sm:text-xl font-bold text-slate-900">{{ editingCustomer ? '编辑客户' : '新增客户' }}</h2>
                  <p class="text-xs text-slate-500">{{ editingCustomer ? '修改客户基本信息和联系方式' : '使用 AI 智能录入或手动填写客户信息' }}</p>
                </div>
              </div>
              <div class="flex items-center gap-2 sm:gap-3">
                <button @click="showAddDialog = false" class="px-4 sm:px-6 py-2 sm:py-2.5 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100 transition-colors">
                  取消
                </button>
                <button @click="handleSubmit" :disabled="submitting" class="px-5 sm:px-8 py-2 sm:py-2.5 bg-primary text-white rounded-xl text-sm font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all flex items-center gap-2 disabled:opacity-50">
                  <span v-if="submitting" class="size-3 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                  <span v-else class="material-symbols-outlined text-sm">save</span>
                  {{ editingCustomer ? '保存' : '创建客户' }}
                </button>
              </div>
            </div>

            <!-- Content -->
            <div class="flex-1 overflow-y-auto p-4 sm:p-6">
              <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
                <!-- Left Column: Form -->
                <div class="lg:col-span-7 space-y-5">
                  <!-- AI 智能录入 (only in create mode) -->
                  <section v-if="!editingCustomer" class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                    <div class="px-5 py-3 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
                      <div class="flex items-center gap-2">
                        <span class="material-symbols-outlined text-primary text-lg">auto_awesome</span>
                        <h3 class="text-xs font-bold text-slate-900 uppercase tracking-wider">AI 智能录入</h3>
                      </div>
                      <span class="text-[9px] font-bold text-slate-400 uppercase tracking-widest">粘贴名片、邮件或简介</span>
                    </div>
                    <div class="p-4 space-y-3">
                      <div class="relative">
                        <textarea
                          v-model="aiInputText"
                          class="w-full h-24 p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-primary/50 outline-none resize-none transition-all"
                          placeholder="在此粘贴客户描述、邮件内容，或直接粘贴 (Ctrl+V) 名片图片..."
                          @paste="handleAiPaste"
                        />
                        <!-- Image preview inside textarea area -->
                        <div v-if="aiImagePreview" class="absolute right-3 bottom-3">
                          <div class="relative group">
                            <img :src="aiImagePreview" alt="名片图片" class="h-16 w-24 object-cover rounded-lg border-2 border-primary shadow-lg" />
                            <button
                              @click="removeAiImage"
                              class="absolute -top-2 -right-2 size-5 bg-red-500 text-white rounded-full flex items-center justify-center shadow-md hover:bg-red-600 transition-colors"
                            >
                              <span class="material-symbols-outlined text-[12px] font-bold">close</span>
                            </button>
                          </div>
                        </div>
                      </div>

                      <div class="flex justify-end items-center gap-3">
                        <span v-if="aiImagePreview && !aiParsing" class="text-[10px] text-primary font-bold flex items-center gap-1 animate-pulse">
                          <span class="material-symbols-outlined text-sm">image</span>
                          检测到名片图片，点击智能提取
                        </span>
                        <button
                          @click="handleAiExtract"
                          :disabled="aiParsing || (!aiInputText.trim() && !aiImageFile)"
                          :class="[
                            'flex items-center gap-2 px-5 py-2 rounded-xl text-xs font-bold transition-all',
                            aiParsing
                              ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
                              : 'bg-slate-900 text-white hover:bg-slate-800 shadow-lg shadow-slate-900/10'
                          ]"
                        >
                          <template v-if="aiParsing">
                            <span class="size-3 border-2 border-slate-300 border-t-slate-600 rounded-full animate-spin"></span>
                            解析中...
                          </template>
                          <template v-else>
                            <span class="material-symbols-outlined text-sm">psychology</span>
                            智能提取
                          </template>
                        </button>
                      </div>

                      <!-- Mobile AI result summary -->
                      <div v-if="isMobile && aiParseResult" class="p-3 bg-primary/5 rounded-xl border border-primary/10 text-xs text-slate-600 space-y-1">
                        <div v-if="aiParseResult.score != null" class="font-bold text-primary">潜力评分: {{ aiParseResult.score }}/100</div>
                        <div v-if="aiParseResult.summary">{{ aiParseResult.summary }}</div>
                        <div v-if="aiParseResult.tags?.length" class="flex flex-wrap gap-1 mt-1">
                          <span v-for="tag in aiParseResult.tags" :key="tag" class="px-2 py-0.5 bg-primary/10 text-primary text-[10px] font-bold rounded-full">{{ tag }}</span>
                        </div>
                      </div>
                    </div>
                  </section>

                  <!-- Basic Information -->
                  <section class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 sm:p-6">
                    <h3 class="text-xs font-bold text-slate-900 mb-5 flex items-center gap-2 uppercase tracking-wider">
                      <span class="w-1 h-3 bg-primary rounded-full"></span>
                      基础信息
                    </h3>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
                      <div class="md:col-span-2 space-y-1.5">
                        <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">公司名称 <span class="text-red-400">*</span></label>
                        <input
                          v-model="formData.companyName"
                          placeholder="请输入公司全称"
                          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all"
                        />
                      </div>
                      <div class="space-y-1.5">
                        <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">所属行业</label>
                        <input
                          v-model="formData.industry"
                          placeholder="例如：互联网 / 科技"
                          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all"
                        />
                      </div>
                      <div class="space-y-1.5">
                        <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">客户级别</label>
                        <select
                          v-model="formData.level"
                          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all appearance-none"
                        >
                          <option value="A">A级客户</option>
                          <option value="B">B级客户</option>
                          <option value="C">C级客户</option>
                        </select>
                      </div>
                      <div class="space-y-1.5">
                        <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">商机阶段</label>
                        <select
                          v-model="formData.stage"
                          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all appearance-none"
                        >
                          <option value="lead">线索</option>
                          <option value="qualified">资格审查</option>
                          <option value="proposal">方案报价</option>
                          <option value="negotiation">谈判中</option>
                          <option value="closed">已成交</option>
                        </select>
                      </div>
                      <!-- Dynamic Custom Fields (inline in same grid) -->
                      <DynamicFieldForm
                        ref="dynamicFieldFormRef"
                        entity-type="customer"
                        v-model="customFieldValues"
                        :show-divider="false"
                        native-style
                      />
                    </div>
                  </section>

                  <!-- Contact Information -->
                  <section class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 sm:p-6">
                    <h3 class="text-xs font-bold text-slate-900 mb-5 flex items-center gap-2 uppercase tracking-wider">
                      <span class="w-1 h-3 bg-indigo-500 rounded-full"></span>
                      联系人信息
                    </h3>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
                      <div class="space-y-1.5">
                        <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">主要联系人</label>
                        <input
                          v-model="formData.contactName"
                          placeholder="请输入联系人姓名"
                          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all"
                        />
                      </div>
                      <div class="space-y-1.5">
                        <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">手机号码</label>
                        <input
                          v-model="formData.contactPhone"
                          placeholder="请输入联系电话"
                          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all"
                        />
                      </div>
                      <div class="space-y-1.5 md:col-span-2">
                        <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">电子邮箱</label>
                        <input
                          v-model="formData.contactEmail"
                          placeholder="请输入邮箱地址"
                          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all"
                        />
                      </div>
                    </div>
                  </section>
                </div>

                <!-- Right Column: AI Insights -->
                <div class="lg:col-span-5 space-y-5" :class="{ 'hidden': isMobile }">
                  <!-- AI Insights (shown after parse) -->
                  <template v-if="aiParseResult">
                    <!-- AI Score Card -->
                    <div v-if="aiParseResult.score != null" class="bg-slate-900 rounded-2xl p-6 text-white relative overflow-hidden">
                      <div class="relative z-10">
                        <div class="flex items-center justify-between mb-4">
                          <span class="text-[9px] font-bold text-primary uppercase tracking-widest">AI 潜力评分</span>
                          <span class="material-symbols-outlined text-primary text-lg">verified</span>
                        </div>
                        <div class="flex items-baseline gap-2 mb-1">
                          <span class="text-5xl font-black">{{ aiParseResult.score }}</span>
                          <span class="text-lg text-slate-400">/ 100</span>
                        </div>
                        <p class="text-[10px] text-slate-400 mb-4">基于行业匹配度、需求迫切度及规模评估</p>
                        <div v-if="aiParseResult.tags?.length" class="flex flex-wrap gap-1.5">
                          <span v-for="tag in aiParseResult.tags" :key="tag" class="px-2 py-0.5 bg-white/10 rounded-md text-[9px] font-bold uppercase tracking-wider">{{ tag }}</span>
                        </div>
                      </div>
                      <div class="absolute -right-8 -bottom-8 size-32 bg-primary/20 rounded-full blur-3xl"></div>
                    </div>

                    <!-- AI Analysis Card -->
                    <div v-if="aiParseResult.summary || aiParseResult.nextStep" class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 space-y-4">
                      <div v-if="aiParseResult.summary">
                        <h4 class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-2 flex items-center gap-2">
                          <span class="material-symbols-outlined text-sm text-primary">analytics</span>
                          AI 深度分析
                        </h4>
                        <p class="text-xs text-slate-700 leading-relaxed">{{ aiParseResult.summary }}</p>
                      </div>
                      <div v-if="aiParseResult.nextStep" :class="{ 'pt-4 border-t border-slate-100': aiParseResult.summary }">
                        <h4 class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-2 flex items-center gap-2">
                          <span class="material-symbols-outlined text-sm text-primary">rocket_launch</span>
                          建议下一步行动
                        </h4>
                        <p class="text-xs text-slate-700 leading-relaxed">{{ aiParseResult.nextStep }}</p>
                      </div>
                    </div>

                    <!-- Key Points -->
                    <div v-if="aiParseResult.keyPoints?.length" class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5">
                      <h4 class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3 flex items-center gap-2">
                        <span class="material-symbols-outlined text-sm text-primary">checklist</span>
                        关键要点
                      </h4>
                      <ul class="space-y-2">
                        <li v-for="(point, i) in aiParseResult.keyPoints" :key="i" class="text-xs text-slate-700 flex items-start gap-2 leading-relaxed">
                          <span class="text-primary mt-0.5 shrink-0">•</span> {{ point }}
                        </li>
                      </ul>
                    </div>
                  </template>

                  <!-- Default placeholder (no AI result yet) -->
                  <div v-else class="bg-white rounded-2xl border border-slate-200 border-dashed p-8 text-center space-y-3 min-h-[300px] flex flex-col items-center justify-center">
                    <div class="size-12 bg-slate-50 rounded-xl flex items-center justify-center mx-auto">
                      <span class="material-symbols-outlined text-slate-300 text-2xl">psychology</span>
                    </div>
                    <div>
                      <h4 class="text-xs font-bold text-slate-900">等待 AI 分析</h4>
                      <p class="text-[10px] text-slate-400 mt-1 leading-relaxed">
                        录入客户信息或使用智能提取后，AI 将在此为您提供<br/>深度洞察与潜力评估。
                      </p>
                    </div>
                  </div>

                  <!-- Quick Tips -->
                  <div class="p-5 bg-primary/5 rounded-2xl border border-primary/10">
                    <h4 class="text-[10px] font-bold text-primary uppercase tracking-widest mb-2 flex items-center gap-2">
                      <span class="material-symbols-outlined text-sm">lightbulb</span>
                      小提示
                    </h4>
                    <p class="text-[10px] text-slate-600 leading-relaxed">
                      您可以直接粘贴该客户的 LinkedIn 简介、公司官网文字，或者<strong>直接在输入框 Ctrl+V 粘贴名片图片</strong>，AI 会自动识别并补全信息。
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Import Dialog -->
    <el-dialog
      v-model="showImportDialog"
      title="导入客户"
      :width="isMobile ? '95%' : '800px'"
      :fullscreen="isMobile"
      @close="resetImport"
    >
      <!-- Step 1: Upload -->
      <div v-if="importStep === 1" class="text-center py-6">
        <el-upload
          ref="importUploadRef"
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleImportFileChange"
          drag
        >
          <span class="material-symbols-outlined text-4xl text-slate-400 mb-2">upload_file</span>
          <div class="text-slate-600">将 Excel 文件拖到此处，或<em class="text-primary">点击上传</em></div>
          <template #tip>
            <div class="text-xs text-slate-400 mt-2">支持 .xlsx / .xls 格式，表头需包含「公司名称」列</div>
            <div class="mt-2">
              <button class="text-primary text-sm font-medium hover:underline" @click.stop="handleDownloadTemplate">
                下载导入模板
              </button>
            </div>
          </template>
        </el-upload>
      </div>

      <!-- Step 2: Preview -->
      <div v-if="importStep === 2">
        <div class="flex gap-4 mb-4 flex-wrap">
          <span class="text-xs font-bold px-2 py-1 bg-slate-100 rounded">总计 {{ importPreview!.totalRows }} 行</span>
          <span class="text-xs font-bold px-2 py-1 bg-emerald-50 text-emerald-600 rounded">有效 {{ importPreview!.validRows }} 行</span>
          <span v-if="importPreview!.duplicateRows > 0" class="text-xs font-bold px-2 py-1 bg-amber-50 text-amber-600 rounded">重复 {{ importPreview!.duplicateRows }} 行</span>
          <span v-if="importPreview!.errorRows > 0" class="text-xs font-bold px-2 py-1 bg-red-50 text-red-600 rounded">错误 {{ importPreview!.errorRows }} 行</span>
        </div>

        <div v-if="importPreview!.duplicateRows > 0" class="mb-4 p-3 bg-yellow-50 rounded-lg">
          <span class="text-sm text-yellow-700 mr-3">重复行统一处理：</span>
          <el-radio-group v-model="globalDuplicateMode" @change="applyGlobalDuplicateMode">
            <el-radio value="skip">全部跳过</el-radio>
            <el-radio value="overwrite">全部覆盖</el-radio>
          </el-radio-group>
        </div>

        <el-table
          :data="importPreview!.rows"
          :max-height="400"
          size="small"
          :row-class-name="importRowClassName"
        >
          <el-table-column label="行号" prop="rowNum" width="60" />
          <el-table-column label="公司名称" prop="companyName" min-width="120" />
          <el-table-column label="行业" prop="industry" width="100" />
          <el-table-column label="阶段" prop="stage" width="90">
            <template #default="{ row }">{{ getStageLabel(row.stage) || row.stage }}</template>
          </el-table-column>
          <el-table-column label="联系人" prop="contactName" width="90" />
          <el-table-column label="状态" width="150">
            <template #default="{ row }">
              <span v-if="row.errors && row.errors.length > 0" class="text-xs font-bold px-2 py-0.5 bg-red-50 text-red-600 rounded">{{ row.errors[0] }}</span>
              <template v-else-if="row.duplicate">
                <el-radio-group v-model="row.handleMode" size="small">
                  <el-radio value="skip">跳过</el-radio>
                  <el-radio value="overwrite">覆盖</el-radio>
                </el-radio-group>
              </template>
              <span v-else class="text-xs font-bold px-2 py-0.5 bg-emerald-50 text-emerald-600 rounded">正常</span>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="importPreview!.errors && importPreview!.errors.length > 0" class="mt-3">
          <el-alert
            v-for="(err, idx) in importPreview!.errors"
            :key="idx"
            :title="err"
            type="error"
            :closable="false"
            class="mb-1"
          />
        </div>
      </div>

      <!-- Step 3: Result -->
      <div v-if="importStep === 3" class="text-center py-6">
        <span class="material-symbols-outlined text-5xl text-green-500 mb-3">check_circle</span>
        <h3 class="text-lg font-bold mb-4">导入完成</h3>
        <div class="flex justify-center gap-6 text-sm">
          <div>新增 <span class="text-primary font-bold text-lg">{{ importResult!.imported }}</span> 条</div>
          <div>更新 <span class="text-orange-500 font-bold text-lg">{{ importResult!.updated }}</span> 条</div>
          <div>跳过 <span class="text-slate-500 font-bold text-lg">{{ importResult!.skipped }}</span> 条</div>
        </div>
        <div v-if="importResult!.errors && importResult!.errors.length > 0" class="mt-4 text-left">
          <el-alert
            v-for="(err, idx) in importResult!.errors"
            :key="idx"
            :title="err"
            type="warning"
            :closable="false"
            class="mb-1"
          />
        </div>
      </div>

      <template #footer>
        <template v-if="importStep === 1">
          <el-button @click="showImportDialog = false">取消</el-button>
          <el-button type="primary" :loading="importLoading" :disabled="!importFile" @click="handleImportPreview">解析文件</el-button>
        </template>
        <template v-else-if="importStep === 2">
          <el-button @click="importStep = 1">上一步</el-button>
          <el-button type="primary" :loading="importLoading" @click="handleImportConfirm">确认导入</el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="showImportDialog = false">完成</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- AI Follow-up Drawer -->
    <AiFollowUpDrawer
      v-model="showAiFollowUpDrawer"
      :customer="aiFollowUpCustomer"
      @saved="handleAiFollowUpSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadInstance, UploadFile } from 'element-plus'
import type { CustomerListVO, CustomerAddBO, CustomerImportPreview, CustomerImportRow, CustomerImportResult, CustomerLevel, CustomerStage } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import { getEnabledFieldsByEntity } from '@/api/customField'
import { transferCustomer, updateCustomerStage, exportCustomers, downloadImportTemplate, importCustomerPreview, confirmCustomerImport, aiParseCustomer } from '@/api/customer'
import type { CustomerAiParseVO } from '@/api/customer'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { queryUserList } from '@/api/auth'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'
import AiFollowUpDrawer from '@/components/customer/AiFollowUpDrawer.vue'

const router = useRouter()
const customerStore = useCustomerStore()
const { isMobile } = useResponsive()

const showAddDialog = ref(false)
const editingCustomer = ref<CustomerListVO | null>(null)
const submitting = ref(false)
const dynamicFieldFormRef = ref<InstanceType<typeof DynamicFieldForm>>()
const customFieldValues = ref<Record<string, any>>({})
const listCustomFields = ref<CustomField[]>([])
const statistics = ref<any>(null)

// Import/Export state
const exporting = ref(false)
const showImportDialog = ref(false)
const importStep = ref(1)
const importFile = ref<File | null>(null)
const importLoading = ref(false)
const importPreview = ref<CustomerImportPreview | null>(null)
const importResult = ref<CustomerImportResult | null>(null)
const globalDuplicateMode = ref('')
const importUploadRef = ref<UploadInstance>()

// AI Smart Input state
const aiInputText = ref('')
const aiParsing = ref(false)
const aiParseResult = ref<CustomerAiParseVO | null>(null)
const aiImageFile = ref<File | null>(null)
const aiImagePreview = ref<string | null>(null)

// AI Follow-up Drawer
const showAiFollowUpDrawer = ref(false)
const aiFollowUpCustomer = ref<CustomerListVO | null>(null)

function handleAiFollowUp(customer: CustomerListVO) {
  aiFollowUpCustomer.value = customer
  showAiFollowUpDrawer.value = true
}

function handleAiFollowUpSaved() {
  customerStore.fetchCustomerList(true)
}

// AI Smart Input handlers
function handleAiPaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return
  for (let i = 0; i < items.length; i++) {
    if (items[i].kind === 'file' && items[i].type.startsWith('image/')) {
      const file = items[i].getAsFile()
      if (file) {
        aiImageFile.value = file
        aiImagePreview.value = URL.createObjectURL(file)
      }
    }
  }
}

function removeAiImage() {
  if (aiImagePreview.value) {
    URL.revokeObjectURL(aiImagePreview.value)
  }
  aiImageFile.value = null
  aiImagePreview.value = null
}

async function handleAiExtract() {
  if (aiParsing.value) return
  if (!aiInputText.value.trim() && !aiImageFile.value) {
    ElMessage.warning('请输入文本或粘贴图片')
    return
  }

  aiParsing.value = true
  try {
    let imageObjectKey: string | undefined
    let imageMimeType: string | undefined

    // Upload image to MinIO if present
    if (aiImageFile.value) {
      const presigned = await getPresignedUploadUrl(aiImageFile.value.name, aiImageFile.value.type)
      await uploadToMinIO(aiImageFile.value, presigned.uploadUrl)
      imageObjectKey = presigned.objectKey
      imageMimeType = aiImageFile.value.type
    }

    const result = await aiParseCustomer({
      content: aiInputText.value.trim() || '请从图片中提取客户信息',
      imageObjectKey,
      imageMimeType
    })

    aiParseResult.value = result

    // Auto-fill form fields with extracted data
    if (result.companyName) formData.companyName = result.companyName
    if (result.industry) formData.industry = result.industry
    if (result.level && ['A', 'B', 'C'].includes(result.level)) formData.level = result.level as CustomerLevel
    if (result.stage && ['lead', 'qualified', 'proposal', 'negotiation', 'closed'].includes(result.stage)) formData.stage = result.stage as CustomerStage
    if (result.contactName) formData.contactName = result.contactName
    if (result.contactPhone) formData.contactPhone = result.contactPhone
    if (result.contactEmail) formData.contactEmail = result.contactEmail

    ElMessage.success('AI 提取完成，信息已自动填充')
  } catch (err: any) {
    ElMessage.error('AI 解析失败: ' + (err.message || '未知错误'))
  } finally {
    aiParsing.value = false
  }
}

// Owner transfer
const userList = ref<any[]>([])
const ownerSearch = ref('')
const userListLoaded = ref(false)

const filteredUserList = computed(() => {
  if (!ownerSearch.value) return userList.value
  const keyword = ownerSearch.value.toLowerCase()
  return userList.value.filter((u: any) =>
    u.realname?.toLowerCase().includes(keyword) || u.username?.toLowerCase().includes(keyword)
  )
})

const formData = reactive<CustomerAddBO>({
  companyName: '',
  industry: '',
  level: 'B',
  stage: 'lead',
  contactName: '',
  contactPhone: '',
  contactEmail: ''
})


// Pagination
const totalPages = computed(() => {
  return Math.ceil(customerStore.totalCount / (customerStore.queryParams.limit || 10))
})

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = customerStore.queryParams.page || 1
  const pages: number[] = []
  const maxVisible = 5
  let start = Math.max(1, current - Math.floor(maxVisible / 2))
  const end = Math.min(total, start + maxVisible - 1)
  start = Math.max(1, end - maxVisible + 1)
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// Computed
const closedCount = computed(() => {
  return customerStore.customerList.filter(c => c.stage === 'closed').length
})

const conversionRate = computed(() => {
  const total = customerStore.totalCount
  if (total === 0) return '0.0'
  return ((closedCount.value / total) * 100).toFixed(1)
})

const negotiationCount = computed(() => {
  return customerStore.customerList.filter(c => c.stage === 'negotiation').length
})

const overdueCount = computed(() => {
  const sevenDaysAgo = new Date()
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
  return customerStore.customerList.filter(c => {
    if (!c.lastContactTime) return true
    return new Date(c.lastContactTime) < sevenDaysAgo
  }).length
})

onMounted(async () => {
  try {
    const allFields = await getEnabledFieldsByEntity('customer')
    listCustomFields.value = allFields.filter(f => f.isShowInList)
  } catch {
    // Error handled by interceptor
  }

  try {
    await customerStore.fetchStatistics()
    statistics.value = customerStore.statistics
  } catch {
    // Statistics loading failed
  }

  await customerStore.fetchCustomerList(true)
})

function handleSearch() {
  customerStore.queryParams.page = 1
  customerStore.fetchCustomerList(true)
}

function handlePageChange(page: number) {
  if (page < 1 || page > totalPages.value) return
  if (customerStore.queryParams.page === page) return
  customerStore.queryParams.page = page
  customerStore.fetchCustomerList(false)
}

function handleRowClick(row: CustomerListVO) {
  router.push(`/customer/${row.customerId}`)
}

function handleEdit(row: CustomerListVO) {
  editingCustomer.value = row
  Object.assign(formData, {
    companyName: row.companyName,
    industry: row.industry || '',
    level: row.level || 'B',
    stage: row.stage || 'lead',
    contactName: row.primaryContactName || '',
    contactPhone: row.primaryContactPhone || '',
    contactEmail: ''
  })
  customFieldValues.value = row.customFields ? { ...row.customFields } : {}
  showAddDialog.value = true
}

async function handleSubmit() {
  if (!formData.companyName?.trim()) {
    ElMessage.warning('请输入公司名称')
    return
  }

  if (dynamicFieldFormRef.value) {
    const missingFields = dynamicFieldFormRef.value.getRequiredFieldLabels()
    if (missingFields.length > 0) {
      ElMessage.warning(`请填写必填字段: ${missingFields.join(', ')}`)
      return
    }
  }

  submitting.value = true
  try {
    const submitData = {
      ...formData,
      customFields: customFieldValues.value
    }
    if (editingCustomer.value) {
      await customerStore.editCustomer({
        ...submitData,
        customerId: editingCustomer.value.customerId
      })
      ElMessage.success('更新成功')
    } else {
      await customerStore.createCustomer(submitData)
      ElMessage.success('创建成功')
    }
    showAddDialog.value = false
    resetForm()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  editingCustomer.value = null
  Object.assign(formData, {
    companyName: '',
    industry: '',
    level: 'B',
    stage: 'lead',
    contactName: '',
    contactPhone: '',
    contactEmail: ''
  })
  customFieldValues.value = {}
  // Reset AI state
  aiInputText.value = ''
  aiParsing.value = false
  aiParseResult.value = null
  removeAiImage()
}

function getStageLabel(stage: string): string {
  const labels: Record<string, string> = {
    lead: '线索',
    qualified: '资格审查',
    proposal: '方案报价',
    negotiation: '谈判中',
    closed: '已成交',
    lost: '已流失'
  }
  return labels[stage] || stage
}

function getStageBadgeClass(stage: string): string {
  const classes: Record<string, string> = {
    lead: 'bg-slate-100 text-slate-800',
    qualified: 'bg-blue-100 text-blue-800',
    proposal: 'bg-amber-100 text-amber-800',
    negotiation: 'bg-purple-100 text-purple-800',
    closed: 'bg-green-100 text-green-800',
    lost: 'bg-red-100 text-red-800'
  }
  return classes[stage] || 'bg-slate-100 text-slate-800'
}

function getStageDotClass(stage: string): string {
  const classes: Record<string, string> = {
    lead: 'bg-slate-400',
    qualified: 'bg-blue-500',
    proposal: 'bg-amber-500',
    negotiation: 'bg-purple-500',
    closed: 'bg-green-500',
    lost: 'bg-red-500'
  }
  return classes[stage] || 'bg-slate-400'
}

const stageFlow = ['lead', 'qualified', 'proposal', 'negotiation', 'closed']
const allStageOptions = [
  { value: 'lead', label: '线索' },
  { value: 'qualified', label: '资格审查' },
  { value: 'proposal', label: '方案报价' },
  { value: 'negotiation', label: '谈判中' },
  { value: 'closed', label: '已成交' },
  { value: 'lost', label: '已流失' }
]

function getNextStage(current: string): string | null {
  const idx = stageFlow.indexOf(current)
  if (idx >= 0 && idx < stageFlow.length - 1) return stageFlow[idx + 1]
  return null
}

async function handleAdvanceStage(customerId: string, newStage: string) {
  try {
    await updateCustomerStage(customerId, newStage)
    await customerStore.fetchCustomerList(true)
    try { await customerStore.fetchStatistics(); statistics.value = customerStore.statistics } catch { /* ignore */ }
    ElMessage.success(`商机阶段已更新为「${allStageOptions.find(s => s.value === newStage)?.label || newStage}」`)
  } catch {
    // Error handled by interceptor
  }
}

function formatMoney(value: number | undefined): string {
  if (!value) return '¥0'
  if (value >= 10000) {
    return `¥${(value / 10000).toFixed(1)}万`
  }
  return `¥${value.toLocaleString()}`
}

function formatRelativeTime(dateStr: string | undefined): string {
  if (!dateStr) return '暂无'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60))

  if (diff < 0) return '刚刚'
  if (diff < 1) return '刚刚'
  if (diff < 24) return `${diff}小时前`
  const days = Math.floor(diff / 24)
  if (days < 30) return `${days}天前`
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

async function loadUserList() {
  if (userListLoaded.value) return
  try {
    const res = await queryUserList()
    userList.value = (res.list || []).filter((u: any) => u.status === 1)
    userListLoaded.value = true
  } catch {
    // Error handled by interceptor
  }
}

async function handleTransfer(customer: CustomerListVO, user: any) {
  if (String(user.userId) === String(customer.ownerId)) return
  try {
    await ElMessageBox.confirm(
      `确定将客户「${customer.companyName}」的负责人变更为「${user.realname}」吗？`,
      '变更负责人',
      { type: 'warning' }
    )
    await transferCustomer([customer.customerId], String(user.userId))
    ElMessage.success('负责人变更成功')
    await customerStore.fetchCustomerList(true)
  } catch {
    // Cancelled or error handled
  }
}

// ==================== Import / Export ====================

async function handleDownloadTemplate() {
  try {
    const blob = await downloadImportTemplate()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '客户导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch {
    // Error handled by interceptor
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportCustomers({
      keyword: customerStore.queryParams.keyword || undefined,
      stage: customerStore.queryParams.stage || undefined,
      level: customerStore.queryParams.level || undefined
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const today = new Date().toISOString().slice(0, 10)
    a.download = `客户数据_${today}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    // Error handled by interceptor
  } finally {
    exporting.value = false
  }
}

function handleImportFileChange(uploadFile: UploadFile) {
  importFile.value = uploadFile.raw || null
}

async function handleImportPreview() {
  if (!importFile.value) return
  importLoading.value = true
  try {
    importPreview.value = await importCustomerPreview(importFile.value)
    importStep.value = 2
    if (importPreview.value.rows) {
      importPreview.value.rows.forEach(row => {
        if (row.duplicate && !row.handleMode) {
          row.handleMode = 'skip'
        }
      })
    }
  } catch {
    // Error handled by interceptor
  } finally {
    importLoading.value = false
  }
}

function applyGlobalDuplicateMode(mode: string) {
  if (!importPreview.value) return
  importPreview.value.rows.forEach(row => {
    if (row.duplicate) {
      row.handleMode = mode as 'skip' | 'overwrite'
    }
  })
}

function importRowClassName({ row }: { row: CustomerImportRow }): string {
  if (row.errors && row.errors.length > 0) return 'bg-red-50'
  if (row.duplicate) return 'bg-yellow-50'
  return ''
}

async function handleImportConfirm() {
  if (!importPreview.value) return
  importLoading.value = true
  try {
    importResult.value = await confirmCustomerImport(importPreview.value.rows)
    importStep.value = 3
    await customerStore.fetchCustomerList(true)
    try { await customerStore.fetchStatistics(); statistics.value = customerStore.statistics } catch { /* ignore */ }
  } catch {
    // Error handled by interceptor
  } finally {
    importLoading.value = false
  }
}

function resetImport() {
  importStep.value = 1
  importFile.value = null
  importPreview.value = null
  importResult.value = null
  globalDuplicateMode.value = ''
  importUploadRef.value?.clearFiles()
}
</script>
