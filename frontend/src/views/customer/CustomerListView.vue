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
            placeholder="搜索公司名称、联系人、电话、标签..."
            class="pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm w-full sm:w-80 focus:ring-2 focus:ring-primary/20 focus:border-primary outline-none transition-all shadow-sm"
            @keydown.enter="handleSearch"
            @input="debouncedSearch"
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
        <div
          ref="tableCardRef"
          class="bg-white border border-slate-200 rounded-xl shadow-sm flex flex-col overflow-hidden"
          :style="tableCardStyle"
          v-loading="customerStore.loading"
        >
          <div class="flex-1 min-h-0 overflow-auto overscroll-none">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50 border-b border-slate-200">
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky left-0 top-0 z-40 bg-slate-50 shadow-[2px_0_5px_-2px_rgba(0,0,0,0.1)]">公司名称</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky top-0 z-30 bg-slate-50">客户级别</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky top-0 z-30 bg-slate-50">联系人</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky top-0 z-30 bg-slate-50">电话</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky top-0 z-30 bg-slate-50">行业</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky top-0 z-30 bg-slate-50">商机阶段</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky top-0 z-30 bg-slate-50">报价金额</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky top-0 z-30 bg-slate-50">最后跟进</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky top-0 z-30 bg-slate-50">负责人</th>
                  <th v-for="field in listCustomFields" :key="field.fieldId" class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap sticky top-0 z-30 bg-slate-50">
                    {{ field.fieldLabel }}
                  </th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider whitespace-nowrap text-right sticky right-0 top-0 z-40 bg-slate-50 shadow-[-2px_0_5px_-2px_rgba(0,0,0,0.1)]">操作</th>
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
                  <td class="px-6 py-4 whitespace-nowrap sticky left-0 z-10 bg-white group-hover:bg-blue-50 transition-colors shadow-[2px_0_5px_-2px_rgba(0,0,0,0.1)]">
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
                  <td class="px-6 py-4 text-right whitespace-nowrap sticky right-0 z-10 bg-white group-hover:bg-blue-50 transition-colors shadow-[-2px_0_5px_-2px_rgba(0,0,0,0.1)]" @click.stop>
                    <button
                      class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-primary/10 text-primary text-xs font-bold rounded-lg hover:bg-primary/20 transition-colors"
                      @click="handleAiFollowUp(customer)"
                    >
                      <span class="material-symbols-outlined text-sm">smart_toy</span>
                      AI 跟进
                    </button>
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
          <div v-if="customerStore.totalCount > 0" class="shrink-0 px-6 py-4 bg-slate-50/50 flex items-center justify-between border-t border-slate-200">
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
      <div
        v-if="!isMobile"
        :class="[
          'w-full xl:shrink-0 overflow-hidden transition-[width] duration-300 ease-out',
          isAiSidebarExpanded ? 'xl:w-80' : 'xl:w-12'
        ]"
      >
        <div
          :class="[
            'w-full xl:w-auto rounded-2xl xl:rounded-none p-1 xl:p-0 border border-transparent xl:border-none',
            isAiSidebarExpanded ? 'bg-slate-50/50 xl:bg-transparent' : 'bg-transparent'
          ]"
        >
          <div class="flex items-center justify-between px-1 mb-2">
            <div class="flex items-center gap-2 min-w-0">
              <h3 v-if="isAiSidebarExpanded" class="text-[10px] font-bold text-slate-400 uppercase tracking-widest truncate">
                AI 智能洞察预警
              </h3>
              <span v-if="isAiSidebarExpanded" class="size-2 rounded-full bg-primary animate-pulse"></span>
            </div>

            <button
              type="button"
              class="size-8 flex items-center justify-center rounded-lg hover:bg-slate-200/50 text-slate-400 hover:text-slate-600 transition-colors"
              :title="isAiSidebarExpanded ? '收起面板' : '展开面板'"
              @click="isAiSidebarExpanded = !isAiSidebarExpanded"
            >
              <span class="material-symbols-outlined text-xl">
                {{ isAiSidebarExpanded ? 'last_page' : 'first_page' }}
              </span>
            </button>
          </div>

          <Transition
            enter-active-class="transition-all duration-200 ease-out"
            enter-from-class="opacity-0 -translate-y-1"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition-all duration-150 ease-in"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 -translate-y-1"
          >
            <div v-if="isAiSidebarExpanded" class="space-y-4">
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
          </Transition>

          <div v-if="!isAiSidebarExpanded" class="hidden xl:flex flex-col items-center gap-6 pt-4">
            <span class="material-symbols-outlined text-primary/40 text-xl animate-pulse">auto_awesome</span>
            <div class="h-20 w-px bg-slate-200"></div>
            <span class="[writing-mode:vertical-rl] text-[10px] font-bold text-slate-400 tracking-widest">
              <span class="[text-combine-upright:all]">AI</span> 智能洞察
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <CustomerUpsertDialog
      v-model="showAddDialog"
      :mode="editingCustomer ? 'edit' : 'create'"
      :customer="editingCustomer"
      @success="handleUpsertSuccess"
    />

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
import { ref, computed, nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCustomerStore } from '@/stores/customer'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadInstance, UploadFile } from 'element-plus'
import type { CustomerListVO, CustomerImportPreview, CustomerImportRow, CustomerImportResult } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import { getEnabledFieldsByEntity } from '@/api/customField'
import { transferCustomer, exportCustomers, downloadImportTemplate, importCustomerPreview, confirmCustomerImport } from '@/api/customer'
import { queryUserList } from '@/api/auth'
import AiFollowUpDrawer from '@/components/customer/AiFollowUpDrawer.vue'
import CustomerUpsertDialog from '@/views/customer/components/CustomerUpsertDialog.vue'

const router = useRouter()
const route = useRoute()
const customerStore = useCustomerStore()
const { isMobile } = useResponsive()
const tableCardRef = ref<HTMLElement | null>(null)
const tableCardMaxHeight = ref<number | null>(null)

// UI only: AI sidebar expand/collapse (persisted)
const AI_SIDEBAR_STORAGE_KEY = 'wk_ai_crm:customer_ai_sidebar_expanded:v1'
const isAiSidebarExpanded = ref(true)

function loadAiSidebarExpanded() {
  try {
    const raw = localStorage.getItem(AI_SIDEBAR_STORAGE_KEY)
    if (raw === null) return
    isAiSidebarExpanded.value = raw === '1'
  } catch {
    // ignore
  }
}

watch(isAiSidebarExpanded, (val) => {
  try {
    localStorage.setItem(AI_SIDEBAR_STORAGE_KEY, val ? '1' : '0')
  } catch {
    // ignore
  }
})

const showAddDialog = ref(false)
const editingCustomer = ref<CustomerListVO | null>(null)
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

function handleUpsertSuccess(payload: { mode: 'create' | 'edit'; customerId?: string }) {
  // keep original behavior: refresh list after submit
  customerStore.fetchCustomerList(true)
  if (payload.mode === 'edit') {
    editingCustomer.value = null
  }
}

// AI Follow-up Drawer
const showAiFollowUpDrawer = ref(false)
const aiFollowUpCustomer = ref<CustomerListVO | null>(null)

const tableCardStyle = computed(() => {
  if (!tableCardMaxHeight.value) return {}
  return { maxHeight: `${tableCardMaxHeight.value}px` }
})

function updateTableCardMaxHeight() {
  const tableCardEl = tableCardRef.value
  if (!tableCardEl) return

  const cardRect = tableCardEl.getBoundingClientRect()
  const scrollContainer = tableCardEl.closest('main')
  const viewportBottom = scrollContainer instanceof HTMLElement
    ? scrollContainer.getBoundingClientRect().bottom
    : window.innerHeight
  const nextHeight = Math.floor(viewportBottom - cardRect.top - 24)

  if (nextHeight > 0) {
    tableCardMaxHeight.value = nextHeight
  }
}

function handleTableViewportResize() {
  updateTableCardMaxHeight()
}

function handleAiFollowUp(customer: CustomerListVO) {
  aiFollowUpCustomer.value = customer
  showAiFollowUpDrawer.value = true
}

function handleAiFollowUpSaved() {
  customerStore.fetchCustomerList(true)
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
  loadAiSidebarExpanded()
  await nextTick()
  updateTableCardMaxHeight()
  window.addEventListener('resize', handleTableViewportResize)

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
  await nextTick()
  updateTableCardMaxHeight()

  if (route.query.action === 'create') {
    showAddDialog.value = true
    router.replace({ path: route.path, query: {} })
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleTableViewportResize)
  if (searchTimer) clearTimeout(searchTimer)
})

function handleSearch() {
  customerStore.queryParams.page = 1
  customerStore.fetchCustomerList(true)
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
function debouncedSearch() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => handleSearch(), 500)
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
